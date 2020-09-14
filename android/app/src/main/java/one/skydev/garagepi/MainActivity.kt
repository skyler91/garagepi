package one.skydev.garagepi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var failCount = 0
    private val MAX_FAILS = 5
    private val RC_SIGN_IN: Int = 1
    private val accountObserver : MutableLiveData<GoogleSignInAccount> = MutableLiveData()
    private lateinit var googleSignInClient : GoogleSignInClient
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

        accountObserver.observe(this, Observer {
            updateSignInStatus()
        })

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(BuildConfig.CLIENT_API_KEY)
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        silentSignIn()
        accountObserver.value = GoogleSignIn.getLastSignedInAccount(this)
        if (accountObserver.value == null) {
            signIn()
        }
        // TODO: Add to settings, separate port

        doorController = DoorController(updateDoorStatusHandler)
        /*
        refreshHandler.post(object : Runnable {
            override fun run() {
                doorController.getDoorStatus()
                refreshHandler.postDelayed(this, 5000)
            }
        })
         */
    }

    fun onToggleButtonClick(view : View) {
        var cmd = DoorController.DoorCommand.CLOSE
        if (toggleButton.text == getString(R.string.opendoor)) {
            cmd = DoorController.DoorCommand.OPEN
        }
        doorController.sendDoorCommand(accountObserver.value?.idToken, cmd)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task : Task<GoogleSignInAccount>) {
        if (!task.isSuccessful) {
            val msg = getString(R.string.failedsignin) + task.exception?.message
            Log.e("ERROR", msg)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            accountObserver.value = null
            return
        }
        try {
            accountObserver.value = task.getResult(ApiException::class.java)
        }
        catch (e: ApiException) {
            val msg = "${getString(R.string.failedsignin)} ${e.message} (${e.statusCode})"
            Log.e("ERROR", msg)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            accountObserver.value = null
        }
    }

    private fun updateSignInStatus() {
        if (accountObserver.value != null) {
            // Login successful
            doorController.getDoorStatus(accountObserver.value?.idToken)
            // TODO: Create setting for refresh delay
            refreshHandler.removeCallbacksAndMessages(null)
            refreshHandler.post(object : Runnable {
                override fun run() {
                    doorController.getDoorStatus(accountObserver.value?.idToken)
                    refreshHandler.postDelayed(this, 5000)
                }
            })
        } else {
            updateDoorStatus(DoorController.DoorStatus.UNKNOWN)
            refreshHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun signIn() {
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    private fun silentSignIn() {
        googleSignInClient.silentSignIn().addOnCompleteListener(this) {
            task -> handleSignInResult(task)
        }
    }
}