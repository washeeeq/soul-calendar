package org.allatra.calendar.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import org.allatra.calendar.common.Constants
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
    fun getApiUrl(languageId: Int, screenHeight: Int, screenWidth: Int, dateTime: DateTime): String {
        val date = DateTime.now()
        val screenResolution = "${screenHeight}x${screenWidth}"
        return "${Constants.API_URL}${Constants.API_PARAM_SC}=${screenResolution}&${Constants.API_PARAM_LI}=$languageId&day=${dateTime.dayOfMonth}&month=${dateTime.monthOfYear}&year=${dateTime.year}"
    }


    /**
     * Returns false when network is not connected.
     * Returns true when network is active.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                // for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                // for check internet over Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }

    /**
     * Check downloadedAt timestamp against new.
     */
    fun shouldLoadFromApiNew(dateTimeNow: DateTime, downloadedAt: DateTime): Boolean {
        Timber.d("Comparing downloadedAt = $downloadedAt, dateTimeNow = $dateTimeNow")

        val dayDownloaded = downloadedAt.dayOfYear
        val dayOfNow = dateTimeNow.dayOfYear

        return if(dayDownloaded == dayOfNow && downloadedAt.year == dateTimeNow.year) {
            dayOfNow > dayDownloaded
        } else {
            true
        }
    }
}