package com.disciplinequest.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.disciplinequest.app.ai.*
import com.disciplinequest.app.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

private val BG=Color(0xFF050505)
private val PANEL=Color(0xFF111111)
private val PANEL2=Color(0xFF181818)
private val RED=Color(0xFFFF1A1A)
private val WHITE=Color(0xFFF5F5F5)
private val MUTED=Color(0xFF9A9A9A)
private val GREEN=Color(0xFF43D17A)
private val GOLD=Color(0xFFFFC107)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            DisciplineTheme { App(vm) }
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7001)
        }
    }
}

@Composable
fun DisciplineTheme(content:@Composable()->Unit) {
    MaterialTheme(
        colorScheme=darkColorScheme(
            primary=RED,secondary=RED,background=BG,surface=PANEL,
            onBackground=WHITE,onSurface=WHITE,onPrimary=Color.White
        ),
        content=content
    )
}

enum class Screen { HOME,TASKS,CALENDAR,HABITS,PROGRESS,MISSIONS,CHARACTER,SHOP,AI,ACHIEVEMENTS,SETTINGS }

@Composable
fun App(vm:MainViewModel) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val p by vm.player.collectAsState()
    val prefs=remember { Prefs(androidx.compose.ui.platform.LocalContext.current) }
    val labels = if(prefs.language=="my") listOf("ပင်မ","အလုပ်","တိုးတက်မှု","ပို") else listOf("Home","Tasks","Progress","More")

    Scaffold(
        containerColor=BG,
        bottomBar={
            NavigationBar(containerColor=Color(0xFF090909)) {
                listOf(Screen.HOME,Screen.TASKS,Screen.PROGRESS,Screen.CHARACTER).forEachIndexed { i,s ->
                    NavigationBarItem(
                        selected=screen==s,
                        onClick={screen=s},
                        icon={Icon(when(s){
                            Screen.HOME->Icons.Default.Home
                            Screen.TASKS->Icons.Default.CheckCircle
                            Screen.PROGRESS->Icons.Default.ShowChart
                            else->Icons.Default.Person
                        },null)},
                        label={Text(labels[i])},
                        colors=NavigationBarItemDefaults.colors(
                            selectedIconColor=RED,selectedTextColor=RED,indicatorColor=Color.Transparent,
                            unselectedIconColor=MUTED,unselectedTextColor=MUTED
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when(screen) {
                Screen.HOME->HomeScreen(vm)
                Screen.TASKS->TasksScreen(vm)
                Screen.CALENDAR->CalendarScreen(vm)
                Screen.HABITS->HabitsScreen(vm)
                Screen.PROGRESS->ProgressScreen(vm)
                Screen.MISSIONS->MissionsScreen(vm)
                Screen.CHARACTER->CharacterScreen(vm,onNavigate={screen=it})
                Screen.SHOP->ShopScreen(vm)
                Screen.AI->AiScreen(vm,prefs)
                Screen.ACHIEVEMENTS->AchievementsScreen(vm)
                Screen.SETTINGS->SettingsScreen(vm,prefs)
            }
        }
    }
}

@Composable
fun Header(title:String, subtitle:String?=null, action:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
            if(subtitle!=null) Text(subtitle,color=MUTED,style=MaterialTheme.typography.bodySmall)
        }
        if(action!=null) IconButton(action) { Icon(Icons.Default.Add,null,tint=RED) }
    }
}

@Composable
fun PlayerCard(p:Player) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(PANEL)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(92.dp).background(PANEL2,RoundedCornerShape(18.dp)),contentAlignment=Alignment.Center) {
                    Icon(Icons.Default.Person,null,tint=RED,modifier=Modifier.size(58.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("LEVEL ${p.level}",color=RED,fontWeight=FontWeight.Black)
                    Text(rank(p.level),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
                    Text("STREAK ${p.overallStreak} DAYS",color=MUTED)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("HP  ${p.hp}/100",fontWeight=FontWeight.Bold)
            LinearProgressIndicator({p.hp/100f},Modifier.fillMaxWidth().height(8.dp),color=RED,trackColor=Color(0xFF292929))
            Spacer(Modifier.height(8.dp))
            Text("XP  ${p.xp % 100}/100  •  TOTAL ${p.xp}",fontWeight=FontWeight.Bold)
            LinearProgressIndicator({(p.xp%100)/100f},Modifier.fillMaxWidth().height(8.dp),color=GOLD,trackColor=Color(0xFF292929))
        }
    }
}
fun rank(level:Int)=when {
    level>=50->"MASTER"; level>=30->"ELITE"; level>=20->"WARRIOR"; level>=10->"DISCIPLINED"; level>=5->"BEGINNER"; else->"ROOKIE"
}

@Composable
fun StatCard(name:String,value:String,modifier:Modifier=Modifier) {
    Card(modifier,colors=CardDefaults.cardColors(PANEL)) {
        Column(Modifier.padding(12.dp)) { Text(name,color=MUTED,style=MaterialTheme.typography.labelSmall); Text(value,fontWeight=FontWeight.Black) }
    }
}

@Composable
fun HomeScreen(vm:MainViewModel) {
    val p by vm.player.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val missions by vm.missions.collectAsState()
    val today=LocalDate.now().toString()
    val todayTasks=tasks.filter{it.deadline==today && it.type==TaskType.ONE_TIME}
    val done=todayTasks.count{it.status==TaskStatus.COMPLETED}
    val percent=if(todayTasks.isEmpty()) 0 else done*100/todayTasks.size
    LazyColumn(Modifier.fillMaxSize().background(BG),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Text("DISCIPLINE QUEST",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
            Text("LEVEL UP YOUR LIFE",color=RED,fontWeight=FontWeight.Bold)
        }
        item{PlayerCard(p)}
        item{
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                StatCard("TODAY","$percent%",Modifier.weight(1f))
                StatCard("COINS","${p.coins}",Modifier.weight(1f))
                StatCard("SCORE","${p.disciplineScore}",Modifier.weight(1f))
            }
        }
        item{
            Card(colors=CardDefaults.cardColors(PANEL)) {
                Column(Modifier.padding(16.dp)) {
                    Text("TODAY'S MISSION",color=MUTED,fontWeight=FontWeight.Bold)
                    Text(missions.firstOrNull{!it.completed}?.title ?: "No active mission",fontWeight=FontWeight.Bold)
                    Text(missions.firstOrNull{!it.completed}?.goal ?: "Create a mission or ask AI Coach.",color=MUTED)
                }
            }
        }
        item{Text("TODAY'S TASKS",fontWeight=FontWeight.Bold)}
        if(todayTasks.isEmpty()) item{EmptyCard("No tasks for today. Add one from Tasks.")}
        items(todayTasks,key={it.id}){TaskRow(it,vm)}
    }
}

