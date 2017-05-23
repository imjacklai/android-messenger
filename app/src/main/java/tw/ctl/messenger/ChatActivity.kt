package tw.ctl.messenger

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_chat.*
import java.util.*

class ChatActivity : AppCompatActivity() {

    private var toUser: User? = null

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
    }

    private fun setupUI() {
        toolbar.title = toUser!!.name

        sendButton.setOnClickListener {
            val message = input.text.toString()
            if (message == "") return@setOnClickListener
            send(message)
        }
    }

    private fun send(message: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("messages").push()
        val toId = toUser?.id
        val fromId = FirebaseAuth.getInstance().currentUser?.uid
        val timestamp = Calendar.getInstance().timeInMillis
        val values = mutableMapOf("toid" to toId, "fromId" to fromId, "timestamp" to timestamp, "text" to message)

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
