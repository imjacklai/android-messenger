package tw.ctl.messenger.activity

import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
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
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_messages.*
import tw.ctl.messenger.R
import tw.ctl.messenger.adapter.UserAdapter
import tw.ctl.messenger.model.Message
import tw.ctl.messenger.model.User

class MessagesActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private val RC_SIGN_IN = 9001
    private val RC_NEW_MESSAGE = 9002
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val users: MutableList<User> = mutableListOf()
    private var adapter: UserAdapter? = null
    private var googleApiClient: GoogleApiClient? = null

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

        // Receive notification data when app launch.
        handleNotification(intent)
    }

    override fun onResume() {
        super.onResume()
        adapter?.notifyDataSetChanged()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Receive notification data when app in background.
        handleNotification(intent)
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
        Log.e("Messenger", "Google api client connection failed: $connectionResult")
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
            val intent = Intent(this, SignInActivity::class.java)
            startActivityForResult(intent, RC_SIGN_IN)
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
        FirebaseDatabase.getInstance().reference.child("user-list").child(uid)
                .addChildEventListener(object : ChildEventListener {
                    override fun onCancelled(error: DatabaseError?) {
                        showFetchError()
                        Log.e("Messenger", "Unable to fetch user messages (self): $error")
                    }

                    override fun onChildMoved(p0: DataSnapshot?, p1: String?) {

                    }

                    override fun onChildChanged(p0: DataSnapshot?, p1: String?) {

                    }

                    override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
                        progressView.visibility = View.VISIBLE
                        val userId = snapshot.key
                        fetchMessages(uid, userId)
                    }

                    override fun onChildRemoved(p0: DataSnapshot?) {

                    }
                })
    }

    private fun fetchMessages(uid: String, userId: String) {
        FirebaseDatabase.getInstance().reference.child("user-list").child(uid).child(userId)
                .addChildEventListener(object : ChildEventListener {
                    override fun onCancelled(error: DatabaseError?) {
                        showFetchError()
                        Log.e("Messenger", "Unable to fetch user messages (partner): $error")
                    }

                    override fun onChildMoved(p0: DataSnapshot?, p1: String?) {

                    }

                    override fun onChildChanged(p0: DataSnapshot?, p1: String?) {

                    }

                    override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
                        val messageId = snapshot.key
                        fetchMessage(messageId)
                    }

                    override fun onChildRemoved(p0: DataSnapshot?) {

                    }
                })
    }

    private fun fetchMessage(messageId: String) {
        FirebaseDatabase.getInstance().reference.child("messages").child(messageId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val message = snapshot.getValue<Message>(Message::class.java)
                        fetchUser(message)
                    }

                    override fun onCancelled(error: DatabaseError?) {
                        showFetchError()
                        Log.e("Messenger", "Unable to fetch message: $error")
                    }
                })
    }

    private fun fetchUser(message: Message) {
        FirebaseDatabase.getInstance().reference.child("users").child(message.chatPartnerId())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue<User>(User::class.java)
                        user.id = snapshot.key
                        user.timestamp = message.timestamp

                        if (message.imageUrl == null) {
                            user.lastMessage = message.text
                        } else {
                            if (message.fromId == FirebaseAuth.getInstance().currentUser?.uid) {
                                user.lastMessage = "你傳送一張圖片"
                            } else {
                                user.lastMessage = "對方傳送一張圖片"
                            }
                        }

                        users.filter { it.id == user.id }.forEach { users.remove(it) }
                        users.add(user)
                        users.sortByDescending { it.timestamp }
                        adapter?.notifyDataSetChanged()
                        progressView.visibility = View.GONE
                    }

                    override fun onCancelled(error: DatabaseError?) {
                        showFetchError()
                        Log.e("Messenger", "Unable to fetch user: $error")
                    }
                })
    }

    private fun showFetchError() {
        progressView.visibility = View.GONE
        Toast.makeText(this, "讀取失敗", Toast.LENGTH_SHORT).show()
    }

    private fun handleNotification(dataIntent: Intent?) {
        val userId = dataIntent?.getStringExtra("user_id") ?: return
        val userName = dataIntent.getStringExtra("user_name") ?: return
        val userEmail = dataIntent.getStringExtra("user_email") ?: return
        val userProfileImageUrl = dataIntent.getStringExtra("user_image") ?: return

        val bundle = Bundle()
        bundle.putString("id", userId)
        bundle.putString("name", userName)
        bundle.putString("email", userEmail)
        bundle.putString("profileImageUrl", userProfileImageUrl)
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
    }

}
