package tw.ctl.messenger.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_new_message.*
import tw.ctl.messenger.Database
import tw.ctl.messenger.R
import tw.ctl.messenger.adapter.UserAdapter
import tw.ctl.messenger.model.User

class NewMessageActivity : AppCompatActivity() {

    private val users: MutableList<User> = mutableListOf()
    private var adapter: UserAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)
        setupUI()
        fetchUsers()
    }

    private fun setupUI() {
        val manager = LinearLayoutManager(this)
        manager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = manager
        adapter = UserAdapter(users, itemClick = { user ->
            startChatActivity(user)
        }, itemLongClick = { _ -> false })
        recyclerView.adapter = adapter
    }

    private fun fetchUsers() {
        Database.getInstance().fetchUsers(onData = { snapshot ->
            snapshot?.children?.forEach { userSnapshot ->
                if (FirebaseAuth.getInstance().currentUser?.uid == userSnapshot.key) return@forEach
                val user = userSnapshot.getValue<User>(User::class.java) ?: return@forEach
                user.id = userSnapshot.key
                users.add(user)
            }
            adapter?.notifyDataSetChanged()
            progressView.visibility = View.GONE
        }, onCancelled = { error ->
            progressView.visibility = View.GONE
            Toast.makeText(this, "讀取失敗", Toast.LENGTH_SHORT).show()
            Logger.e("fetch users cancelled: $error")
        })
    }

    private fun startChatActivity(user: User) {
        val intent = Intent()
        val bundle = Bundle()
        bundle.putString("id", user.id)
        bundle.putString("name", user.name)
        bundle.putString("email", user.email)
        bundle.putString("profileImageUrl", user.profileImageUrl)
        intent.putExtras(bundle)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

}
