package org.allatra.calendar.ui.activity

import android.Manifest
import android.animation.ObjectAnimator
import android.app.TimePickerDialog
import android.content.Intent
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.withContext
import org.allatra.calendar.BuildConfig
import org.allatra.calendar.R
import org.allatra.calendar.db.RealmHandlerObject
import org.allatra.calendar.db.RealmHandlerObject.DEFAULT_ID
import org.allatra.calendar.db.Settings
import org.allatra.calendar.service.WakefulReceiver
import org.allatra.calendar.service.WakefulReceiver.Companion.WAKE_RECEIVE_NOTIF
import org.allatra.calendar.util.PictureLoaderHelper
import org.allatra.calendar.util.UtilHelper
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
    private var settings: Settings? = null
    private lateinit var isUpdating: MutableLiveData<Boolean>
    private var languageArrayAdapter: ArrayAdapter<CharSequence>? = null
    private var wakefulReceiver: WakefulReceiver? = null

    companion object {
        private const val MOTIVATOR_NAME = "motivator_of_the_day.jpeg"
        private const val CONTENT_TYPE_IMAGE = "image/jpeg"
        private const val FILE_PROVIDER = ".fileprovider"
        private const val HTTP_CONST = "http"

        private const val DB_DEFAULT_ALLOW_NOTIFICATIONS = false
        private const val DB_DEFAULT_NOTIFICATION_TIME = "10:00"
        private const val DB_DEFAULT_LANGUAGE = "ru"
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
            if(isChecked){
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
            settings?.notificationTime?.let {
                hourOfDay = it.hourOfDay
                minute = it.minuteOfHour
            }

            val timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { view, setHour, setMinute ->
                if(setHour < 10){
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
            launch(Dispatchers.IO) {
                updateDbAndSchedule()
            }

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

            }?: kotlin.run {
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
        getDailyPictureAndSet()
    }

    override fun onDestroy() {
        super.onDestroy()
        // deregister on quit
        unregisterReceiver(wakefulReceiver)
    }

    private suspend fun updateLastDownloadedAt(lastDownloadedAt: Date) = withContext(Dispatchers.IO) {
        RealmHandlerObject.updateDefaultSettings(lastDownloadedAt)
    }

    private suspend fun updateDbAndSchedule() = withContext(Dispatchers.IO) {
        val languageString = spnLanguage.selectedItem.toString()
        val allowNotifications = switchShowNotif.isChecked
        val notificationTimeString = "${txtHours.text}${txtDivider.text}${txtMinutes.text}"
        val notificationTime = LocalTime.parse(notificationTimeString)

        if(settings == null){
            Timber.tag("updateDbModel").e("This shall not happen. Settings are null.")
        } else {
            var isChanged = false

            settings?.let {
                if(!it.language.equals(languageString)){
                    isChanged = true
                } else if(!it.allowNotifications.equals(allowNotifications)){
                    isChanged = true
                } else if(!it.notificationTime.equals(notificationTime)){
                    isChanged = true
                } else {
                    Timber.i("There was not change in settings, nothing to save.")
                }

                if(isChanged){
                    it.language = languageString
                    it.allowNotifications = allowNotifications
                    it.notificationTime = notificationTime

                    RealmHandlerObject.updateDefaultSettings(it)

                    if(allowNotifications) {
                        // remove previous
                        removeNotificationsSchedule()
                        // schedule new
                        scheduleNotifications()
                    } else {
                        removeNotificationsSchedule()
                    }
                }
            }
        }

        Thread.sleep(500)
        isUpdating.postValue(false)
    }

    private fun registerBroadCastReceiver(){
        wakefulReceiver = WakefulReceiver()
        registerReceiver(wakefulReceiver, IntentFilter(WAKE_RECEIVE_NOTIF))
    }

    private fun removeNotificationsSchedule(){
        wakefulReceiver?.cancelAlarm(applicationContext)
    }

    private fun scheduleNotifications(){
        settings?.notificationTime?.let {
            wakefulReceiver?.setAlarm(applicationContext, it.hourOfDay, it.minuteOfHour)
            Timber.i("Notification was scheduled.")
        }
    }

    /**
     * Create DEFAULT db model if none.
     */
    private fun createNewDbDefaultModel(){
        settings = Settings(DEFAULT_ID, DB_DEFAULT_LANGUAGE, DB_DEFAULT_ALLOW_NOTIFICATIONS, LocalTime.parse(DB_DEFAULT_NOTIFICATION_TIME), null)

        settings?.let{
            RealmHandlerObject.createDefaultSettings(it)
        }?: kotlin.run {
            Timber.e("App failed to create settings object.")
        }
    }

    /**
     * Hide or shows settings.
     */
    private fun showOrHideSettings(){
        sliderSettings?.let {
            if(it.getIsContracted()){
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


    private fun hideTimeNotificationLayout(){
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

    private fun init(){
        moveYtoDefaultPosition = (getHeight().div(2)) * 0.8f
        itemElementHeight = (getHeight().div(2)) * 0.105f

        // hide it
        ObjectAnimator.ofFloat(mainSliderGroup, "translationY", moveYtoDefaultPosition).apply {
            duration = hideAnimationDurationSec
            start()
        }

        // init languages
        languageArrayAdapter = ArrayAdapter.createFromResource(this, R.array.languages_image, R.layout.custom_spinner_textview)
        (languageArrayAdapter as ArrayAdapter<CharSequence>).setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        spnLanguage.adapter = languageArrayAdapter
        spnLanguage.setSelection(0)
        spnLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // nothing here
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val stringValue = parent?.getItemAtPosition(position) as String
                val stringCancelValue = languageArrayAdapter?.getItem(1).toString()

                if(stringValue.equals(stringCancelValue)){
                    Timber.i("User cancelled, back to RU.")
                    spnLanguage.setSelection(0, false)
                }
            }

        }

        RealmHandlerObject.initWithContext(this)
        settings = RealmHandlerObject.getDefaultSettings()

        if(settings == null){
            Timber.i("Settings are not created yet. Default will be added")
            createNewDbDefaultModel()
        }

        // set previously stored
        settings?.let {
            Timber.i("Settings existing and will set up UI.")
            // set language
            val positionToSelect = languageArrayAdapter?.getPosition(it.language)
            positionToSelect?.let { position ->
                if(positionToSelect != -1) {
                    spnLanguage.setSelection(position)
                } else {
                    Timber.e("Position is -1, text not found.")
                }
            }

            // set is checked
            switchShowNotif.isChecked = it.allowNotifications

            if(!it.allowNotifications){
                hideTimeNotificationLayout()
            } else {
                showTimeNotificationLayout()
            }

            // set time / hour and minute
            txtHours.text = if(it.notificationTime.hourOfDay < 10){
                "0${it.notificationTime.hourOfDay}"
            } else {it.notificationTime.hourOfDay.toString()}

            txtMinutes.text = if(it.notificationTime.minuteOfHour < 10){
                "0${it.notificationTime.minuteOfHour}"
            } else {it.notificationTime.minuteOfHour.toString()}
        }?: kotlin.run {
            Timber.e("Settings were not initialized, this shall not happen.")
        }

        isUpdating = MutableLiveData()

        isUpdating.observe(this, Observer { updatingDb ->
            if(updatingDb){
                loader.show()
            } else {
                loader.hide()
            }
        })

        registerBroadCastReceiver()
    }

    /**
     * Method which downloads new motivator picture from api and loads it into placeholder.
     */
    private fun getDailyPictureAndSet(){
        Timber.i("Method = getDailyPictureAndSet")
        settings?.let {
            it.lastDownloadAt?.let { lastDate ->
                if(UtilHelper.shouldLoadFromApiNew(lastDate)){
                    Timber.i("Shall load new picture.")
                    deletePreviousPicture()
                    downloadNewAndUpdateDb()
                } else {
                    Timber.i("Picture already exists on storage, will load.")
                    loadPictureFromStorage(getLocalMotivatorFile().absolutePath)
                }
            }?: kotlin.run {
                downloadNewAndUpdateDb()
            }
        }?: kotlin.run {
            Timber.e("Settings are null, this shall not happen.")
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
    private fun downloadNewAndUpdateDb(){
        Timber.i("New picture will be downloaded. height = ${getHeight()}, width = ${getWidth()}, Lang id = 1.")
        val apiUrl = UtilHelper.getApiUrl(1, getHeight(), getWidth())
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
    private fun loadPictureAndStore(url: String){
        if(UtilHelper.isConnected(this)) {
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
                                if(url.contains(HTTP_CONST)) {
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

                                    launch(Dispatchers.IO) {
                                        val lastDownloadedAt = Date()
                                        updateLastDownloadedAt(lastDownloadedAt)
                                    }
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
            moveYtoDefaultPosition
        } else {
            -1f
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
}
