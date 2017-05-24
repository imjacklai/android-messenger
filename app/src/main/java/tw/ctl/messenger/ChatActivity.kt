package tw.ctl.messenger

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chat.*
import java.util.*

class ChatActivity : AppCompatActivity() {

    private var toUser: User? = null
    private var adapter: MessageAdapter? = null
    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val bundle = intent.extras ?: return

        toUser = User(
                bundle.getString("id"),
                bundle.getString("name"),
                bundle.getString("email"),
                bundle.getString("profileImageUrl")
        )

        setupUI()
        fetchUserMessages()
    }

    private fun setupUI() {
        toolbar.title = toUser!!.name

        sendButton.setOnClickListener {
            val message = input.text.toString()
            if (message == "") return@setOnClickListener
            send(message)
        }

        val manager = LinearLayoutManager(this)
        manager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = manager
        adapter = MessageAdapter(messages)
        recyclerView.adapter = adapter

        // Scroll to bottom when keyboard show up.
        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) manager.smoothScrollToPosition(recyclerView, null, adapter!!.itemCount)
        }
    }

    private fun fetchUserMessages() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        FirebaseDatabase.getInstance().reference.child("user-messages").child(uid).child(toUser?.id)
                .addChildEventListener(object : ChildEventListener {
                    override fun onCancelled(error: DatabaseError?) {
                        Log.e("Messenger", "Unable to fetch user messages: $error")
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
                        messages.add(message)
                        adapter?.notifyItemInserted(messages.indexOf(message))
                        recyclerView.smoothScrollToPosition(messages.size - 1)
                    }

                    override fun onCancelled(error: DatabaseError?) {
                        Log.e("Messenger", "Unable to fetch message: $error")
                    }
                })
    }

    private fun send(message: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("messages").push()
        val toId = toUser?.id
        val fromId = FirebaseAuth.getInstance().currentUser?.uid
        val timestamp = Calendar.getInstance().timeInMillis
        val values = mutableMapOf("toId" to toId, "fromId" to fromId, "timestamp" to timestamp, "text" to message)

        reference.updateChildren(values, { error, _ ->
            if (error != null) {
                Toast.makeText(this, "訊息傳送失敗", Toast.LENGTH_SHORT).show()
                Log.d("Messenger", "Unable to save message: $error")
                return@updateChildren
            }

            val messageId = reference.key

            FirebaseDatabase.getInstance().reference
                    .child("user-messages").child(fromId).child(toId)
                    .updateChildren(mutableMapOf(messageId to 1) as Map<String, Any>?)

            FirebaseDatabase.getInstance().reference
                    .child("user-messages").child(toId).child(fromId)
                    .updateChildren(mutableMapOf(messageId to 1) as Map<String, Any>?)

            input.setText("")
        })
    }

}
