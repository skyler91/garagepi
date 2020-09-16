package one.skydev.garagepi

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
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

        GlobalScope.async {validateIdToken()}
    }

    private fun continueCreation(validToken : Boolean) {
        if (validToken) {
            doorController = DoorController(updateDoorStatusHandler)
            refreshHandler.post(object : Runnable {
                override fun run() {
                    doorController.getDoorStatus(getGoogleIdTokenFromSharedPrefs())
                    refreshHandler.postDelayed(this, 5000)
                }
            })
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    fun onToggleButtonClick(view : View) {
        var cmd = DoorController.DoorCommand.CLOSE
        if (toggleButton.text == getString(R.string.opendoor)) {
            cmd = DoorController.DoorCommand.OPEN
        }
        doorController.sendDoorCommand(getGoogleIdTokenFromSharedPrefs(), cmd)
    }

    fun getGoogleIdTokenFromSharedPrefs() : String? {
        return getSharedPreferences(getString(R.string.sharedprefskey), MODE_PRIVATE).getString(
            getString(R.string.googleloginkey), null)
    }

    private fun validateIdToken() {
        val cachedToken = getGoogleIdTokenFromSharedPrefs()
        if (cachedToken == null) {
            continueCreation(false)
        }
        val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), JacksonFactory())
            .setAudience(Collections.singletonList(BuildConfig.CLIENT_API_KEY))
            .build()
        val idToken = verifier.verify(cachedToken)
        continueCreation(idToken != null)
    }
}