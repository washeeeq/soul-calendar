package org.allatra.calendar.ui.view.activity

import android.Manifest
import android.animation.ObjectAnimator
import android.app.TimePickerDialog
import android.content.Intent
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import com.thelittlefireman.appkillermanager.managers.KillerManager
import com.thelittlefireman.appkillermanager.ui.DialogKillerManagerBuilder
import kotlinx.android.synthetic.main.activity_calendar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.allatra.calendar.BuildConfig
import org.allatra.calendar.R
import org.allatra.calendar.common.Constants
import org.allatra.calendar.common.Constants.DEFAULT_LOCALE
import org.allatra.calendar.data.api.model.Resource
import org.allatra.calendar.data.api.resource.LanguageResource
import org.allatra.calendar.db.entity.UserSettings
import org.allatra.calendar.service.WakefulReceiver
import org.allatra.calendar.service.WakefulReceiver.Companion.WAKE_RECEIVE_NOTIF
import org.allatra.calendar.ui.factory.CalendarFactory
import org.allatra.calendar.ui.viewmodel.CalendarViewModel
import org.allatra.calendar.util.PictureLoaderHelper
import org.allatra.calendar.util.UtilHelper
import org.joda.time.DateTime
import org.joda.time.LocalTime
import timber.log.Timber
import java.io.File
import java.util.*

class CalendarActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Default), ActivityCompat.OnRequestPermissionsResultCallback {
    private var moveYtoDefaultPosition: Float = 0f
    private val contractAnimationDurationSec = 700L
    private val hideAnimationTimeNotifLayoutDuration = 500L
    private val hideAnimationDurationSec = 100L
    private var itemElementHeight: Float = 0f
    private val hidePercentage = 0.43f
    private lateinit var isUpdating: MutableLiveData<Boolean>
    private var languageArrayAdapter: ArrayAdapter<String>? = null
    private var wakefulReceiver: WakefulReceiver? = null
    private lateinit var model: CalendarViewModel

    companion object {
        private const val MOTIVATOR_NAME = "motivator_of_the_day.jpeg"
        private const val CONTENT_TYPE_IMAGE = "image/jpeg"
        private const val FILE_PROVIDER = ".fileprovider"
        private const val HTTP_CONST = "http"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        init()

        /**
         * Expand settings.
         */
        btnExpand?.setOnClickListener {
            Timber.d("User clicked expand.")

            showOrHideSettings()
        }

        /**
         * Hide/Show notification settings.
         */
        switchShowNotif?.setOnCheckedChangeListener { _, isChecked ->
            // show time
            if (isChecked) {
                showTimeNotificationLayout()
                // request permissions
                val dialog = DialogKillerManagerBuilder().setContext(this).setAction(KillerManager.Actions.ACTION_POWERSAVING).show()
            }
            // hide
            else {
                hideTimeNotificationLayout()
            }
        }

        /**
         * Show timePicker dialogue on layoutgroup click.
         */
        timeNotificationLayoutGroup.setOnClickListener {
            Timber.i("User clicked on the timeNotificationLayoutGroup layout.")

            var hourOfDay = 10
            var minute = 10

            model.userSettings.value?.let { value ->
                value.data?.notificationTime?.let {
                    hourOfDay = it.hourOfDay
                    minute = it.minuteOfHour
                }
            }

            val timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { view, setHour, setMinute ->
                if (setHour < 10) {
                    txtHours.text = "0${setHour.toString()}"
                } else {
                    txtHours.text = setHour.toString()
                }

                if(setMinute < 10){
                    txtMinutes.text = "0${setMinute.toString()}"
                } else {
                    txtMinutes.text = setMinute.toString()
                }

            }, hourOfDay, minute, true)
            timePickerDialog.show()
        }

        /**
         * Do nothing and just hide settings.
         */
        btnCancel.setOnClickListener {
            showOrHideSettings()
        }

        /**
         * Save all changed settings to DB.
         */
        btnSave.setOnClickListener {
            isUpdating.postValue(true)
            updateDbAndSchedule()

            showOrHideSettings()
        }

