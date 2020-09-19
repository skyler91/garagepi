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
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import java.util.*

class MainActivity : AppCompatActivity() {
    private var failCount = 0
    private val maxRetries = 5
    private var previousStatus = DoorController.DoorStatus.LOADING
    private lateinit var doorController : DoorController

    private val updateDoorStatusHandler : Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            handleDoorMessage(msg)
        }
    }

    private fun handleDoorMessage(msg: Message) {
        val bundle = msg.data
        val status = bundle.get("status") as DoorController.DoorStatus
        val statusTextView = findViewById<TextView>(R.id.statusTextView)
        val statusSpinner = findViewById<ProgressBar>(R.id.statusSpinner)
        val statusIcon = findViewById<ImageView>(R.id.statusIcon)
        val statusGifImageView = findViewById<GifImageView>(R.id.garagedoorgif)

        // TODO: Clean up this logic, perhaps with a more robust DoorStatus class?
        when(status) {
            DoorController.DoorStatus.OPEN -> {
                failCount = 0
                statusSpinner.visibility = View.INVISIBLE
                statusIcon.visibility = View.INVISIBLE

                when(previousStatus) {
                    DoorController.DoorStatus.LOADING, DoorController.DoorStatus.UNKNOWN -> {
                        statusGifImageView.setImageResource(R.drawable.garagedoorclose)
                        (statusGifImageView.drawable as GifDrawable).stop()
                    }
                    DoorController.DoorStatus.CLOSED -> {
                        statusGifImageView.setImageResource(R.drawable.garagedooropen)
                        (statusGifImageView.drawable as GifDrawable).loopCount = 1
                    }
                    DoorController.DoorStatus.CLOSING -> {
                        // TODO: Handle case where door fails to close (cover OPENING case below too)
                        //       Track how long we have been in closing status?
                        return
                    }
                }
                statusTextView.text = getString(R.string.open)
                statusGifImageView.visibility = View.VISIBLE
                toggleButton.text = getString(R.string.closedoor)
                toggleButton.visibility = View.VISIBLE
            }
            DoorController.DoorStatus.CLOSED -> {
                failCount = 0
                statusTextView.text = getString(R.string.close)
                statusSpinner.visibility = View.INVISIBLE
                statusIcon.visibility = View.INVISIBLE
                when(previousStatus) {
                    DoorController.DoorStatus.LOADING, DoorController.DoorStatus.UNKNOWN -> {
                        statusGifImageView.setImageResource(R.drawable.garagedooropen)
                        (statusGifImageView.drawable as GifDrawable).stop()
                    }
                    DoorController.DoorStatus.OPEN -> {
                        statusGifImageView.setImageResource(R.drawable.garagedoorclose)
                        (statusGifImageView.drawable as GifDrawable).loopCount = 1
                    }
                    DoorController.DoorStatus.OPENING -> {
                        return
                    }
                }
                statusGifImageView.visibility = View.VISIBLE
                toggleButton.text = getString(R.string.opendoor)
                toggleButton.visibility = View.VISIBLE
            }
            DoorController.DoorStatus.OPENING -> {
                statusTextView.text = getString(R.string.opening)
                statusSpinner.visibility = View.INVISIBLE
                statusIcon.visibility = View.INVISIBLE
                statusGifImageView.setImageResource(R.drawable.garagedooropen)
                (statusGifImageView.drawable as GifDrawable).loopCount = 1
                statusGifImageView.visibility = View.VISIBLE
            }
            DoorController.DoorStatus.CLOSING -> {
                statusTextView.text = getString(R.string.closing)
                statusSpinner.visibility = View.INVISIBLE
                statusIcon.visibility = View.INVISIBLE
                statusGifImageView.setImageResource(R.drawable.garagedoorclose)
                (statusGifImageView.drawable as GifDrawable).loopCount = 1
                statusGifImageView.visibility = View.VISIBLE
            }
            DoorController.DoorStatus.LOADING -> {
                statusIcon.visibility = View.INVISIBLE
                statusSpinner.visibility = View.VISIBLE
                statusGifImageView.visibility = View.INVISIBLE
                toggleButton.visibility = View.INVISIBLE
            }
            else -> {
                if (failCount < maxRetries) {
                    failCount++
                    return
                }
                statusTextView.text = getString(R.string.error)
                statusSpinner.visibility = View.INVISIBLE
                toggleButton.visibility = View.INVISIBLE
                statusIcon.visibility = View.VISIBLE
                statusIcon.setImageResource(R.drawable.ic_error)
                statusGifImageView.visibility = View.INVISIBLE
            }
        }

        previousStatus = status
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
            Handler(Looper.getMainLooper()).post(object : Runnable {
                override fun run() {
                    doorController.getDoorStatus(getGoogleIdTokenFromSharedPrefs())
                    Handler(Looper.getMainLooper()).postDelayed(this, 5000)
                }
            })
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    fun onToggleButtonClick(view : View) {
        when(previousStatus) {
            DoorController.DoorStatus.CLOSED -> {
                updateDoorStatus(DoorController.DoorStatus.OPENING)
                doorController.sendDoorCommand(
                    getGoogleIdTokenFromSharedPrefs(),
                    DoorController.DoorCommand.OPEN
                )
            }
            DoorController.DoorStatus.OPEN -> {
                updateDoorStatus(DoorController.DoorStatus.CLOSING)
                doorController.sendDoorCommand(
                    getGoogleIdTokenFromSharedPrefs(),
                    DoorController.DoorCommand.CLOSE
                )
            }
        }
        view.isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed(Runnable { view.isEnabled = true }, 10000)
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