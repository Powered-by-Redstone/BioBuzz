package org.firstinspires.ftc.teamcode.mechanisms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HoodTest {
    @Test fun startTakesLowestAngleAboveLowEnd() {
        assertEquals(50.0, hoodStartDeg(50.0, 30.0), 1e-9)
        assertEquals(30.0, hoodStartDeg(30.0, 30.0), 1e-9)
        assertEquals(380.0, hoodStartDeg(20.0, 30.0), 1e-9)
    }

    @Test fun followsSmallSteps() {
        assertEquals(105.0, unwrapDeg(100.0, 105.0, 90.0)!!, 1e-9)
        assertEquals(95.0, unwrapDeg(100.0, 95.0, 90.0)!!, 1e-9)
    }

    @Test fun crossesWrapBothWays() {
        assertEquals(365.0, unwrapDeg(355.0, 5.0, 90.0)!!, 1e-9)
        assertEquals(355.0, unwrapDeg(365.0, 355.0, 90.0)!!, 1e-9)
        assertEquals(415.0, unwrapDeg(410.0, 55.0, 90.0)!!, 1e-9)
    }

    @Test fun rejectsImplausibleJump() {
        assertNull(unwrapDeg(200.0, 0.0, 90.0))
        assertNull(unwrapDeg(400.0, 200.0, 90.0))
    }

    @Test fun trackStartsLowInsideRange() {
        assertEquals(50.0, hoodTrack(Double.NaN, 50.0, 40.0, 420.0)!!, 1e-9) // could be 410: hood starts low
        assertEquals(370.0, hoodTrack(Double.NaN, 10.0, 40.0, 420.0)!!, 1e-9)
    }

    @Test fun trackFaultsOutsideRangeOrOnJump() {
        assertNull(hoodTrack(425.0, 75.0, 40.0, 420.0)) // 435 > 420 + 10 margin
        assertNull(hoodTrack(35.0, 25.0, 40.0, 420.0)) // 25 < 40 - 10 margin
        assertEquals(428.0, hoodTrack(425.0, 68.0, 40.0, 420.0)!!, 1e-9) // inside the margin
        assertNull(hoodTrack(100.0, 300.0, 40.0, 420.0))
    }

    @Test fun trackUnconfiguredStartsAtRawWithoutRangeCheck() {
        assertEquals(200.0, hoodTrack(Double.NaN, 200.0, Double.NaN, Double.NaN)!!, 1e-9)
        assertEquals(1000.0, hoodTrack(995.0, 280.0, Double.NaN, Double.NaN)!!, 1e-9)
    }

    private val s = 1_000_000_000L // ns per second

    @Test fun checkFaultsWhenMovingAgainstPower() {
        val p = HoodProgress(100.0, 0L, 1.0)
        assertNotNull(hoodCheck(p, 95.0, 0.5, s / 10))
        assertNull(hoodCheck(p, 90.0, 0.5, s / 10))
        assertNull(hoodCheck(HoodProgress(100.0, 0L, -1.0), 110.0, -0.5, s / 10))
    }

    @Test fun checkFaultsWhenStalled() {
        val p = HoodProgress(100.0, 0L, 1.0)
        assertEquals(p, hoodCheck(p, 100.5, 0.5, s / 4))
        assertNull(hoodCheck(p, 100.5, 0.5, s))
    }

    @Test fun checkRestartsOnProgressNoPowerOrReversal() {
        val p = HoodProgress(100.0, 0L, 1.0)
        assertEquals(HoodProgress(101.0, s, 1.0), hoodCheck(p, 101.0, 0.5, s)) // moved 1° the right way
        assertEquals(HoodProgress(80.0, s, 0.0), hoodCheck(p, 80.0, 0.0, s)) // unpowered
        assertEquals(HoodProgress(95.0, s, -1.0), hoodCheck(p, 95.0, -0.5, s)) // overshoot reversal is not wrong-way
        assertEquals(HoodProgress(50.0, s, 1.0), hoodCheck(HoodProgress(), 50.0, 0.5, s)) // fresh OpMode
    }
}
