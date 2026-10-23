package se.vatten.paminnelse

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import java.time.LocalDateTime
import java.time.ZoneId

object Reminder {
    const val NOTIF_ID = 42
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    // Android låser ljud/vibration per kanal, så vi byter kanal när inställningen ändras
    private fun ensureChannel(ctx: Context, p: Prefs): String {
        val nm = ctx.getSystemService(NotificationManager::class.java)!!
        val id = "vatten_" + (if (p.sound) "s" else "x") + (if (p.vibrate) "v" else "x")
        nm.notificationChannels
            .filter { it.id.startsWith("vatten_") && it.id != id }
            .forEach { nm.deleteNotificationChannel(it.id) }
        if (nm.getNotificationChannel(id) == null) {
            val ch = NotificationChannel(id, "Vattenpåminnelser", NotificationManager.IMPORTANCE_HIGH)
            if (!p.sound) ch.setSound(null, null)
            ch.enableVibration(p.vibrate)
            if (p.vibrate) ch.vibrationPattern = longArrayOf(0, 300, 200, 300)
            nm.createNotificationChannel(ch)
        }
        return id
    }

    fun show(ctx: Context) {
        val p = Prefs(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)!!
        val channel = ensureChannel(ctx, p)
        val openPi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), FLAGS)
        val drinkPi = PendingIntent.getBroadcast(ctx, 2, Intent(ctx, DrinkReceiver::class.java), FLAGS)
        val drinkAction = Notification.Action.Builder(
            Icon.createWithResource(ctx, R.drawable.ic_drop), "Jag drack ett glas 💧", drinkPi
        ).build()

        val n = Notification.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_drop)
            .setContentTitle(p.title)
            .setContentText("${p.message}  (${p.count}/${p.goal} glas)")
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .addAction(drinkAction)
            .build()
        try { nm.notify(NOTIF_ID, n) } catch (_: SecurityException) { }
    }

    fun inWindow(p: Prefs, m: Int): Boolean = when {
        p.startMin == p.endMin -> true
        p.startMin < p.endMin -> m >= p.startMin && m < p.endMin
        else -> m >= p.startMin || m < p.endMin   // över midnatt, t.ex. 22–02
    }

    private fun nextTime(p: Prefs, now: LocalDateTime): LocalDateTime {
        val c = now.plusMinutes(p.intervalMin.toLong())
        if (inWindow(p, c.hour * 60 + c.minute)) return c
        var s = c.toLocalDate().atTime(p.startMin / 60, p.startMin % 60)
        if (!s.isAfter(c)) s = s.plusDays(1)
        return s
    }

    fun schedule(ctx: Context) {
        val p = Prefs(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)!!
        val pi = PendingIntent.getBroadcast(ctx, 1, Intent(ctx, ReminderReceiver::class.java), FLAGS)
        am.cancel(pi)
        if (!p.enabled) { p.nextAt = 0L; return }
        val ms = nextTime(p, LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        p.nextAt = ms
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
    }
}
