package org.allatra.calendar.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
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
import org.allatra.calendar.data.db.AppDatabase
import org.allatra.calendar.data.db.entity.Motivator
import org.allatra.calendar.data.db.entity.UserSettings
import org.joda.time.DateTime
import org.joda.time.LocalTime
import timber.log.Timber
import java.util.concurrent.TimeUnit

class CalendarViewModel(
    application: Application
) : ViewModel() {
    // get repository
    private val repository = CalendarRepository(AppDatabase.getDatabase(application).userSettingsDao(), AppDatabase.getDatabase(application).motivatorDao())
    // get api languages
    private val _languagesResource = MutableLiveData<Resource<LanguageResource>>()
    private val _userSettingsResource = MutableLiveData<Resource<UserSettings>>()
    private val _motivatorResource = MutableLiveData<Resource<Motivator>>()
    // getter
    val languagesResource: MutableLiveData<Resource<LanguageResource>>
        get() = _languagesResource

    val userSettingsResource: MutableLiveData<Resource<UserSettings>>
        get() = _userSettingsResource

    val motivatorResource: MutableLiveData<Resource<Motivator>>
        get() = _motivatorResource

    init {
        fetchLanguagesFromApi()
        // get from DB
        getUserSettings()
        getMotivator()
    }

    private fun getMotivator() {
        // loading
        _motivatorResource.postValue(Resource.loading(null))
        Timber.i("We will fetch motivator.")

        viewModelScope.launch(Dispatchers.IO) {
            repository.getMotivator().collect {
                // cancel the context, we need to fetch only once
                coroutineContext.cancel()
                Timber.i("We have fetched motivatorObject = $it.")
                _motivatorResource.postValue(Resource.success(it))
            }
        }
    }

    private fun getUserSettings() {
        // loading
        _userSettingsResource.postValue(Resource.loading(null))
        Timber.i("We will fetch user settings.")
        // launch coroutine
        viewModelScope.launch(Dispatchers.IO) {
            repository.getUserSettings().collect {
                // cancel the context, we need to fetch only once
                coroutineContext.cancel()
                Timber.i("We have fetched userSettings = $it.")
                _userSettingsResource.postValue(Resource.success(it))
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun fetchLanguagesFromApi() {
        _languagesResource.postValue(Resource.loading(null))
        repository.getLanguagesFromApi()
            .subscribeOn(Schedulers.io())
            .retryWhen { errorObservable ->
                errorObservable.zipWith(Observable.range(1, 3), BiFunction { throwable: Throwable, count: Int -> Pair(throwable, count) })
                    .flatMap { count: Pair<Throwable, Int> ->
                        if (count.second < 3) {
                            Observable.timer(3, TimeUnit.SECONDS)
                        } else {
                            Observable.error(count.first)
                        }
                    }
            }
            .subscribe ({ response ->
                response?.let {
                    if (response.ok) {
                        response.data
                        _languagesResource.postValue(Resource.success(response))
                    } else {
                        Timber.e("Call ended with error.")
                        _languagesResource.postValue(
                            Resource.error(
                                Constants.ErrorType.BACKEND_API,
                                null
                            )
                        )
                    }
                } ?: kotlin.run {
                    Timber.e("Call ended with error.")
                    _languagesResource.postValue(
                        Resource.error(
                            Constants.ErrorType.BACKEND_API,
                            null
                        )
                    )
                }
            }
                ,
                {
                    Timber.e("Call ended with failure.")
                    _languagesResource.postValue(Resource.error(Constants.ErrorType.BACKEND_API, null))
                }
            )
    }

    /**
     * Creates or updates existing user settings
     */
    fun createOrUpdateUserSettings (isToUpdate: Boolean, apiLanguageId: String, sendNotifications: Boolean, notificationTime: LocalTime) {
        if (isToUpdate) {
            var valuesChanged = false

            _userSettingsResource.value?.data?.let {
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
                        _userSettingsResource.postValue(Resource.success(it))
                    }
                }
            } ?: kotlin.run {
                Timber.e("_userSettings are null..")
            }
        } else {
            // Get the FCM registration token
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.w("Fetching FCM registration token failed, ${task.exception}")
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result.toString()
                Timber.i("Token has been successfully fetched, token: ${token}")

                // create new
                viewModelScope.launch(Dispatchers.IO) {
                    val userSettings = UserSettings(DEFAULT_USER_SETTINGS_ID, apiLanguageId, sendNotifications, notificationTime, token)
                    repository.insertUserSettings(userSettings)
                    Timber.i("New values has been stored userSettings = $userSettings")
                    // post it
                    _userSettingsResource.postValue(Resource.success(userSettings))
                }
            })
        }
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
}
