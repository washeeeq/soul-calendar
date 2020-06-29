package org.allatra.calendar.ui.activity

import android.animation.ObjectAnimator
import android.app.TimePickerDialog
import android.content.Intent
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
import org.allatra.calendar.service.NotificationWorker
import org.allatra.calendar.util.PictureLoaderHelper
import org.allatra.calendar.util.UtilHelper
import org.joda.time.LocalTime
import timber.log.Timber
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit


class CalendarActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private var moveYtoDefaultPosition: Float = 0f
    private val contractAnimationDurationSec = 700L
    private val hideAnimationTimeNotifLayoutDuration = 500L
    private val hideAnimationDurationSec = 100L
    private var itemElementHeight: Float = 0f
    private var settings: Settings? = null
    private lateinit var isUpdating: MutableLiveData<Boolean>
    private var languageArrayAdapter: ArrayAdapter<CharSequence>? = null

    companion object {
        private const val MOTIVATOR_NAME = "motivator_of_the_day.jpeg"
        private const val CONTENT_TYPE_IMAGE = "image/jpeg"
        private const val FILE_PROVIDER = ".fileprovider"
        private const val HTTP_CONST = "http"

        private const val DB_DEFAULT_ALLOW_NOTIFICATIONS = false
        private const val DB_DEFAULT_NOTIFICATION_TIME = "10:00"
        private const val DB_DEFAULT_LANGUAGE = "ru"

        const val NOTIF_HOUR = "notif_hour"
        const val NOTIF_MINUTE = "notif_minute"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        initUi()
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
        switchShowNotif?.setOnCheckedChangeListener { buttonView, isChecked ->
            // show time
            if(isChecked){
                showTimeNotificationLayout()
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

            val timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                if(hourOfDay < 10){
                    txtHours.text = "0${hourOfDay.toString()}"
                } else {
                    txtHours.text = hourOfDay.toString()
                }

                if(minute < 10){
                    txtMinutes.text = "0${minute.toString()}"
                } else {
                    txtMinutes.text = minute.toString()
                }

            }, 10, 0, true)
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
                updateDbModel()
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
                //TODO: check if to show user message
            }
        }
    }

    /**
     * When app is resumed, get new daily picture and set.
     */
    override fun onResume() {
        super.onResume()

        // we load picture only here
        getDailyPictureAndSet()
    }

    private suspend fun updateLastDownloadedAt(lastDownloadedAt: Date) = withContext(Dispatchers.IO) {
        RealmHandlerObject.updateDefaultSettings(lastDownloadedAt)
    }

    private suspend fun updateDbModel() = withContext(Dispatchers.IO) {
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

                    //TODO: Remove notifications
                    //Register new:
                    createWorkRequest()
                }
            }
        }

        Thread.sleep(500)
        isUpdating.postValue(false)
    }

    private fun createWorkRequest(){
        settings?.notificationTime?.let {
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance()

            // Set Execution around 05:00:00 AM
            dueDate.set(Calendar.HOUR_OF_DAY, it.hourOfDay)
            dueDate.set(Calendar.MINUTE, it.minuteOfHour)
            dueDate.set(Calendar.SECOND, 0)

            if (dueDate.before(currentDate)) {
                dueDate.add(Calendar.HOUR_OF_DAY, 24)
            }

            val timeDiff = dueDate.timeInMillis.minus(currentDate.timeInMillis)

            val dailyWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .build()

            // Put time there
            val data = Data.Builder()
            data.putInt(NOTIF_HOUR, it.hourOfDay)
            data.putInt(NOTIF_MINUTE, it.minuteOfHour)

            WorkManager.getInstance(this).enqueue(dailyWorkRequest)
            Timber.i("Work has been scheduled for ${it.hourOfDay}:${it.minuteOfHour} from CalendarActivity.")
        }?.let {
            Timber.e("Work request cannot be created.")
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

    private fun initUi(){
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
    }

    private fun init() {
        RealmHandlerObject.initWithContext(this)
        settings = RealmHandlerObject.getDefaultSettings()

        if(settings == null){
            Timber.i("Settings are not created yet. Default will be added")
            createNewDbDefaultModel()
        }

        // set previously stored
        settings?.let {
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

            // set time
            txtHours.text = it.notificationTime.hourOfDay.toString()

            val minuteString = it.notificationTime.minuteOfHour.toString()
            if(minuteString.equals("0")){
                txtMinutes.text = "00"
            } else {
                txtMinutes.text = minuteString
            }
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
    }

    /**
     * Method which downloads new motivator picture from api and loads it into placeholder.
     */
    private fun getDailyPictureAndSet(){
        Timber.i("Method = getDailyPictureAndSet")
        settings?.let {
            it.lastDownloadAt?.let { lastDate ->
                if(UtilHelper.shouldLoadFromApiNew(lastDate)){
                    deletePreviousPicture()
                    downloadNewAndUpdateDb()
                } else {
                    Timber.i("Picture already exists.")
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
            Timber.i("Motivator exists - deleting.")
            localFile.delete()
        }
    }

    /**
     * Main method which generates api url for the day and update db.
     */
    private fun downloadNewAndUpdateDb(){
        Timber.i("New picture will be downloaded. height = ${getHeight()}, width = ${getWidth()}, Lang id = 1.")
        val apiUrl = UtilHelper.getApiUrl(1, getHeight(), getWidth())
        val lastDownloadedAt = Date()
        Timber.i("apiUrl = $apiUrl.")

        loadPictureAndStore(apiUrl)
        launch(Dispatchers.IO) {
            updateLastDownloadedAt(lastDownloadedAt)
        }
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

                                    if (localFile.exists()) {
                                        localFile.delete()
                                    } else {
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
            .withEndAction { Runnable {
                if(moveY == -1f){
                    sliderSettings?.visibility = View.GONE
                    Timber.d("Settings hidding...")
                }
            } }
            .withStartAction{ Runnable {
                if(moveY == moveYtoDefaultPosition){
                    sliderSettings?.visibility = View.VISIBLE
                    Timber.d("Settings to visible...")
                }
            }}
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
}
