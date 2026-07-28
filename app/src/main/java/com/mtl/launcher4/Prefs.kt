package com.mtl.launcher4

import android.content.Context

data class SlotApp(val packageName: String, val activityName: String, val label: String)

object Prefs {
    private const val FILE = "multilaunch4_prefs"

    fun save(context: Context, index: Int, app: SlotApp) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        p.edit()
            .putString("pkg_$index", app.packageName)
            .putString("act_$index", app.activityName)
            .putString("label_$index", app.label)
            .apply()
    }

    fun load(context: Context, index: Int): SlotApp? {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val pkg = p.getString("pkg_$index", null) ?: return null
        val act = p.getString("act_$index", null) ?: return null
        val label = p.getString("label_$index", pkg) ?: pkg
        return SlotApp(pkg, act, label)
    }
}
