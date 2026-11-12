package org.firstinspires.ftc.teamcode.mechanisms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShotTableTest {
    private val table = listOf(Shot(40.0, 3000.0, 0.2), Shot(60.0, 3400.0, 0.4), Shot(100.0, 4200.0, 0.6))

    @Test fun interpolatesBetweenEntries() {
        val mid = interpolate(table, 50.0)!!
        assertEquals(3200.0, mid.rpm, 1e-9)
        assertEquals(0.3, mid.hood, 1e-9)
        assertEquals(3800.0, interpolate(table, 80.0)!!.rpm, 1e-9)
    }

    @Test fun hitsEntriesExactly() {
        assertEquals(Shot(40.0, 3000.0, 0.2), interpolate(table, 40.0))
        assertEquals(Shot(100.0, 4200.0, 0.6), interpolate(table, 100.0))
    }

    @Test fun usesNearestEndOutsideTable() {
        assertEquals(Shot(40.0, 3000.0, 0.2), interpolate(table, 10.0))
        assertEquals(Shot(100.0, 4200.0, 0.6), interpolate(table, 150.0))
        assertEquals(Shot(100.0, 4200.0, 0.6), interpolate(table, Double.POSITIVE_INFINITY))
    }

    @Test fun singleEntryCoversEveryDistance() {
        val only = Shot(50.0, 3000.0, 0.2)
        assertEquals(only, interpolate(listOf(only), 20.0))
        assertEquals(only, interpolate(listOf(only), 90.0))
    }

    @Test fun refusesNaNAndUnusableTables() {
        assertNull(interpolate(table, Double.NaN))
        assertNull(interpolate(emptyList(), 50.0))
        assertNull(interpolate(table.reversed(), 50.0))
        assertNull(interpolate(listOf(Shot(40.0, 3000.0, 0.2), Shot(40.0, 3100.0, 0.3)), 40.0))
    }
}