@Composable
fun EmptyCard(text:String) {
    Card(colors=CardDefaults.cardColors(PANEL)) { Text(text,Modifier.padding(18.dp),color=MUTED) }
}

@Composable
fun TasksScreen(vm:MainViewModel) {
    val tasks by vm.tasks.collectAsState()
    var add by remember{mutableStateOf(false)}
    var editing by remember{mutableStateOf<Task?>(null)}
    Scaffold(containerColor=BG,floatingActionButton={FloatingActionButton({add=true},containerColor=RED){Icon(Icons.Default.Add,null)}}){pad->
        LazyColumn(Modifier.padding(pad).fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            item{Header("TASKS","One-time tasks and habits")}
            if(tasks.isEmpty()) item{EmptyCard("No tasks yet.")}
            items(tasks,key={it.id}){t->TaskRow(t,vm,{editing=t})}
        }
    }
    if(add) TaskDialog(vm,null){add=false}
    if(editing!=null) TaskDialog(vm,editing){editing=null}
}

@Composable
fun TaskRow(t:Task,vm:MainViewModel,onEdit:(()->Unit)?=null) {
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(PANEL)) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Checkbox(
                checked=t.status==TaskStatus.COMPLETED||t.status==TaskStatus.RECOVERED,
                onCheckedChange={if(it)vm.complete(t)},
                colors=CheckboxDefaults.colors(checkedColor=RED)
            )
            Column(Modifier.weight(1f)) {
                Text(t.title,fontWeight=FontWeight.Bold)
                Text("${t.deadline} • ${t.difficulty} • ${t.status}",color=if(t.status==TaskStatus.FAILED)RED else MUTED,style=MaterialTheme.typography.bodySmall)
                Text("+${t.xp} XP  +${t.coins} C  -${t.hpPenalty} HP",color=MUTED,style=MaterialTheme.typography.bodySmall)
            }
            when(t.status){
                TaskStatus.PENDING->IconButton({vm.fail(t)}){Icon(Icons.Default.Warning,null,tint=RED)}
                TaskStatus.FAILED->IconButton({vm.recover(t)}){Icon(Icons.Default.Restore,null,tint=GREEN)}
                else->Icon(Icons.Default.CheckCircle,null,tint=GREEN)
            }
            if(onEdit!=null)IconButton(onEdit){Icon(Icons.Default.Edit,null,tint=MUTED)}
            IconButton({vm.deleteTask(t)}){Icon(Icons.Default.Delete,null,tint=MUTED)}
        }
    }
}

