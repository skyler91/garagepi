package one.skydev.garagepi

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private val accountObserver : MutableLiveData<GoogleSignInAccount> = MutableLiveData()
    private lateinit var auth : FirebaseAuth
    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var gso : GoogleSignInOptions
    private val RC_SIGN_IN: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        accountObserver.observe(this, Observer {
            updateSignInStatus()
        })

        auth = Firebase.auth

        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener(this)
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(BuildConfig.CLIENT_API_KEY)
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        /*
        if (accountObserver.value == null) {
            silentSignIn()
        }*/
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            //val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            //handleSignInResult(task)
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account?.idToken!!)
            } catch (e: ApiException) {
                // Failed login
                updateUI(null)
            }
        }
    }

    private fun handleSignInResult(task : Task<GoogleSignInAccount>) {
        if (!task.isSuccessful) {
            val exception = task.exception
            if (exception is ApiException) {
                // Handle non-errors
                when(exception.statusCode) {
                    4 -> { // silent sign in failed
                        signIn()
                        return
                    }
                    12501 -> return // user closed sign in dialog
                }
            }
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

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun updateSignInStatus() {
        val prefs = getSharedPreferences(getString(R.string.sharedprefskey), Context.MODE_PRIVATE)
        if (accountObserver.value == null) {
            // Login failed. Clear idToken from prefs
            if (prefs.getString(getString(R.string.googleloginkey), null) != null) {
                with (prefs.edit()) {
                    putString(getString(R.string.googleloginkey), null)
                    commit()
                }
            }
            return
        }

        // Login successful
        with (prefs.edit()) {
            putString(getString(R.string.googleloginkey), accountObserver.value!!.idToken)
            commit()
        }
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun signIn() {
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    private fun silentSignIn() {
        val task = googleSignInClient.silentSignIn()
            task.addOnCompleteListener { handleSignInResult(task) }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.sign_in_button -> signIn()
        }
    }
}