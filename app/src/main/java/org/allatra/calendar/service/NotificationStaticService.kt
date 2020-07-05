package org.allatra.calendar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.allatra.calendar.R
import org.allatra.calendar.ui.activity.CalendarActivity
import timber.log.Timber

object NotificationStaticService {
    private lateinit var notificationManagerCompat: NotificationManagerCompat
    private var currentNotificationIdNumber = 0
    private var appContext: Context? = null

    private const val CHANNEL_ID = "CHANNEL_NOTIF_124"
    private const val CHANNEL_NAME = "CHANNEL_NOTIF_SOUL_CAL"
    private const val NOTIFICATION_ID_DEFAULT = 1

    /**
     * Creates a notification channel.
     */
    fun createNotificationChannel(appContext: Context) {
        this.appContext = appContext
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationChannel.enableLights(true)

            val manager = appContext.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(notificationChannel)
        }

        currentNotificationIdNumber = NOTIFICATION_ID_DEFAULT
    }

    /**
     * Sends the notification.
     */
    fun sendNewNotification(){
        appContext?.let { appContext ->
            notificationManagerCompat = NotificationManagerCompat.from(appContext)
            val contextTitle = appContext.getString(R.string.app_name)
            val contextText = appContext.getString(R.string.txt_notification_arrived)

            // Create an explicit intent for an Activity in your app
            val intent = Intent(appContext, CalendarActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, 0, intent, 0)

            val channel = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_notif_small)
                .setLargeIcon(BitmapFactory.decodeResource(appContext.resources, R.drawable.icon_notif_large))
                .setContentTitle(contextTitle)
                .setContentText(contextText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(contextText))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            notificationManagerCompat.notify(currentNotificationIdNumber, channel.build())
            currentNotificationIdNumber += 1
        }?: kotlin.run {
            Timber.e("AppContext is null!")
        }
    }
}