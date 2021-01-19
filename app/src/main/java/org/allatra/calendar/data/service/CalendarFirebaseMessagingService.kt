package org.allatra.calendar.data.service

import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.allatra.calendar.data.db.AppDatabase
import org.allatra.calendar.data.repository.CalendarRepository
import timber.log.Timber

class CalendarFirebaseMessagingService: FirebaseMessagingService() {
    // get repository
    private val repository = CalendarRepository(AppDatabase.getDatabase(application).userSettingsDao(), AppDatabase.getDatabase(application).motivatorDao())

    override fun onNewToken(token: String) {
        Timber.i("Token has been refreshed token: $token")

        CoroutineScope(Dispatchers.IO).launch {
            repository.getUserSettings().collect { userSettings ->
                if (userSettings != null) {
                    Timber.i("User settings are not null so we update the token")
                    userSettings.fcmRegistrationToken = token
                    // let us update
                    repository.updateUserSettings(userSettings)
                    // TODO: Fire against api call
                }
            }
        }

        super.onNewToken(token)
    }
}