@Composable
fun TaskDialog(vm:MainViewModel,existing:Task?,close:()->Unit) {
    var title by remember{mutableStateOf(existing?.title?:"")}
    var desc by remember{mutableStateOf(existing?.description?:"")}
    var date by remember{mutableStateOf(existing?.deadline?:LocalDate.now().toString())}
    var diff by remember{mutableStateOf(existing?.difficulty?:Difficulty.MEDIUM)}
    var type by remember{mutableStateOf(existing?.type?:TaskType.ONE_TIME)}
    AlertDialog(onDismissRequest=close,title={Text(if(existing==null)"NEW TASK" else "EDIT TASK")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(title,{title=it},label={Text("Title")},singleLine=true)
            OutlinedTextField(desc,{desc=it},label={Text("Description")})
            OutlinedTextField(date,{date=it},label={Text("Deadline YYYY-MM-DD")},singleLine=true)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){Difficulty.values().forEach{d->FilterChip(diff==d,{diff=d},label={Text(d.name.take(1))})}}
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){FilterChip(type==TaskType.ONE_TIME,{type=TaskType.ONE_TIME},label={Text("One-time")});FilterChip(type==TaskType.HABIT,{type=TaskType.HABIT},label={Text("Habit")})}
        }
    },confirmButton={Button({if(existing==null)vm.addTask(title,desc,date,diff,type)else vm.editTask(existing,title,desc,date,diff);close()},colors=ButtonDefaults.buttonColors(RED)){Text("SAVE")}},dismissButton={TextButton(close){Text("CANCEL")}})
}

