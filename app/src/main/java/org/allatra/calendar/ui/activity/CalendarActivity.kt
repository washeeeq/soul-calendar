package org.allatra.calendar.ui.activity

import android.animation.ObjectAnimator
import android.app.TimePickerDialog
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_calendar.*
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.allatra.calendar.BuildConfig
import org.allatra.calendar.R
import org.allatra.calendar.common.EnumDefinition
import org.allatra.calendar.db.RealmHandlerObject
import org.allatra.calendar.db.RealmHandlerObject.DEFAULT_ID
import org.allatra.calendar.db.Settings
import org.allatra.calendar.util.UtilHelper
import org.joda.time.LocalTime
import timber.log.Timber


class CalendarActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private var moveYtoDefaultPosition: Float = 0f
    private val contractAnimationDurationSec = 700L
    private val hideAnimationTimeNotifLayoutDuration = 500L
    private val hideAnimationDurationSec = 100L
    private var itemElementHeight: Float = 0f
    private var settings: Settings? = null
    private lateinit var isUpdating: MutableLiveData<Boolean>
    private var languageArrayAdapter: ArrayAdapter<CharSequence>? = null

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
                txtHours.text = hourOfDay.toString()
                txtMinutes.text = minute.toString()
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
            //TODO: Update backend service for the notifications.
            showOrHideSettings()
        }

        //TODO: Add SHARE button, once picture is to download
    }

    private suspend fun updateDbModel() = withContext(Dispatchers.IO) {
        val languageString = spnLanguage.selectedItem.toString()
        val allowNotifications = switchShowNotif.isChecked
        val notificationTimeString = "${txtHours.text}${txtDivider.text}${txtMinutes.text}"
        val notificationTime = LocalTime.parse(notificationTimeString)

        if(settings == null){
            settings = Settings(DEFAULT_ID, languageString, allowNotifications, notificationTime)

            settings?.let{
                RealmHandlerObject.createDefaultSettings(it)
            }?: kotlin.run {
                Timber.e("App failed to create settings object.")
            }
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
                }
            }
        }

        Thread.sleep(500)
        isUpdating.postValue(false)
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

        hideTimeNotificationLayout()
    }

    private fun init() {
        RealmHandlerObject.initWithContext(this)
        settings = RealmHandlerObject.getDefaultSettings()

        // init UI
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
            // set time
            txtHours.text = it.notificationTime.hourOfDay.toString()

            val minuteString = it.notificationTime.minuteOfHour.toString()
            if(minuteString.equals("0")){
                txtMinutes.text = "00"
            } else {
                txtMinutes.text = minuteString
            }
        }

        isUpdating = MutableLiveData()

        isUpdating.observe(this, Observer { updatingDb ->
            if(updatingDb){
                loader.show()
            } else {
                loader.hide()
            }
        })

        // download picture
        //TODO: Use Util helper to construcr URL, use glide to load it, check internet connction before
        val apiUrl = UtilHelper.getApiUrl(1, getHeight(), getWidth())
        Timber.i("apiUrl = $apiUrl")

        // load it
        motivatorOfDay?.let {
            Glide
                .with(this)
                .load(apiUrl)
                .optionalCenterCrop()
                .into(it)
        }
    }

    private fun switchUi(){
        var moveY: Float = if(sliderSettings.getIsContracted()){
            moveYtoDefaultPosition
        } else {
            -1f
        }

        ObjectAnimator.ofFloat(mainSliderGroup, "translationY", moveY).apply {
            duration = contractAnimationDurationSec
            start()
        }
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
}
