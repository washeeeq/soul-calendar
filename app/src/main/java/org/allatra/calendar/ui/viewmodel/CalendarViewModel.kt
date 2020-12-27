package org.allatra.calendar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.allatra.calendar.common.Constants
import org.allatra.calendar.common.Constants.DEFAULT_MOTIVATOR_ID
import org.allatra.calendar.common.Constants.DEFAULT_USER_SETTINGS_ID
import org.allatra.calendar.data.api.model.Resource
import org.allatra.calendar.data.api.resource.LanguageResource
import org.allatra.calendar.data.repository.CalendarRepository
import org.allatra.calendar.db.AppDatabase
import org.allatra.calendar.db.entity.Motivator
import org.allatra.calendar.db.entity.UserSettings
import org.joda.time.DateTime
import org.joda.time.LocalTime
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class CalendarViewModel(
    application: Application
) : ViewModel() {
    // get repository
    private val repository = CalendarRepository(AppDatabase.getDatabase(application).userSettingsDao(), AppDatabase.getDatabase(application).motivatorDao())
    // get api languages
    private val _listOfLanguages = MutableLiveData<Resource<LanguageResource>>()
    private val _userSettings = MutableLiveData<Resource<UserSettings>>()
    private val _motivatorResource = MutableLiveData<Resource<Motivator>>()
    // getter
    val listOfLanguages: MutableLiveData<Resource<LanguageResource>>
        get() = _listOfLanguages

    val userSettings: MutableLiveData<Resource<UserSettings>>
        get() = _userSettings

    val motivatorResource: MutableLiveData<Resource<Motivator>>
        get() = _motivatorResource

    init {
        fetchLanguagesFromApi()
        // get from DB
        getUserSettings()
        getMotivator()
    }

    fun updateMotivator(lastDownloadedAt: DateTime) {
        viewModelScope.launch(Dispatchers.IO) {
            _motivatorResource.value?.data?.let {
                Timber.i("Motivator object exist. Let us update")
                it.lastDownloadAt = lastDownloadedAt
                repository.updateMotivator(it)
            } ?: kotlin.run {
                Timber.i("Motivator object does not exist, is new.")
                val motivator = Motivator(DEFAULT_MOTIVATOR_ID, lastDownloadedAt)
                repository.insertMotivator(motivator)
            }
        }
    }

    private fun getMotivator() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getMotivator().collect {
                // cancel the context, we need to fetch only once
                coroutineContext.cancel()
                Timber.i("We have fetched motivator = $it.")

                if (it != null) {
                    _motivatorResource.postValue(Resource.success(it))
                } else {
                    Timber.i("Motivator is null")
                }
            }
        }
    }

    /**
     * Creates or updates existing user settings
     */
    fun createOrUpdateUserSettings (isToUpdate: Boolean, apiLanguageId: String, sendNotifications: Boolean, notificationTime: LocalTime) {
        if (isToUpdate) {
            var valuesChanged = false

            _userSettings.value?.data?.let {
                when {
                    it.apiLanguageId != apiLanguageId -> {
                        valuesChanged = true
                    }
                    it.sendNotifications != sendNotifications -> {
                        valuesChanged = true
                    }
                    it.notificationTime != notificationTime -> {
                        valuesChanged = true
                    }
                    else -> {
                        Timber.i("There was not change in settings, nothing to save.")
                    }
                }

                if (valuesChanged) {
                    // create new
                    viewModelScope.launch(Dispatchers.IO) {
                        it.apiLanguageId = apiLanguageId
                        it.sendNotifications = sendNotifications
                        it.notificationTime = notificationTime
                        repository.updateUserSettings(it)
                        Timber.i("Values has been updated, userSettings = $it")
                        // post it
                        _userSettings.postValue(Resource.success(it))
                    }
                }
            } ?: kotlin.run {
                Timber.e("_userSettings are null..")
            }
        } else {
            // create new
            viewModelScope.launch(Dispatchers.IO) {
                val userSettings = UserSettings(DEFAULT_USER_SETTINGS_ID, apiLanguageId, sendNotifications, notificationTime)
                repository.insertUserSettings(userSettings)
                Timber.i("New values has been stored userSettings = $userSettings")
                // post it
                _userSettings.postValue(Resource.success(userSettings))
            }
        }
    }

    private fun getUserSettings() {
        Timber.i("We will fetch user settings.")
        // launch coroutine
        viewModelScope.launch(Dispatchers.IO) {
            repository.getUserSettings().collect {
                // cancel the context, we need to fetch only once
                coroutineContext.cancel()
                Timber.i("We have fetched userSettings = $it.")
                _userSettings.postValue(Resource.success(it))
            }
        }
    }

    private fun fetchLanguagesFromApi() {
        val request = repository.getLanguagesFromApi()
        request.enqueue(object: Callback<LanguageResource> {
            override fun onResponse(
                call: Call<LanguageResource>,
                response: Response<LanguageResource>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        // post it
                        _listOfLanguages.postValue(Resource.success(it))
                    } ?: kotlin.run {
                        Timber.e("No body returned!")
                        _listOfLanguages.postValue(Resource.error(Constants.ErrorType.BACKEND_API, null))
                    }
                } else {
                    Timber.e("Call ended with error.")
                    _listOfLanguages.postValue(Resource.error(Constants.ErrorType.BACKEND_API, null))
                }
            }

            override fun onFailure(call: Call<LanguageResource>, t: Throwable) {
                Timber.e("Call ended with failure.")
                _listOfLanguages.postValue(Resource.error(Constants.ErrorType.BACKEND_API, null))
            }
        })
    }
}
