package com.disciplinequest.app

import com.disciplinequest.app.data.*

object AppDefaults {
    val shop = listOf(
        ShopItem("outfit_01","Crimson Operator","Outfit",300),
        ShopItem("outfit_02","Night Recon","Outfit",500),
        ShopItem("gear_01","Tactical Vest","Gear",700),
        ShopItem("gear_02","Urban Pack","Gear",450),
        ShopItem("bg_01","Red Mountain","Background",250),
        ShopItem("bg_02","Kyoto Night","Background",400),
        ShopItem("title_01","Iron Discipline","Title",800),
        ShopItem("title_02","Never Miss Twice","Title",1000)
    )
    val achievements = listOf(
        Achievement("first","FIRST MISSION","Complete your first task."),
        Achievement("streak7","7 DAY STREAK","Maintain a 7-day streak."),
        Achievement("streak30","30 DAY STREAK","Maintain a 30-day streak."),
        Achievement("tasks100","100 TASKS","Complete 100 tasks."),
        Achievement("perfect","PERFECT WEEK","Complete every required task for 7 days."),
        Achievement("comeback","COMEBACK","Recover after a failed task."),
        Achievement("nomiss","NO MISSES","Complete 20 tasks without a failure."),
        Achievement("master","DISCIPLINE MASTER","Reach 100 discipline score.")
    )
}
