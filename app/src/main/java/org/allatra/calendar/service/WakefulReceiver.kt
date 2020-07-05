package org.allatra.calendar.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import timber.log.Timber
import java.util.*

class WakefulReceiver: BroadcastReceiver() {
    private var mAlarmManager: AlarmManager? = null

    companion object {
        const val WAKE_RECEIVE_NOTIF = "wake_notif"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        NotificationStaticService.sendNewNotification()
    }

    fun init(appContext: Context){
        Timber.i("WakefulReceiver has been initialized.")
        NotificationStaticService.createNotificationChannel(appContext)
    }

    fun setAlarm(appContext: Context, hour: Int, minute: Int, second: Int){
        Timber.i("Setting up alarm for: $hour:$minute:${second}0.")

        mAlarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        mAlarmManager?.let {
            val intent = Intent(appContext, WakefulReceiver::class.java)
            val alarmIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0)

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, second)

            val alarmDate = calendar.time

            it.setExact(AlarmManager.RTC_WAKEUP, alarmDate.time, alarmIntent)

            val receiver = ComponentName(appContext, BootReceiver::class.java)
            // Enable {@code BootReceiver} to automatically restart when the
            // device is rebooted.
            appContext.packageManager.setComponentEnabledSetting(
                receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }?: kotlin.run {
            Timber.e("Instance of mAlarmManager is null.")
        }
    }

    fun cancelAlarm(appContext: Context){
        Timber.i("Cancelling alarm.")

        mAlarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        mAlarmManager?.let {
            val intent = Intent(appContext, WakefulReceiver::class.java)
            val alarmIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0)

            it.cancel(alarmIntent)

            // Disable {@code BootReceiver} so that it doesn't automatically restart when the device is rebooted.
            //// TODO: you may need to reference the context by ApplicationActivity.class
            // Disable {@code BootReceiver} so that it doesn't automatically restart when the device is rebooted.
            //// TODO: you may need to reference the context by ApplicationActivity.class
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
}