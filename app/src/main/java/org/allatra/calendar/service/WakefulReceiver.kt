package org.allatra.calendar.service

import android.app.*
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.allatra.calendar.R
import org.allatra.calendar.ui.view.activity.CalendarActivity
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import timber.log.Timber

class WakefulReceiver : BroadcastReceiver() {
    private var alarmManager: AlarmManager? = null
    private var currentNotificationIdNumber = 0

    companion object {
        const val WAKE_RECEIVE_NOTIF = "wake_notif"
        private const val CHANNEL_ID = "CHANNEL_NOTIF_124"
        private const val CHANNEL_NAME = "CHANNEL_NOTIF_SOUL_CAL"
        private const val NOTIFICATION_ID_DEFAULT = 1
        const val TIME_FORMAT_ALARM_DATE = "MM-dd-yyyy HH:mm"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            createNotificationChannel(it)
            sendNewNotification(it)
        } ?: kotlin.run {
            Timber.e("Cannot fire the notification. Context is null!")
        }
    }

    /**
     * Sets the alarm on particular hour:minute:second.
     */
    fun setAlarm(appContext: Context, hour: Int, minute: Int) {
        Timber.i("Setting up alarm for: $hour:$minute.")

        alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager?.let {
            val alarmIntent = Intent(appContext, WakefulReceiver::class.java).let { intent ->
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                PendingIntent.getBroadcast(appContext, 0, intent, 0)
            }

            val datetimeNow = DateTime.now()
            val dateTimeFormatter = DateTimeFormat.forPattern(TIME_FORMAT_ALARM_DATE)
            var alarmDateTime = DateTime.parse("${datetimeNow.monthOfYear}-${datetimeNow.dayOfMonth}-${datetimeNow.year} $hour:$minute", dateTimeFormatter)

            if (alarmDateTime.isBefore(datetimeNow)) {
                alarmDateTime = DateTime.parse("${datetimeNow.monthOfYear}-${datetimeNow.dayOfMonth + 1}-${datetimeNow.year} $hour:$minute", dateTimeFormatter)
            }

            // setRepeating() lets you specify a precise custom interval--in this case,
            // 24 h
            Timber.d("Android version is ${Build.VERSION.SDK_INT}")
            when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP -> {
                    it.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmDateTime.millis,
                        alarmIntent
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                    it.setAlarmClock(AlarmManager.AlarmClockInfo(alarmDateTime.millis, alarmIntent), alarmIntent)
                }

                Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                    it.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmDateTime.millis,
                        alarmIntent
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    it.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmDateTime.millis,
                        alarmIntent
                    )
                }
            }
        } ?: kotlin.run {
            Timber.e("Instance of mAlarmManager is null.")
        }
    }

    /**
     * Cancel alarm and clean.
     */
    fun cancelAlarm(appContext: Context) {
        Timber.i("Cancelling alarm.")

        alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager?.let {
            val intent = Intent(appContext, WakefulReceiver::class.java)
            val alarmIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0)

            it.cancel(alarmIntent)
        } ?: kotlin.run {
            Timber.e("Instance of mAlarmManager is null.")
        }
    }

    /**
     * Creates a notification channel.
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            .setSound(
                Uri.parse(
                    "android.resource://" +
                        context.packageName + "/" + R.raw.slow_spring_board
                )
            )

        notificationManagerCompat.notify(
            currentNotificationIdNumber,
            channel.build()
        )
        currentNotificationIdNumber += 1
    }
}
