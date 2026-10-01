# Verification — September 24, 2026

## Automated checks

- `:app:testDebugUnitTest`: 10 tests passed (event selection, exclusions, overlaps/recurrence identity, seven-day boundaries, count limits, dismissal policy, event-boundary scheduling and relative/date/DST formatting).
- `:app:connectedDebugAndroidTest`: 6 tests passed on Android 15 / API 35 emulator-5554. Exercises real CalendarProvider recurrence, ongoing events, cancelled/declined/all-day/hidden exclusions; DataStore persistence and stale dismissal-intent generations; preview after dismissal; silent/private notification and permission notice; Compose count selection and permission recovery controls.
- `:app:lintDebug`: 0 errors, 15 warnings. Warnings concern pinned dependency versions and redundant SDK/version qualifiers; no lint errors are suppressed.
- `:app:assembleDebug`: passed. Final system-bar contrast correction rebuilt with unit tests and lint.
- `apksigner verify --verbose`: APK verified, one signer, APK Signature Scheme v2.
- APK manifest inspected: READ_CALENDAR, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED plus WorkManager/AndroidX runtime permissions. No INTERNET, WRITE_CALENDAR or exact-alarm permission. WorkManager contributes WAKE_LOCK, ACCESS_NETWORK_STATE and FOREGROUND_SERVICE; NextUp does not run a foreground service or make network requests.

## Emulator checks

- Installed the built APK and used a dedicated local sample calendar; three upcoming events appeared in both app and notification.
- Inspected collapsed and expanded notification: silent channel, private visibility, generic public version, correct three event titles and absolute times.
- Inspected light mode, dark mode and 1.3× text. Layout scrolls; controls and event titles remain usable. Fixed system-bar icon contrast on live theme changes.
- Rebooted with notification active. NextUp's worker completed successfully and the three-event notification returned without launching the app.
- Swiped away notification, confirmed it was removed, and rebooted again. The post-boot worker completed successfully and the notification remained absent. Reopening NextUp restored it.
- Revoked READ_CALENDAR on the installed app, relaunched, and checked that the notification changed to “Calendar access required” with sample titles removed. Restored permission and removed the temporary sample calendar afterward.
- A pre-existing emulator System UI ANR dialog was encountered. The event log identified `com.android.systemui/.keyguard.KeyguardService` at 00:11, preceding NextUp testing; it was dismissed. No NextUp crash was observed.

Screenshots are native emulator captures in `screenshots/`, using sample data only.

## Remaining physical Pixel checks

- Actual Google account sync: emulator tests use local CalendarProvider fixtures, not your Google account.
- Long idle/Doze periods and your battery restriction settings. Background refresh delays are expected; no exact timing guarantee is made.
- Your lock-screen privacy configuration: Android controls whether to reveal private notification content; the app supplies a generic public version.
- Opening a real recurring Google Calendar event in your installed Calendar app.

## Delivery

`dist/NextUp-0.1.0-debug.apk` is the signed personal-use APK. A separate source ZIP contains Gradle files, Kotlin/XML source, tests and documentation, excluding build caches, signing keys and machine-specific SDK paths.
