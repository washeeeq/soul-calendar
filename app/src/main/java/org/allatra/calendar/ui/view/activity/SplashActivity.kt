package org.allatra.calendar.ui.view.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_splash.*
import org.allatra.calendar.R

class SplashActivity : AppCompatActivity() {
    companion object {
        private const val DELAY_SPLASH = 1L
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // set orientation to LANDSCAPE
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        Handler().postDelayed(
            {
                startActivity(Intent(this@SplashActivity, CalendarActivity::class.java))
                // close this activity
                finish()
            }, DELAY_SPLASH * 1000
        ) // wait for 3 seconds
    }
}