@Composable
fun CalendarScreen(vm:MainViewModel) {
    val tasks by vm.tasks.collectAsState()
    var month by remember{mutableStateOf(YearMonth.now())}
    var selected by remember{mutableStateOf(LocalDate.now())}
    val first=month.atDay(1)
    val days=first.dayOfWeek.value%7
    val cells=(0 until days).map{null}+(1..month.lengthOfMonth()).map{month.atDay(it)}
    LazyColumn(Modifier.fillMaxSize().background(BG),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{
            Row(verticalAlignment=Alignment.CenterVertically){
                IconButton({month=month.minusMonths(1)}){Icon(Icons.Default.ChevronLeft,null)}
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),Modifier.weight(1f),fontWeight=FontWeight.Black)
                IconButton({month=month.plusMonths(1)}){Icon(Icons.Default.ChevronRight,null)}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround){listOf("S","M","T","W","T","F","S").forEach{Text(it,color=MUTED)}} 
        }
        item{
            LazyVerticalGrid(columns=GridCells.Fixed(7),modifier=Modifier.height(310.dp),userScrollEnabled=false){
                items(cells){d->
                    if(d==null)Box(Modifier.padding(3.dp).aspectRatio(1f))
                    else{
                        val count=tasks.count{it.deadline==d.toString()}
                        val isSelected=d==selected
                        Box(Modifier.padding(3.dp).aspectRatio(1f).background(if(isSelected)RED else PANEL,RoundedCornerShape(10.dp)).clickable{selected=d},contentAlignment=Alignment.Center){
                            Column(horizontalAlignment=Alignment.CenterHorizontally){
                                Text("${d.dayOfMonth}",fontWeight=FontWeight.Bold)
                                if(count>0)Text("•",color=if(tasks.any{it.deadline==d.toString()&&it.status==TaskStatus.FAILED})RED else GREEN)
                            }
                        }
                    }
                }
            }
        }
        item{Text("SELECTED: $selected",fontWeight=FontWeight.Bold)}
        items(tasks.filter{it.deadline==selected.toString()},key={it.id}){TaskRow(it,vm)}
    }
}

@Composable
fun HabitsScreen(vm:MainViewModel) {
    val habits by vm.habits.collectAsState()
    var add by remember{mutableStateOf(false)}
    Scaffold(containerColor=BG,floatingActionButton={FloatingActionButton({add=true},containerColor=RED){Icon(Icons.Default.Add,null)}}){pad->
        LazyColumn(Modifier.padding(pad).fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            item{Header("HABITS","Daily consistency and streaks")}
            if(habits.isEmpty())item{EmptyCard("No habits yet.")}
            items(habits,key={it.id}){h->
                Card(colors=CardDefaults.cardColors(PANEL)){Column(Modifier.padding(16.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){Text(h.name,fontWeight=FontWeight.Bold);Text("${h.frequency} • ${h.currentStreak} day streak • best ${h.bestStreak}",color=MUTED)}
                        Button({vm.completeHabit(h)},colors=ButtonDefaults.buttonColors(RED)){Text("DONE")}
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${h.completionCount} completions  •  +${h.xp} XP  •  -${h.hpPenalty} HP",color=MUTED)
                    TextButton({vm.deleteHabit(h)}){Text("DELETE",color=MUTED)}
                }}
            }
        }
    }
    if(add)HabitDialog(vm){add=false}
}

@Composable
fun HabitDialog(vm:MainViewModel,close:()->Unit){
    var name by remember{mutableStateOf("")};var freq by remember{mutableStateOf("DAILY")};var diff by remember{mutableStateOf(Difficulty.MEDIUM)}
    AlertDialog(onDismissRequest=close,title={Text("NEW HABIT")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(name,{name=it},label={Text("Habit name")})
        OutlinedTextField(freq,{freq=it},label={Text("Frequency")})
        Row{Difficulty.values().forEach{d->FilterChip(diff==d,{diff=d},label={Text(d.name.take(1))})}}
    }},confirmButton={Button({vm.addHabit(name,freq,diff);close()},colors=ButtonDefaults.buttonColors(RED)){Text("CREATE")}},dismissButton={TextButton(close){Text("CANCEL")}})
}

