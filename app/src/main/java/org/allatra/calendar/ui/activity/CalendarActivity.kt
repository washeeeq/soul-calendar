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
    private var moveYtoDefaultPosition: Float = 0f
    private val contractAnimationDurationSec = 700L
    private val hideAnimationDurationSec = 100L

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
    }

    private fun initUi(){
        moveYtoDefaultPosition = (getHeight().div(2)) * 0.8f

        // hide it
        ObjectAnimator.ofFloat(mainSliderGroup, "translationY", moveYtoDefaultPosition).apply {
            duration = hideAnimationDurationSec
            start()
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

    private fun getHeight(): Float{
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        return size.y.toFloat()
    }
}
