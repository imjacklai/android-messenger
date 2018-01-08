package tw.ctl.messenger.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.iid.FirebaseInstanceId
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_sign_in.*
import tw.ctl.messenger.Database
import tw.ctl.messenger.R

class SignInActivity : BaseActivity(), GoogleApiClient.OnConnectionFailedListener {

    private val signInRequestCode = 9001
    private val signInCancelledStatusCode = 12501

    private val auth = FirebaseAuth.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        setupGoogleSignInClient()
        googleSignInButton.setOnClickListener { signIn() }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Toast.makeText(this, "發生錯誤", Toast.LENGTH_SHORT).show()
        Logger.e("Google api client connection failed")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != signInRequestCode) return

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            val account = task.getResult(ApiException::class.java)
            authWithGoogle(account)
        } catch (e: ApiException) {
            showSignInError(e.statusCode != signInCancelledStatusCode)
            Logger.e("Google sign in failed: $e")
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    private fun setupGoogleSignInClient() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, signInRequestCode)
    }

    private fun authWithGoogle(account: GoogleSignInAccount?) {
        if (account == null) {
            Logger.e("account is null")
            return
        }

        googleSignInButton.isEnabled = false
        progressView.visibility = View.VISIBLE

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this, { task ->
            if (task.isSuccessful) {
                saveUser(auth.currentUser, account)
            } else {
                showSignInError()
                Logger.e("Google sign in failed")
            }
        })
    }

    private fun saveUser(user: FirebaseUser?, account: GoogleSignInAccount?) {
        if (user == null || account == null) {
            showSignInError()
            Logger.e("user is null or account is null")
            return
        }

        val registrationId = FirebaseInstanceId.getInstance().token ?: ""
        val values = mutableMapOf(
                "email" to account.email,
                "name" to account.displayName,
                "profileImageUrl" to account.photoUrl.toString(),
                "registrationId" to registrationId)

        Database.getInstance().createOrUpdateUser(auth.currentUser?.uid, values, success = {
            setResult(Activity.RESULT_OK)
            finish()
        }, failure = {
            showSignInError()
            Logger.e("Failed to create or update user for database")
        })
    }

    private fun showSignInError(show: Boolean = true) {
        googleSignInButton.isEnabled = true
        progressView.visibility = GONE
        if (show) {
            Toast.makeText(this, "登入發生錯誤", Toast.LENGTH_SHORT).show()
        }
    }

}
