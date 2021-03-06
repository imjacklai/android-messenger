package tw.ctl.messenger.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import tw.ctl.messenger.R
import tw.ctl.messenger.activity.ChatActivity

class MessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        /**
         * Wake lock screen.
         */
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "Tag")
        wakeLock.acquire()
        wakeLock.release()

        /**
         * Get data from notification.
         */
        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body

        val userId = remoteMessage.data["user_id"]
        val userName = remoteMessage.data["user_name"]
        val userEmail = remoteMessage.data["user_email"]
        val userProfileImageUrl = remoteMessage.data["user_image"]

        val bundle = Bundle()
        bundle.putString("id", userId)
        bundle.putString("name", userName)
        bundle.putString("email", userEmail)
        bundle.putString("profileImageUrl", userProfileImageUrl)

        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtras(bundle)
        /**
         * Intent.FLAG_ACTIVITY_NEW_TASK:   在堆疊中開啟一個新的任務
         * Intent.FLAG_ACTIVITY_SINGLE_TOP: 將Activity顯示在最上層
         * Intent.FLAG_ACTIVITY_CLEAR_TOP:  當前Activity會被新的Intent覆蓋
         */
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        /**
         * FLAG_UPDATE_CURRENT: 開啟新的Intent時，將自動更新extra資料並取代舊有的資料
         */
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(0, notificationBuilder.build())
    }

}
