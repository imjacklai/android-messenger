package tw.ctl.messenger.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_chat.*
import tw.ctl.messenger.Database
import tw.ctl.messenger.R
import tw.ctl.messenger.adapter.MessageAdapter
import tw.ctl.messenger.model.Message
import tw.ctl.messenger.model.User
import java.io.ByteArrayOutputStream
import java.util.*

class ChatActivity : AppCompatActivity() {

    private val pickImageRequestCode = 9001
    private var toUser: User? = null
    private var adapter: MessageAdapter? = null
    private val messages = mutableListOf<Message>()
    private var refListener: Pair<DatabaseReference, ChildEventListener>? = null

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

    override fun onDestroy() {
        super.onDestroy()
        if (refListener != null) {
            refListener?.first?.removeEventListener(refListener?.second)
        }
        refListener = null
        adapter = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != pickImageRequestCode || resultCode != Activity.RESULT_OK || data == null) return

        val selectedImage = data.data
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
        uploadImage(bitmap)
    }

    private fun setupUI() {
        toolbar.title = toUser?.name

        pickPhotoButton.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "選擇一張照片"), pickImageRequestCode)
        }

        sendButton.setOnClickListener {
            val message = input.text.toString()
            if (message == "") return@setOnClickListener
            sendText(message)
        }

        val manager = LinearLayoutManager(this)
        manager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = manager
        adapter = MessageAdapter(messages, toUser?.profileImageUrl)
        recyclerView.adapter = adapter

        // Scroll to bottom when keyboard show up.
        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val itemCount = adapter?.itemCount ?: return@addOnLayoutChangeListener
            if (bottom < oldBottom) manager.smoothScrollToPosition(recyclerView, null, itemCount)
        }

        recyclerView.setOnTouchListener { view, motionEvent ->
            input.clearFocus()
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            false
        }
    }

    private fun fetchUserMessages() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val partnerId = toUser?.id ?: return

        refListener = Database.getInstance().fetchChatMessages(uid, partnerId, onChildAdded = { snapshot ->
            val messageId = snapshot?.key
            fetchUserMessage(messageId)
        }, onCancelled = { error ->
            Toast.makeText(this, "讀取失敗", Toast.LENGTH_SHORT).show()
            Logger.e("fetch user messages cancelled: $error")
        })
    }

    private fun fetchUserMessage(messageId: String?) {
        if (messageId == null) return

        Database.getInstance().fetchMessage(messageId, onData = { snapshot ->
            val message = snapshot?.getValue<Message>(Message::class.java) ?: return@fetchMessage
            messages.add(message)
            adapter?.notifyItemInserted(messages.indexOf(message))
            recyclerView.smoothScrollToPosition(messages.size - 1)
        }, onCancelled = { error ->
            Toast.makeText(this, "讀取失敗", Toast.LENGTH_SHORT).show()
            Logger.e("fetch message cancelled: $error")
        })
    }

    private fun sendText(text: String) {
        send(mutableMapOf("text" to text))
    }

    private fun sendImage(imageUrl: String, image: Bitmap) {
        send(mutableMapOf("imageUrl" to imageUrl, "imageWidth" to image.width, "imageHeight" to image.height))
    }

    private fun send(properties: MutableMap<String, Any>) {
        val toId = toUser?.id
        val fromId = FirebaseAuth.getInstance().currentUser?.uid
        val timestamp = Calendar.getInstance().timeInMillis
        val values = mutableMapOf("toId" to toId, "fromId" to fromId, "timestamp" to timestamp)

        for ((key, value) in properties) { values[key] = value }

        Database.getInstance().sendMessage(values, fromId, toId, success = {
            input.setText("")
        }, failure = { error ->
            Toast.makeText(this, "訊息傳送失敗", Toast.LENGTH_SHORT).show()
            Logger.e("send message failed: $error")
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
                    Toast.makeText(this, "照片傳送失敗", Toast.LENGTH_SHORT).show()
                    Logger.e("Failed to upload image: ${exception.localizedMessage}")
                }
    }

}
