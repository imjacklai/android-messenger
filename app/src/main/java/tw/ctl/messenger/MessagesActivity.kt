package tw.ctl.messenger

import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.format.DateUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_messages.*
import java.util.*

class MessagesActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private val RC_SIGN_IN = 9001
    private val RC_NEW_MESSAGE = 9002
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val users: MutableList<User> = mutableListOf()
    private val messages: MutableList<Message> = mutableListOf()
    private val messagesMap: MutableMap<String, Message> = mutableMapOf()
    private var adapter: UserAdapter? = null
    private var googleApiClient: GoogleApiClient? = null
    private val handler = Handler()
    private val task = Runnable {
        messages.clear()
        users.clear()
        messages.addAll(messagesMap.values)
        messages.sortByDescending { it.timestamp }
        messages.map { message -> fetchUser(message) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)
        setupGoogleApiClient()

        val currentUser = auth.currentUser

        if (currentUser == null) {
            val intent = Intent(this, SignInActivity::class.java)
            startActivityForResult(intent, RC_SIGN_IN)
        } else {
            setupUI()
            fetchUserMessages(currentUser.uid)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == RC_SIGN_IN) {
            setupUI()
            fetchUserMessages(auth.currentUser!!.uid)
        } else if (requestCode == RC_NEW_MESSAGE && data != null) {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtras(data.extras)
            startActivity(intent)
        }
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

    private fun setupUI() {
        newMessageButton.setOnClickListener {
            val intent = Intent(this, NewMessageActivity::class.java)
            startActivityForResult(intent, RC_NEW_MESSAGE)
        }

        progressView.indeterminateDrawable.setColorFilter(
                ContextCompat.getColor(this, R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN)

        val manager = LinearLayoutManager(this)
        manager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = manager
        adapter = UserAdapter(users, itemClick = { user ->
            startChatActivity(user)
        })
        recyclerView.adapter = adapter
    }

    private fun startChatActivity(user: User) {
        val bundle = Bundle()
        bundle.putString("id", user.id)
        bundle.putString("name", user.name)
        bundle.putString("email", user.email)
        bundle.putString("profileImageUrl", user.profileImageUrl)
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    private fun fetchUserMessages(uid: String) {
        FirebaseDatabase.getInstance().reference.child("user-messages").child(uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        dataSnapshot.children.forEach { snapshot ->
                            val userId = snapshot.key
                            fetchMessages(uid, userId)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showFetchError()
                        Log.d("Messenger", "Fetch user-messages self error: $error")
                    }
                })
    }

    private fun fetchMessages(uid: String, userId: String) {
        FirebaseDatabase.getInstance().reference.child("user-messages").child(uid).child(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        dataSnapshot.children.forEach { snapshot ->
                            val messageId = snapshot.key
                            fetchMessage(messageId)
                        }
                    }

                    override fun onCancelled(error: DatabaseError?) {
                        showFetchError()
                        Log.d("Messenger", "Fetch user-messages partner error: $error")
                    }
                })
    }

    private fun fetchMessage(messageId: String) {
        FirebaseDatabase.getInstance().reference.child("messages").child(messageId)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val message = snapshot.getValue<Message>(Message::class.java)
                        messagesMap.put(message.chatPartnerId(), message)
                        handler.removeCallbacks(task)
                        handler.postDelayed(task, 500)
                    }

                    override fun onCancelled(error: DatabaseError?) {
                        showFetchError()
                        Log.d("Messenger", "Fetch messages error: $error")
                    }
                })
    }

    private fun fetchUser(message: Message) {
        FirebaseDatabase.getInstance().reference.child("users").child(message.chatPartnerId())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue<User>(User::class.java)
                        user.id = snapshot.key
                        user.lastMessage = message.text
                        user.timestamp = "" + DateUtils.getRelativeTimeSpanString(message.timestamp!!, Date().time, DateUtils.SECOND_IN_MILLIS)
                        users.add(user)
                        adapter?.notifyDataSetChanged()
                        progressView.visibility = View.GONE
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showFetchError()
                        Log.d("Messenger", "Fetch user error: $error")
                    }
                })
    }

    private fun showFetchError() {
        progressView.visibility = View.GONE
        Toast.makeText(this, "讀取失敗", Toast.LENGTH_SHORT).show()
    }

}
