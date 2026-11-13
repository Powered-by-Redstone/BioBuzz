package org.firstinspires.ftc.teamcode.mechanisms

import com.pedropathing.math.Pose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurretAngleTest {
    // cases are written in degrees for readability; chooseTurretAngle works in radians
    private fun angle(deg: Double, current: Double, min: Double, max: Double, seekRange: Double? = null) =
        Math.toDegrees(
            chooseTurretAngle(
                Math.toRadians(deg), Math.toRadians(current), Math.toRadians(min), Math.toRadians(max),
                seekRange?.let { Math.toRadians(it) },
            ),
        )

    @Test fun keepsReachableAngle() = assertEquals(30.0, angle(30.0, 0.0, -60.0, 60.0), 1e-6)

    @Test fun usesEquivalentAngle() = assertEquals(40.0, angle(400.0, 0.0, -60.0, 60.0), 1e-6)

    @Test fun parksAtNearerLimit() {
        assertEquals(60.0, angle(90.0, 0.0, -60.0, 60.0), 1e-6)
        assertEquals(-60.0, angle(200.0, 0.0, -60.0, 60.0), 1e-6)
    }

    @Test fun picksEquivalentNearestCurrent() = assertEquals(200.0, angle(-160.0, 170.0, -250.0, 250.0), 1e-6)

    @Test fun unwindsAtCableLimit() {
        assertEquals(-100.0, angle(260.0, 240.0, -250.0, 250.0), 1e-6)
        assertEquals(-170.0, angle(190.0, 180.0, -180.0, 180.0), 1e-6)
    }

    @Test fun neverLeavesLimits() {
        for (current in listOf(-250.0, -100.0, 0.0, 100.0, 250.0))
            for (deg in -720..720 step 7) {
                val result = angle(deg.toDouble(), current.coerceIn(-60.0, 60.0), -60.0, 60.0)
                assertTrue("$deg from $current gave $result", result in -60.000001..60.000001)
                val wide = angle(deg.toDouble(), current, -250.0, 250.0)
                assertTrue("$deg from $current gave $wide", wide in -250.000001..250.000001)
            }
    }

    @Test fun seekRangeUnwindsEarlyOnlyPastMargin() {
        // range 0..500, middle 250: unwind once current is more than 180 + 30 from 250
        assertEquals(445.0, angle(445.0, 450.0, 0.0, 500.0, 30.0), 1e-6)
        assertEquals(85.0, angle(445.0, 470.0, 0.0, 500.0, 30.0), 1e-6)
        assertEquals(15.0, angle(15.0, 50.0, 0.0, 500.0, 30.0), 1e-6)
        assertEquals(375.0, angle(15.0, 30.0, 0.0, 500.0, 30.0), 1e-6)
        assertEquals(445.0, angle(445.0, 470.0, 0.0, 500.0), 1e-6)
    }

    @Test fun seekRangeDoesNotFlipBack() {
        // after unwinding from 450 to 90, coming back up to 445 (85 + 360) stays on the low side
        assertEquals(85.0, angle(445.0, 90.0, 0.0, 500.0, 30.0), 1e-6)
    }

    @Test fun seekRangeNeverLeavesLimits() {
        for (current in listOf(0.0, 100.0, 250.0, 400.0, 500.0))
            for (deg in -720..720 step 7) {
                val result = angle(deg.toDouble(), current, 0.0, 500.0, 30.0)
                assertTrue("$deg from $current gave $result", result in -0.000001..500.000001)
                val narrow = angle(deg.toDouble(), current.coerceIn(-60.0, 60.0), -60.0, 60.0, 30.0)
                assertTrue("$deg from $current gave $narrow", narrow in -60.000001..60.000001)
            }
    }

    @Test fun keepsCurrentOnNonFiniteTarget() = assertEquals(20.0, angle(Double.NaN, 20.0, -60.0, 60.0), 1e-6)

    @Test fun aimsFromOffsetAxis() {
        fun aimDeg(heading: Double, tx: Double, ty: Double, axisY: Double) =
            Math.toDegrees(turretAngleTo(Pose(0.0, 0.0, Math.toRadians(heading)), Pose(tx, ty), axisY))
        assertEquals(45.0, aimDeg(0.0, 10.0, -10.0, 0.0), 1e-6)
        assertEquals(0.0, aimDeg(0.0, 10.0, -10.0, -10.0), 1e-6)
        assertEquals(0.0, aimDeg(90.0, 10.0, 20.0, -10.0), 1e-6)
    }
}
