package tw.ctl.messenger.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_messages.*
import tw.ctl.messenger.Database
import tw.ctl.messenger.R
import tw.ctl.messenger.adapter.UserAdapter
import tw.ctl.messenger.model.Message
import tw.ctl.messenger.model.User

class MessagesActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private val signInRequestCode = 9001
    private val newMessageRequestCode = 9002
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val users = mutableListOf<User>()
    private var adapter: UserAdapter? = null
    private var googleApiClient: GoogleApiClient? = null
    private var refListeners = mutableListOf<Pair<DatabaseReference, ChildEventListener>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)
        setupGoogleSignInClient()
        setupUI()

        val currentUser = auth.currentUser

        if (currentUser == null) {
            startSignInActivity()
        } else {
            fetchUserMessages(currentUser.uid)
        }

        // Receive notification data when app launch.
        handleNotification(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        detachListeners()
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
            showSignOutDialog()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == signInRequestCode) {
            fetchUserMessages(auth.currentUser?.uid)
        } else if (requestCode == newMessageRequestCode && data != null) {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtras(data.extras)
            startActivity(intent)
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Toast.makeText(this, "發生錯誤", Toast.LENGTH_SHORT).show()
        Logger.e("Google api client connection failed")
    }

    private fun setupGoogleSignInClient() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()

        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build()
    }

    private fun setupUI() {
        newMessageButton.setOnClickListener {
            val intent = Intent(this, NewMessageActivity::class.java)
            startActivityForResult(intent, newMessageRequestCode)
        }

        val manager = LinearLayoutManager(this)
        manager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = manager
        adapter = UserAdapter(users, itemClick = { user ->
            startChatActivity(user)
        }, itemLongClick = { user ->
            showDeleteDialog(user)
            true
        })
        recyclerView.adapter = adapter
    }

    private fun startSignInActivity() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivityForResult(intent, signInRequestCode)

        users.clear()
        adapter?.notifyDataSetChanged()
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

    private fun fetchUserMessages(uid: String?) {
        if (uid == null) return

        val refListenerPair = Database.getInstance().fetchUserMessages(uid, onChildAdded = { snapshot ->
            progressView.visibility = View.VISIBLE
            val userId = snapshot?.key
            if (userId != null) {
                fetchMessages(uid, userId)
            }
        }, onCancelled = { error ->
            showFetchError()
            Logger.e("fetch user messages cancelled: $error")
        })

        refListeners.add(refListenerPair)
    }

    private fun fetchMessages(uid: String, partnerId: String) {
        val refListenerPair = Database.getInstance().fetchMessages(uid, partnerId, onChildAdded = { snapshot ->
            val messageId = snapshot?.key
            if (messageId != null) {
                fetchMessage(messageId)
            }
        }, onCancelled = { error ->
            showFetchError()
            Logger.e("fetch messages cancelled: $error")
        })

        refListeners.add(refListenerPair)
    }

    private fun fetchMessage(messageId: String) {
        Database.getInstance().fetchMessage(messageId, onData = { snapshot ->
            val message = snapshot?.getValue<Message>(Message::class.java)
            if (message != null) {
                fetchMessageUser(message)
            }
        }, onCancelled = { error ->
            showFetchError()
            Logger.e("fetch message cancelled: $error")
        })
    }

    private fun fetchMessageUser(message: Message) {
        Database.getInstance().fetchUser(message, onData = { snapshot ->
            val user = snapshot?.getValue<User>(User::class.java) ?: return@fetchUser
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
        }, onCancelled = { error ->
            showFetchError()
            Logger.e("fetch user cancelled: $error")
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

    private fun showDeleteDialog(user: User) {
        AlertDialog.Builder(this)
                .setTitle("確定要刪除？")
                .setPositiveButton("確定", { _, _ -> removeUserMessage(user) })
                .setNegativeButton("取消", { _, _ -> })
                .show()
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(this)
                .setTitle("要記住您的帳號嗎？")
                .setPositiveButton("是", { _, _ -> signOut(true) })
                .setNegativeButton("否", { _, _ -> signOut(false)})
                .show()
    }

    private fun signOut(remember: Boolean) {
        detachListeners()

        auth.signOut()

        if (remember) {
            startSignInActivity()
        } else {
            Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback { _ ->
                startSignInActivity()
            }
        }
    }

    private fun detachListeners() {
        for (pair in refListeners) {
            pair.first.removeEventListener(pair.second)
        }
        refListeners.clear()
    }

    private fun removeUserMessage(user: User) {
        val uid = auth.currentUser?.uid ?: return

        Database.getInstance().removeUserMessage(uid, user, success = {
            users.remove(user)
            adapter?.notifyDataSetChanged()
        }, failure = { error ->
            Toast.makeText(this, "發生錯誤", Toast.LENGTH_SHORT).show()
            Logger.e("Failed to remove user messages: $error")
        })
    }

}
