package ee.taltech.a15

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.util.Timer
import java.util.TimerTask

class TimerService : Service() {
    private lateinit var timerTask: MyTimerTask
    private lateinit var timer: Timer
    private var secondsElapsed: Int = 0
    private var isTimerPaused: Boolean = true

    companion object {
        const val timerUpdated = TimerActions.timerUpdated
        const val ACTION_PAUSE_TIMER = TimerActions.timerPaused
        const val ACTION_RESUME_TIMER = TimerActions.timerResumed
    }

    override fun onCreate() {
        super.onCreate()
        timer = Timer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_PAUSE_TIMER -> {
                    pauseTimer()
                }
                ACTION_RESUME_TIMER -> {
                    resumeTimer()
                }
                else -> startTimer()
            }
        } else {
            startTimer()
        }
        return START_NOT_STICKY
    }
    private fun pauseTimer() {
        isTimerPaused = true
    }

    private fun resumeTimer() {
        isTimerPaused = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    private fun startTimer() {
        timerTask = MyTimerTask()
        timer.scheduleAtFixedRate(timerTask, 0, 1000)
        isTimerPaused = false // Set isTimerPaused to false when you start the timer
    }

    private fun stopTimer() {
        timer.cancel()
        super.onDestroy()
    }

    private inner class MyTimerTask : TimerTask() {
        override fun run() {
            if (!isTimerPaused) {
                secondsElapsed++
                val intent = Intent(timerUpdated)
                intent.putExtra(timerUpdated, secondsElapsed)
                sendBroadcast(intent)
            }
        }
    }
}