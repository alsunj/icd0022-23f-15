package ee.taltech.a15

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper


class TimerService : Service() {
    private var startTimeMillis: Long = 0
    private var elapsedTimeSeconds: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    var isTimerRunning = false

    inner class LocalBinder : Binder() {
        fun getService(): TimerService {
            return this@TimerService
        }
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Record the start time when the service starts if the timer is running
        isTimerRunning = true
        if (isTimerRunning) {
            startTimeMillis = System.currentTimeMillis()
        }

        // Create a runnable that calculates the elapsed time only if the timer is running
        val elapsedTimeCalculator = object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    val currentTimeMillis = System.currentTimeMillis()
                    elapsedTimeSeconds = ((currentTimeMillis - startTimeMillis) / 1000).toInt()
                    sendTimerUpdate(elapsedTimeSeconds)
                }
                handler.postDelayed(this, 1000) // Update every second
            }
        }

        // Start calculating elapsed time if the timer is running
        if (isTimerRunning) {
            handler.postDelayed(elapsedTimeCalculator, 1000)
        }

        return START_STICKY
    }

    fun pauseTimer() {
        isTimerRunning = false

    }

    fun resumeTimer() {
        isTimerRunning = true
        startTimeMillis = System.currentTimeMillis() - (elapsedTimeSeconds * 1000)

    }
    private fun sendTimerUpdate(elapsedTimeSeconds: Int) {
        val intent = Intent("timer_update")
        intent.putExtra("timer_value", elapsedTimeSeconds)
        sendBroadcast(intent)
        val sharedPreferences = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("timer_value", elapsedTimeSeconds).apply()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
    fun resetTimer() {
        elapsedTimeSeconds = 0
        startTimeMillis = 0

    }


}