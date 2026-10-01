package com.adith.nextup

import org.junit.Assert.*
import org.junit.Test

class WalkingPolicyTest {
    @Test fun cutoffUsesRawSeconds() {
        assertEquals(40, walkingMinutes(2400.0))
        assertNull(walkingMinutes(2400.01))
        assertEquals(2, walkingMinutes(60.01))
        assertEquals(1, walkingMinutes(0.0))
        assertNull(walkingMinutes(-1.0))
        assertNull(walkingMinutes(Double.NaN))
        assertNull(walkingMinutes(Double.POSITIVE_INFINITY))
    }
    @Test fun rejectsOldInaccurateAndFutureFixes() {
        assertTrue(usableFix(119_999, 40f))
        assertFalse(usableFix(120_001, 40f))
        assertFalse(usableFix(-1, 10f))
        assertFalse(usableFix(10, 101f))
        assertFalse(usableFix(10, Float.NaN))
    }
    @Test fun excludesRemoteAndBlankLocations() {
        assertFalse(isPhysicalLocation("  "))
        assertFalse(isPhysicalLocation("https://meet.google.com/abc"))
        assertFalse(isPhysicalLocation("Zoom meeting"))
        assertTrue(isPhysicalLocation("Engineering Hall, Irvine, CA"))
    }
    @Test fun freshnessRejectsClockRollbackAndExpiry() {
        val estimate = WalkEstimate(12, 1000, "Engineering Hall")
        assertTrue(estimate.fresh(1001))
        assertFalse(estimate.fresh(999))
        assertFalse(estimate.fresh(901000))
    }
    @Test fun ambiguousGeocodingDoesNotPickArbitrarily() {
        assertTrue(confidentMatch(.95, .6))
        assertFalse(confidentMatch(.7, null))
        assertFalse(confidentMatch(.95, .92))
        assertFalse(confidentMatch(Double.NaN, null))
    }
    @Test fun cooldownIsBoundedAndRollbackSafe() {
        assertEquals(60_000L, routingDelay(100_000, 40_000))
        assertEquals(0L, routingDelay(160_000, 40_000))
        assertEquals(120_000L, routingDelay(30_000, 40_000))
        assertEquals(0L, routingDelay(1000, 0))
    }
}
