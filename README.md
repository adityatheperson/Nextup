# NextUp

A quiet Android notification for the next events already synced to your phone. Built for a Pixel 8 Pro; Android 14 or newer is required.

## Install

Open `dist/NextUp-0.5.0-debug.apk` on your phone and allow installation from the app you used to open it. Launch NextUp, then grant Calendar and notification access. Google Calendar must already be syncing events to the device.

Or install using Android platform tools:

```powershell
adb install -r dist/NextUp-0.5.0-debug.apk
```

This is a locally signed debug APK for personal use, not a Play Store release. Keep the same signing key when building future updates, or Android will require uninstalling the earlier build (which resets settings).

## Behavior

- Ongoing and future timed events over the next seven days, sorted by start time.
- Only calendars marked visible in Android's calendar provider. Declined, cancelled and all-day events are excluded.
- Select 1–5 events; default 3. Overlapping events each occupy a slot.
- The preview includes relative times. The silent notification uses absolute clock times, honoring 12/24-hour settings.
- The notification heading changes from “Upcoming” to “Now” while a displayed event is ongoing. Ongoing rows show “Now” and their end time. Ended events drop out and the next events take their place; the generic empty-agenda notification remains when none are left. Background transitions may be delayed by Android.
- Tap the notification to open the first event in your calendar application. If no handler is installed, NextUp stays open.
- The public lock-screen notification uses generic text; Android's lock-screen privacy settings control whether private notification details are revealed.
- Dismiss the notification to hide it until you reopen NextUp. This choice survives reboot.
- Empty calendar: a “No events in the next 7 days” notification remains visible.
- Revoked calendar access: stale details are cleared when NextUp next runs, and a permission notice replaces the agenda.

The app has no custom backend, Google OAuth, analytics, ads or calendar write permission. Optional walking estimates use HTTPS to openrouteservice with your position and destination, never event titles. Your API key is encrypted locally with Android Keystore and excluded from backup. Event and route caches are in memory.

## Refresh and battery

Opening the app, changing the count and pressing Refresh reload the agenda immediately. While visible, local calendar changes are observed, ended rows are hidden on the screen's one-second clock tick, and the agenda reloads at displayed event boundaries as well as once a minute. In the background, an inexact non-wakeup alarm requests a refresh at the next displayed event boundary; WorkManager provides a 15-minute periodic backup. Android may delay both while idle or under battery restrictions. These are **not exact-time guarantees**.

Reboot, clock/timezone changes and app updates request recovery work. After a reboot, recovery runs after unlock when Android permits it, unless the notification was dismissed. Android force-stop prevents automatic work until you reopen the app. Notification permission and the “Upcoming events” channel must both be enabled.

## Build

Requires JDK 17, Android SDK 35 (Build Tools 34.0.0 or the AGP default), and network access for Gradle dependencies. Open the folder in Android Studio or set `JAVA_HOME` and put the SDK location in your untracked `local.properties`:

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
```

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

macOS/Linux: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

With an Android 14+ emulator connected:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Integration tests create and delete a dedicated local test calendar in the emulator. Use an emulator with no personal calendar data: selection assertions intentionally assume only the test fixtures exist.

## Project map

The 0.2.0 screen puts your agenda first: a compact date header with Refresh, a prominent next event, timed rows for later events, and a segmented 1–5 control beneath them. Granted permissions appear as a short status line; recovery actions appear when access is missing.

The `preview` build type has its own application ID (`com.adith.nextup.preview`) and synthetic visual fixtures. It can run UI-only tests on a connected phone without clearing the installed app or touching calendar data:

```powershell
.\gradlew.bat -PuiTestBuildType=preview -Pandroid.testInstrumentationRunnerArguments.class=com.adith.nextup.NextUpScreenTest :app:connectedPreviewAndroidTest
```

Keep the full CalendarProvider integration suite on an empty emulator as described above.

- `Agenda.kt`, `TimeLabels.kt`: event selection and presentation rules.
- `CalendarRepository.kt`: off-main-thread CalendarContract Instances query.
- `SettingsStore.kt`: singleton DataStore for count, dismissal and first-run state.
- `RefreshCoordinator.kt`: serialized refresh and notification lifecycle.
- `AgendaNotifications.kt`: silent/private notification and intents.
- `RefreshScheduler.kt`, `RefreshWorker.kt`, `AgendaReceiver.kt`: boundary, periodic and reboot recovery.
- `MainActivity.kt`, `NextUpScreen.kt`: permissions and Material 3 interface.
- `docs/spec.md`: approved product decisions.
- `docs/verification.md`: verification evidence and remaining physical-device checks.

## Platform references

- [Calendar provider and recurring instances](https://developer.android.com/identity/providers/calendar-provider)
- [Android 14 ongoing notification dismissal](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications)
- [Periodic work and timing restrictions](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Inexact alarms and reboot](https://developer.android.com/develop/background-work/services/alarms)



## Walking times (0.4.0)

Open Walking times beneath the event count. Enter your openrouteservice key locally and save it. Allow precise location, then in Android app permissions choose Location > Allow all the time. Keep device Location enabled. The dialog blocks screenshots; no key should be pasted into chat or committed to source.

NextUp resolves event location text and requests foot-walking routes for displayed events. It shows only routes at most 40 minutes, with an estimate timestamp. Blank, online, ambiguous and unavailable destinations are hidden. Complete addresses work better than room abbreviations. No real-provider call has been verified until a valid key is entered on-device.

A platform exit-proximity alert around the routing origin requests updates after about 200 metres, with a two-minute cooldown and existing 15-minute periodic fallback. Android can defer both. Estimates expire after 15 minutes in the app; notification cleanup is best effort, with timestamps indicating age. Dismissing the notification or removing the key stops monitoring. Background location requires explicit user permission; the app does not grant it through ADB.

Run isolated key/parser tests with `-PuiTestBuildType=preview -Pandroid.testInstrumentationRunnerArguments.class=com.adith.nextup.WalkingAndroidTest :app:connectedPreviewAndroidTest`. Never run the CalendarProvider fixture tests on a personal phone.
