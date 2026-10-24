package se.vatten.paminnelse

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

object Scheduler {
    const val EXTRA_ID = "id"
    const val EXTRA_SNOOZE = "snooze"
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    private fun alarmPi(ctx: Context, id: Int, snooze: Boolean): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .putExtra(EXTRA_ID, id).putExtra(EXTRA_SNOOZE, snooze)
        return PendingIntent.getBroadcast(ctx, if (snooze) 100_000 + id else id, i, FLAGS)
    }

    fun inWindow(r: Reminder, m: Int): Boolean = when {
        r.startMin == r.endMin -> true
        r.startMin < r.endMin -> m >= r.startMin && m < r.endMin
        else -> m >= r.startMin || m < r.endMin
    }

    fun next(r: Reminder, now: LocalDateTime): LocalDateTime? {
        if (r.days == 0) return null
        if (r.useTimes) {
            val ts = r.times.sorted()
            if (ts.isEmpty()) return null
            for (d in 0..7) {
                val date = now.toLocalDate().plusDays(d.toLong())
                if (!r.dayOn(date.dayOfWeek.value)) continue
                for (t in ts) {
                    val dt = date.atTime(t / 60, t % 60)
                    if (dt.isAfter(now)) return dt
                }
            }
            return null
        }
        var c = now.plusMinutes(r.intervalMin.toLong().coerceAtLeast(1))
        repeat(16) {
            if (r.dayOn(c.dayOfWeek.value) && inWindow(r, c.hour * 60 + c.minute)) return c
            var s = c.toLocalDate().atTime(r.startMin / 60, r.startMin % 60)
            if (!s.isAfter(c)) s = s.plusDays(1)
            c = s
        }
        return null
    }

    private fun setAlarm(am: AlarmManager, ms: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
        }
    }

    /** Planerar nästa larm och returnerar påminnelsen med uppdaterad nextAt (spara den!). */
    fun schedule(ctx: Context, r: Reminder): Reminder {
        val am = ctx.getSystemService(AlarmManager::class.java)!!
        val pi = alarmPi(ctx, r.id, false)
        am.cancel(pi)
        val t = if (r.enabled) next(r, LocalDateTime.now()) else null
        val ms = t?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L
        if (ms > 0) setAlarm(am, ms, pi)
        return r.copy(nextAt = ms)
    }

    fun snooze(ctx: Context, id: Int, minutes: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)!!
        setAlarm(am, System.currentTimeMillis() + minutes * 60_000L, alarmPi(ctx, id, true))
    }

    fun cancel(ctx: Context, id: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)!!
        am.cancel(alarmPi(ctx, id, false))
        am.cancel(alarmPi(ctx, id, true))
        ctx.getSystemService(NotificationManager::class.java)?.cancel(id)
    }

    fun rescheduleAll(ctx: Context) {
        Store.saveAll(ctx, Store.load(ctx).map { schedule(ctx, it) })
    }
}
