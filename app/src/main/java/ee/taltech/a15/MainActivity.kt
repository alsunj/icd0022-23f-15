@file:Suppress("SameParameterValue")

package ee.taltech.a15


import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import java.util.Stack


class MainActivity : AppCompatActivity() {
    private lateinit var gameBoardLayout: ConstraintLayout
    private lateinit var buttonOnClickListener: View.OnClickListener
    private lateinit var scrambleButton: Button
    private lateinit var endButton: ImageView
    private lateinit var testButton: Button
    private lateinit var exitButton: Button
    private lateinit var undoButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var turnsMadeTextView: TextView


    private var gameActiveState = "gameActive"
    private var gameActive = false
    private var firstStart = true
    private lateinit var board: gameBoard
    private lateinit var buttonMap: MutableMap<Int, String>
    private var moveStack = Stack<Pair<Int, Int>>()
    private var buttonWithNoTextId: Int? = null
    private var timerService: TimerService? = null
    private var turnCount = 0
    private var homeButtonClickCount = 0
    private var themeId = 0
    private var buttonIds = intArrayOf(
        R.id.button1,
        R.id.button2,
        R.id.button3,
        R.id.button4,
        R.id.button5,
        R.id.button6,
        R.id.button7,
        R.id.button8,
        R.id.button9,
        R.id.button10,
        R.id.button11,
        R.id.button12,
        R.id.button13,
        R.id.button14,
        R.id.button15,
        R.id.button16
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        themeId = theme()
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        initView()
        setBoard(savedInstanceState)
        checkAllButtonColors()
        timer()
        findEmpty()
        buttons()
    }

    private fun buttons() {
        startButton()
        menuButton()
        undoButton()
    }


    private fun theme(): Int {
        val sharedPrefs = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val isDarkTheme = sharedPrefs.getBoolean("isDarkTheme", false)
        return if (isDarkTheme) R.style.DarkTheme else R.style.LightTheme
    }

    private fun switchTheme(darkTheme: Boolean) {
        val mode =
            if (darkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)


        val sharedPrefs = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("isDarkTheme", darkTheme).apply()

        recreate()
    }