@Composable
fun ProgressScreen(vm:MainViewModel) {
    val p by vm.player.collectAsState();val tasks by vm.tasks.collectAsState();val daily by vm.daily.collectAsState()
    val completed=tasks.count{it.status==TaskStatus.COMPLETED};val failed=tasks.count{it.status==TaskStatus.FAILED};val recovered=tasks.count{it.status==TaskStatus.RECOVERED}
    val completion=if(tasks.isEmpty())0 else completed*100/tasks.size
    LazyColumn(Modifier.fillMaxSize().background(BG),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Header("PROGRESS","Daily • Weekly • Monthly • Yearly")}
        item{ScoreCard("DISCIPLINE SCORE",p.disciplineScore)}
        item{ScoreCard("TASK COMPLETION",completion)}
        item{ScoreCard("STREAK POWER",minOf(100,p.overallStreak*10))}
        item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){StatCard("DONE","$completed",Modifier.weight(1f));StatCard("FAILED","$failed",Modifier.weight(1f));StatCard("RECOVERED","$recovered",Modifier.weight(1f))}}
        item{
            Card(colors=CardDefaults.cardColors(PANEL)){Column(Modifier.padding(16.dp)){
                Text("HISTORY",fontWeight=FontWeight.Bold)
                daily.take(14).forEach{r->Row(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(r.date,color=MUTED);Text("+${r.xpEarned} XP");Text("✓${r.completed}  ✕${r.failed}",color=MUTED)
                }}
            }}
        }
        item{Text("Yearly view is calculated from saved daily records; all data remains local.",color=MUTED,style=MaterialTheme.typography.bodySmall)}
    }
}
@Composable fun ScoreCard(title:String,value:Int){Card(colors=CardDefaults.cardColors(PANEL)){Column(Modifier.padding(16.dp)){Text(title,color=MUTED,fontWeight=FontWeight.Bold);Text("$value / 100",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black);LinearProgressIndicator({value/100f},Modifier.fillMaxWidth().height(8.dp),color=RED,trackColor=Color(0xFF292929))}}}

@Composable
fun MissionsScreen(vm:MainViewModel){
    val missions by vm.missions.collectAsState();val tasks by vm.tasks.collectAsState();var add by remember{mutableStateOf(false)}
    Scaffold(containerColor=BG,floatingActionButton={FloatingActionButton({add=true},containerColor=RED){Icon(Icons.Default.Add,null)}}){pad->
        LazyColumn(Modifier.padding(pad).fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            item{Header("MISSIONS","Long-term goals")}
            items(missions,key={it.id}){m->
                val done=tasks.count{it.missionId==m.id&&it.status==TaskStatus.COMPLETED}
                val pct=if(m.targetCount==0)0 else (done*100/m.targetCount).coerceAtMost(100)
                Card(colors=CardDefaults.cardColors(PANEL)){Column(Modifier.padding(16.dp)){
                    Text(m.title,fontWeight=FontWeight.Black);Text(m.goal,color=MUTED)
                    Spacer(Modifier.height(8.dp));Text("$done / ${m.targetCount} tasks")
                    LinearProgressIndicator({pct/100f},Modifier.fillMaxWidth().height(8.dp),color=RED,trackColor=Color(0xFF292929))
                    Text("Deadline ${m.deadline} • Reward ${m.rewardXp} XP / ${m.rewardCoins} coins",color=MUTED,style=MaterialTheme.typography.bodySmall)
                    TextButton({vm.deleteMission(m)}){Text("DELETE",color=MUTED)}
                }}
            }
        }
    }
    if(add)MissionDialog(vm){add=false}
}
@Composable fun MissionDialog(vm:MainViewModel,close:()->Unit){
    var title by remember{mutableStateOf("")};var goal by remember{mutableStateOf("")};var start by remember{mutableStateOf(LocalDate.now().toString())}
    var deadline by remember{mutableStateOf(LocalDate.now().plusDays(30).toString())};var target by remember{mutableStateOf("30")}
    AlertDialog(onDismissRequest=close,title={Text("NEW MISSION")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(title,{title=it},label={Text("Title")});OutlinedTextField(goal,{goal=it},label={Text("Goal")})
        OutlinedTextField(start,{start=it},label={Text("Start YYYY-MM-DD")});OutlinedTextField(deadline,{deadline=it},label={Text("Deadline YYYY-MM-DD")})
        OutlinedTextField(target,{target=it},label={Text("Target task count")})
    }},confirmButton={Button({vm.addMission(title,goal,start,deadline,target.toIntOrNull()?:30,200,100);close()},colors=ButtonDefaults.buttonColors(RED)){Text("CREATE")}},dismissButton={TextButton(close){Text("CANCEL")}})
}

