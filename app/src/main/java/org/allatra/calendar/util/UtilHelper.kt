package org.allatra.calendar.util

import org.allatra.calendar.common.EnumDefinition
import org.joda.time.DateTime

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
}