        /**
         * Share picture.
         */
        shareLayoutGroup.setOnClickListener {
            motivatorOfDay.drawable?.let {
                val localFile = getLocalMotivatorFile()
                Timber.i("Path to load is ${localFile.absolutePath}")

                val shareUri = FileProvider.getUriForFile(applicationContext, applicationContext.packageName.toString() + FILE_PROVIDER, localFile)

                val intent = Intent(Intent.ACTION_SEND)
                intent.type = CONTENT_TYPE_IMAGE
                intent.putExtra(Intent.EXTRA_STREAM, shareUri)
                intent.addFlags(FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivity(Intent.createChooser(intent, "I would like to share with you an interesting picture."))

            } ?: kotlin.run {
                Timber.e("Drawable is null, no sharing can be done.")
                showCustomMessage(getString(R.string.txt_error_no_picture_saved))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissions.forEachIndexed() { index, permissionString ->
            if(permissionString.equals(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)){
                if(grantResults[index].equals(PackageManager.PERMISSION_GRANTED)){
                    Timber.i("Permission granted!!")
                } else {
                    showCustomMessage(getString(R.string.txt_warn_no_notifications_will_deliver))
                    Timber.w("Notifications will not be sent!")
                }
            }
        }
    }

    /**
     * When app is resumed, get new daily picture and set.
     */
    override fun onResume() {
        Timber.tag("onResume").i("Status -> Resumed.")
        super.onResume()

        // we load picture only here
        //getDailyPictureAndSet()
    }

    override fun onDestroy() {
        super.onDestroy()
        // deregister on quit
        unregisterReceiver(wakefulReceiver)
    }

    private fun updateLastDownloadedAt(lastDownloadedAt: DateTime) {
        model.updateMotivator(lastDownloadedAt)
    }

    private fun updateDbAndSchedule() {
        val languageCodeString = spnLanguage.selectedItem.toString()
        val apiLanguageId = getCurrentApiLanguageId(languageCodeString)
        val sendNotifications = switchShowNotif.isChecked
        val notificationTimeString = "${txtHours.text}${txtDivider.text}${txtMinutes.text}"
        val notificationTime = LocalTime.parse(notificationTimeString)

        apiLanguageId?.let { apiCurrentLanguageId ->
            model.userSettings.value?.data?.let {
                Timber.i("User settings exist already! Let us update them.")
                model.createOrUpdateUserSettings(true, apiCurrentLanguageId, sendNotifications, notificationTime)
            } ?: run {
                Timber.i("User settings are null! We must create new.")
                model.createOrUpdateUserSettings(false, apiCurrentLanguageId, sendNotifications, notificationTime)
            }
        }

        if (sendNotifications) {
            // remove previous
            removeNotificationsSchedule()
            // schedule new
            scheduleNotifications(notificationTime)
        } else {
            removeNotificationsSchedule()
        }

        Thread.sleep(500)
        isUpdating.postValue(false)
    }

    private fun getCurrentApiLanguageId(languageCode: String): String? {
        var apiLangId: String? = null

        model.listOfLanguages.value?.data?.let {
            val indexLangCode = it.data.columnsOrder.indexOf(Constants.LANG_CODE)
            val indexLangId = it.data.columnsOrder.indexOf(Constants.LANG_ID)

            it.data.table.forEach lang_loop@{ recordArray ->
                val langId = getIntFromAny(recordArray[indexLangId])
                val langCode = recordArray[indexLangCode] as String

                if (langCode == languageCode) {
                    apiLangId = langId.toString()
                    Timber.i("Language code found langId = $langId, langCode = $langCode")
                    return@lang_loop
                }
            }
        }?: kotlin.run {
            Timber.e("Something returned from api is null")
        }
        return apiLangId
    }

    private fun getIntFromAny(value: Any): Int {
        return when (value) {
            is Double -> {
                value.toInt()
            }
            is Int -> {
                value
            }
            is Float -> {
                value.toInt()
            }
            is String -> {
                value.toInt()
            }

            else -> {
                Timber.e("Unknown format of value = $value")
                return -1
            }
        }
    }

    private fun registerBroadCastReceiverAndSchedule(userSettings: UserSettings) {
        wakefulReceiver = WakefulReceiver()
        registerReceiver(wakefulReceiver, IntentFilter(WAKE_RECEIVE_NOTIF))

        if (userSettings.sendNotifications) {
            // remove previous
            removeNotificationsSchedule()
            // schedule new
            scheduleNotifications(userSettings.notificationTime)
        }
    }

    private fun removeNotificationsSchedule() {
        wakefulReceiver?.cancelAlarm(applicationContext)
    }

    private fun scheduleNotifications(notificationTime: LocalTime) {
        wakefulReceiver?.setAlarm(applicationContext, notificationTime.hourOfDay, notificationTime.minuteOfHour)
        Timber.i("Notification was scheduled.")
    }

    /**
     * Hide or shows settings.
     */
    private fun showOrHideSettings() {
        sliderSettings?.let {
            if (it.getIsContracted()) {
                it.setIsContracted(false)
            } else {
                it.setIsContracted(true)
            }
        }

        switchUi()
    }

    private fun showTimeNotificationLayout(){
        timeNotificationLayoutGroup.animate()
            .withStartAction{
                timeNotificationLayoutGroup.visibility = View.VISIBLE
            }
            .alpha(1f)
            .scaleY(1f)
            .duration = hideAnimationTimeNotifLayoutDuration

        shareLayoutGroup.animate()
            .translationY(0f)
            .duration = hideAnimationTimeNotifLayoutDuration
    }

    private fun hideTimeNotificationLayout() {
        timeNotificationLayoutGroup.animate()
            .withEndAction {
                Timber.i("Let us hide.")
                timeNotificationLayoutGroup.visibility = View.GONE
            }
            .alpha(0.0f)
            .scaleY(0.0f)
            .duration = hideAnimationTimeNotifLayoutDuration

        shareLayoutGroup.animate()
            .translationY(-itemElementHeight)
            .duration = hideAnimationTimeNotifLayoutDuration
    }

    private fun init() {
        val listOfLanguages = mutableListOf<String>()
        val mapOfLanguages = mutableMapOf<Int, String>()
        // get model
        model = ViewModelProvider(this, CalendarFactory(application)).get(CalendarViewModel::class.java)

        /**
         * Observe only once.
         */
        model.listOfLanguages.observeOnce(this,
            Observer<Resource<LanguageResource>> { resource ->
                when (resource.apiStatus) {
                    Constants.ApiStatus.SUCCESS -> {
                        Timber.i("Received data from api.")

                        resource.data?.let {
                            Timber.i(resource.data.toString())

                            val indexLangCode = it.data.columnsOrder.indexOf(Constants.LANG_CODE)
                            val indexLangId = it.data.columnsOrder.indexOf(Constants.LANG_ID)

                            if (indexLangCode != -1) {
                                // create languagelist
                                it.data.table.forEach { recordArray ->
                                    val langId = recordArray[indexLangId]
                                    val langCode = recordArray[indexLangCode]

                                    if (langCode is String) {
                                        listOfLanguages.add(langCode)
                                        when (langId) {
                                            is Double -> {
                                                mapOfLanguages[langId.toInt()] = langCode
                                            }
                                            is Int -> {
                                                mapOfLanguages[langId] = langCode
                                            }
                                            is Float -> {
                                                mapOfLanguages[langId.toInt()] = langCode
                                            }

                                            else -> {
                                                Timber.e("Unknown format of LangId = ${langId.javaClass}")
                                            }
                                        }
                                    } else {
                                        listOfLanguages.add(langCode.toString())
                                        when (langId) {
                                            is Double -> {
                                                mapOfLanguages[langId.toInt()] = langCode.toString()
                                            }
                                            is Int -> {
                                                mapOfLanguages[langId] = langCode.toString()
                                            }
                                            is Float -> {
                                                mapOfLanguages[langId.toInt()] = langCode.toString()
                                            }

                                            else -> {
                                                Timber.e("Unknown format of LangId = ${langId.javaClass}")
                                            }
                                        }
                                    }
                                }

                            } else {
                                Timber.e("LanguageCode is null")
                            }
                        }
                    }

                    Constants.ApiStatus.ERROR -> {
                        resource.errorType?.let {
                            when (it) {
                                Constants.ErrorType.BACKEND_API -> {
                                    // showCustomMessage(resources.getString(R.string.error_backend_failure))
                                }
                                Constants.ErrorType.LOCAL_DB -> {
                                    // showCustomMessage(resources.getString(R.string.error_local_db_failure))
                                }
                                Constants.ErrorType.NETWORK -> {
                                    // showCustomMessage(resources.getString(R.string.error_network_failure))
                                }
                            }
                        }
                    }

                    Constants.ApiStatus.LOADING -> {
                    }
                }

                // init languages
                languageArrayAdapter = ArrayAdapter(
                    this, R.layout.custom_spinner_textview, listOfLanguages.toTypedArray()
                )
                languageArrayAdapter!!.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
                spnLanguage.adapter = languageArrayAdapter

                var apiLanguageId: String? = null
                model.userSettings.value?.let { resourceUserSettings ->
                    resourceUserSettings.data?.let { userSettings ->
                        apiLanguageId = userSettings.apiLanguageId
                        val languageToSetString = mapOfLanguages[apiLanguageId!!.toInt()]

                        languageToSetString?.let {
                            Timber.i("Language from the map is $languageToSetString")
                            setLanguageFromUserSettings(it)
                        }?: kotlin.run {
                            Timber.e("Could not find langId = ${apiLanguageId!!} in map ${mapOfLanguages.toString()}")
                        }
                    } ?: kotlin.run {
                        setDefaultLanguage()
                    }

                    // hide layout
                    hideTimeNotificationLayout()
                }

                apiLanguageId?.let {
                    // set picture with
                    Timber.i("Api language id from userSettings, id = $it")
                    getDailyPictureAndSet(it.toInt())
                } ?: run {
                    val apiIdByDefault = getCurrentApiLanguageId(getDefaultLanguageCode())
                    Timber.i("apiIdByDefault = $apiIdByDefault")

                    apiIdByDefault?.let {
                        getDailyPictureAndSet(it.toInt())
                    } ?: kotlin.run {
                        Timber.e("ApiByDefault cannot be fetched.")
                    }
                }
            })

        /**
         * Let us observe on user settings, once.
         */
        model.userSettings.observeOnce(this, Observer<Resource<UserSettings>> {resource ->
            when (resource.apiStatus) {
                Constants.ApiStatus.SUCCESS -> {
                    Timber.i("Received userSettings from api.")

                    resource.data?.let {
                        Timber.i("We have settings! ${it.toString()}")
                        // set is checked
                        switchShowNotif.isChecked = it.sendNotifications
                        // hide/show notif layout
                        if (!it.sendNotifications) {
                            hideTimeNotificationLayout()
                        } else {
                            showTimeNotificationLayout()
                        }

                        // set time / hour and minute
                        txtHours.text = if (it.notificationTime.hourOfDay < 10) {
                            "0${it.notificationTime.hourOfDay}"
                        } else {
                            it.notificationTime.hourOfDay.toString()
                        }

                        txtMinutes.text = if(it.notificationTime.minuteOfHour < 10) {
                            "0${it.notificationTime.minuteOfHour}"
                        } else {
                            it.notificationTime.minuteOfHour.toString()
                        }

                        // register broadcastReceiver
                        registerBroadCastReceiverAndSchedule(it)
                    } ?: kotlin.run {
                        Timber.i("UserSettings are null")
                        hideTimeNotificationLayout()
                    }
                }

                Constants.ApiStatus.ERROR -> {
                    hideTimeNotificationLayout()

                    resource.errorType?.let {
                        when (it) {
                            Constants.ErrorType.BACKEND_API -> {
                                // showCustomMessage(resources.getString(R.string.error_backend_failure))
                            }
                            Constants.ErrorType.LOCAL_DB -> {
                                // showCustomMessage(resources.getString(R.string.error_local_db_failure))
                            }
                            Constants.ErrorType.NETWORK -> {
                                // showCustomMessage(resources.getString(R.string.error_network_failure))
                            }
                        }
                    }
                }

                Constants.ApiStatus.LOADING -> {
                }
            }

        })

        moveYtoDefaultPosition = getHeight() * hidePercentage
        itemElementHeight = (getHeight().div(2)) * 0.105f

        // hide it
        ObjectAnimator.ofFloat(mainSliderGroup, "translationY", moveYtoDefaultPosition).apply {
            duration = hideAnimationDurationSec
            start()
        }

        isUpdating = MutableLiveData()

        isUpdating.observe(this, Observer { updatingDb ->
            if (updatingDb) {
                loader.show()
            } else {
                loader.hide()
            }
        })
    }

    private fun setLanguageFromUserSettings(language: String) {
        Timber.i("Setting language from the settings.")
        languageArrayAdapter?.let {
            var langPosition = it.getPosition(language)
            Timber.d("Position of language ${language}, is $langPosition.")

            if (langPosition != -1) {
                Timber.w("Language $langPosition is -1, setting to default $DEFAULT_LOCALE")
                langPosition = it.getPosition(DEFAULT_LOCALE)
            }

            spnLanguage.setSelection(langPosition)
        } ?: kotlin.run {
            Timber.e("languageArrayAdapter is not initialized")
        }
    }

    private fun setDefaultLanguage() {
        Timber.i("Setting default language from LOCALE.")
        val langOfDevice = getCurrentLocale().language

        languageArrayAdapter?.let {
            var langPosition = it.getPosition(langOfDevice)
            Timber.d("Position of language $langOfDevice, is $langPosition.")

            if (langPosition == -1) {
                langPosition = it.getPosition(DEFAULT_LOCALE)
                Timber.w("Language of device = $langOfDevice has position = -1, setting to default $DEFAULT_LOCALE, position = $langPosition")
            }

            spnLanguage.setSelection(langPosition)
        } ?: kotlin.run {
            Timber.e("languageArrayAdapter is not initialized")
        }
    }

    private fun getDefaultLanguageCode(): String {
        Timber.i("Setting default language from LOCALE.")
        var defaultLanguageCode = DEFAULT_LOCALE
        val langOfDevice = getCurrentLocale().language

        languageArrayAdapter?.let {
            val langPosition = it.getPosition(langOfDevice)
            Timber.d("Position of language $langOfDevice, is $langPosition.")

            if (langPosition != -1) {
                defaultLanguageCode = langOfDevice
            }
        } ?: kotlin.run {
            Timber.e("languageArrayAdapter is not initialized")
        }

        return defaultLanguageCode
    }

    private fun getCurrentLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.resources.configuration.locales.get(0)
        } else {
            this.resources.configuration.locale
        }
    }

