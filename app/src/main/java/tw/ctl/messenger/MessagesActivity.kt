package tw.ctl.messenger

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_messages.*

class MessagesActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var googleApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)
        setupGoogleApiClient()

        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
        } else {
            newMessageButton.setOnClickListener { startActivity(Intent(this, NewMessageActivity::class.java)) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.messages_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item == null) { return super.onOptionsItemSelected(item) }

        if (item.itemId == R.id.signOut) {
            signOut()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d("Messenger", "onConnectionFailed: $connectionResult")
        Toast.makeText(this, "Google Play Service 錯誤", Toast.LENGTH_SHORT).show()
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

    private fun signOut() {
        auth.signOut()

        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback { _ ->
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }

}
