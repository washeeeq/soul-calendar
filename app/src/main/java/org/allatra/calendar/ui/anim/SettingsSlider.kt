package org.allatra.calendar.ui.anim

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import org.allatra.calendar.R
import timber.log.Timber

class SettingsSlider: View {

    private var sliderContracted = true
    private var initiated = false
    private var paintBezier: Paint? = null

    // DEFS
    private var defaultSliderColor: Int? = null

    constructor(context: Context?) : super(context){

    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){
        attrs?.let {
            initCustomAttributes(attrs)
        }
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        attrs?.let {
            initCustomAttributes(attrs)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            paintBezier?.let { paint ->
                it.drawPath(drawBezierPathObject(), paint)
            }?: kotlin.run {
                Timber.e("Paint is null.")
            }
        }?: kotlin.run {
            Timber.e("Canvas not initialized.")
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if(!initiated) {
            // define paint
            paintBezier = object : Paint() {
                init {
                    style = Style.FILL
                    isAntiAlias = true
                    defaultSliderColor?.let {
                        color = it
                    }
                }
            }
        }
    }

    private fun initCustomAttributes(attrs: AttributeSet){
        val customAttributeSet = context.obtainStyledAttributes(
            attrs,
            R.styleable.SettingsSlider
        )

        defaultSliderColor = customAttributeSet.getInt(R.styleable.SettingsSlider_contracted_color, Color.BLUE)
        customAttributeSet.recycle()
    }

    private fun drawBezierPathObject(): Path{
        val bezierPath = Path()

        /**
         * Start from bottom, left corner to draw.
         */
        val startY = height.toFloat()
        val startX = 0f

        // start
        bezierPath.moveTo(startX, startY)

        val endPointX1 = 0 + (width * 0.05)
        val endPointY1 = height - (height * 0.07)

        val controlPointX1 = -5f
        val controlPointY1 = endPointY1

        // first right corner
        bezierPath.quadTo(
            controlPointX1,
            controlPointY1.toFloat(),
            endPointX1.toFloat(),
            endPointY1.toFloat())

        val controlPointX2 = (width * 0.4).toFloat()
        val controlPointY2 = endPointY1.toFloat()

        // straight line to
        bezierPath.lineTo(controlPointX2, controlPointY2)

        val endPointX3 = (width * 0.6).toFloat()
        val endPointY3 = endPointY1.toFloat()

        val controlPointX31 = (width * 0.43).toFloat()
        val controlPointY31 = (endPointY1 - (height * 0.12)).toFloat()

        val controlPointX32 = (width * 0.57).toFloat()
        val controlPointY32 = (endPointY1 - (height * 0.12)).toFloat()

        // draw over path
        bezierPath.cubicTo(controlPointX31,controlPointY31,controlPointX32,controlPointY32,endPointX3, endPointY3)

        // draw to end
        val controlPointX4 = (0 + (width * 0.95)).toFloat()
        val controlPointY4 = endPointY1.toFloat()

        bezierPath.lineTo(controlPointX4, controlPointY4)

        // right corner
        val endPointX5 = width.toFloat()
        val endPointY5 = height.toFloat()

        val controlPointX5 = width + 5f
        val controlPointY5 = endPointY1.toFloat()

        bezierPath.quadTo(
            controlPointX5,
            controlPointY5,
            endPointX5,
            endPointY5)

        bezierPath.close()

        return bezierPath
    }
}