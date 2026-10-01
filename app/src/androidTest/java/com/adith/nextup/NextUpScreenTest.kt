package com.adith.nextup

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class NextUpScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun userCanSelectFiveEventsAndReadPreview() {
        val now = System.currentTimeMillis()
        val state = mutableStateOf(AgendaState(calendarAllowed = true, notificationsAllowed = true,
            loading = false, events = listOf(CalendarEvent(1, "ICS 33 Lecture", now + 60_000, now + 3_600_000))))
        compose.setContent {
            NextUpTheme { NextUpScreen(state.value, { state.value = state.value.copy(settings = Settings(count = it)) }, {}, {}, {}) }
        }
        compose.onNodeWithContentDescription("5 events").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("ICS 33 Lecture").assertExists()
    }

    @Test fun deniedPermissionsShowRecoveryAndDoNotHideRefresh() {
        compose.setContent { NextUpScreen(AgendaState(loading = false), {}, {}, {}, {}) }
        compose.onNodeWithText("Allow Calendar Access").assertExists()
        compose.onNodeWithText("Enable notifications").assertExists()
        compose.onNodeWithText("Calendar access required").assertExists()
        compose.onNodeWithContentDescription("Refresh").assertIsDisplayed()
    }

    @Test fun agendaIsVisibleBeforeSettingsAndRefreshIsAccessible() {
        val now = System.currentTimeMillis()
        var refreshed = false
        compose.setContent {
            NextUpTheme { NextUpScreen(AgendaState(calendarAllowed = true, notificationsAllowed = true,
                loading = false, events = listOf(CalendarEvent(1, "First commitment", now + 60_000, now + 3_600_000))),
                {}, { refreshed = true }, {}, {}) }
        }
        compose.onNodeWithText("First commitment").assertIsDisplayed()
        val agendaTop = compose.onNodeWithText("First commitment").fetchSemanticsNode().boundsInRoot.top
        val settingsTop = compose.onNodeWithText("Events to show").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue("Agenda must lead the screen", agendaTop < settingsTop)
        compose.onNodeWithContentDescription("Refresh").performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(refreshed) }
    }
}