    /**
     * Method which downloads new motivator picture from api and loads it into placeholder.
     */
    private fun getDailyPictureAndSet(apiLanguageId: Int) {
        Timber.i("Method = getDailyPictureAndSet")
        model.motivatorResource.value?.data?.let {
            Timber.i("We have record from DB.")
            if (UtilHelper.shouldLoadFromApiNew(it.lastDownloadAt)) {
                Timber.d("Shall load new picture.")
                deletePreviousPicture()
                // download new
                downloadNewAndUpdateDb(apiLanguageId)
            } else {
                Timber.i("Picture already exists on storage, will load from there.")
                loadPictureFromStorage(getLocalMotivatorFile().absolutePath)
            }
        } ?: kotlin.run {
            Timber.i("Motivator resource is null, there is no record yet.")
            downloadNewAndUpdateDb(apiLanguageId)
        }
    }

    private fun deletePreviousPicture() {
        val localFile = getLocalMotivatorFile()

        if(localFile.exists()){
            val deleted = localFile.delete()

            if(deleted){
                Timber.i("File exists on the storage: ${localFile.absolutePath} and has been successfully deleted.")
            } else {
                Timber.e("File exists on the storage: ${localFile.absolutePath} but could not be deleted.")
            }
        } else {
            Timber.e("File = ${localFile.absolutePath} does not exists on the storage!")
        }
    }

