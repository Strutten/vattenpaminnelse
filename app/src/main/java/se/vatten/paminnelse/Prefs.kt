package se.vatten.paminnelse

import android.content.Context
import java.time.LocalDate

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("vatten", Context.MODE_PRIVATE)

    private fun putB(k: String, v: Boolean) = p.edit().putBoolean(k, v).apply()
    private fun putI(k: String, v: Int) = p.edit().putInt(k, v).apply()
    private fun putS(k: String, v: String) = p.edit().putString(k, v).apply()

    var enabled: Boolean
        get() = p.getBoolean("enabled", false)
        set(v) = putB("enabled", v)
    var title: String
        get() = p.getString("title", "Dags att dricka vatten 💧") ?: ""
        set(v) = putS("title", v)
    var message: String
        get() = p.getString("message", "Ta ett glas vatten nu!") ?: ""
        set(v) = putS("message", v)
    var intervalMin: Int
        get() = p.getInt("interval", 60)
        set(v) = putI("interval", v)
    var startMin: Int
        get() = p.getInt("start", 8 * 60)
        set(v) = putI("start", v)
    var endMin: Int
        get() = p.getInt("end", 22 * 60)
        set(v) = putI("end", v)
    var goal: Int
        get() = p.getInt("goal", 8)
        set(v) = putI("goal", v)
    var sound: Boolean
        get() = p.getBoolean("sound", true)
        set(v) = putB("sound", v)
    var vibrate: Boolean
        get() = p.getBoolean("vibrate", true)
        set(v) = putB("vibrate", v)
    var stopAtGoal: Boolean
        get() = p.getBoolean("stopAtGoal", false)
        set(v) = putB("stopAtGoal", v)
    var nextAt: Long
        get() = p.getLong("nextAt", 0L)
        set(v) = p.edit().putLong("nextAt", v).apply()

    // Antal glas idag – nollställs automatiskt vid nytt datum
    var count: Int
        get() {
            val today = LocalDate.now().toString()
            if (p.getString("countDate", "") != today) {
                p.edit().putInt("count", 0).putString("countDate", today).apply()
            }
            return p.getInt("count", 0)
        }
        set(v) = p.edit().putInt("count", v)
            .putString("countDate", LocalDate.now().toString()).apply()
}
