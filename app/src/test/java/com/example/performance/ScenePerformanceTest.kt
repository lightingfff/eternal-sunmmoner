package com.example.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenePerformanceTest {
    @Test
    fun balancedHalvesRenderWorkWithoutChangingLogicalTime() {
        val balanced = SceneFramePacer(30)
        val high = SceneFramePacer(60)
        val balancedSimulation = FixedSceneSimulation()
        val highSimulation = FixedSceneSimulation()
        var balancedFrames = 0
        var highFrames = 0
        var balancedTicks = 0
        var highTicks = 0
        for (vsync in 0..600) {
            val now = vsync * 1_000_000_000L / 60
            if (balanced.onVsync(now)) {
                balancedFrames++
                balancedTicks += balancedSimulation.advance(balanced.deltaSeconds) { }
            }
            if (high.onVsync(now)) {
                highFrames++
                highTicks += highSimulation.advance(high.deltaSeconds) { }
            }
        }
        assertEquals(301, balancedFrames)
        assertEquals(601, highFrames)
        assertEquals(10.0, balanced.elapsedSeconds, 0.00001)
        assertEquals(high.elapsedSeconds, balanced.elapsedSeconds, 0.00001)
        assertEquals(600, balancedTicks)
        assertEquals(balancedTicks, highTicks)
    }

    @Test
    fun fixedSimulationRetainsFractionalFramesAndResetClearsThem() {
        val simulation = FixedSceneSimulation(0.02f)
        var elapsed = 0f
        assertEquals(0, simulation.advance(0.01f) { elapsed += it })
        assertEquals(1, simulation.advance(0.01f) { elapsed += it })
        assertEquals(5, simulation.advance(0.1f) { elapsed += it })
        assertEquals(0.12f, elapsed, 0.00001f)
        simulation.advance(0.01f) { }
        simulation.reset()
        assertEquals(0, simulation.advance(0.01f) { })
    }

    @Test
    fun worksAtHighRefreshAndResetsPauseTime() {
        val pacer = SceneFramePacer(60)
        var frames = 0
        for (vsync in 0..120) {
            if (pacer.onVsync(vsync * 1_000_000_000L / 120)) frames++
        }
        assertEquals(61, frames)
        pacer.reset()
        assertTrue(pacer.onVsync(120_000_000_000L))
        assertEquals(0f, pacer.deltaSeconds, 0f)
        assertEquals(1.0, pacer.elapsedSeconds, 0.00001)
        assertFalse(pacer.onVsync(120_001_000_000L))
        assertTrue(pacer.onVsync(120_016_666_666L))
        assertEquals(1f / 60f, pacer.deltaSeconds, 0.00001f)
    }

    @Test
    fun samplesAtMostOncePerSecondAndReportsMeasuredCounters() {
        val probe = ScenePerformanceProbe(enabled = true, targetFps = 30)
        for (frame in 0..30) {
            val now = frame * 1_000_000_000L / 30
            probe.recordFrame(now, updateNanos = 2_000_000L)
            probe.recordDraw(4_000_000L)
            if (frame < 30) assertNull(probe.poll(now))
        }
        val result = probe.poll(1_000_000_000L, 12, 3, 48, 8_388_608L)!!
        assertEquals(30.0, result.fps, 0.00001)
        assertEquals(33.333333, result.averageFrameMs, 0.00001)
        assertEquals(33.333334, result.p95FrameMs, 0.00001)
        assertEquals(2.0, result.averageUpdateMs, 0.0)
        assertEquals(4.0, result.averageDrawMs, 0.0)
        assertEquals(0, result.jankyFrames)
        assertEquals(12, result.activeHeroes)
        assertEquals(3, result.activeMonsters)
        assertEquals(48, result.visibleDrawables)
        assertEquals(8_388_608L, result.bitmapCacheBytes)
        assertTrue(result.allocationGcWarning.contains("unavailable"))
        assertNull(probe.poll(1_500_000_000L))
    }

    @Test
    fun boundedSamplesReportTailAndJankWithoutRetainingPause() {
        val probe = ScenePerformanceProbe(enabled = true, targetFps = 60, sampleCapacity = 20)
        probe.recordFrame(0L)
        var now = 0L
        repeat(99) {
            now += 10_000_000L
            probe.recordFrame(now)
        }
        now += 100_000_000L
        probe.recordFrame(now)
        val sample = probe.poll(now)!!
        assertEquals(100, sample.frameSamples)
        assertEquals(10.0, sample.p95FrameMs, 0.0)
        assertEquals(1, sample.jankyFrames)
        assertEquals(-1, sample.visibleDrawables)
        probe.reset()
        probe.recordFrame(100_000_000_000L)
        assertNull(probe.poll(100_000_000_000L))
    }

    @Test
    fun disabledProbeDoesNoSamplingAndEnablingStartsFresh() {
        val probe = ScenePerformanceProbe()
        probe.recordFrame(0L, 5L)
        probe.recordFrame(1_000_000_000L, 5L)
        probe.recordDraw(5L)
        assertNull(probe.poll(1_000_000_000L))
        probe.enabled = true
        probe.recordFrame(2_000_000_000L)
        assertNull(probe.poll(2_000_000_000L))
        probe.recordFrame(3_000_000_000L)
        assertEquals(1.0, probe.poll(3_000_000_000L)!!.fps, 0.0)
        probe.enabled = false
        assertNull(probe.poll(5_000_000_000L))
    }
}
