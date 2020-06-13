package org.allatra.calendar.ui.activity

import android.animation.ObjectAnimator
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.android.synthetic.main.activity_calendar.*
import org.allatra.calendar.BuildConfig
import org.allatra.calendar.R
import timber.log.Timber


class CalendarActivity : AppCompatActivity() {
    private var moveYtoDefaultPosition: Float = 0f
    private val contractAnimationDurationSec = 700L
    private val hideAnimationTimeNotifLayoutDuration = 500L
    private val hideAnimationDurationSec = 100L
    private var itemElementHeight: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        initUi()

        btnExpand?.setOnClickListener {
            Timber.d("User clicked expand.")

            sliderSettings?.let {
                if(it.getIsContracted()){
                    it.setIsContracted(false)
                } else {
                    it.setIsContracted(true)
                }
            }

            switchUi()
        }

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
    }

    private fun showTimeNotificationLayout(){
        timeNotificationLayoutGroup.animate()
            .alpha(1f)
            .translationY(0f)
            .duration = hideAnimationTimeNotifLayoutDuration

        shareLayoutGroup.animate()
            .translationY(0f)
            .duration = hideAnimationTimeNotifLayoutDuration
    }

    private fun hideTimeNotificationLayout(){
        timeNotificationLayoutGroup.animate()
            .alpha(0.0f)
            .translationY(-itemElementHeight)
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
        val adapter = ArrayAdapter.createFromResource(this, R.array.languages_image, R.layout.custom_spinner_textview)
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        spnLanguage.adapter = adapter

        spnLanguage.setSelection(0)

        hideTimeNotificationLayout()
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

    private fun getHeight(): Float{
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        return size.y.toFloat()
    }
}