@Composable
fun CharacterScreen(vm:MainViewModel,onNavigate:(Screen)->Unit){
    val p by vm.player.collectAsState();var more by remember{mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize().background(BG),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Header("CHARACTER","Your real-life RPG profile")}
        item{PlayerCard(p)}
        item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){StatCard("COMPLETED","${p.completed}",Modifier.weight(1f));StatCard("FAILED","${p.failed}",Modifier.weight(1f));StatCard("RECOVERED","${p.recovered}",Modifier.weight(1f))}}
        item{
            val shop by vm.shop.collectAsState()
            val equipped=shop.filter{it.equipped}
            Card(colors=CardDefaults.cardColors(PANEL)){Column(Modifier.padding(16.dp)){
                Text("EQUIPPED LOADOUT",fontWeight=FontWeight.Bold)
                Text(if(equipped.isEmpty())"Default Operator" else equipped.joinToString(" • "){it.name},color=MUTED)
            }}
        }
        item{Button({more=true},Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(RED)){Text("OPEN CHARACTER HUB")}}
        item{if(more)MoreHub(onNavigate={s->more=false;onNavigate(s)})}
    }
}

@Composable
fun MoreHub(onNavigate:(Screen)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        listOf(Screen.CALENDAR to "Calendar",Screen.HABITS to "Habits",Screen.MISSIONS to "Missions",Screen.SHOP to "Shop",Screen.ACHIEVEMENTS to "Achievements",Screen.AI to "AI Coach",Screen.SETTINGS to "Settings").forEach{(s,n)->
            OutlinedButton({onNavigate(s)},Modifier.fillMaxWidth()){Text(n)}
        }
    }
}

@Composable
fun ShopScreen(vm:MainViewModel){
    val items by vm.shop.collectAsState();val p by vm.player.collectAsState()
    LazyColumn(Modifier.fillMaxSize().background(BG),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Header("SHOP","Cosmetics only • ${p.coins} coins")}
        items(items,key={it.id}){i->
            Card(colors=CardDefaults.cardColors(PANEL)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){Text(i.name,fontWeight=FontWeight.Bold);Text("${i.category} • ${i.price} coins",color=MUTED)}
                if(!i.owned)Button({vm.purchase(i)},colors=ButtonDefaults.buttonColors(RED)){Text("BUY")}
                else if(i.equipped)Text("EQUIPPED",color=GREEN,fontWeight=FontWeight.Bold)
                else OutlinedButton({vm.equip(i)}){Text("EQUIP")}
            }}
        }
    }
}

@Composable
fun AchievementsScreen(vm:MainViewModel){
    val a by vm.achievements.collectAsState()
    LazyColumn(Modifier.fillMaxSize().background(BG),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Header("ACHIEVEMENTS","Milestones")}
        items(a,key={it.id}){x->Card(colors=CardDefaults.cardColors(if(x.unlocked)PANEL else Color(0xFF0B0B0B))){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(if(x.unlocked)Icons.Default.EmojiEvents else Icons.Default.Lock,null,tint=if(x.unlocked)GOLD:MUTED,modifier=Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp));Column{Text(x.title,fontWeight=FontWeight.Bold);Text(x.description,color=MUTED)}}
        }}
    }
}

