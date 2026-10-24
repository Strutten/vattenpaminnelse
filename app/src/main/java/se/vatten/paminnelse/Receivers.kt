package se.vatten.paminnelse

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.time.LocalDateTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(Scheduler.EXTRA_ID, -1)
        if (id < 0) { Scheduler.rescheduleAll(ctx); return }
        val r = Store.get(ctx, id) ?: return

        if (intent.getBooleanExtra(Scheduler.EXTRA_SNOOZE, false)) {
            if (r.enabled) Notifier.show(ctx, r)
            return
        }
        val now = LocalDateTime.now()
        val goalDone = r.track && r.stopAtGoal && r.todayCount() >= r.goal
        val ok = r.enabled && r.dayOn(now.dayOfWeek.value) && !goalDone &&
            (r.useTimes || Scheduler.inWindow(r, now.hour * 60 + now.minute))
        if (ok) Notifier.show(ctx, r)
        Store.upsert(ctx, Scheduler.schedule(ctx, r))
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(Scheduler.EXTRA_ID, -1)
        ctx.getSystemService(NotificationManager::class.java)?.cancel(id)
        when (intent.action) {
            Notifier.ACTION_DONE -> {
                val r = Store.get(ctx, id) ?: return
                if (r.track) {
                    val u = Store.addCount(ctx, id, 1) ?: return
                    Toast.makeText(ctx, "${u.emoji} ${u.todayCount()} av ${u.goal} idag", Toast.LENGTH_SHORT).show()
                }
            }
            Notifier.ACTION_SNOOZE -> {
                Scheduler.snooze(ctx, id, 10)
                Toast.makeText(ctx, "Påminner igen om 10 minuter", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = Scheduler.rescheduleAll(ctx)
}
