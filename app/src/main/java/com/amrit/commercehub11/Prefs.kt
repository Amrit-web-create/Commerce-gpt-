package com.amrit.commercehub11

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/** localStorage ka replacement */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("commerce_hub_prefs", Context.MODE_PRIVATE)
    }

    fun getString(key: String, def: String = ""): String = sp.getString(key, def) ?: def
    fun putString(key: String, value: String) { sp.edit().putString(key, value).apply() }
    fun getBool(key: String, def: Boolean): Boolean = sp.getBoolean(key, def)
    fun putBool(key: String, value: Boolean) { sp.edit().putBoolean(key, value).apply() }
    fun getLong(key: String, def: Long = 0L): Long = sp.getLong(key, def)
    fun putLong(key: String, value: Long) { sp.edit().putLong(key, value).apply() }
    fun has(key: String): Boolean = sp.contains(key)

    // ---- Theme ----
    var themeMode: String
        get() = getString("app_theme_mode", "light")
        set(v) = putString("app_theme_mode", v)

    // ---- Saved books ----
    fun loadSavedBooks(): List<String> {
        return try {
            val arr = JSONArray(getString("my_saved_books", "[]"))
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
    }

    fun saveSavedBooks(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        putString("my_saved_books", arr.toString())
    }

    // ---- Support ----
    var supportActive: Boolean
        get() = getBool("global_support_active", false)
        set(v) = putBool("global_support_active", v)

    var eyeLiveMessages: Boolean
        get() = getString("eye_live_messages", "on") != "off"
        set(v) = putString("eye_live_messages", if (v) "on" else "off")

    var identity: String
        get() {
            val saved = getString("app_user_identity", "")
            if (saved.isNotEmpty()) return saved
            val gen = "User_" + (1000 + (Math.random() * 9000).toInt())
            putString("app_user_identity", gen)
            return gen
        }
        set(v) = putString("app_user_identity", v)

    var supportTgOffset: Long
        get() = getLong("support_tg_offset", 0L)
        set(v) = putLong("support_tg_offset", v)
}

var Prefs.notifEnabled: Boolean
    get() = getBool("notif_enabled", false)
    set(v) = putBool("notif_enabled", v)