@Composable
fun AiScreen(vm:MainViewModel,prefs:Prefs){
    val context=androidx.compose.ui.platform.LocalContext.current
    var key by remember{mutableStateOf(prefs.geminiKey)}
    var goal by remember{mutableStateOf("")};var days by remember{mutableStateOf("30")}
    var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    var draft by remember{mutableStateOf<AiPlanDraft?>(null)}
    val coach=remember{GeminiCoach()}
    LazyColumn(Modifier.fillMaxSize().background(BG),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Header("AI COACH","Free-tier Gemini • preview before saving")}
        item{
            Card(colors=CardDefaults.cardColors(PANEL)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("GEMINI API KEY",fontWeight=FontWeight.Bold)
                OutlinedTextField(key,{key=it},label={Text("Free-tier API key")},visualTransformation=PasswordVisualTransformation())
                Text("No paid billing is required for a free-tier project. Never paste a paid key.",color=MUTED,style=MaterialTheme.typography.bodySmall)
                Button({prefs.geminiKey=key;coach.setKey(key);Toast.makeText(context,"Saved",Toast.LENGTH_SHORT).show()},colors=ButtonDefaults.buttonColors(RED)){Text("SAVE KEY")}
            }}
        }
        item{
            Card(colors=CardDefaults.cardColors(PANEL)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(goal,{goal=it},label={Text("Your goal")})
                OutlinedTextField(days,{days=it},label={Text("Days")},singleLine=true)
                Button({
                    loading=true;error=null;coach.setKey(key)
                    coach.generate(goal,days.toIntOrNull()?:30){result->
                        loading=false
                        result.onSuccess{draft=it}.onFailure{error=it.message}
                    }
                },enabled=!loading&&goal.isNotBlank()&&key.isNotBlank(),colors=ButtonDefaults.buttonColors(RED)){Text(if(loading)"GENERATING..." else "GENERATE PLAN")}
                error?.let{Text("AI error: $it",color=RED)}
            }}
        }
        draft?.let{plan->
            item{AiPreview(plan,onChange={draft=it},onAccept={vm.acceptAiPlan(it);draft=null;Toast.makeText(context,"Mission saved",Toast.LENGTH_SHORT).show()})}
        }
    }
}


@Composable
fun AiPreview(plan:AiPlanDraft,onChange:(AiPlanDraft)->Unit,onAccept:(AiPlanDraft)->Unit){
    Card(colors=CardDefaults.cardColors(PANEL)){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("PREVIEW — EDIT BEFORE SAVE",color=RED,fontWeight=FontWeight.Black)
            var missionTitle by remember(plan){mutableStateOf(plan.missionTitle)}
            var goal by remember(plan){mutableStateOf(plan.goal)}
            OutlinedTextField(missionTitle,{v->missionTitle=v;onChange(plan.copy(missionTitle=v))},label={Text("Mission title")})
            OutlinedTextField(goal,{v->goal=v;onChange(plan.copy(goal=v))},label={Text("Goal")})
            plan.tasks.forEachIndexed{index,t->
                var title by remember(plan,index){mutableStateOf(t.title)}
                var desc by remember(plan,index){mutableStateOf(t.description)}
                var date by remember(plan,index){mutableStateOf(t.deadline)}
                var xp by remember(plan,index){mutableStateOf(t.xp.toString())}
                var coins by remember(plan,index){mutableStateOf(t.coins.toString())}
                var hp by remember(plan,index){mutableStateOf(t.hpPenalty.toString())}
                var diff by remember(plan,index){mutableStateOf(runCatching{Difficulty.valueOf(t.difficulty.uppercase())}.getOrDefault(Difficulty.MEDIUM))}
                Card(colors=CardDefaults.cardColors(PANEL2)){
                    Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text("TASK ${index+1}",color=RED,fontWeight=FontWeight.Bold)
                        OutlinedTextField(title,{v->title=v;val n=plan.tasks.toMutableList();n[index]=t.copy(title=v);onChange(plan.copy(tasks=n))},label={Text("Title")})
                        OutlinedTextField(desc,{v->desc=v;val n=plan.tasks.toMutableList();n[index]=t.copy(description=v);onChange(plan.copy(tasks=n))},label={Text("Description")})
                        OutlinedTextField(date,{v->date=v;val n=plan.tasks.toMutableList();n[index]=t.copy(deadline=v);onChange(plan.copy(tasks=n))},label={Text("Deadline YYYY-MM-DD")})
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            Difficulty.values().forEach{d->
                                FilterChip(diff==d,{diff=d;val n=plan.tasks.toMutableList();n[index]=t.copy(difficulty=d.name);onChange(plan.copy(tasks=n))},label={Text(d.name.take(1))})
                            }
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            OutlinedTextField(xp,{v->xp=v;val n=plan.tasks.toMutableList();n[index]=t.copy(xp=v.toIntOrNull()?:t.xp);onChange(plan.copy(tasks=n))},label={Text("XP")},modifier=Modifier.weight(1f),singleLine=true)
                            OutlinedTextField(coins,{v->coins=v;val n=plan.tasks.toMutableList();n[index]=t.copy(coins=v.toIntOrNull()?:t.coins);onChange(plan.copy(tasks=n))},label={Text("Coins")},modifier=Modifier.weight(1f),singleLine=true)
                            OutlinedTextField(hp,{v->hp=v;val n=plan.tasks.toMutableList();n[index]=t.copy(hpPenalty=v.toIntOrNull()?:t.hpPenalty);onChange(plan.copy(tasks=n))},label={Text("HP")},modifier=Modifier.weight(1f),singleLine=true)
                        }
                        TextButton({
                            val n=plan.tasks.toMutableList();n.removeAt(index);onChange(plan.copy(tasks=n))
                        }){Text("DELETE TASK",color=RED)}
                    }
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button({onAccept(plan)},colors=ButtonDefaults.buttonColors(RED)){Text("ACCEPT & SAVE")}
                OutlinedButton({onChange(plan.copy(tasks=emptyList()))}){Text("REJECT")}
            }
        }
    }
}

