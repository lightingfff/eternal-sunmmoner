package com.example.performance

import kotlin.math.ceil

/** Vsync gate; simulation uses elapsed time, independently of the selected render rate. */
class SceneFramePacer(val targetFps: Int = 30) {
    init {
        require(targetFps in 1..120)
    }

    private val intervalNanos = 1_000_000_000L / targetFps
    private var previousFrameNanos = UNSET
    private var nextFrameNanos = UNSET

    var deltaSeconds: Float = 0f
        private set
    var elapsedSeconds: Double = 0.0
        private set

    /** Call only from the scene's single withFrameNanos loop. Does not wait or allocate. */
    fun onVsync(frameTimeNanos: Long): Boolean {
        if (previousFrameNanos == UNSET) {
            previousFrameNanos = frameTimeNanos
            nextFrameNanos = frameTimeNanos + intervalNanos
            deltaSeconds = 0f
            return true
        }
        // Display clocks have minor rounding/jitter; do not accidentally halve a 60 Hz scene.
        if (frameTimeNanos + VSYNC_TOLERANCE_NANOS < nextFrameNanos) return false
        val elapsedNanos = (frameTimeNanos - previousFrameNanos).coerceAtLeast(0L)
        deltaSeconds = elapsedNanos / 1_000_000_000f
        elapsedSeconds += elapsedNanos / 1_000_000_000.0
        previousFrameNanos = frameTimeNanos
        val deadlinesPassed = ((frameTimeNanos + VSYNC_TOLERANCE_NANOS - nextFrameNanos) /
            intervalNanos).coerceAtLeast(0L) + 1L
        nextFrameNanos += deadlinesPassed * intervalNanos
        return true
    }

    /** Reset on background/modal pause. Resuming never simulates time spent paused. */
    fun reset() {
        previousFrameNanos = UNSET
        nextFrameNanos = UNSET
        deltaSeconds = 0f
    }

    private companion object {
        const val UNSET = Long.MIN_VALUE
        const val VSYNC_TOLERANCE_NANOS = 1_000_000L
    }
}

/** Fixed logical updates shared by both render modes. The caller owns any long-stall policy. */
class FixedSceneSimulation(val stepSeconds: Float = 1f / 60f) {
    init {
        require(stepSeconds > 0f && stepSeconds.isFinite())
    }

    private val step = stepSeconds.toDouble()
    private var accumulator = 0.0

    fun advance(deltaSeconds: Float, onStep: (Float) -> Unit): Int {
        require(deltaSeconds >= 0f && deltaSeconds.isFinite())
        accumulator += deltaSeconds.toDouble()
        var count = 0
        // Float wall-time deltas can differ by a few nanoseconds; retain the residual rather
        // than dropping it on each frame or creating a 30/60 Hz simulation-speed difference.
        while (accumulator + step * 0.0001 >= step) {
            onStep(stepSeconds)
            accumulator -= step
            count++
        }
        return count
    }

    fun reset() {
        accumulator = 0.0
    }
}

/** CPU timings only: Canvas timing measures draw-command submission, never GPU completion. */
data class ScenePerformanceSnapshot(
    val fps: Double,
    val averageFrameMs: Double,
    val p95FrameMs: Double,
    val averageUpdateMs: Double,
    val averageDrawMs: Double,
    val frameSamples: Int,
    val updateSamples: Int,
    val drawSamples: Int,
    val jankyFrames: Int,
    val activeHeroes: Int,
    val activeMonsters: Int,
    val visibleDrawables: Int,
    val bitmapCacheBytes: Long,
    val allocationGcWarning: String = "Allocation/GC unavailable; use Android Studio or Perfetto",
) {
    /** Format only the sampled snapshot, never from a per-frame draw callback. */
    fun toOverlayText(): String {
        fun count(value: Int) = if (value >= 0) value.toString() else "N/A"
        val cache = if (bitmapCacheBytes >= 0L) {
            String.format(java.util.Locale.US, "%.1f MiB", bitmapCacheBytes / 1_048_576.0)
        } else "N/A"
        val update = if (updateSamples > 0) {
            String.format(java.util.Locale.US, "%.2f", averageUpdateMs)
        } else "N/A"
        val draw = if (drawSamples > 0) {
            String.format(java.util.Locale.US, "%.2f", averageDrawMs)
        } else "N/A"
        return String.format(
            java.util.Locale.US,
            "FPS %.1f · avg/p95 %.1f/%.1f ms · jank %d\n" +
                "CPU update/draw %s/%s ms · hero %s · monster %s\n" +
                "Drawable %s · cache %s\n%s",
            fps, averageFrameMs, p95FrameMs, jankyFrames, update, draw,
            count(activeHeroes), count(activeMonsters), count(visibleDrawables), cache,
            allocationGcWarning,
        )
    }
}

