package tw.ctl.messenger

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

class InstanceIDService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        super.onTokenRefresh()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val values = mutableMapOf("registrationId" to FirebaseInstanceId.getInstance().token)

        FirebaseDatabase.getInstance().reference.child("users").child(uid)
                .setValue(values, { error, _ ->
                    if (error != null) {
                        Log.e("Messenger", "Unable to save user: $error")
                        return@setValue
                    }

                    Log.d("Messenger", "registration id uploaded")
                })
    }

}
