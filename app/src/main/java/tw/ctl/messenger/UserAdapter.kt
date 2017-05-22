package tw.ctl.messenger

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_user.view.*

class UserAdapter(val users: MutableList<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return UserViewHolder(layoutInflater.inflate(R.layout.item_user, parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bindUser(users[position])
    }

    override fun getItemCount(): Int {
        return users.size
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindUser(user: User) {
            with(user) {
                Glide.with(itemView.context)
                        .load(user.profileImageUrl)
                        .error(R.mipmap.ic_launcher_round)
                        .into(itemView.profileImage)
                itemView.name.text = user.name
            }
        }
    }

}
