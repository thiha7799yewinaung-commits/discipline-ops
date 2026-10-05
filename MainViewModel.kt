package com.disciplinequest.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.disciplinequest.app.ai.AiPlanDraft
import com.disciplinequest.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val tasksDao = db.taskDao()
    private val missionDao = db.missionDao()
    private val habitDao = db.habitDao()
    private val playerDao = db.playerDao()
    private val shopDao = db.shopDao()
    private val achievementDao = db.achievementDao()
    private val dailyDao = db.dailyDao()

    val tasks = tasksDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val missions = missionDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val habits = habitDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val player = playerDao.observe().filterNotNull().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Player())
    val shop = shopDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val achievements = achievementDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val daily = dailyDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (playerDao.observe().firstOrNull() == null) playerDao.save(Player())
            if (shop.firstOrNull().isNullOrEmpty()) shopDao.insertAll(AppDefaults.shop)
            if (achievements.firstOrNull().isNullOrEmpty()) achievementDao.insertAll(AppDefaults.achievements)
            refreshFailedOverdue()
        }
    }

    private suspend fun refreshFailedOverdue() {
        val today = LocalDate.now().toString()
        tasks.value.filter { it.status == TaskStatus.PENDING && it.deadline < today }.forEach { fail(it) }
    }

    fun addTask(title: String, description: String, deadline: String, difficulty: Difficulty, type: TaskType, missionId: Long? = null) {
        if (title.isBlank()) return
        val xp = when (difficulty) { Difficulty.EASY -> 10; Difficulty.MEDIUM -> 20; Difficulty.HARD -> 50; Difficulty.CRITICAL -> 75 }
        val hp = when (difficulty) { Difficulty.EASY -> 5; Difficulty.MEDIUM -> 10; Difficulty.HARD -> 20; Difficulty.CRITICAL -> 30 }
        viewModelScope.launch {
            tasksDao.insert(Task(title=title.trim(),description=description,deadline=deadline.ifBlank{LocalDate.now().toString()},
                difficulty=difficulty,type=type,xp=xp,coins=xp/2,hpPenalty=hp,missionId=missionId))
        }
    }

    fun addHabit(name: String, frequency: String, difficulty: Difficulty) {
        if (name.isBlank()) return
        val xp = when (difficulty) { Difficulty.EASY -> 10; Difficulty.MEDIUM -> 20; Difficulty.HARD -> 50; Difficulty.CRITICAL -> 75 }
        val hp = when (difficulty) { Difficulty.EASY -> 5; Difficulty.MEDIUM -> 10; Difficulty.HARD -> 20; Difficulty.CRITICAL -> 30 }
        viewModelScope.launch { habitDao.insert(Habit(name=name.trim(),frequency=frequency,xp=xp,coins=xp/2,hpPenalty=hp)) }
    }

    fun addMission(title: String, goal: String, start: String, deadline: String, target: Int, xp: Int, coins: Int) {
        if (title.isBlank()) return
        viewModelScope.launch { missionDao.insert(Mission(title=title.trim(),goal=goal,startDate=start,deadline=deadline,targetCount=target,rewardXp=xp,rewardCoins=coins)) }
    }

    fun complete(task: Task) {
        if (task.status != TaskStatus.PENDING) return
        viewModelScope.launch {
            tasksDao.update(task.copy(status=TaskStatus.COMPLETED))
            val p = player.value
            val newXp = p.xp + task.xp
            val newLevel = 1 + newXp / 100
            val newCoins = p.coins + task.coins
            val streak = if (p.lastActiveDate == LocalDate.now().minusDays(1).toString()) p.overallStreak + 1 else 1
            val np = p.copy(xp=newXp,level=newLevel,coins=newCoins,completed=p.completed+1,
                overallStreak=streak,bestStreak=maxOf(p.bestStreak,streak),lastActiveDate=LocalDate.now().toString())
            playerDao.save(recalc(np))
            saveDaily(task.deadline, xp=task.xp, coins=task.coins, completedDelta=1)
            checkAchievements()
            updateMission(task.missionId)
        }
    }

    fun fail(task: Task) {
        if (task.status != TaskStatus.PENDING) return
        viewModelScope.launch {
            tasksDao.update(task.copy(status=TaskStatus.FAILED))
            val p=player.value
            val np=p.copy(xp=maxOf(0,p.xp-task.hpPenalty),hp=maxOf(0,p.hp-task.hpPenalty),
                failed=p.failed+1,overallStreak=0,disciplineScore=0)
            playerDao.save(recalc(np))
            saveDaily(task.deadline,hp=np.hp,failedDelta=1)
            checkAchievements()
        }
    }

    fun recover(task: Task) {
        if (task.status != TaskStatus.FAILED) return
        viewModelScope.launch {
            tasksDao.update(task.copy(status=TaskStatus.RECOVERED))
            val p=player.value
            val np=p.copy(hp=minOf(100,p.hp+task.hpPenalty),recovered=p.recovered+1)
            playerDao.save(recalc(np))
            saveDaily(task.deadline,hp=np.hp,recoveredDelta=1)
            checkAchievements()
        }
    }


    fun completeHabit(habit: Habit) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existing = tasks.value.firstOrNull { it.habitKey == "habit_${habit.id}_$today" && it.status == TaskStatus.COMPLETED }
            if (existing != null) return@launch
            val streak = habit.currentStreak + 1
            habitDao.update(habit.copy(currentStreak=streak,bestStreak=maxOf(habit.bestStreak,streak),completionCount=habit.completionCount+1))
            val p=player.value
            val newXp=p.xp+habit.xp
            val newStreak=if(p.lastActiveDate==LocalDate.now().minusDays(1).toString()) p.overallStreak+1 else 1
            playerDao.save(recalc(p.copy(xp=newXp,level=1+newXp/100,coins=p.coins+habit.coins,completed=p.completed+1,
                overallStreak=newStreak,bestStreak=maxOf(p.bestStreak,newStreak),lastActiveDate=today)))
            tasksDao.insert(Task(title=habit.name,description="Habit completion",type=TaskType.HABIT,category="Habit",
                difficulty=Difficulty.EASY,deadline=today,xp=habit.xp,coins=habit.coins,hpPenalty=habit.hpPenalty,
                status=TaskStatus.COMPLETED,habitKey="habit_${habit.id}_$today"))
            saveDaily(today,hp=player.value.hp,completedDelta=1,xp=habit.xp,coins=habit.coins)
            checkAchievements()
        }
    }

    fun updateHabit(habit: Habit, name: String, frequency: String) {
        viewModelScope.launch { habitDao.update(habit.copy(name=name,frequency=frequency)) }
    }

    fun deleteTask(t: Task) = viewModelScope.launch { tasksDao.delete(t) }
    fun deleteMission(m: Mission) = viewModelScope.launch { missionDao.delete(m) }
    fun deleteHabit(h: Habit) = viewModelScope.launch { habitDao.delete(h) }

    fun purchase(item: ShopItem) {
        viewModelScope.launch {
            val p=player.value
            if (!item.owned && p.coins >= item.price) {
                playerDao.save(p.copy(coins=p.coins-item.price))
                shopDao.update(item.copy(owned=true))
            }
        }
    }
    fun equip(item: ShopItem) {
        viewModelScope.launch {
            if (!item.owned) return@launch
            shopDao.unequipCategory(item.category)
            shopDao.update(item.copy(equipped=true))
        }
    }

    fun editTask(task: Task, title: String, desc: String, date: String, difficulty: Difficulty) {
        val xp=when(difficulty){Difficulty.EASY->10;Difficulty.MEDIUM->20;Difficulty.HARD->50;Difficulty.CRITICAL->75}
        val hp=when(difficulty){Difficulty.EASY->5;Difficulty.MEDIUM->10;Difficulty.HARD->20;Difficulty.CRITICAL->30}
        viewModelScope.launch { tasksDao.update(task.copy(title=title,description=desc,deadline=date,difficulty=difficulty,xp=xp,coins=xp/2,hpPenalty=hp)) }
    }

    fun acceptAiPlan(plan: AiPlanDraft) {
        viewModelScope.launch {
            val start=LocalDate.now()
            val deadline=start.plusDays((plan.tasks.size-1).coerceAtLeast(0).toLong()).toString()
            val missionId=missionDao.insert(Mission(title=plan.missionTitle,goal=plan.goal,startDate=start.toString(),deadline=deadline,
                targetCount=plan.tasks.size,rewardXp=plan.tasks.sumOf{it.xp},rewardCoins=plan.tasks.sumOf{it.coins}))
            plan.tasks.forEach { t ->
                val d=runCatching{Difficulty.valueOf(t.difficulty.uppercase())}.getOrDefault(Difficulty.MEDIUM)
                tasksDao.insert(Task(title=t.title,description=t.description,deadline=t.deadline, difficulty=d,xp=t.xp,coins=t.coins,hpPenalty=t.hpPenalty,missionId=missionId))
            }
        }
    }

    private suspend fun updateMission(id: Long?) {
        if (id == null) return
        val m=missions.value.firstOrNull{it.id==id} ?: return
        val count=tasks.value.count{it.missionId==id && it.status==TaskStatus.COMPLETED}+1
        if (!m.completed && count >= m.targetCount) {
            missionDao.update(m.copy(completed=true))
            val p=player.value
            playerDao.save(recalc(p.copy(xp=p.xp+m.rewardXp,coins=p.coins+m.rewardCoins)))
        }
    }

    private suspend fun saveDaily(date:String,hp:Int?=null,completedDelta:Int=0,failedDelta:Int=0,recoveredDelta:Int=0,xp:Int=0,coins:Int=0) {
        val old=daily.value.firstOrNull{it.date==date} ?: DailyRecord(date=date)
        dailyDao.save(old.copy(completed=old.completed+completedDelta,failed=old.failed+failedDelta,recovered=old.recovered+recoveredDelta,
            xpEarned=old.xpEarned+xp,coinsEarned=old.coinsEarned+coins,hpAtEnd=hp?:player.value.hp))
    }

    private fun recalc(p:Player):Player {
        val total=p.completed+p.failed
        val completion=if(total==0)0.0 else p.completed.toDouble()/total
        val consistency=minOf(1.0,p.overallStreak/30.0)
        val recovery=minOf(1.0,p.recovered/10.0)
        val score=(completion*60+consistency*30+recovery*10).coerceIn(0.0,100.0).toInt()
        return p.copy(disciplineScore=score)
    }

    private suspend fun checkAchievements() {
        val p=player.value
        val a=achievements.value
        fun unlock(id:String): Achievement?=a.firstOrNull{it.id==id && !it.unlocked}
        listOfNotNull(
            unlock("first")?.takeIf{p.completed>=1},
            unlock("streak7")?.takeIf{p.overallStreak>=7},
            unlock("streak30")?.takeIf{p.overallStreak>=30},
            unlock("tasks100")?.takeIf{p.completed>=100},
            unlock("comeback")?.takeIf{p.recovered>=1},
            unlock("nomiss")?.takeIf{p.completed>=20 && p.failed==0},
            unlock("master")?.takeIf{p.disciplineScore>=100}
        ).forEach { achievementDao.update(it.copy(unlocked=true,unlockedAt=System.currentTimeMillis())) }
    }


    fun exportJson(): String {
        fun esc(x:String)=x.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")
        val taskJson=tasks.value.joinToString(",") { """{"id":${it.id},"title":"${esc(it.title)}","description":"${esc(it.description)}","type":"${it.type}","category":"${esc(it.category)}","difficulty":"${it.difficulty}","deadline":"${it.deadline}","xp":${it.xp},"coins":${it.coins},"hpPenalty":${it.hpPenalty},"status":"${it.status}","missionId":${it.missionId ?: "null"},"habitKey":${it.habitKey?.let{"k" -> "\"${esc(k)}\""} ?: "null"}}""" }
        val missionJson=missions.value.joinToString(",") { """{"id":${it.id},"title":"${esc(it.title)}","goal":"${esc(it.goal)}","startDate":"${it.startDate}","deadline":"${it.deadline}","targetCount":${it.targetCount},"rewardXp":${it.rewardXp},"rewardCoins":${it.rewardCoins},"completed":${it.completed}}""" }
        val habitJson=habits.value.joinToString(",") { """{"id":${it.id},"name":"${esc(it.name)}","frequency":"${it.frequency}","xp":${it.xp},"coins":${it.coins},"hpPenalty":${it.hpPenalty},"currentStreak":${it.currentStreak},"bestStreak":${it.bestStreak},"completionCount":${it.completionCount},"active":${it.active}}""" }
        return """{"version":1,"exportedAt":${System.currentTimeMillis()},"player":${player.value.let{"""{"xp":${it.xp},"level":${it.level},"hp":${it.hp},"coins":${it.coins},"overallStreak":${it.overallStreak},"bestStreak":${it.bestStreak},"completed":${it.completed},"failed":${it.failed},"recovered":${it.recovered},"disciplineScore":${it.disciplineScore}}"""},"tasks":[$taskJson],"missions":[$missionJson],"habits":[$habitJson]}"""
    }

    fun importJson(json:String) {
        // Import is intentionally conservative: only trusted app exports are accepted.
        viewModelScope.launch {
            try {
                val o=org.json.JSONObject(json)
                val po=o.optJSONObject("player")
                if(po!=null) playerDao.save(Player(
                    xp=po.optInt("xp"),level=po.optInt("level",1),hp=po.optInt("hp",100),coins=po.optInt("coins"),
                    overallStreak=po.optInt("overallStreak"),bestStreak=po.optInt("bestStreak"),
                    completed=po.optInt("completed"),failed=po.optInt("failed"),recovered=po.optInt("recovered"),
                    disciplineScore=po.optInt("disciplineScore")
                ))
                tasksDao.clear(); missionDao.clear(); habitDao.clear()
                val ta=o.optJSONArray("tasks")
                if(ta!=null) for(i in 0 until ta.length()) {
                    val x=ta.getJSONObject(i)
                    tasksDao.insert(Task(
                        id=0,title=x.optString("title"),description=x.optString("description"),
                        type=runCatching{TaskType.valueOf(x.optString("type"))}.getOrDefault(TaskType.ONE_TIME),
                        category=x.optString("category","General"),
                        difficulty=runCatching{Difficulty.valueOf(x.optString("difficulty"))}.getOrDefault(Difficulty.MEDIUM),
                        deadline=x.optString("deadline"),xp=x.optInt("xp",20),coins=x.optInt("coins",10),hpPenalty=x.optInt("hpPenalty",10),
                        status=runCatching{TaskStatus.valueOf(x.optString("status"))}.getOrDefault(TaskStatus.PENDING),
                        missionId=if(x.isNull("missionId")) null else x.optLong("missionId"),
                        habitKey=if(x.isNull("habitKey")) null else x.optString("habitKey")
                    ))
                }
                val ma=o.optJSONArray("missions")
                if(ma!=null) for(i in 0 until ma.length()) { val x=ma.getJSONObject(i); missionDao.insert(Mission(
                    title=x.optString("title"),goal=x.optString("goal"),startDate=x.optString("startDate"),deadline=x.optString("deadline"),
                    targetCount=x.optInt("targetCount",30),rewardXp=x.optInt("rewardXp",200),rewardCoins=x.optInt("rewardCoins",100),completed=x.optBoolean("completed")
                )) }
                val ha=o.optJSONArray("habits")
                if(ha!=null) for(i in 0 until ha.length()) { val x=ha.getJSONObject(i); habitDao.insert(Habit(
                    name=x.optString("name"),frequency=x.optString("frequency","DAILY"),xp=x.optInt("xp",10),coins=x.optInt("coins",5),
                    hpPenalty=x.optInt("hpPenalty",5),currentStreak=x.optInt("currentStreak"),bestStreak=x.optInt("bestStreak"),
                    completionCount=x.optInt("completionCount"),active=x.optBoolean("active",true)
                )) }
            } catch (_:Exception) {}
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            tasksDao.clear(); missionDao.clear(); habitDao.clear(); dailyDao.clear()
            playerDao.save(Player())
            shopDao.insertAll(AppDefaults.shop)
            achievementDao.insertAll(AppDefaults.achievements)
        }
    }
}
