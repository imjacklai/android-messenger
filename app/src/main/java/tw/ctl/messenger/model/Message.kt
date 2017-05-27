package tw.ctl.messenger.model

import com.google.firebase.auth.FirebaseAuth

data class Message(
        val fromId: String? = null,
        val toId: String? = null,
        val text: String? = null,
        val timestamp: Long? = null,
        val imageUrl: String? = null,
        val imageWidth: Int? = null,
        val imageHeight: Int? = null
) {

    fun chatPartnerId(): String {
        return if (fromId == FirebaseAuth.getInstance().currentUser?.uid) toId!! else fromId!!
    }

}
