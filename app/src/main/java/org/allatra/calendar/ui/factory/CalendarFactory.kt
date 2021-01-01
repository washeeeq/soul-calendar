package org.allatra.calendar.ui.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.allatra.calendar.ui.viewmodel.CalendarViewModel

class CalendarFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return CalendarViewModel(application) as T
    }
}
