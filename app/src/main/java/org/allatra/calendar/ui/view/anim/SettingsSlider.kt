package org.allatra.calendar.ui.view.anim

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
    private var bezierPath: Path? = null

    // DEFS
    private var colorGradientStart: Int? = null
    private var colorGradientEnd: Int? = null

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
            if(!initiated) {
                // define paint
                paintBezier = object : Paint() {
                    init {
                        style = Style.FILL
                        isAntiAlias = true
                        shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colorGradientStart!!, colorGradientEnd!!, Shader.TileMode.MIRROR)
                    }
                }

                bezierPath = generateBezierPathObject()
                initiated = true
            }

            bezierPath?.let { path ->
                it.drawPath(path, paintBezier!!)
            } ?: kotlin.run {
                Timber.e("Paint is null.")
            }
        }?: kotlin.run {
            Timber.e("Canvas not initialized.")
        }
    }

    private fun initCustomAttributes(attrs: AttributeSet){
        val customAttributeSet = context.obtainStyledAttributes(
            attrs,
            R.styleable.SettingsSlider
        )

        colorGradientStart = customAttributeSet.getInt(R.styleable.SettingsSlider_color_start, Color.BLUE)
        colorGradientEnd = customAttributeSet.getInt(R.styleable.SettingsSlider_color_end, Color.BLUE)
        customAttributeSet.recycle()
    }

    private fun generateBezierPathObject(): Path {
        val bezierPath = Path()

        /**
         * Start from bottom, left corner to draw.
         */
        val startX = 0f
        val startY = height.toFloat()

        /**
         * Start left corner down.
         */
        bezierPath.moveTo(startX, startY)

        val endPointX1 = startX
        val endPointY1 = startY - (height * 0.84f)

        /**
         * Move up till 0,34 percent of total HEIGHT.
         */
        bezierPath.lineTo(endPointX1, endPointY1)

        val endPointX2 = width * 0.05f
        val endPointY2 = endPointY1 - (endPointY1 * 0.36f)

        //val endPointY1 = height - (height * 0.05)

        val controlPointX2 = -5f
        val controlPointY2 = endPointY2

        /**
         * Draw quarter left circle.
         */
        bezierPath.quadTo(
            controlPointX2,
            controlPointY2,
            endPointX2,
            endPointY2)

        val endPointX3 = (width * 0.42).toFloat()
        val endPointY3 = endPointY2

        /**
         * Draw straight line to the middle.
         */
        bezierPath.lineTo(endPointX3, endPointY3)

        val endPointX4 = (width * 0.58).toFloat()
        val endPointY4 = endPointY3

        val controlPointX31 = (width * 0.43).toFloat()
        val controlPointY31 = (endPointY3 - (startY * 0.11)).toFloat()

        val controlPointX32 = (width * 0.57).toFloat()
        val controlPointY32 = (endPointY3 - (startY * 0.11)).toFloat()

        /**
         * Draw middle half circle where is button located.
         */
        bezierPath.cubicTo(controlPointX31,controlPointY31,controlPointX32,controlPointY32,endPointX4, endPointY4)

        val endPointX5 = (0 + (width * 0.95)).toFloat()
        val endPointY5 = endPointY3

        /**
         * Draw line till the end.
         */
        bezierPath.lineTo(endPointX5, endPointY5)

        // right half corner
        val endPointX6 = width.toFloat()
        val endPointY6 = endPointY1

        val controlPointX6 = width + 5f
        val controlPointY6 = endPointY3

        /**
         * Quad to down quarter circle.
         */
        bezierPath.quadTo(
            controlPointX6,
            controlPointY6,
            endPointX6,
            endPointY6)

        val endPointX7 = width.toFloat()
        val endPointY7  = height.toFloat()

        /**
         * Move up till 0,34 percent of total HEIGHT.
         */
        bezierPath.lineTo(endPointX7, endPointY7)

        bezierPath.close()

        return bezierPath
    }

    fun getIsContracted() = sliderContracted
    fun setIsContracted(value: Boolean) {
        sliderContracted = value
        invalidate()
    }
}