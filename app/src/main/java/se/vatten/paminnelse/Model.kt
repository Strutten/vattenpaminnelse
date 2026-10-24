package se.vatten.paminnelse

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Reminder(
    val id: Int = 0,
    val title: String = "",
    val message: String = "",
    val emoji: String = "🔔",
    val color: Long = 0xFF3D5AFE,
    val useTimes: Boolean = false,
    val intervalMin: Int = 60,
    val startMin: Int = 8 * 60,
    val endMin: Int = 22 * 60,
    val times: List<Int> = listOf(9 * 60),
    val days: Int = ALL_DAYS,          // bit 0 = måndag … bit 6 = söndag
    val enabled: Boolean = true,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val track: Boolean = false,
    val goal: Int = 8,
    val count: Int = 0,
    val countDate: String = "",
    val stopAtGoal: Boolean = false,
    val actionLabel: String = "Klart",
    val nextAt: Long = 0L,
) {
    fun dayOn(dow: Int): Boolean = (days and (1 shl (dow - 1))) != 0
    fun todayCount(): Int = if (countDate == LocalDate.now().toString()) count else 0

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("message", message); put("emoji", emoji)
        put("color", color); put("useTimes", useTimes); put("intervalMin", intervalMin)
        put("startMin", startMin); put("endMin", endMin); put("times", JSONArray(times))
        put("days", days); put("enabled", enabled); put("sound", sound); put("vibrate", vibrate)
        put("track", track); put("goal", goal); put("count", count); put("countDate", countDate)
        put("stopAtGoal", stopAtGoal); put("actionLabel", actionLabel); put("nextAt", nextAt)
    }

    companion object {
        const val ALL_DAYS = 0b1111111
        const val WEEKDAYS = 0b0011111
        const val WEEKEND = 0b1100000

        fun fromJson(o: JSONObject): Reminder {
            val d = Reminder()
            val arr = o.optJSONArray("times")
            val times = if (arr != null) (0 until arr.length()).map { arr.getInt(it) } else d.times
            return Reminder(
                id = o.optInt("id"),
                title = o.optString("title", d.title),
                message = o.optString("message", d.message),
                emoji = o.optString("emoji", d.emoji),
                color = o.optLong("color", d.color),
                useTimes = o.optBoolean("useTimes", d.useTimes),
                intervalMin = o.optInt("intervalMin", d.intervalMin),
                startMin = o.optInt("startMin", d.startMin),
                endMin = o.optInt("endMin", d.endMin),
                times = times,
                days = o.optInt("days", d.days),
                enabled = o.optBoolean("enabled", d.enabled),
                sound = o.optBoolean("sound", d.sound),
                vibrate = o.optBoolean("vibrate", d.vibrate),
                track = o.optBoolean("track", d.track),
                goal = o.optInt("goal", d.goal),
                count = o.optInt("count", 0),
                countDate = o.optString("countDate", ""),
                stopAtGoal = o.optBoolean("stopAtGoal", d.stopAtGoal),
                actionLabel = o.optString("actionLabel", d.actionLabel),
                nextAt = o.optLong("nextAt", 0L),
            )
        }
    }
}

val TEMPLATES = listOf(
    Reminder(title = "Drick vatten", message = "Ta ett glas vatten nu", emoji = "💧", color = 0xFF2F80ED,
        intervalMin = 60, track = true, goal = 8, actionLabel = "Drack ett glas"),
    Reminder(title = "Ta medicin", message = "Dags för din medicin", emoji = "💊", color = 0xFFE5484D,
        useTimes = true, times = listOf(8 * 60, 20 * 60), actionLabel = "Tagit"),
    Reminder(title = "Sträck på dig", message = "Res dig och rör på kroppen en minut", emoji = "🧘", color = 0xFF30A46C,
        intervalMin = 45, startMin = 9 * 60, endMin = 17 * 60, days = Reminder.WEEKDAYS, actionLabel = "Gjort"),
    Reminder(title = "Vila ögonen", message = "Titta på något långt bort i 20 sekunder", emoji = "👀", color = 0xFF8E4EC6,
        intervalMin = 20, startMin = 9 * 60, endMin = 21 * 60, actionLabel = "Gjort"),
    Reminder(title = "Vattna växterna", message = "Växterna är törstiga", emoji = "🌱", color = 0xFF46A758,
        useTimes = true, times = listOf(18 * 60), days = 0b0001001, actionLabel = "Vattnat"),
    Reminder(title = "Plugga", message = "Dags för ett pluggpass", emoji = "📚", color = 0xFFF76B15,
        useTimes = true, times = listOf(16 * 60), days = Reminder.WEEKDAYS, actionLabel = "Börjar nu"),
    Reminder(title = "Borsta tänderna", message = "Två minuter, inte mindre", emoji = "🪥", color = 0xFF12A594,
        useTimes = true, times = listOf(7 * 60 + 30, 21 * 60 + 30), actionLabel = "Borstat"),
    Reminder(title = "", message = "", emoji = "✨", color = 0xFF3D5AFE),
)

object Store {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    fun load(ctx: Context): List<Reminder> {
        val p = prefs(ctx)
        if (!p.getBoolean("seeded", false)) migrateOld(ctx)
        val arr = JSONArray(p.getString("list", "[]"))
        return (0 until arr.length()).map { Reminder.fromJson(arr.getJSONObject(it)) }
    }

    fun saveAll(ctx: Context, list: List<Reminder>) {
        prefs(ctx).edit().putString("list", JSONArray(list.map { it.toJson() }).toString()).commit()
    }

    fun get(ctx: Context, id: Int): Reminder? = load(ctx).find { it.id == id }

    fun upsert(ctx: Context, r: Reminder) {
        val l = load(ctx).toMutableList()
        val i = l.indexOfFirst { it.id == r.id }
        if (i >= 0) l[i] = r else l.add(r)
        saveAll(ctx, l)
    }

    fun delete(ctx: Context, id: Int) = saveAll(ctx, load(ctx).filter { it.id != id })

    fun newId(ctx: Context): Int = (load(ctx).maxOfOrNull { it.id } ?: 0) + 1

    fun addCount(ctx: Context, id: Int, delta: Int): Reminder? {
        val r = get(ctx, id) ?: return null
        val u = r.copy(count = (r.todayCount() + delta).coerceAtLeast(0), countDate = LocalDate.now().toString())
        upsert(ctx, u)
        return u
    }

    // Flyttar över vattenpåminnelsen från version 1
    private fun migrateOld(ctx: Context) {
        val old = ctx.getSharedPreferences("vatten", Context.MODE_PRIVATE)
        val list = mutableListOf<Reminder>()
        if (old.contains("enabled")) {
            list.add(
                TEMPLATES[0].copy(
                    id = 1,
                    title = old.getString("title", null)?.removeSuffix(" 💧")?.ifBlank { null } ?: "Drick vatten",
                    message = old.getString("message", null) ?: TEMPLATES[0].message,
                    intervalMin = old.getInt("interval", 60),
                    startMin = old.getInt("start", 480),
                    endMin = old.getInt("end", 1320),
                    goal = old.getInt("goal", 8),
                    sound = old.getBoolean("sound", true),
                    vibrate = old.getBoolean("vibrate", true),
                    stopAtGoal = old.getBoolean("stopAtGoal", false),
                    enabled = old.getBoolean("enabled", false),
                    count = old.getInt("count", 0),
                    countDate = old.getString("countDate", "") ?: "",
                )
            )
        }
        prefs(ctx).edit()
            .putString("list", JSONArray(list.map { it.toJson() }).toString())
            .putBoolean("seeded", true).commit()
    }
}