    private fun menuButton() {
        endButton.setOnClickListener {


            val popupMenu = PopupMenu(this@MainActivity, endButton)
            popupMenu.menuInflater.inflate(R.menu.main_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                buttonPauseService()

                when (menuItem.itemId) {
                    R.id.nav_home -> {
                        homeButtonClickCount++
                        val message = "Your luck is increased by $homeButtonClickCount times"
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }

                    R.id.nav_exit -> {
                        exitButton()
                    }

                    R.id.nav_Light -> {
                        switchTheme(false)
                    }

                    R.id.nav_Dark -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        switchTheme(true)
                    }
                }
                true
            }

            popupMenu.setOnDismissListener {
                buttonResumeService()
            }
            popupMenu.show()
        }
    }

    private fun exitButton() {
        exitButton.setOnClickListener {
            closeApplication(this)
            buttonStopTimerService()

        }
    }

    private fun buttonStopTimerService() {
        val stopServiceIntent = Intent(this, TimerService::class.java)
        stopService(stopServiceIntent)
    }


    private fun startButton() {
        scrambleButton.setOnClickListener {

            if (firstStart)
                buildGame()
            else resetGame()

        }

    }

    private fun undoButton() {
        undoButton.setOnClickListener {
            undoMove()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        gameActive = savedInstanceState.getBoolean(gameActiveState)
        if (gameActive) {
            activateBoard(true)
            val moveStackString = savedInstanceState.getString("moveStack")
            if (!moveStackString.isNullOrBlank()) {
                val pairs = moveStackString.split(",")
                if (pairs.size % 2 == 0) {
                    moveStack.clear()
                    for (i in pairs.indices step 2) {
                        try {
                            val first = pairs[i].toInt()
                            val second = pairs[i + 1].toInt()
                            moveStack.push(Pair(first, second))
                        } catch (_: NumberFormatException) {
                        }
                    }
                }

            }
        }
    }

    private fun checkButtonText(button: Button) {
        val buttonIdString = resources.getResourceEntryName(button.id)
        val buttonText = button.text.toString()
        val buttonId = buttonIdString.removePrefix("button").toInt()
        val isDarkTheme =
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        if (buttonText.isNotEmpty() && buttonText == buttonId.toString()) {
            val colorResId =
                if (isDarkTheme) R.color.DarkModeButtonSolved else R.color.lightModeButtonSolved
            val color = ContextCompat.getColor(this, colorResId)
            button.setBackgroundColor(color)
        } else {
            val defaultColorResId =
                if (isDarkTheme) R.color.DarkModeButton else R.color.lightModeButton
            val defaultColor = ContextCompat.getColor(this, defaultColorResId)
            button.setBackgroundColor(defaultColor)
        }
    }

    private fun checkAllButtonColors() {

        val isDarkTheme =
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        for (buttonId in buttonIds) {
            val button = findViewById<Button>(buttonId)
            val buttonIdString = resources.getResourceEntryName(buttonId)
            val buttonText = button.text.toString()
            val buttonIds = buttonIdString.removePrefix("button").toInt()

            if (buttonText.isNotEmpty() && buttonText == buttonIds.toString()) {

                val colorResId =
                    if (isDarkTheme) R.color.DarkModeButtonSolved else R.color.lightModeButtonSolved
                val color = ContextCompat.getColor(this, colorResId)
                button.setBackgroundColor(color)
            } else {

                val defaultColorResId =
                    if (isDarkTheme) R.color.DarkModeButton else R.color.lightModeButton
                val defaultColor = ContextCompat.getColor(this, defaultColorResId)
                button.setBackgroundColor(defaultColor)
            }
        }
    }


    private fun activateBoard(activate: Boolean) {
        if (activate) {
            buttonOnClickListener = View.OnClickListener { view ->
                val idString = resources.getResourceEntryName(view.id)
                handleButtonClick(idString)
            }
        }

        for (i in 0 until gameBoardLayout.childCount) {
            val button = gameBoardLayout.getChildAt(i)
            if (activate) button.setOnClickListener(buttonOnClickListener)
            else button.setOnClickListener(null)
        }
    }

    private fun checkButtonOrder() {
        var isCorrectOrder = true
        var isLastButtonEmpty = false

        for (i in 0 until buttonIds.size - 1) {
            val buttonId = buttonIds[i]
            val expectedText = (i + 1).toString()
            val button = findViewById<Button>(buttonId)
            val buttonText = button.text.toString()

            if (buttonText != expectedText) {
                isCorrectOrder = false
            }
        }

        val lastButtonId = buttonIds.last()
        val lastButton = findViewById<Button>(lastButtonId)
        val lastButtonText = lastButton.text.toString()

        if (lastButtonText.isEmpty()) {
            isLastButtonEmpty = true
        }

        if (isCorrectOrder && isLastButtonEmpty) {
            Toast.makeText(this, "Buttons are in the correct order!", Toast.LENGTH_SHORT).show()
            buttonStopTimerService()
            gameActive = false
        }
    }

    private fun drawBoard(newGame: Boolean = true) {
        val puzzle = board.createPuzzle()

        for ((i, buttonId) in buttonIds.withIndex()) {
            val buttonNumber = puzzle[i]

            val button = findViewById<Button>(buttonId)


            if (buttonNumber == 16) {

                if (buttonMap.size == 16 && !newGame) {
                    button.text = buttonMap[buttonId]
                } else {
                    button.text = ""
                    buttonMap[buttonId] = button.text as String
                }

                buttonMap[buttonId] = button.text as String
            } else {

                if (buttonMap.size == 16 && !newGame) {
                    button.text = buttonMap[buttonId]
                } else {
                    button.text = buttonNumber.toString()
                    buttonMap[buttonId] = button.text as String
                }
            }
        }
    }

    private fun closeApplication(activity: Activity) {

        activity.finishAffinity()
        Process.killProcess(Process.myPid())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (::buttonMap.isInitialized) {
            val keys = (buttonMap.keys).toIntArray()
            val values = buttonMap.values.toTypedArray()

            outState.putBoolean(gameActiveState, !firstStart)
            outState.putIntArray("buttonIds", keys)
            outState.putStringArray("buttonValues", values)

        }
        val moveStackString = moveStack.joinToString(",") { "${it.first},${it.second}" }
        outState.putString("moveStack", moveStackString)
    }

    private fun buildGame() {
        activateBoard(true)
        findEmpty()
        buttonStartTimerService()
        firstStart = false
    }

    private fun resetGame() {

        drawBoard()
        buildGame()
    }

    private fun findEmpty() {
        for (buttonId in buttonIds) {
            val button = findViewById<Button>(buttonId)
            if (button.text.isBlank()) {
                val buttonWithNoText = resources.getResourceEntryName(buttonId)
                buttonWithNoTextId = buttonWithNoText.split("button")[1].toInt()
                break
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun handleButtonClick(buttonId: String) {
        val buttonNumber = buttonId.split("button")[1].toInt()
        val emptyButtonId = buttonWithNoTextId
        val difference = buttonNumber - emptyButtonId!!

        if ((difference == 1 && emptyButtonId % 4 != 0) ||
            (difference == -1 && buttonNumber % 4 != 0) ||
            difference == 4 ||
            difference == -4
        ) {
            val emptyButtonR = buttonIds[buttonWithNoTextId!! - 1]
            val emptyButton = findViewById<Button>(emptyButtonR)
            val clickedButtonR = buttonIds[buttonNumber - 1]
            val clickedButton = findViewById<Button>(clickedButtonR)

            turnCount++
            moveStack.push(Pair(buttonNumber, emptyButtonId))
            emptyButton.visibility = View.VISIBLE
            emptyButton.text = clickedButton.text
            buttonMap[emptyButtonR] = emptyButton.text as String
            clickedButton.visibility = View.INVISIBLE
            clickedButton.text = ""
            buttonMap[clickedButtonR] = clickedButton.text as String
            buttonWithNoTextId = buttonNumber
            checkButtonText(emptyButton)
            checkButtonOrder()



            turnsMadeTextView.text = "Turns Made: $turnCount"
        }


    }

    private fun setBoard(savedInstanceState: Bundle?) {
        board = gameBoard()

        val buttonIdsArray = savedInstanceState?.getIntArray("buttonIds")
        val buttonValuesArray = savedInstanceState?.getStringArray("buttonValues")

        if (buttonIdsArray != null && buttonValuesArray != null) {
            buttonMap = mutableMapOf<Int, String>().apply {
                for (i in buttonIdsArray.indices) this[buttonIdsArray[i]] = buttonValuesArray[i]
            }

            drawBoard(false)
        } else {
            buttonMap = mutableMapOf()
            drawBoard(true)
        }

        gameActive = savedInstanceState?.getBoolean(gameActiveState) ?: false
        if (gameActive) {
            firstStart = false
            activateBoard(true)
        }


    }


    @SuppressLint("SetTextI18n")
    private fun undoMove() {
        Log.d("movestack", moveStack.toString())
        if (moveStack.isNotEmpty()) {
            val (buttonId, originalId) = moveStack.pop()
            val undoButton = findViewById<Button>(buttonIds[buttonId - 1])
            val undoEmptyButton = findViewById<Button>(buttonIds[originalId - 1])
            undoButton.text = undoEmptyButton.text
            undoButton.visibility = View.VISIBLE
            undoEmptyButton.text = ""
            undoEmptyButton.visibility = View.INVISIBLE
            buttonWithNoTextId = originalId


            val turnsMadeTextView = findViewById<TextView>(R.id.TextViewTurnsMade)
            turnCount--
            turnsMadeTextView.text = "Turns Made: $turnCount"

        }
    }

    private fun initView() {
        setContentView(R.layout.activity_main)

        scrambleButton = findViewById(R.id.scrambleButton)
        endButton = findViewById(R.id.buttonEnd)
        testButton = findViewById(R.id.buttontest)
        undoButton = findViewById(R.id.ButtonUndo)
        turnsMadeTextView = findViewById(R.id.TextViewTurnsMade)
        timerTextView = findViewById(R.id.timerTextView)
        gameBoardLayout = findViewById(R.id.gameBoardLayout)


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

    private fun buttonResumeService() {
        connectService()
        timerService?.resumeTimer()

    }

    private fun buttonPauseService() {
        connectService()
        timerService?.pauseTimer()


    }

    private fun restartTimerService() {
        connectService()
        timerService?.resetTimer()


    }


    private fun buttonStartTimerService() {
        val startServiceIntent = Intent(this, TimerService::class.java)
        startService(startServiceIntent)
        connectService()
        timerService?.resumeTimer()
    }

    private fun connectService() {
        val serviceIntent = Intent(this, TimerService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun timer() {
        restartTimerService()
        val sharedPreferences = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        val timerValue = sharedPreferences.getInt("timer_value", 0)


        val formattedTime = String.format("%02d:%02d", timerValue / 60, timerValue % 60)
        timerTextView.text = formattedTime
        val timerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val timerValues = intent?.getIntExtra("timer_value", 0)
                val formattedTimes = if (timerValues != null) {
                    String.format("%02d:%02d", timerValues / 60, timerValues % 60)
                } else {
                    "00:00"
                }
                timerTextView.text = formattedTimes

            }
        }

        val timerIntentFilter = IntentFilter("timer_update")
        registerReceiver(timerReceiver, timerIntentFilter)
    }


}



