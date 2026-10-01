# Event status verification — 0.4.1

Date: 2026-09-30. Version code: 7.

- Collapsed and expanded private notification headings now use “Now” while any displayed event is ongoing. Ongoing rows show “Now” and an absolute end time; future rows keep their absolute start time. Public lock-screen content remains generic.
- Each notification post filters ended events. The open app filters ended rows on its existing one-second clock tick, and foreground refresh checks displayed start/end boundaries every second to refill the agenda. The existing minute refresh, inexact background boundary alarm, and periodic fallback remain. Android can defer background transitions; exact timing is not guaranteed.
- Regression coverage includes just before start, exact start, exact end, the next event taking the vacated slot, an empty agenda, and overlapping events. The initial test run failed to compile because the new formatting methods were not yet implemented. After implementation, all 18 JVM tests passed (7 agenda, 5 time-label, 6 walking-policy tests), with zero failures/errors.
- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` completed with BUILD SUCCESSFUL using the existing JDK 17 and offline Gradle 8.9. Kotlin compilation used `-Pkotlin.compiler.execution.strategy=in-process` because the default daemon attempted an inaccessible cache directory. The SDK XML version warning remains. Lint reported no errors.
- Read-only independent code review found no substantive issues.
- Packaged `dist/NextUp-0.4.1-debug.apk` and its adjacent SHA-256 file. `apksigner verify` succeeded; the signing certificate SHA-256 matches the 0.4.0 APK: `4944e10f75b721a6fbfdc06830e2ce8db9f099d0f02621096a0dfafc07967895`.
- `adb devices -l` returned no connected devices. Installation, activity launch, rendered notification/app transitions, and device instrumentation tests were not verified for this release. No personal calendar fixtures or permission changes were used. Existing live walking-routing and movement verification gaps remain.

## Installation follow-up — 2026-09-30

- ADB detected the authorized Pixel 8 Pro (39151FDJG001JQ). Installation with adb install -r returned Success, preserving existing app data.
- Package inspection confirmed versionName 0.4.1 and versionCode 7. Explicit MainActivity launch returned Status: ok.
- Rendered event transitions and real-world background timing remain unverified; this follow-up verifies installation and launch only.
