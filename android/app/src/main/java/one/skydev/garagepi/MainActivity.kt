package one.skydev.garagepi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var failCount = 0
    private val MAX_FAILS = 5
    private lateinit var doorController : DoorController

    private val updateDoorStatusHandler : Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            handleDoorMessage(msg)
        }
    }

    private fun handleDoorMessage(msg: Message) {
        val bundle = msg.data
        val status = bundle.get("status")
        val statusTextView = findViewById<TextView>(R.id.statusTextView)
        val statusSpinner = findViewById<ProgressBar>(R.id.statusSpinner)
        val statusIcon = findViewById<ImageView>(R.id.statusIcon)

        when(status) {
            DoorController.DoorStatus.OPEN -> {
                failCount = 0
                statusTextView.text = getString(R.string.open)
                statusSpinner.visibility = View.INVISIBLE
                statusIcon.setImageResource(R.drawable.ic_online)
                statusIcon.visibility = View.VISIBLE
                toggleButton.text = getString(R.string.closedoor)
                toggleButton.visibility = View.VISIBLE
            }
            DoorController.DoorStatus.CLOSED -> {
                failCount = 0
                statusTextView.text = getString(R.string.close)
                statusSpinner.visibility = View.INVISIBLE
                statusIcon.setImageResource(R.drawable.ic_offline)
                statusIcon.visibility = View.VISIBLE
                toggleButton.text = getString(R.string.opendoor)
                toggleButton.visibility = View.VISIBLE
            }
            DoorController.DoorStatus.LOADING -> {
                statusSpinner.visibility = View.VISIBLE
                statusIcon.visibility = View.INVISIBLE
                toggleButton.visibility = View.INVISIBLE
            }
            else -> {
                if (failCount < MAX_FAILS) {
                    failCount++
                    return
                }
                statusTextView.text = getString(R.string.error)
                statusSpinner.visibility = View.INVISIBLE
                toggleButton.visibility = View.INVISIBLE
                statusIcon.setImageResource(R.drawable.ic_error)
                statusIcon.visibility = View.VISIBLE
            }
        }

        findViewById<TextView>(R.id.lastUpdatedTime).text = Calendar.getInstance().time.toString()
    }

    private fun updateDoorStatus(status : DoorController.DoorStatus) {
        val message = Message.obtain()
        val bundle = Bundle()
        bundle.putSerializable("status", status)
        message.data = bundle
        handleDoorMessage(message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateDoorStatus(DoorController.DoorStatus.LOADING)

        if (getGoogleIdTokenFromSharedPrefs() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        doorController = DoorController(updateDoorStatusHandler)
        refreshHandler.post(object : Runnable {
            override fun run() {
                doorController.getDoorStatus(getGoogleIdTokenFromSharedPrefs())
                refreshHandler.postDelayed(this, 5000)
            }
        })
    }

    fun onToggleButtonClick(view : View) {
        var cmd = DoorController.DoorCommand.CLOSE
        if (toggleButton.text == getString(R.string.opendoor)) {
            cmd = DoorController.DoorCommand.OPEN
        }
        doorController.sendDoorCommand(getGoogleIdTokenFromSharedPrefs(), cmd)
    }

    fun getGoogleIdTokenFromSharedPrefs() : String? {
        // TODO: better validation of key
        // TODO: If key is bad, login again
        return getSharedPreferences(getString(R.string.sharedprefskey), MODE_PRIVATE).getString(
            getString(R.string.googleloginkey), null)
    }
}