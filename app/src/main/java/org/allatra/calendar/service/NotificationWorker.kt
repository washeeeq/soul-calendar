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
import androidx.work.*
import org.allatra.calendar.R
import org.allatra.calendar.ui.activity.CalendarActivity
import org.allatra.calendar.ui.activity.CalendarActivity.Companion.NOTIF_HOUR
import org.allatra.calendar.ui.activity.CalendarActivity.Companion.NOTIF_MINUTE
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

// https://developer.android.com/topic/libraries/architecture/workmanager/basics
class NotificationWorker(private val appContext: Context, workerParameters: WorkerParameters): Worker(appContext, workerParameters){
    private lateinit var notificationManagerCompat: NotificationManagerCompat
    private val TAG = this::class.java.simpleName
    private var currentNotificationIdNumber = 0
    private var notificationChannelCreated = false

    companion object {
        private const val CHANNEL_ID = "CHANNEL_NOTIF_124"
        private const val CHANNEL_NAME = "CHANNEL_NOTIF_SOUL_CAL"
        private const val NOTIFICATION_ID_DEFAULT = 1
    }

    /**
     *  Result.success(): The work finished successfully.
        Result.failure(): The work failed.
        Result.retry(): The work failed and should be tried at another time according to its retry policy.
     */
    override fun doWork(): Result {
        // Do the work here--in this case, upload the images.
        //uploadImages()

        if(!notificationChannelCreated){
            createNotificationChannel()
            notificationChannelCreated = true
        }

        // send the notification
        sendNewNotification()
        // enqueue new request
        enqueueNewOneTimeRequest()
        // return
        return Result.success()
    }

    private fun enqueueNewOneTimeRequest(){
        val hour = inputData.getInt(NOTIF_HOUR, 10)
        val minute = inputData.getInt(NOTIF_MINUTE, 0)

        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        // Set Execution around 05:00:00 AM
        dueDate.set(Calendar.HOUR_OF_DAY, hour)
        dueDate.set(Calendar.MINUTE, minute)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis.minus(currentDate.timeInMillis)

        val dailyWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()

        //TODO: Check and delte maybe only id
        //WorkManager.getInstance(appContext).

        // first cancel all work
        WorkManager.getInstance(appContext).cancelAllWork()
        // enqueue new
        WorkManager.getInstance(appContext).enqueue(dailyWorkRequest)
        Timber.tag(" NotificationWorker").i("Work has been scheduled from  NotificationWorker.")
    }

    private fun createNotificationChannel() {
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

    private fun sendNewNotification(){
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
    }
}