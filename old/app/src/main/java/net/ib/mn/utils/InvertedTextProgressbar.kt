package net.ib.mn.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import net.ib.mn.R
import kotlin.math.roundToInt

/**
 * ImageView that is animated like a progress view and clips
 * the text. This can be used to animate a progress like view
 * and overlap texts changing the color of the text.
 * The text and the bar are draw on canvas.
 *
 * If the pivot positions are not set, then the center of the
 * canvas will be used.
 *
 * Created by danilo on 24-03-2016.
 */
class InvertedTextProgressbar : AppCompatImageView {
    /**
     * Gets the current text to draw.
     *
     * @return The current text to draw.
     */
    /**
     * Sets the text that will overlay.
     *
     * @param text The text to draw.
     */
    /**
     * Text displayed in the progress bar.
     */
    var text: String? = ""

    /**
     * Rectangle to draw on canvas.
     */
    private val mRect = Rect()

    /**
     * Flag that indicates if the bar is being animated.
     */
    private var mIsAnimating = false
    private var mStartTime: Long = 0
    private var mDurationMs = 0
    private var endTime: Long = 0
    /**
     * Gets the max progress. Note that if this value has not
     * been set before it will return -1.
     *
     * @return The max progress.
     */
    /**
     * Sets the maximum progress value.
     *
     * @param maxProgress The maximum progress.
     */
    var maxProgress = -1
    /**
     * Gets the min progress. Note that if this value has not
     * been set before it will return -1.
     *
     * @return The min progress.
     */
    /**
     * Sets the minimum progress value.
     *
     * @param minProgress The minimum progress.
     */
    var minProgress = -1

    /**
     * Gets the current progress. Note that if it never had been
     * set before this method will return -1.
     *
     * @return The current progress of the progress bar.
     */
    var currentProgress = -1
        private set

    /**
     * X position of the text to be draw.
     */
    private var mPosX = -1

    /**
     * Y position of the text to be draw.
     */
    private var mPosY = -1

    /**
     * Paint to use for drawing the text.
     */
    private var mTextPaint: Paint? = null

    /**
     * Paint to use for drawing the text.
     */
    private var mTextInvertedPaint: Paint? = null

    /**
     * Callback observer.
     */
    private var mCallback: Callback? = null

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initComponents(context, attrs, defStyle, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initComponents(context, attrs, 0, 0)
    }

    constructor(context: Context?) : super(context!!)

    /**
     * Initializes the text paint. This has a fix size.
     *
     * @param attrs The XML attributes to use.
     */
    private fun initComponents(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.InvertedTextProgressbar,
            defStyleAttr,
            defStyleRes
        )
        mTextPaint = Paint()

        // Define the normal text paint.
        mTextPaint!!.color = typedArray.getColor(
            R.styleable.InvertedTextProgressbar_text_color,
            Color.BLACK
        )
        mTextPaint!!.style = Paint.Style.FILL
        mTextPaint!!.textSize = typedArray.getDimensionPixelSize(
            R.styleable.InvertedTextProgressbar_text_size,
            context.resources.getDimensionPixelSize(R.dimen.text_size_default)
        ).toFloat()
        mTextPaint!!.setTypeface(
            Typeface.defaultFromStyle(
                typedArray.getInteger(
                    R.styleable.InvertedTextProgressbar_text_typeface,
                    Typeface.defaultFromStyle(Typeface.NORMAL).style
                )
            )
        )
        mTextPaint!!.textAlign = Paint.Align.CENTER // Text draw is started in the middle
        mTextPaint!!.isLinearText = true
        mTextPaint!!.isAntiAlias = true

        // Define the inverted text paint.
        mTextInvertedPaint = Paint(mTextPaint)
        mTextInvertedPaint!!.color = typedArray.getColor(
            R.styleable.InvertedTextProgressbar_text_inverted_color,
            Color.WHITE
        )

        // Define the text.
        text = typedArray.getString(R.styleable.InvertedTextProgressbar_text)
        if (text == null) {
            text = "Loading..."
        }

        // Set maximum or minimum values if there's any.
        maxProgress = typedArray.getInteger(
            R.styleable.InvertedTextProgressbar_max_progress,
            UNINITIALIZED_INT_VALUE
        )
        minProgress = typedArray.getInteger(
            R.styleable.InvertedTextProgressbar_min_progress,
            UNINITIALIZED_INT_VALUE
        )

        // Recycle the TypedArray.
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.getClipBounds(mRect)
        if (mPosX == -1) {
            mPosX = width / 2
        }
        if (mPosY == -1) {
            mPosY = (height / 2 - (mTextPaint!!.descent() + mTextPaint!!.ascent()) / 2).toInt()
        }

