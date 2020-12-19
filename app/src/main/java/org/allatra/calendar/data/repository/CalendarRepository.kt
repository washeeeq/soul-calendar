package org.allatra.calendar.data.repository

import org.allatra.calendar.data.api.ApiClient

/**
 * This class is the only single point of contact for the ViewModel.
 */
class CalendarRepository() {

    /**
     * Returns resource of book languages.
     */
    fun getLanguagesFromApi() = ApiClient.getLanguages()
}
