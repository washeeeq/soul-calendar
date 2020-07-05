package org.allatra.calendar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.allatra.calendar.db.RealmHandlerObject
import timber.log.Timber
import java.util.*

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            //context = CalendarActivity::class.java
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WakefulReceiver::class.java)
            val alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val settings = RealmHandlerObject.getDefaultSettings()

            settings?.let {
                if(it.allowNotifications){
                    if(it.notificationTime!=null) {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, it.notificationTime.hourOfDay)
                        calendar.set(Calendar.MINUTE, it.notificationTime.minuteOfHour)
                        calendar.set(Calendar.SECOND, 0)

                        val alarmDate = calendar.time

                        Timber.i("Android version is ${Build.VERSION.SDK_INT}")
                        if(Build.VERSION.SDK_INT >= 23){
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                alarmIntent)
                        } else {
                            alarmManager.setRepeating(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                1000 * 60 * 60 * 24,
                                alarmIntent
                            )
                        }
                    }
                }
            }?: kotlin.run {
                Timber.e("Settings are null, not setup will be done.")
            }
        } else {
            Timber.i("Unmapped action ${intent.action}.")
        }
    }
}