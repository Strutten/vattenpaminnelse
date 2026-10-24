package se.vatten.paminnelse

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon

object Notifier {
    const val ACTION_DONE = "se.vatten.paminnelse.DONE"
    const val ACTION_SNOOZE = "se.vatten.paminnelse.SNOOZE"
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    // Android låser ljud/vibration per kanal, så det finns en kanal per kombination
    private fun channel(ctx: Context, sound: Boolean, vib: Boolean): String {
        val nm = ctx.getSystemService(NotificationManager::class.java)!!
        val id = "pm_" + (if (sound) "s" else "x") + (if (vib) "v" else "x")
        if (nm.getNotificationChannel(id) == null) {
            val name = when {
                sound && vib -> "Ljud och vibration"
                sound -> "Bara ljud"
                vib -> "Bara vibration"
                else -> "Tysta"
            }
            val ch = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            if (!sound) ch.setSound(null, null)
            ch.enableVibration(vib)
            if (vib) ch.vibrationPattern = longArrayOf(0, 250, 150, 250)
            nm.createNotificationChannel(ch)
        }
        return id
    }

    fun body(r: Reminder): String {
        val progress = if (r.track) "${r.todayCount()} av ${r.goal} idag" else ""
        return listOf(r.message, progress).filter { it.isNotBlank() }.joinToString("\n")
    }

    fun show(ctx: Context, r: Reminder) {
        val nm = ctx.getSystemService(NotificationManager::class.java)!!
        val open = PendingIntent.getActivity(ctx, r.id, Intent(ctx, MainActivity::class.java), FLAGS)
        val icon = Icon.createWithResource(ctx, R.drawable.ic_bell)

        val done = Intent(ctx, ActionReceiver::class.java).setAction(ACTION_DONE).putExtra(Scheduler.EXTRA_ID, r.id)
        val snooze = Intent(ctx, ActionReceiver::class.java).setAction(ACTION_SNOOZE).putExtra(Scheduler.EXTRA_ID, r.id)
        val text = body(r)

        val n = Notification.Builder(ctx, channel(ctx, r.sound, r.vibrate))
            .setSmallIcon(R.drawable.ic_bell)
            .setColor(r.color.toInt())
            .setContentTitle("${r.emoji}  ${r.title.ifBlank { "Påminnelse" }}")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(icon, r.actionLabel.ifBlank { "Klart" },
                PendingIntent.getBroadcast(ctx, 200_000 + r.id, done, FLAGS)).build())
            .addAction(Notification.Action.Builder(icon, "Om 10 min",
                PendingIntent.getBroadcast(ctx, 300_000 + r.id, snooze, FLAGS)).build())
            .build()
        try { nm.notify(r.id, n) } catch (_: SecurityException) { }
    }
}