@Composable
fun SettingsScreen(vm:MainViewModel,prefs:Prefs){
    val context=androidx.compose.ui.platform.LocalContext.current
    val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null){context.contentResolver.openOutputStream(uri)?.use{it.write(vm.exportJson().toByteArray())};Toast.makeText(context,"Exported",Toast.LENGTH_SHORT).show()}
    }
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){val json=context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText();if(json!=null)vm.importJson(json);Toast.makeText(context,"Imported",Toast.LENGTH_SHORT).show()}
    }
    var notify by remember{mutableStateOf(prefs.notifications)}
    var my by remember{mutableStateOf(prefs.language=="my")}
    var confirmReset by remember{mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize().background(BG),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Header("SETTINGS","Personalize the system")}
        item{Card(colors=CardDefaults.cardColors(PANEL)){Column(Modifier.padding(16.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Burmese UI",Modifier.weight(1f));Switch(my,{my=it;prefs.language=if(it)"my" else "en"})}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Notifications",Modifier.weight(1f));Switch(notify,{notify=it;prefs.notifications=it;if(it)ReminderScheduler.schedule(context)else ReminderScheduler.cancel(context)})}
        }}}
        item{Button({create.launch("discipline_quest_backup.json")},Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(RED)){Text("EXPORT DATA")}}
        item{OutlinedButton({open.launch(arrayOf("application/json"))},Modifier.fillMaxWidth()){Text("IMPORT DATA")}}
        item{OutlinedButton({confirmReset=true},Modifier.fillMaxWidth()){Text("RESET ALL DATA",color=RED)}}
        item{Card(colors=CardDefaults.cardColors(PANEL)){Column(Modifier.padding(16.dp)){Text("ABOUT",fontWeight=FontWeight.Bold);Text("Discipline Quest v1.0.0",color=MUTED);Text("Offline-first personal discipline RPG. No ads. No subscriptions. AI is optional.",color=MUTED)}}}
    }
    if(confirmReset)AlertDialog(onDismissRequest={confirmReset=false},title={Text("RESET EVERYTHING?")},text={Text("All tasks, missions, habits and progress will be deleted.")},confirmButton={Button({vm.resetAll();confirmReset=false},colors=ButtonDefaults.buttonColors(RED)){Text("RESET")}},dismissButton={TextButton({confirmReset=false}){Text("CANCEL")}})
}