        // Draw text to overlap.
        if (text!!.isNotEmpty()) {
            canvas.drawText(text!!, mPosX.toFloat(), mPosY.toFloat(), mTextPaint!!)
        }
        if (mIsAnimating) {
            // Only start timing from first frame of animation
            if (mStartTime == UNINITIALIZED_INT_VALUE.toLong()) {
                mStartTime = SystemClock.uptimeMillis()
                endTime = mStartTime + mDurationMs
            }

            // Adjust clip bounds according to the time fraction
            val currentTime = SystemClock.uptimeMillis()
            if (currentTime < endTime) {
                val timeFraction = (currentTime - mStartTime) / (mDurationMs * 1f)
                val alpha =
                    (mRect.width() * currentProgress / maxProgress * timeFraction).roundToInt()
                val r = Rect(mRect)
                r.right = r.left + alpha
                canvas.clipRect(r)
                Util.log("" + alpha)
            } else {
                mRect.right =
                    mRect.left + mRect.width() * currentProgress / maxProgress // Regra de 3 simples.
                canvas.clipRect(mRect)
                mIsAnimating = false
                if (mCallback != null) {
                    mCallback!!.onAnimationEnd()
                }
            }
        } else if (minProgress > -1 && maxProgress > minProgress && currentProgress >= minProgress && currentProgress <= maxProgress) {
            mRect.right =
                mRect.left + mRect.width() * currentProgress / maxProgress // Regra de 3 simples.
            canvas.clipRect(mRect)
        }

        // Draw current state.
        super.onDraw(canvas)
        if (text!!.isNotEmpty()) {
            // Draw text in position set.
            canvas.drawText(text!!, mPosX.toFloat(), mPosY.toFloat(), mTextInvertedPaint!!)
        }

        // Request another draw operation until time is up
        if (mIsAnimating) {
            invalidate()
        }
    }

    var textPaint: Paint?
        /**
         * Gets the paint currently being used for the overlapped text.
         *
         * @return The overlapped text paint.
         */
        get() = mTextPaint
        /**
         * Sets the paint to be used for the overlapped text.
         *
         * @param textPaint The Paint to be set for the overlapped text.
         */
        set(textPaint) {
            mTextPaint = textPaint
        }
    var textInvertedPaint: Paint?
        /**
         * Gets the paint currently being used for the overlapping text.
         *
         * @return The overlapping text paint.
         */
        get() = mTextInvertedPaint
        /**
         * Sets the paint to be used for the overlapping text.
         *
         * @param textInvertedPaint The Paint to be set for the
         * overlapping text.
         */
        set(textInvertedPaint) {
            mTextInvertedPaint = textInvertedPaint
        }

    /**
     * Sets coordinates of the pivot position of the text to draw.
     * If any of the coordinates are -1 then the text positions
     * will be the center of the canvas.
     *
     * @param x The X position.
     * @param y The Y position.
     */
    fun setTextPivot(x: Int, y: Int) {
        mPosX = x
        mPosY = y
    }

    /**
     * Sets the current progress. Note that it's needed to set
     * maximum and minimum progress values, otherwise nothing
     * will occur.
     *
     * [.setMaxProgress]
     * [.setMinProgress]
     *
     * @param progress The progress to be set.
     */
    fun setProgress(progress: Int) {
        currentProgress = progress
        invalidate()
    }

    /**
     * Sets the text size for both paints.
     * If the paints have not been initizialized yet this method
     * won't do anything.
     *
     * @param size The new text size to be set.
     */
    fun setTextSize(size: Int) {
        if (mTextPaint != null && mTextInvertedPaint != null) {
            mTextPaint!!.textSize = size.toFloat()
            mTextInvertedPaint!!.textSize = size.toFloat()
        }
    }

    /**
     * Starts the animation of the progress and text draw.
     *
     * @param durationMs The duration of the bar filling animation.
     */
    fun startAnimation(durationMs: Int) {
        mIsAnimating = true
        mStartTime = UNINITIALIZED_INT_VALUE.toLong()
        mDurationMs = durationMs
        invalidate()
    }

    /**
     * Interface that holds methods that are called during the
     * animation.
     */
    interface Callback {
        fun onAnimationEnd()
    }

    /**
     * Sets the callback to handle animation end.
     *
     * @param callback The callback to call upon events.
     */
    fun setCallback(callback: Callback?) {
        mCallback = callback
    }

    companion object {
        /**
         * Uninitialized integer value.
         */
        private const val UNINITIALIZED_INT_VALUE = -1
    }
}
