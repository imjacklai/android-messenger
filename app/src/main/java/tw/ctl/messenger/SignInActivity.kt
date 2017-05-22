package tw.ctl.messenger

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private val RC_SIGN_IN = 9001
    private val auth = FirebaseAuth.getInstance()
    private var googleApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        setupGoogleApiClient()
        googleSignInButton.setOnClickListener { signIn() }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d("Messenger", "onConnectionFailed: $connectionResult")
        Toast.makeText(this, "Google Play Service 錯誤", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != RC_SIGN_IN) return

        val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

        if (result.isSuccess) {
            progressView.visibility = VISIBLE
            authWithGoogle(result.signInAccount)
        } else {
            showSignInError()
        }
    }

    private fun setupGoogleApiClient() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()

        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build()
    }

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
        googleSignInButton.isEnabled = false
    }

    private fun authWithGoogle(account: GoogleSignInAccount?) {
        if (account == null) {
            showSignInError()
            return
        }

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, { task ->
                    if (task.isSuccessful) {
                        saveUserToDatabase(auth.currentUser, account)
                    } else {
                        showSignInError()
                        Log.d("Messenger", "signInWithCredential:failure: ${task.exception}")
                    }
                })
    }

    private fun saveUserToDatabase(user: FirebaseUser?, account: GoogleSignInAccount?) {
        if (user == null || account == null) {
            showSignInError()
            return
        }

        val values = mutableMapOf("email" to account.email, "name" to account.displayName,
                "profileImageUrl" to account.photoUrl.toString())

        FirebaseDatabase.getInstance().reference.child("users").child(user.uid)
                .setValue(values, { error, _ ->
                    if (error != null) {
                        showSignInError()
                        Log.d("Messenger", "Unable to save user to database: $error")
                        return@setValue
                    }

                    finish()
        })
    }

    private fun showSignInError() {
        googleSignInButton.isEnabled = true
        progressView.visibility = GONE
        Toast.makeText(this, "無法使用Google登入", Toast.LENGTH_SHORT).show()
    }

}
