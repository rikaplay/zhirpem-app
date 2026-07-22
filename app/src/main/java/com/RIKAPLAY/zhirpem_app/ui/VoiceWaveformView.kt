package com.RIKAPLAY.zhirpem_app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.RIKAPLAY.zhirpem_app.R
import kotlin.math.max

class VoiceWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bars = mutableListOf<Float>()
    private var progress = 0f
    private var isRecording = false

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val playedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var barWidth = 8f
    private var barGap = 4f
    private var cornerRadius = 4f
    private val minBarHeight = 10f

    var onSeekListener: ((Float) -> Unit)? = null

    init {
        // Default colors, should be updated via code or attributes
        barPaint.color = ContextCompat.getColor(context, android.R.color.darker_gray)
        playedBarPaint.color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
    }

    fun setWaveformColor(color: Int, playedColor: Int) {
        barPaint.color = color
        playedBarPaint.color = playedColor
        invalidate()
    }

    fun setAmplitudes(amplitudes: List<Float>) {
        bars.clear()
        bars.addAll(amplitudes)
        isRecording = false
        invalidate()
    }

    fun addAmplitude(amplitude: Float) {
        bars.add(amplitude)
        isRecording = true
        invalidate()
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    fun clear() {
        bars.clear()
        progress = 0f
        invalidate()
    }

    private val barRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2

        // Calculate how many bars we can fit or use existing ones
        val maxBars = (width / (barWidth + barGap)).toInt()
        
        // During recording, we might want to show only the last N bars that fit
        val barsToShow = if (isRecording && bars.size > maxBars) {
            bars.takeLast(maxBars)
        } else {
            bars
        }

        val actualBarWidth = if (!isRecording && barsToShow.isNotEmpty()) {
             (width - (barsToShow.size - 1) * barGap) / barsToShow.size
        } else {
            barWidth
        }

        barsToShow.forEachIndexed { index, amplitude ->
            val x = index * (actualBarWidth + barGap)
            // Normalize amplitude to height (assuming amplitude is 0.0 to 1.0)
            val bHeight = max(minBarHeight, amplitude * height * 0.8f)
            
            val top = centerY - bHeight / 2
            val bottom = centerY + bHeight / 2
            
            barRect.set(x, top, x + actualBarWidth, bottom)
            
            val isPlayed = !isRecording && (index.toFloat() / barsToShow.size) <= progress
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, if (isPlayed) playedBarPaint else barPaint)
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isRecording) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                performClick()
                val newProgress = event.x / width
                onSeekListener?.invoke(newProgress.coerceIn(0f, 1f))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val newProgress = event.x / width
                onSeekListener?.invoke(newProgress.coerceIn(0f, 1f))
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
