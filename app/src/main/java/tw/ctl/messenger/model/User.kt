package tw.ctl.messenger.model

data class User(
        var id: String? = null,
        val name: String? = null,
        val email: String? = null,
        val profileImageUrl: String? = null,
        var lastMessage: String? = null,
        var timestamp: Long? = null
)
