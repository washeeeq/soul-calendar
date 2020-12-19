package org.allatra.calendar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.allatra.calendar.common.Constants
import org.allatra.calendar.data.api.model.Resource
import org.allatra.calendar.data.api.resource.LanguageResource
import org.allatra.calendar.data.repository.CalendarRepository
import org.allatra.calendar.db.RealmHandlerObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class CalendarViewModel(
    application: Application
) : ViewModel() {
    // get repository
    private val repository = CalendarRepository()
    // get api languages
    private val _listOfLanguages = MutableLiveData<Resource<LanguageResource>>()
    // getter
    val listOfLanguages: MutableLiveData<Resource<LanguageResource>>
        get() = _listOfLanguages

    init {
        RealmHandlerObject.initWithContext(application.applicationContext)
        fetchLanguagesFromApi()
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
                TODO("Not yet implemented")
            }

        })
    }
}
