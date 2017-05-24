package tw.ctl.messenger

import android.graphics.Color
import android.support.constraint.ConstraintSet
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.item_message.view.*

class MessageAdapter(val messages: MutableList<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return MessageViewHolder(layoutInflater.inflate(R.layout.item_message, parent, false))
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bindMessage(messages[position])
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindMessage(message: Message) {
            with(message) {
                if (message.fromId == FirebaseAuth.getInstance().currentUser?.uid) {
                    // Self message.
                    itemView.profileImage.visibility = View.GONE
                    itemView.message.setBackgroundResource(R.drawable.self_message_bubble_bg)
                    itemView.message.setTextColor(Color.WHITE)
                    updateConstraint(true)
                } else {
                    // Partner message.
                    FirebaseDatabase.getInstance().reference.child("users").child(message.fromId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val user = snapshot.getValue<User>(User::class.java)
                                    Glide.with(itemView.context)
                                            .load(user.profileImageUrl)
                                            .error(R.mipmap.ic_launcher_round)
                                            .into(itemView.profileImage)
                                }

                                override fun onCancelled(error: DatabaseError?) {
                                    Log.e("Messenger", "Unable to fetch user: $error")
                                }
                            })

                    itemView.profileImage.visibility = View.VISIBLE
                    itemView.message.setBackgroundResource(R.drawable.partner_message_bubble_bg)
                    itemView.message.setTextColor(Color.BLACK)
                    updateConstraint(false)
                }

                itemView.message.text = message.text
            }
        }

        private fun updateConstraint(isSelf: Boolean) {
            val layout = itemView.constraintLayout
            val set = ConstraintSet()
            set.clone(layout)
            set.clear(itemView.message.id, ConstraintSet.LEFT)
            set.clear(itemView.message.id, ConstraintSet.RIGHT)

            if (isSelf) {
                set.connect(itemView.message.id, ConstraintSet.RIGHT, layout.id, ConstraintSet.RIGHT)
            } else {
                set.connect(itemView.message.id, ConstraintSet.LEFT, itemView.profileImage.id, ConstraintSet.RIGHT)
            }

            set.applyTo(layout)
        }
    }

}
