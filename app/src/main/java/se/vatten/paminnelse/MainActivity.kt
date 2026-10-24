package se.vatten.paminnelse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val reminders = mutableStateOf(emptyList<Reminder>())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        reminders.value = Store.load(this)
        setContent {
            AppTheme {
                App(reminders.value) { reminders.value = Store.load(this@MainActivity) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reminders.value = Store.load(this)
    }
}
