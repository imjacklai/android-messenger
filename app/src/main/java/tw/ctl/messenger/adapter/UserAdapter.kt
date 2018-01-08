package tw.ctl.messenger.adapter

import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_user.view.*
import tw.ctl.messenger.R
import tw.ctl.messenger.model.User
import java.util.*

class UserAdapter(
        private val users: MutableList<User>,
        val itemClick: (User) -> Unit,
        val itemLongClick: (User) -> Boolean) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return UserViewHolder(
                layoutInflater.inflate(R.layout.item_user, parent, false),
                itemClick,
                itemLongClick
        )
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bindUser(users[position])
    }

    override fun getItemCount(): Int {
        return users.size
    }

    class UserViewHolder(view: View,
                         private val itemClick: (User) -> Unit,
                         private val itemLongClick: (User) -> Boolean) : RecyclerView.ViewHolder(view) {
        fun bindUser(user: User) {
            with(user) {
                Glide.with(itemView.context)
                        .load(user.profileImageUrl)
                        .into(itemView.profileImage)

                itemView.name.text = user.name

                if (user.lastMessage == null) {
                    itemView.lastMessage.visibility = View.GONE
                } else {
                    itemView.lastMessage.visibility = View.VISIBLE
                    itemView.lastMessage.text = user.lastMessage
                }

                if (user.timestamp == null) {
                    itemView.timestamp.visibility = View.GONE
                } else {
                    itemView.timestamp.visibility = View.VISIBLE
                    itemView.timestamp.text = DateUtils.getRelativeTimeSpanString(
                            user.timestamp!!,
                            Date().time,
                            DateUtils.SECOND_IN_MILLIS)
                }

                itemView.setOnClickListener { itemClick(user) }
                itemView.setOnLongClickListener { itemLongClick(user) }
            }
        }
    }

}
