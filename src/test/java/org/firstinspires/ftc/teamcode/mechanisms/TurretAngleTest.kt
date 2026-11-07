package org.firstinspires.ftc.teamcode.mechanisms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurretAngleTest {
    // cases are written in degrees for readability; chooseTurretAngle works in radians
    private fun angle(deg: Double, current: Double, min: Double, max: Double) = Math.toDegrees(
        chooseTurretAngle(Math.toRadians(deg), Math.toRadians(current), Math.toRadians(min), Math.toRadians(max)),
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
}
