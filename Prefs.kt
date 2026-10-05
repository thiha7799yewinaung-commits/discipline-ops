package com.disciplinequest.app

import android.content.Context

class Prefs(context: Context) {
    private val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var language: String
        get() = p.getString("language","en") ?: "en"
        set(v) = p.edit().putString("language",v).apply()
    var notifications: Boolean
        get() = p.getBoolean("notifications", true)
        set(v) = p.edit().putBoolean("notifications",v).apply()
    var geminiKey: String
        get() = p.getString("gemini_key","") ?: ""
        set(v) = p.edit().putString("gemini_key",v.trim()).apply()
}
