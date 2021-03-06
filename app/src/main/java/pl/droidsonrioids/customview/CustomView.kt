package pl.droidsonrioids.customview

import android.animation.*
import android.animation.ValueAnimator.REVERSE
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcelable
import android.support.animation.DynamicAnimation
import android.support.animation.FloatValueHolder
import android.support.animation.SpringAnimation
import android.support.animation.SpringForce
import android.support.animation.SpringForce.STIFFNESS_LOW
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


class CustomView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val paint = Paint()
    private val cutOutPaint = Paint()

    var androidWidth: Int = context.resources.getDimensionPixelSize(R.dimen.width)
        set(value) {
            field = value
            requestLayout()
        }
    var limitToBounds: Boolean = false
    var color: Int
        get() = paint.color
        set(value) {
            paint.color = value
            invalidate()
        }

    private val handAnimator = AnimatorSet()
    private val translation = floatArrayOf(0f, 0f)
    private val detectorListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            val handleTouch = touchArea.contains(e.x, e.y)
            if (handleTouch) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
            return handleTouch
        }

        override fun onScroll(start: MotionEvent, end: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            cancelSpring()
            translation[0] -= distanceX
            translation[1] -= distanceY
            invalidate()
            return true
        }

        override fun onFling(start: MotionEvent, end: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            return onScrollEnd(velocityX, velocityY)
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            return performClick()
        }
    }

    private val touchArea = RectF()

    private val detector = GestureDetector(context, detectorListener)

    private var handRotation = 0f
    private val listener = ValueAnimator.AnimatorUpdateListener {
        handRotation = it.animatedValue as Float
        invalidate()
    }

    private var isRotating = false
    private var baseRotation = 0f

    private val springX = SpringAnimation(FloatValueHolder()).setSpring(SpringForce().setStiffness(STIFFNESS_LOW))
    private val springY = SpringAnimation(FloatValueHolder()).setSpring(SpringForce().setStiffness(STIFFNESS_LOW))
    private val springXListener = DynamicAnimation.OnAnimationUpdateListener { _, value, _ -> translation[0] = value; invalidate() }
    private val springYListener = DynamicAnimation.OnAnimationUpdateListener { _, value, _ -> translation[1] = value; invalidate() }

    private val drawingWidth get() = max(width - paddingLeft - paddingRight, 0)
    private val drawingHeight get() = max(height - paddingTop - paddingBottom, 0)
    private val maxTranslationX get() = if (limitToBounds) max(drawingWidth / 2f - androidWidth * 0.9f + paddingRight, 0f) else Float.POSITIVE_INFINITY
    private val minTranslationX get() = if (limitToBounds) min(-drawingWidth / 2f + androidWidth * 0.5f - paddingLeft, 0f) else Float.NEGATIVE_INFINITY
    private val maxTranslationY get() = if (limitToBounds) max(drawingHeight / 2f - androidWidth * 0.65f + paddingBottom, 0f) else Float.POSITIVE_INFINITY
    private val minTranslationY get() = if (limitToBounds) min(-drawingHeight / 2f + androidWidth * 0.65f - paddingTop, 0f) else Float.NEGATIVE_INFINITY
    private val currentTranslationX get() = translation[0].coerceIn(minTranslationX, maxTranslationX)
    private val currentTranslationY get() = translation[1].coerceIn(minTranslationY, maxTranslationY)

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CustomView, defStyleAttr, defStyleRes)
        androidWidth = attributes.getDimensionPixelSize(R.styleable.CustomView_androidWidth, androidWidth)
        limitToBounds = attributes.getBoolean(R.styleable.CustomView_limitToBounds, limitToBounds)
        paint.color = attributes.getColor(R.styleable.CustomView_color, Color.parseColor("#A4C639"))
        cutOutPaint.color = Color.WHITE
        attributes.recycle()
    }

    private fun cancelSpring() {
        springX.cancel()
        springY.cancel()
    }

    private fun onScrollEnd(velocityX: Float, velocityY: Float): Boolean {
        parent.requestDisallowInterceptTouchEvent(false)
        val x = currentTranslationX
        val y = currentTranslationY
        if (x != 0f || y != 0f) {
            springX.setStartVelocity(velocityX)
                    .setStartValue(x)
                    .setMaxValue(maxTranslationX)
                    .setMinValue(minTranslationX)
                    .animateToFinalPosition(0f)
            springY.setStartVelocity(velocityY)
                    .setStartValue(y)
                    .setMaxValue(maxTranslationY)
                    .setMinValue(minTranslationY)
                    .animateToFinalPosition(0f)
            return true
        }
        return false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handAnimator
                .apply { startDelay = (Math.random() * 10).toLong() * 50 }
                .playSequentially(
                        ValueAnimator.ofFloat(0f, -170f)
                                .setDuration(800)
                                .apply {
                                    addUpdateListener(listener)
                                    interpolator = TimeInterpolator { getPowInOut(it, 2) }
                                },
                        ValueAnimator.ofFloat(-170f, -100f)
                                .setDuration(500)
                                .apply {
                                    addUpdateListener(listener)
                                    interpolator = TimeInterpolator { getPowInOut(it, 2) }
                                    repeatMode = REVERSE
                                    repeatCount = 5
                                },
                        ValueAnimator.ofFloat(-170f, 0f)
                                .setDuration(800)
                                .apply {
                                    addUpdateListener(listener)
                                    interpolator = TimeInterpolator { getPowInOut(it, 5) }
                                }
                )
        handAnimator.addListener(object : AnimatorListenerAdapter() {
            private var canceled: Boolean = false

            override fun onAnimationCancel(animation: Animator?) {
                canceled = true
            }

            override fun onAnimationEnd(animation: Animator?) {
                if (!canceled) {
                    handAnimator.start()
                }
                canceled = false
            }
        })
        springX.addUpdateListener(springXListener)
        springY.addUpdateListener(springYListener)
        handAnimator.start()
    }

    private fun getPowInOut(fraction: Float, pow: Int): Float {
        (fraction * 2).let {
            return if (it < 1) 0.5f * it.pow(pow)
            else 1f - 0.5f * abs((2f - it).pow(pow))
        }

    }

    override fun onDetachedFromWindow() {
        springX.removeUpdateListener(springXListener)
        springY.removeUpdateListener(springYListener)
        handAnimator.cancel()
        cancelSpring()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        setMeasuredDimension(
                measureDimension(desiredWidth, widthMeasureSpec),
                measureDimension(desiredHeight, heightMeasureSpec)
        )
    }

    @SuppressLint("SwitchIntDef")
    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)

        return when (specMode) {
            View.MeasureSpec.EXACTLY -> specSize
            View.MeasureSpec.AT_MOST -> min(desiredSize, specSize)
            else -> desiredSize
        }
    }

    override fun getSuggestedMinimumHeight(): Int {
        return max(super.getSuggestedMinimumHeight(), (androidWidth * 1.3f).toInt())
    }

    override fun getSuggestedMinimumWidth(): Int {
        return max(super.getSuggestedMinimumWidth(), (androidWidth * 1.8f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val left = (drawingWidth - androidWidth) / 2f + paddingLeft
        val top = (drawingHeight - androidWidth * 1.30f) / 2f + paddingTop
        val handOffset = androidWidth * 0.42f
        val legOffset = androidWidth * 0.75f
        canvas.run {
            save()
            translate(currentTranslationX, currentTranslationY)
            rotate(androidRotation, left + androidWidth / 2f, top + androidWidth / 2f)

            //head
            drawHead(left + androidWidth * 0.18f, top + androidWidth * 0.05f)
            //left hand
            drawLimb(left, top + handOffset)
            //left leg
            drawLimb(left + androidWidth * 0.28f, top + legOffset)
            //right leg
            drawLimb(left + androidWidth * 0.56f, top + legOffset)
            //body
            drawBody(left, top + handOffset)
            //right hand
            rotate(handRotation, left + androidWidth * 0.925f, top + handOffset + androidWidth * 0.075f)
            drawLimb(left + androidWidth * 0.84f, top + handOffset)
            restore()
        }
        touchArea.set(left + translationX, top + translationY, left + androidWidth + translationX, top + androidWidth * 1.30f + translationY)
    }

    private fun Canvas.drawBody(left: Float, top: Float) {
        val right = left + androidWidth * 0.82f
        val leftWithOffset = left + androidWidth * 0.18f
        drawRect(leftWithOffset, top, right, top + androidWidth * 0.5f, paint)
        drawRoundRect(leftWithOffset, top, right, top + androidWidth * 0.6f, androidWidth * 0.08f, androidWidth * 0.08f, paint)
    }

    private fun Canvas.drawLimb(left: Float, top: Float) {
        drawRoundRect(left, top, left + androidWidth * 0.16f, top + androidWidth * 0.55f, androidWidth * 0.1f, androidWidth * 0.1f, paint)
    }

    private fun Canvas.drawHead(left: Float, top: Float) {
        drawArc(left, top, left + androidWidth * 0.64f, top + androidWidth * 0.7f, 180f, 180f, true, paint)
        val eyesOffset = androidWidth * 0.17f
        rotate(40f, left + androidWidth * 0.32f, top + androidWidth * 0.25f)
        drawRoundRect(left + androidWidth * 0.29f, top - androidWidth * 0.13f, left + androidWidth * 0.35f, top + androidWidth * 0.1f, androidWidth * 0.2f, androidWidth * 0.2f, paint)
        rotate(-80f, left + androidWidth * 0.32f, top + androidWidth * 0.25f)
        drawRoundRect(left + androidWidth * 0.29f, top - androidWidth * 0.13f, left + androidWidth * 0.35f, top + androidWidth * 0.1f, androidWidth * 0.2f, androidWidth * 0.2f, paint)
        rotate(40f, left + androidWidth * 0.32f, top + androidWidth * 0.25f)
        drawCircle(left + androidWidth * 0.2f, top + eyesOffset, androidWidth * 0.02f, cutOutPaint)
        drawCircle(left + androidWidth * 0.42f, top + eyesOffset, androidWidth * 0.02f, cutOutPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount == 2) {
                baseRotation = androidRotation + calculateRotation(event)
                isRotating = true
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> if (event.pointerCount == 2) isRotating = false
            MotionEvent.ACTION_MOVE -> if (isRotating) updateRotation(event)
        }
        return isRotating || detector.onTouchEvent(event) || (event.action == MotionEvent.ACTION_UP && onScrollEnd(0f, 0f))
    }

    private var androidRotation = 0f

    private fun updateRotation(event: MotionEvent) {
        androidRotation = baseRotation - calculateRotation(event)
        invalidate()
    }

    private fun calculateRotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = Math.atan2(deltaY, deltaX)
        return -Math.toDegrees(radians).toFloat()
    }

    override fun onSaveInstanceState(): Parcelable {
        return CustomSavedState(super.onSaveInstanceState(), androidRotation)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val customSavedState = state as? CustomSavedState
        super.onRestoreInstanceState(customSavedState?.superState)
        customSavedState?.let {
            androidRotation = it.androidRotation
        }
    }
}

class CustomSavedState(superState: Parcelable, val androidRotation: Float) : View.BaseSavedState(superState)