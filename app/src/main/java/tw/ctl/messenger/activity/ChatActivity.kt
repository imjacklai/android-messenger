package tw.ctl.messenger.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_chat.*
import tw.ctl.messenger.R
import tw.ctl.messenger.adapter.MessageAdapter
import tw.ctl.messenger.model.Message
import tw.ctl.messenger.model.User
import java.io.ByteArrayOutputStream
import java.util.*

class ChatActivity : AppCompatActivity() {

    private val RC_PICK_IMAGE = 9001
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != RC_PICK_IMAGE || resultCode != Activity.RESULT_OK || data == null) return

        val selectedImage = data.data
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
        uploadImage(bitmap)
    }

    private fun setupUI() {
        toolbar.title = toUser!!.name

        pickPhotoButton.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "選擇一張照片"), RC_PICK_IMAGE)
        }

        sendButton.setOnClickListener {
            val message = input.text.toString()
            if (message == "") return@setOnClickListener
            sendText(message)
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

    private fun sendText(text: String) {
        send(mutableMapOf("text" to text))
    }

    private fun sendImage(imageUrl: String, image: Bitmap) {
        send(mutableMapOf("imageUrl" to imageUrl, "imageWidth" to image.width, "imageHeight" to image.height))
    }

    private fun send(properties: MutableMap<String, Any>) {
        val reference = FirebaseDatabase.getInstance().reference.child("messages").push()
        val toId = toUser?.id
        val fromId = FirebaseAuth.getInstance().currentUser?.uid
        val timestamp = Calendar.getInstance().timeInMillis
        val values = mutableMapOf("toId" to toId, "fromId" to fromId, "timestamp" to timestamp)

        properties.forEach { key, value -> values[key] = value }

        reference.updateChildren(values, { error, _ ->
            if (error != null) {
                Toast.makeText(this, "訊息傳送失敗", Toast.LENGTH_SHORT).show()
                Log.e("Messenger", "Unable to save message: $error")
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

    private fun uploadImage(image: Bitmap) {
        val imageName = UUID.randomUUID().toString()
        val stream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 50, stream)
        val data = stream.toByteArray()

        FirebaseStorage.getInstance().reference.child("message_images").child(imageName).putBytes(data)
                .addOnSuccessListener { snapshot ->
                    val uri = snapshot.downloadUrl
                    sendImage(uri.toString(), image)
                }
                .addOnFailureListener { exception ->
                    Log.e("Messenger", "Unable to upload image: ${exception.localizedMessage}")
                }
    }

}
