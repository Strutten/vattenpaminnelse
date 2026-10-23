package se.vatten.paminnelse

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {
    private lateinit var p: Prefs
    private lateinit var swEnabled: Switch
    private lateinit var swSound: Switch
    private lateinit var swVibrate: Switch
    private lateinit var swStopGoal: Switch
    private lateinit var etTitle: EditText
    private lateinit var etMessage: EditText
    private lateinit var etInterval: EditText
    private lateinit var etGoal: EditText
    private lateinit var btnStart: Button
    private lateinit var btnEnd: Button
    private lateinit var tvCount: TextView
    private lateinit var tvNext: TextView
    private var startMin = 0
    private var endMin = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        p = Prefs(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        swEnabled = findViewById(R.id.swEnabled)
        swSound = findViewById(R.id.swSound)
        swVibrate = findViewById(R.id.swVibrate)
        swStopGoal = findViewById(R.id.swStopGoal)
        etTitle = findViewById(R.id.etTitle)
        etMessage = findViewById(R.id.etMessage)
        etInterval = findViewById(R.id.etInterval)
        etGoal = findViewById(R.id.etGoal)
        btnStart = findViewById(R.id.btnStart)
        btnEnd = findViewById(R.id.btnEnd)
        tvCount = findViewById(R.id.tvCount)
        tvNext = findViewById(R.id.tvNext)

        swEnabled.isChecked = p.enabled
        swSound.isChecked = p.sound
        swVibrate.isChecked = p.vibrate
        swStopGoal.isChecked = p.stopAtGoal
        etTitle.setText(p.title)
        etMessage.setText(p.message)
        etInterval.setText(p.intervalMin.toString())
        etGoal.setText(p.goal.toString())
        startMin = p.startMin
        endMin = p.endMin
        updateTimeButtons()

        btnStart.setOnClickListener { pickTime(startMin) { startMin = it; updateTimeButtons() } }
        btnEnd.setOnClickListener { pickTime(endMin) { endMin = it; updateTimeButtons() } }
        swEnabled.setOnCheckedChangeListener { _, _ -> save() }

        findViewById<Button>(R.id.btnPlus).setOnClickListener { p.count = p.count + 1; updateStatus() }
        findViewById<Button>(R.id.btnMinus).setOnClickListener {
            if (p.count > 0) p.count = p.count - 1
            updateStatus()
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            save()
            Toast.makeText(this, "Sparat ✔", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener { save(); Reminder.show(this) }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun save() {
        p.enabled = swEnabled.isChecked
        p.title = etTitle.text.toString().ifBlank { "Dags att dricka vatten 💧" }
        p.message = etMessage.text.toString()
        p.intervalMin = (etInterval.text.toString().toIntOrNull() ?: 60).coerceIn(1, 1440)
        p.goal = (etGoal.text.toString().toIntOrNull() ?: 8).coerceIn(1, 100)
        p.startMin = startMin
        p.endMin = endMin
        p.sound = swSound.isChecked
        p.vibrate = swVibrate.isChecked
        p.stopAtGoal = swStopGoal.isChecked
        Reminder.schedule(this)
        updateStatus()
    }

    private fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) }, current / 60, current % 60, true).show()
    }

    private fun fmt(m: Int) = String.format("%02d:%02d", m / 60, m % 60)

    private fun updateTimeButtons() {
        btnStart.text = "Från ${fmt(startMin)}"
        btnEnd.text = "Till ${fmt(endMin)}"
    }

    private fun updateStatus() {
        tvCount.text = "${p.count} / ${p.goal} glas idag"
        tvNext.text = if (p.enabled && p.nextAt > 0) {
            val t = Instant.ofEpochMilli(p.nextAt).atZone(ZoneId.systemDefault())
            "Nästa påminnelse: " + t.format(DateTimeFormatter.ofPattern("EEE HH:mm"))
        } else "Påminnelser är avstängda"
    }
}
