package ee.taltech.a15


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Process


import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder


import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.Random
import java.util.Stack

class MainActivity : AppCompatActivity() {
    private val buttonList = mutableListOf<Button>()
    private val solvedOrder = (1..15).toList() + 0
    private var timerService: TimerService? = null
    private var turnCount = 0
    private val moveStack = Stack<Pair<Button, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        for (i in 1..16) {
            val buttonId = resources.getIdentifier("button$i", "id", packageName)
            val button = findViewById<Button>(buttonId)
            buttonList.add(button)
            button.isClickable = false
            button.visibility = View.INVISIBLE
            button.setOnClickListener {
                handleButtonClick(button)

            }
        }

        val scrambleButton = findViewById<Button>(R.id.scrambleButton)
        scrambleButton.setOnClickListener {
            scrambleBoard()
            buttonStartTimerService()
            turnCount = 0
        }
        val PauseButton = findViewById<Button>(R.id.buttonPause)
        PauseButton.setOnClickListener {
            buttonPauseService()
        }
        val EndButton = findViewById<Button>(R.id.buttonEnd)
        EndButton.setOnClickListener {
            buttonStopTimerService()
            closeApplication(this)
        }
        val ResumeButton = findViewById<Button>(R.id.ButtonResume)
        ResumeButton.setOnClickListener {
            buttonResumeService()
        }
        val buttonUndo = findViewById<Button>(R.id.ButtonUndo)
        buttonUndo.setOnClickListener {
            if(turnCount != 0) {
                undoLastMove()
            }
        }


        // Retrieve the last known timer value from shared preferences
        val sharedPreferences = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        val timerValue = sharedPreferences.getInt("timer_value", 0)

        // Update the timer value in your UI
        val timerTextView = findViewById<TextView>(R.id.timerTextView)
        val formattedTime = String.format("%02d:%02d", timerValue / 60, timerValue % 60)
        timerTextView.text = formattedTime
        val timerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val timerValue = intent?.getIntExtra("timer_value", 0)
                val timerTextView = findViewById<TextView>(R.id.timerTextView)

                // Display the timer value in the TextView
                val formattedTime = if (timerValue != null) {
                    String.format("%02d:%02d", timerValue / 60, timerValue % 60)
                } else {
                    "00:00" // Default value if timerValue is null
                }
                timerTextView.text = formattedTime

            }
        }

        val timerIntentFilter = IntentFilter("timer_update")
        registerReceiver(timerReceiver, timerIntentFilter)

    }


    private fun handleButtonClick(button: Button) {
        val clickedButtonId = buttonList.indexOf(button)
        val emptyButtonIndex = buttonList.indexOfFirst { it.text.toString().isBlank() }
        val difference = clickedButtonId - emptyButtonIndex
        if ((difference == 1 && clickedButtonId / 4 == (clickedButtonId - 1) / 4) ||
            (difference == -1 && clickedButtonId / 4 == (clickedButtonId + 1) / 4) ||
            (difference == 4 && clickedButtonId % 4 == (clickedButtonId - 4) % 4) ||
            (difference == -4 && clickedButtonId % 4 == (clickedButtonId + 4) % 4)) {
            turnCount++

            val move = Pair(button, emptyButtonIndex)

            moveStack.push(Pair(button, emptyButtonIndex))
            val clickedButtonText = button.text
            button.visibility = View.INVISIBLE
            button.text = ""
            buttonList[emptyButtonIndex].text = clickedButtonText
            buttonList[emptyButtonIndex].visibility = View.VISIBLE
            checkButtonText(button)
            if (checkIfPuzzleIsSolved()) {
                buttonStopTimerService()
                println("Puzzle is solved!")
                restartApp(this)
            }


            val turnsMadeTextView = findViewById<TextView>(R.id.TextViewTurnsMade)
            turnsMadeTextView.text = "Turns made: $turnCount"



        }
    }

    private fun checkButtonText(button: Button)
    {
        for (i in 0 until buttonList.size) {
            val button = buttonList[i]
            val buttonId = i + 1
            val buttonText = button.text.toString()
            if (buttonText.isNotEmpty() && buttonText.toInt() == buttonId) {
                button.setBackgroundColor(Color.GREEN)
            } else {
                button.setBackgroundColor(Color.WHITE)
            }
        }
    }

    private fun checkIfPuzzleIsSolved(): Boolean {
        return buttonList.withIndex().all { (i, button) ->
            val buttonText = button.text.toString()
            buttonText == solvedOrder[i].toString() || buttonText.isBlank()
        }
    }
    private fun scrambleBoard() {
        val random = Random()
        val indices = (0 until 16).toMutableList()
        indices.shuffle(random)

        for (i in 0 until 15) {
            val currentIndex = indices[i]
            val randomIndex = indices[i + 1]

            val currentText = buttonList[currentIndex].text
            buttonList[currentIndex].text = buttonList[randomIndex].text
            buttonList[randomIndex].text = currentText

        }
        for (button in buttonList) {
            button.isClickable = true
            button.visibility = View.VISIBLE
        }
        checkButtonColors()

    }
    private fun buttonStopTimerService() {
        val stopServiceIntent = Intent(this, TimerService::class.java)
        stopService(stopServiceIntent)
    }
    private fun buttonStartTimerService() {
        val startServiceIntent = Intent(this, TimerService::class.java)
        startService(startServiceIntent)
        connectService()
        timerService?.resumeTimer()
    }
    private fun buttonPauseService() {
        connectService()
        timerService?.pauseTimer()


    }
    private fun buttonResumeService() {
        connectService()
        timerService?.resumeTimer()

    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
        }
    }


    private fun connectService() {
        val serviceIntent = Intent(this, TimerService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun restartApp(context: Context) {
        // Stop the timer before restarting the app
        timerService?.pauseTimer()

        // Restart the app
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }
    fun closeApplication(activity: Activity) {
        // Finish all open activities
        activity.finishAffinity()

        // Exit the application process
        Process.killProcess(Process.myPid())
    }

    private fun undoLastMove() {
        if (moveStack.isNotEmpty()) {
            val (button, originalIndex) = moveStack.pop()
            val emptyButtonIndex = buttonList.indexOfFirst { it.text.toString().isBlank() }

            // Swap the button text and visibility back to the original state
            val buttonText = button.text
            button.text = buttonList[originalIndex].text
            buttonList[originalIndex].text = buttonText

            button.visibility = View.VISIBLE
            buttonList[originalIndex].visibility = View.INVISIBLE
            checkButtonText(button)


            turnCount--
            val turnsMadeTextView = findViewById<TextView>(R.id.TextViewTurnsMade)
            turnsMadeTextView.text = "Turns made: $turnCount"
        }
    }
    private fun checkButtonColors() {
        for (button in buttonList) {
            val buttonId = buttonList.indexOf(button) + 1
            val buttonText = button.text.toString()
            if (buttonText.isNotEmpty() && buttonText.toInt() == buttonId) {
                button.setBackgroundColor(Color.GREEN)
            } else {
                button.setBackgroundColor(Color.WHITE)
            }
        }
    }


}




