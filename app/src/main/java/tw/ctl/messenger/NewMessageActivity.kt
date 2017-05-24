package tw.ctl.messenger

import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_new_message.*

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

    private fun fetchUsers() {
        FirebaseDatabase.getInstance().reference.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach { snapshot ->
                    if (FirebaseAuth.getInstance().currentUser?.uid == snapshot.key) return@forEach
                    val user = snapshot.getValue<User>(User::class.java)
                    user.id = snapshot.key
                    users.add(user)
                }
                adapter?.notifyDataSetChanged()
                progressView.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError?) {
                Log.e("Messenger", "Unable to fetch users: $error")
                progressView.visibility = View.GONE
            }
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