/**
 * Opt-in, main-thread probe. Recording has no allocations; sorting/creating a snapshot happens
 * at most once per second. Unknown counters are -1, rather than invented zero measurements.
 */
class ScenePerformanceProbe(
    enabled: Boolean = false,
    targetFps: Int = 30,
    sampleCapacity: Int = 240,
) {
    init {
        require(targetFps in 1..120)
        require(sampleCapacity > 0)
    }

    private val frameTimes = LongArray(sampleCapacity)
    private val sortedTimes = LongArray(sampleCapacity)
    private val jankThresholdNanos = (1_000_000_000.0 / targetFps * 1.5).toLong()
    private var sampleIndex = 0
    private var retainedSamples = 0
    private var frameCount = 0
    private var frameNanos = 0L
    private var updateCount = 0
    private var updateNanos = 0L
    private var drawCount = 0
    private var drawNanos = 0L
    private var jankCount = 0
    private var lastFrameNanos = UNSET
    private var windowStartNanos = UNSET

    var enabled: Boolean = enabled
        set(value) {
            if (field != value) reset()
            field = value
        }

    fun recordFrame(frameTimeNanos: Long, updateNanos: Long = -1L) {
        if (!enabled) return
        if (windowStartNanos == UNSET) windowStartNanos = frameTimeNanos
        if (lastFrameNanos != UNSET && frameTimeNanos > lastFrameNanos) {
            val duration = frameTimeNanos - lastFrameNanos
            frameTimes[sampleIndex] = duration
            sampleIndex = (sampleIndex + 1) % frameTimes.size
            retainedSamples = (retainedSamples + 1).coerceAtMost(frameTimes.size)
            frameCount++
            frameNanos += duration
            if (duration > jankThresholdNanos) jankCount++
        }
        lastFrameNanos = frameTimeNanos
        if (updateNanos >= 0L) {
            this.updateNanos += updateNanos
            updateCount++
        }
    }

    fun recordDraw(drawNanos: Long) {
        if (!enabled || drawNanos < 0L) return
        this.drawNanos += drawNanos
        drawCount++
    }

    fun poll(
        nowNanos: Long,
        activeHeroes: Int = -1,
        activeMonsters: Int = -1,
        visibleDrawables: Int = -1,
        bitmapCacheBytes: Long = -1L,
    ): ScenePerformanceSnapshot? {
        if (!enabled || windowStartNanos == UNSET ||
            nowNanos - windowStartNanos < SAMPLE_INTERVAL_NANOS || frameCount == 0
        ) return null

        frameTimes.copyInto(sortedTimes, endIndex = retainedSamples)
        java.util.Arrays.sort(sortedTimes, 0, retainedSamples)
        val p95Index = (ceil(retainedSamples * 0.95).toInt() - 1).coerceAtLeast(0)
        val result = ScenePerformanceSnapshot(
            fps = frameCount * 1_000_000_000.0 / frameNanos,
            averageFrameMs = frameNanos / frameCount / 1_000_000.0,
            p95FrameMs = sortedTimes[p95Index] / 1_000_000.0,
            averageUpdateMs = if (updateCount > 0) updateNanos / updateCount / 1_000_000.0 else 0.0,
            averageDrawMs = if (drawCount > 0) drawNanos / drawCount / 1_000_000.0 else 0.0,
            frameSamples = frameCount,
            updateSamples = updateCount,
            drawSamples = drawCount,
            jankyFrames = jankCount,
            activeHeroes = activeHeroes,
            activeMonsters = activeMonsters,
            visibleDrawables = visibleDrawables,
            bitmapCacheBytes = bitmapCacheBytes,
        )
        clearWindow()
        windowStartNanos = nowNanos
        return result
    }

    /** Call with the pacer reset so background time is excluded from measured frame intervals. */
    fun reset() {
        clearWindow()
        lastFrameNanos = UNSET
        windowStartNanos = UNSET
    }

    private fun clearWindow() {
        sampleIndex = 0
        retainedSamples = 0
        frameCount = 0
        frameNanos = 0L
        updateCount = 0
        updateNanos = 0L
        drawCount = 0
        drawNanos = 0L
        jankCount = 0
    }

    private companion object {
        const val UNSET = Long.MIN_VALUE
        const val SAMPLE_INTERVAL_NANOS = 1_000_000_000L
    }
}
