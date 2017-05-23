package tw.ctl.messenger

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_chat.*

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
            
        }
    }

}
