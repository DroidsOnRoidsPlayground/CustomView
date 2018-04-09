package pl.droidsonrioids.customview

import android.animation.ValueAnimator
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.RESTART
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlinx.android.parcel.Parcelize


class CustomView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val paint = Paint()
    private val arcPaint = Paint()
    private val androidWidth = context.resources.getDimensionPixelSize(R.dimen.width)
    private val androidHeight = context.resources.getDimensionPixelSize(R.dimen.height)
    private val radius = context.resources.getDimension(R.dimen.radius)
    private val animator = ValueAnimator.ofFloat(0f, -180f, -100f, -180f, -100f, -180f, 0f)
            .setDuration(3000)
            .apply {
                repeatMode = RESTART
                repeatCount = INFINITE
            }
    private var translation = Pair(0f, 0f)
    private val detectorListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return touchArea.contains(e.x, e.y)
        }

        override fun onScroll(start: MotionEvent, end: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            with(translation) {
                translation = first - distanceX to second - distanceY
            }
            return true
        }

    }
    private val touchArea = RectF()

    private val detector = GestureDetector(context, detectorListener)

    private var handRotation = 0f
    private val listener = ValueAnimator.AnimatorUpdateListener {
        handRotation = it.animatedValue as Float
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.addUpdateListener(listener)
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.removeUpdateListener(listener)
        animator.cancel()

    }

    override fun onDraw(canvas: Canvas) {
        val left = (width - androidWidth) / 2f
        val top = (height - androidHeight) / 2f
        paint.color = Color.parseColor("#FF0000")
        arcPaint.color = Color.parseColor("#A4C639")
        val right = width - left
        val bottom = height - top
        val halfWidth = (right - left) / 2
        canvas.save()
        val (translationX, translationY) = translation
        canvas.translate(translationX, translationY)
        //head
        canvas.drawArc(left, top - halfWidth / 8 - halfWidth, right, top - halfWidth / 8 + halfWidth, 180f, 180f, true, arcPaint)
        //left hand
        canvas.drawRoundRect(left - halfWidth / 2, top, left - halfWidth / 8, bottom - halfWidth / 2, radius, radius, arcPaint)
        //left leg
        canvas.drawRoundRect(left + halfWidth / 4, bottom - radius, left + halfWidth / 1.3f, bottom + halfWidth, radius, radius, arcPaint)
        //right leg
        canvas.drawRoundRect(right - halfWidth / 4, bottom - radius, right - halfWidth / 1.3f, bottom + halfWidth, radius, radius, arcPaint)
        //body
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, arcPaint)
        //right hand
        canvas.rotate(handRotation, right + halfWidth / 3, top + halfWidth / 8)
        canvas.drawRoundRect(right + halfWidth / 8, top, right + halfWidth / 2, bottom - halfWidth / 2, radius, radius, arcPaint)
        canvas.restore()
        touchArea.set(left + translationX, top + translationY, right + translationX, bottom + translationY)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return detector.onTouchEvent(event)
    }
}