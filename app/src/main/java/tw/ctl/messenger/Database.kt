package tw.ctl.messenger

import com.google.firebase.database.*
import tw.ctl.messenger.model.Message
import tw.ctl.messenger.model.User

class Database {

    private var reference = FirebaseDatabase.getInstance().reference

    companion object {
        private var database: Database? = null

        fun getInstance(): Database {
            if (database == null) database = Database()
            return database as Database
        }
    }

    fun createOrUpdateUser(uid: String?, values: MutableMap<String, String?>,
                           success: () -> Unit, failure: () -> Unit) {
        reference.child("users").child(uid).setValue(values, { error, _ ->
            if (error != null) {
                failure()
                return@setValue
            }
            success()
        })
    }

    fun fetchUserMessages(uid: String, onChildAdded: (snapshot: DataSnapshot?) -> Unit,
                          onCancelled: (error: DatabaseError?) -> Unit): Pair<DatabaseReference, ChildEventListener> {
        val listener = object : ChildEventListener {
            override fun onCancelled(error: DatabaseError?) {
                onCancelled(error)
            }

            override fun onChildMoved(p0: DataSnapshot?, p1: String?) {}

            override fun onChildChanged(p0: DataSnapshot?, p1: String?) {}

            override fun onChildAdded(snapshot: DataSnapshot?, p1: String?) {
                onChildAdded(snapshot)
            }

            override fun onChildRemoved(p0: DataSnapshot?) {}
        }

        val ref = reference.child("user-list").child(uid)
        ref.addChildEventListener(listener)

        return Pair(ref, listener)
    }

    fun fetchMessages(uid: String, partnerId: String, onChildAdded: (snapshot: DataSnapshot?) -> Unit,
                      onCancelled: (error: DatabaseError?) -> Unit): Pair<DatabaseReference, ChildEventListener> {
        val listener = object : ChildEventListener {
            override fun onCancelled(error: DatabaseError?) {
                onCancelled(error)
            }

            override fun onChildMoved(p0: DataSnapshot?, p1: String?) {}

            override fun onChildChanged(p0: DataSnapshot?, p1: String?) {}

            override fun onChildAdded(snapshot: DataSnapshot?, p1: String?) {
                onChildAdded(snapshot)
            }

            override fun onChildRemoved(p0: DataSnapshot?) {}
        }

        val ref = reference.child("user-list").child(uid).child(partnerId)
        ref.addChildEventListener(listener)

        return Pair(ref, listener)
    }

    fun fetchMessage(messageId: String, onData: (snapshot: DataSnapshot?) -> Unit,
                     onCancelled: (error: DatabaseError?) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                onData(snapshot)
            }

            override fun onCancelled(error: DatabaseError?) {
                onCancelled(error)
            }
        }

        reference.child("messages").child(messageId).addListenerForSingleValueEvent(listener)
    }

    fun fetchUser(message: Message, onData: (snapshot: DataSnapshot?) -> Unit,
                  onCancelled: (error: DatabaseError?) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                onData(snapshot)
            }

            override fun onCancelled(error: DatabaseError?) {
                onCancelled(error)
            }
        }

        reference.child("users").child(message.chatPartnerId()).addListenerForSingleValueEvent(listener)
    }

    fun fetchUsers(onData: (snapshot: DataSnapshot?) -> Unit,
                   onCancelled: (error: DatabaseError?) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                onData(snapshot)
            }

            override fun onCancelled(error: DatabaseError?) {
                onCancelled(error)
            }
        }

        reference.child("users").addListenerForSingleValueEvent(listener)
    }

    fun removeUserMessage(uid: String, user: User, success: () -> Unit, failure: (error: DatabaseError?) -> Unit) {
        reference.child("user-messages").child(uid).child(user.id).removeValue { error, _ ->
            if (error != null) {
                failure(error)
                return@removeValue
            }

            reference.child("user-list").child(uid).child(user.id).removeValue removeLabel@ { error2, _ ->
                if (error2 != null) {
                    failure(error)
                    return@removeLabel
                }

                success()
            }
        }
    }

}
