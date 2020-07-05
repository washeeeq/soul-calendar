package org.allatra.calendar.service

import android.app.*
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.allatra.calendar.R
import org.allatra.calendar.ui.activity.CalendarActivity
import timber.log.Timber
import java.util.*

class WakefulReceiver: BroadcastReceiver() {
    private var alarmManager: AlarmManager? = null
    private var currentNotificationIdNumber = 0

    companion object {
        const val WAKE_RECEIVE_NOTIF = "wake_notif"
        private const val CHANNEL_ID = "CHANNEL_NOTIF_124"
        private const val CHANNEL_NAME = "CHANNEL_NOTIF_SOUL_CAL"
        private const val NOTIFICATION_ID_DEFAULT = 1
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            createNotificationChannel(it)
            sendNewNotification(it)
        }?: kotlin.run {
            Timber.e("Cannot fire the notification. Context is null!")
        }
    }

    /**
     * Sets the alarm on particular hour:minute:second.
     */
    fun setAlarm(appContext: Context, hour: Int, minute: Int){
        Timber.i("Setting up alarm for: $hour:$minute.")

        alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager?.let {
            val alarmIntent = Intent(appContext, WakefulReceiver::class.java).let { intent ->
                PendingIntent.getBroadcast(appContext, 0, intent, 0)
            }

            // Set the alarm to start at particular time
            val calendar: Calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }

            // setRepeating() lets you specify a precise custom interval--in this case,
            // 24 h
            Timber.i("Android version is ${Build.VERSION.SDK_INT}")
            if(Build.VERSION.SDK_INT >= 23){
                it.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    alarmIntent)
            } else {
                it.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    1000 * 60 * 60 * 24,
                    alarmIntent
                )
            }

            val receiver = ComponentName(appContext, BootReceiver::class.java)
            // Enable {@code BootReceiver} to automatically restart when the
            // device is rebooted.
            appContext.packageManager.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }?: kotlin.run {
            Timber.e("Instance of mAlarmManager is null.")
        }
    }

    /**
     * Cancel alarm and clean.
     */
    fun cancelAlarm(appContext: Context){
        Timber.i("Cancelling alarm.")

        alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager?.let {
            val intent = Intent(appContext, WakefulReceiver::class.java)
            val alarmIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0)

            it.cancel(alarmIntent)

            val receiver = ComponentName(appContext, BootReceiver::class.java)
            val pm = appContext.packageManager
            pm.setComponentEnabledSetting(
                receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }?: kotlin.run {
            Timber.e("Instance of mAlarmManager is null.")
        }
    }

    /**
     * Creates a notification channel.
     */
    private fun createNotificationChannel(context: Context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                IMPORTANCE_HIGH
            )

            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationChannel.enableLights(true)

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(notificationChannel)
        }

        currentNotificationIdNumber = NOTIFICATION_ID_DEFAULT
    }

    /**
     * Sends the notification.
     */
    private fun sendNewNotification(context: Context) {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        val contextTitle = context.getString(R.string.app_name)
        val contextText = context.getString(R.string.txt_notification_arrived)

        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, CalendarActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val channel = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_notif_small)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.icon_notif_large
                )
            )
            .setContentTitle(contextTitle)
            .setContentText(contextText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contextText)
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
                // https://notificationsounds.com/message-tones/demonstrative-516
            .setSound(Uri.parse("android.resource://"
                    + context.packageName + "/" + R.raw.demonstrative))

        notificationManagerCompat.notify(
            currentNotificationIdNumber,
            channel.build()
        )
        currentNotificationIdNumber += 1
    }
}