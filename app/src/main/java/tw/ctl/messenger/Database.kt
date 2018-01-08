package tw.ctl.messenger

import com.google.firebase.database.FirebaseDatabase

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

}
