package com.example.skyyo.animatingobjects

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList

class CustomAnimatingView : View {

    private var suns = CopyOnWriteArrayList<Sun>()
    private var moons = CopyOnWriteArrayList<Moon>()
    private val mRnd = Random(SEED.toLong())

    private var timeAnimator: TimeAnimator? = null
    private lateinit var sunDrawable: Drawable
    private lateinit var moonDrawable: Drawable

    private var baseSpeed: Float = 0F
    private var baseSizeSun: Float = 0F
    private var baseSizeMoon: Float = 0F
    private var currentPlayTime: Long = 0

    private class Sun {
        var x: Float = 0F
        var y: Float = 0F
        var scale: Float = 0F
        var alpha: Float = 0F
        var speed: Float = 0F
    }

    private class Moon {
        var x: Float = 0F
        var y: Float = 0F
        var scale: Float = 0F
        var alpha: Float = 0F
        var speed: Float = 0F
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    override fun onDraw(canvas: Canvas) {
        val viewHeight = height
        for (sun in suns) {
            // Ignore the sun if it's outside of the view bounds
            val imageSize = sun.scale * baseSizeSun
            if (sun.y + imageSize < 0 || sun.y - imageSize > viewHeight) {
                continue
            }

            // Save the current canvas state
            val save = canvas.save()

            // Move the canvas to the center of the sun
            canvas.translate(sun.x, sun.y)

            // Rotate the canvas based on how far the sun has moved
            val progress = (sun.y + imageSize) / viewHeight
            canvas.rotate(360 * progress)

            // Prepare the size and alpha of the drawable
            val size = Math.round(imageSize)
            sunDrawable.setBounds(-size, -size, size, size)
            sunDrawable.alpha = Math.round(255 * sun.alpha)

            // Draw the sun to the canvas
            sunDrawable.draw(canvas)

            // Restore the canvas to it's previous position and rotation
            canvas.restoreToCount(save)
        }
        for (moon in moons) {
            // Ignore the moon if it's outside of the view bounds
            val imageSize = moon.scale * baseSizeMoon
            if (moon.y + imageSize < 0 || moon.y - imageSize > viewHeight) {
                continue
            }
            val save = canvas.save()
            canvas.translate(moon.x, moon.y)
            val progress = (moon.y + imageSize) / viewHeight
            canvas.rotate(360 * progress)
            val size = Math.round(imageSize)
            moonDrawable.setBounds(-size, -size, size, size)
            moonDrawable.alpha = Math.round(255 * moon.alpha)
            moonDrawable.draw(canvas)
            canvas.restoreToCount(save)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        timeAnimator = TimeAnimator()
        timeAnimator?.setTimeListener(TimeAnimator.TimeListener { animation, totalTime, deltaTime ->
            if (!isLaidOut) {
                // Ignore all calls before the view has been measured and laid out.
                return@TimeListener
            }
            updateState(deltaTime.toFloat())
            invalidate()
        })
        timeAnimator?.start()
    }

    fun addSun() {
        val sun = Sun()
        initSun(sun, width, height)
        suns.add(sun)
        timeAnimator?.start()
    }

    fun addMoon() {
        val moon = Moon()
        initMoon(moon, measuredWidth, measuredHeight)
        moons.add(moon)
        timeAnimator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timeAnimator?.let {
            it.cancel()
            it.setTimeListener(null)
            it.removeAllListeners()


        }.apply { timeAnimator = null }
    }

    //Pause the animation if it's running
    fun pause() {
        timeAnimator?.let {
            if (it.isRunning) {
                // Store the current play time for later.
                currentPlayTime = it.currentPlayTime
                it.pause()
            }
        }
    }

    //Resume the animation if not already running
    fun resume() {
        timeAnimator?.let {
            if (it.isPaused) {
                // Why set the current play time?
                // TimeAnimator uses timestamps internally to determine the delta given
                // in the TimeListener. When resumed, the next delta received will the whole
                // pause duration, which might cause a huge jank in the animation.
                // By setting the current play time, it will pick of where it left off.
                it.start()
                it.currentPlayTime = currentPlayTime
            }
        }
    }

    /**
     * Progress the animation by moving the stars based on the elapsed time
     *
     * @param deltaMs time delta since the last frame, in millis
     */
    private fun updateState(deltaMs: Float) {
        // Converting to seconds since PX/S constants are easier to understand
        val deltaSeconds = deltaMs / 1000f
//        val viewWidth = width
//        val viewHeight = height

        for (sun in suns) {
            // Move the sun based on the elapsed time and it's speed
            sun.y -= sun.speed * deltaSeconds

            //used for continuous animation
            //If the sun is completely outside of the view bounds after
            //updating it's position, recycle it.
//            val size = sun.scale * baseSizeSun
//            if (sun.y + size < 0) {
//                invalidate()
//                initSun(sun, viewWidth, viewHeight)
//            }
        }

        for (moon in moons) {
            moon.y -= moon.speed * deltaSeconds
        }
    }

    //Initialize the given object by randomizing it's position, scale and alpha
    private fun initSun(sun: Sun, viewWidth: Int, viewHeight: Int) {
        // Set the scale based on a min value and a random multiplier
        sun.scale = SCALE_MIN_PART + SCALE_RANDOM_PART * mRnd.nextFloat()

        // Set X to a random value within the width of the view
        sun.x = viewWidth * mRnd.nextFloat()

        // Set the Y position
        // Start at the bottom of the view
        sun.y = viewHeight.toFloat()
        // The Y value is in the center of the sun, add the size
        // to make sure it starts outside of the view bound
        sun.y += sun.scale * baseSizeSun
        // Add a random offset to create a small delay before the
        // sun appears again.
        sun.y += viewHeight * mRnd.nextFloat() / 4f

        // The alpha is determined by the scale of the sun and a random multiplier.
        sun.alpha = ALPHA_SCALE_PART * sun.scale + ALPHA_RANDOM_PART * mRnd.nextFloat()
        // The bigger and brighter a sun is, the faster it moves
        sun.speed = baseSpeed * sun.alpha * sun.scale
    }

    private fun initMoon(moon: Moon, viewWidth: Int, viewHeight: Int) {
        moon.scale = SCALE_MIN_PART + SCALE_RANDOM_PART * mRnd.nextFloat()
        moon.x = viewWidth * mRnd.nextFloat()
        moon.y = viewHeight.toFloat()
        moon.y += moon.scale * baseSizeMoon
        moon.y += viewHeight * mRnd.nextFloat() / 4f
        moon.alpha = ALPHA_SCALE_PART * moon.scale + ALPHA_RANDOM_PART * mRnd.nextFloat()
        moon.speed = baseSpeed * moon.alpha * moon.scale
    }

    private fun init() {
        baseSpeed = BASE_SPEED_DP_PER_S * resources.displayMetrics.density
        sunDrawable = ContextCompat.getDrawable(context, R.drawable.ic_sun)!!
        baseSizeSun = Math.max(sunDrawable.intrinsicWidth, sunDrawable.intrinsicHeight) / 2f
        moonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_moon)!!
        baseSizeMoon = Math.max(moonDrawable.intrinsicWidth, moonDrawable.intrinsicHeight) / 2f
    }

    companion object {

        //Minimum scale of a image
        private const val SCALE_MIN_PART = 0.45f
        //How much of the scale that's based on randomness
        private const val SCALE_RANDOM_PART = 0.55f
        //How much of the alpha that's based on the scale of the image
        private const val ALPHA_SCALE_PART = 0.5f
        //How much of the alpha that's based on randomness
        private const val ALPHA_RANDOM_PART = 0.5f

        private const val BASE_SPEED_DP_PER_S = 200
        private const val SEED = 1337
        //private val TOTAL = 0
    }
}