    /**
     * Main method which generates api url for the day and update db.
     */
    private fun downloadNewAndUpdateDb(apiLanguageId: Int){
        Timber.i("New picture will be downloaded. height = ${getHeight()}, width = ${getWidth()}, Lang id = $apiLanguageId.")
        val apiUrl = UtilHelper.getApiUrl(apiLanguageId, getHeight(), getWidth())
        Timber.i("apiUrl = $apiUrl.")

        loadPictureAndStore(apiUrl)
    }

    private fun loadPictureFromStorage(url: String){
        motivatorOfDay?.let {
            Glide
                .with(this)
                .load(url)
                .centerInside()
                .into(it)
        }?: kotlin.run {
            Timber.e("Cannot load picture as ImageView has not been initialized.")
        }
    }

    /**
     * Get picture from URL and store into file (prepare for sharing) and load it into placeholder of image.
     */
    private fun loadPictureAndStore(url: String) {
        if (UtilHelper.isConnected(this)) {
            motivatorOfDay?.let {
                Glide
                    .with(this)
                    .load(url)
                    .centerInside()
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            Timber.e("Resource failed to load.")
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            resource?.let { drawable ->
                                if (url.contains(HTTP_CONST)) {
                                    val localFile = getLocalMotivatorFile()
                                    Timber.i("Path to store is ${localFile.absolutePath}")

                                    if (!localFile.exists()) {
                                        Timber.i("File was not created let us create parent dir.")
                                        localFile.parentFile?.mkdirs()
                                    }

                                    val bitmapWidth = resource.intrinsicWidth
                                    val bitmapHeight = resource.intrinsicHeight
                                    Timber.i("intrinsicWidth = $bitmapWidth, intrinsicHeight = $bitmapHeight.")

                                    if (bitmapWidth != -1 && bitmapHeight != -1) {
                                        Timber.i("Using intrinsic diameters to store the picture.")
                                        PictureLoaderHelper.writeBitmapToLocalFile(
                                            drawable.toBitmap(
                                                bitmapWidth,
                                                bitmapHeight,
                                                null
                                            ), localFile
                                        )
                                    } else {
                                        Timber.i("Using params of screen to store the picture.")
                                        PictureLoaderHelper.writeBitmapToLocalFile(
                                            drawable.toBitmap(
                                                getWidth(),
                                                getHeight(),
                                                null
                                            ), localFile
                                        )
                                    }

                                    // get date now
                                    updateLastDownloadedAt(DateTime.now())
                                } else {
                                    Timber.w("Not need to store new motivator. Path to load = $url.")
                                }
                            }?: kotlin.run {
                                Timber.e("Loaded resource is null.")
                            }

                            return false
                        }

                    })
                    .into(it)
            }?: kotlin.run {
                Timber.e("Cannot load picture as ImageView has not been initialized.")
            }
        } else {
            Timber.e("Internet not connected.")
        }
    }

    private fun switchUi(){
        val moveY: Float = if(sliderSettings.getIsContracted()){
            Timber.i("Will hide settings.")
            moveYtoDefaultPosition
        } else {
            Timber.i("Will show settings.")
            1f
        }

        mainSliderGroup.animate()
            .translationY(moveY)
            .duration = contractAnimationDurationSec
    }

    private fun getHeight(): Int{
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        return size.y
    }

    private fun getWidth(): Int{
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        return size.x
    }

    private fun getLocalMotivatorFile(): File = File(filesDir, "/images/$MOTIVATOR_NAME")

    private fun showCustomMessage(stringMessage: String){
        val snackBar: Snackbar = Snackbar
            .make(rootLayout, stringMessage, Snackbar.LENGTH_LONG)
        snackBar.show()
    }

    fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
        observe(lifecycleOwner, object : Observer<T> {
            override fun onChanged(t: T?) {
                observer.onChanged(t)
                removeObserver(this)
            }
        })
    }
}
