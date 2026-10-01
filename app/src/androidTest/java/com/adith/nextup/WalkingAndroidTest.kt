package com.adith.nextup

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class WalkingAndroidTest {
    @Test fun keyRoundTripsEncryptedAndCanBeRemoved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Credential tests must never replace a user's production configuration.
        check(context.packageName.endsWith(".preview"))
        val store = WalkingSettings(context)
        try {
            store.save("fixture-key-not-a-real-api-key")
            assertEquals("fixture-key-not-a-real-api-key", store.key())
            val raw = context.getSharedPreferences("walking", 0).getString("key", "")!!
            assertFalse(raw.contains("fixture"))
            store.clear()
            assertNull(store.key())
        } finally { store.clear() }
    }
    @Test fun geocoderReadsLongitudeLatitudeAndLabel() {
        val response = JSONObject("""{"features":[{"properties":{"confidence":0.99,"label":"Sample Hall"},"geometry":{"coordinates":[-117.8,33.6]}}]}""")
        val result = WalkingRoutes().parseDestination(response, "hall")!!
        assertEquals(-117.8, result.longitude, .0001)
        assertEquals(33.6, result.latitude, .0001)
        assertEquals("Sample Hall", result.label)
    }
    @Test fun geocoderRejectsAmbiguousOrMissingConfidence() {
        val ambiguous = JSONObject("""{"features":[{"properties":{"confidence":0.95}},{"properties":{"confidence":0.94}}]}""")
        assertNull(WalkingRoutes().parseDestination(ambiguous, "hall"))
        assertNull(WalkingRoutes().parseDestination(JSONObject("""{"features":[{"properties":{}}]}"""), "hall"))
        assertNull(WalkingRoutes().parseDestination(JSONObject("""{"features":[]}"""), "hall"))
    }
    @Test fun geocoderRejectsInvalidCoordinates() {
        val response = JSONObject("""{"features":[{"properties":{"confidence":0.99},"geometry":{"coordinates":[-500,95]}}]}""")
        assertNull(WalkingRoutes().parseDestination(response, "hall"))
    }
}
