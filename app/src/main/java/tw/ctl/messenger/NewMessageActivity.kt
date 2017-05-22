package tw.ctl.messenger

import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
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
        adapter = UserAdapter(users)
        recyclerView.adapter = adapter
    }

    private fun fetchUsers() {
        FirebaseDatabase.getInstance().reference.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach { snapshot ->
                    val user = snapshot.getValue<User>(User::class.java)
                    user.id = snapshot.key
                    users.add(user)
                }
                adapter!!.notifyDataSetChanged()
                progressView.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Messenger", "Fetch users on cancelled: ${error.toException()}")
                progressView.visibility = View.GONE
            }
        })
    }

}
