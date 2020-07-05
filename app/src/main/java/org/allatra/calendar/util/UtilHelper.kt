package org.allatra.calendar.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import org.allatra.calendar.common.EnumDefinition
import org.joda.time.DateTime
import timber.log.Timber
import java.util.*

object UtilHelper {
    /**
     * func fetchUrl(forDate date: Date = Date()) -> String {
    let currentLanguage = LanguageManager.default.currentLanguage
    let screenBounds = UIScreen.main.bounds
    let screenScale = UIScreen.main.scale
    let resolution = "\(Int(screenBounds.size.height * screenScale))x\(Int(screenBounds.size.width * screenScale))"
    let day = date.day
    let month = date.month
    let year = date.year % 1000

    return "https://calendar.allatra.info/motivators/get_image?screen_resolution=\(resolution)&language_id=\(currentLanguage.id)&day=\(day)&month=\(month)&year=\(year)"
    }
     */
    fun getApiUrl(languageId: Int, screenHeight: Int, screenWidth: Int): String {
        val date = DateTime.now()
        val screenResolution = "${screenHeight}x${screenWidth}"
        return "${EnumDefinition.API_URL}${EnumDefinition.API_PARAM_SC}=${screenResolution}&${EnumDefinition.API_PARAM_LI}=$languageId&day=${date.dayOfMonth().get()}&month=${date.monthOfYear().get()}&year=${date.year}"
    }

    /**
     * Returns false when network is not connected.
     * Returns true when network is active.
     */
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        cm?.let { connectivityM ->
            if (Build.VERSION.SDK_INT < 23) {
                return connectivityM.activeNetworkInfo != null && connectivityM.activeNetworkInfo.isConnected
            } else {
                val nc = connectivityM.getNetworkCapabilities(connectivityM.activeNetwork)

                nc?.let {
                    return it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || it.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI)
                }?: kotlin.run {
                    Timber.e("System service Network capibilities returns null.")
                    return false
                }
            }
        }?: kotlin.run {
            Timber.e("System service CONNECTIVITY_SERVICE returns null.")
            return false
        }
    }

    /**
     * Check downloadedAt timestamp against new.
     */
    fun shouldLoadFromApiNew(downloadedAt: Date): Boolean {
        val currentTime = Date()
        val calendarCurrentTime = dateToCalendar(currentTime)
        val calendarDownloadedAt = dateToCalendar(downloadedAt)

        val dayDownloaded = calendarDownloadedAt.get(Calendar.DAY_OF_MONTH)
        val dayOfNow = calendarCurrentTime.get(Calendar.DAY_OF_MONTH)

        return if(calendarDownloadedAt.get(Calendar.MONTH) == calendarCurrentTime.get(Calendar.MONTH)
            && calendarDownloadedAt.get(Calendar.YEAR) == calendarCurrentTime.get(Calendar.YEAR) ){
            dayOfNow > dayDownloaded
        } else {
            true
        }
    }

    //Convert Date to Calendar
    private fun dateToCalendar(date: Date): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar
    }
}