package org.allatra.calendar.util

import org.allatra.calendar.common.EnumDefinition
import org.joda.time.DateTime

object UtilHelper {

    fun getApiUrl(languageId: String, screenHeight: Int, screenWidth: Int): String {
        val date = DateTime.now()
        return "${EnumDefinition.API_URL}${EnumDefinition.API_PARAM_SC}"
    }
}