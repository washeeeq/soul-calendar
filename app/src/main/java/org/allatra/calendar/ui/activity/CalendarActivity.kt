package org.allatra.calendar.ui.activity

import android.animation.ObjectAnimator
import android.graphics.Point
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_calendar.*
import org.allatra.calendar.BuildConfig
import org.allatra.calendar.R
import timber.log.Timber


class CalendarActivity : AppCompatActivity() {
    private var moveY: Float = 0f
    private val contractAnimationDurationSec = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {

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


    }

    private fun initUi(){
        moveY = (getHeight().div(2)) * 0.8f

        // move back to position 0
        ObjectAnimator.ofFloat(sliderSettings, "translationY", moveY).apply {
            duration = 100
            start()
        }
    }

    private fun switchUi(){
        var moveSliderSettingsY: Float = 0f
        var moveBtnExpandY: Float = 0f

        if(sliderSettings.getIsContracted()){
            moveBtnExpandY = 1f
            moveSliderSettingsY = moveY
        } else {
            moveBtnExpandY = -moveY
            moveSliderSettingsY = -1f
        }

        ObjectAnimator.ofFloat(sliderSettings, "translationY", moveSliderSettingsY).apply {
            duration = contractAnimationDurationSec
            start()
        }

        ObjectAnimator.ofFloat(btnExpand, "translationY", moveBtnExpandY).apply {
            duration = contractAnimationDurationSec
            start()
        }

        ObjectAnimator.ofFloat(txtSettings, "translationY", moveBtnExpandY).apply {
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
