package se.vatten.paminnelse

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.time.LocalTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val p = Prefs(ctx)
        val now = LocalTime.now()
        val goalDone = p.stopAtGoal && p.count >= p.goal
        if (p.enabled && Reminder.inWindow(p, now.hour * 60 + now.minute) && !goalDone) {
            Reminder.show(ctx)
        }
        Reminder.schedule(ctx)
    }
}

class DrinkReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val p = Prefs(ctx)
        p.count = p.count + 1
        ctx.getSystemService(NotificationManager::class.java)?.cancel(Reminder.NOTIF_ID)
        Toast.makeText(ctx, "Bra! ${p.count}/${p.goal} glas idag", Toast.LENGTH_SHORT).show()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminder.schedule(ctx)
    }
}
