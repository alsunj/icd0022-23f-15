package ee.taltech.a15

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TimerBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val timerUpdated = TimerActions.timerUpdated
        const val timerPaused = TimerActions.timerPaused
        const val timerResumed = TimerActions.timerResumed
        const val timerStopped = TimerActions.timerStopped
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            timerUpdated -> {
                val secondsElapsed = intent.getIntExtra(timerUpdated, 0)
                Log.d("TimerBroadcastReceiver", "Timer updated broadcast received with secondsElapsed: $secondsElapsed")
            }
            timerPaused -> {
                // Pause the timer
                val pauseIntent = Intent(context, TimerService::class.java)
                pauseIntent.action = timerPaused
                context.startService(pauseIntent)
            }
            timerResumed -> {
                val resumeIntent = Intent(context, TimerService::class.java)
                resumeIntent.action = timerResumed
                context.startService(resumeIntent)
            }
            timerStopped -> {
                // Stop the timer
                val stopIntent = Intent(context, TimerService::class.java)
                stopIntent.action = timerStopped
                context.startService(stopIntent)
            }
        }
    }
}