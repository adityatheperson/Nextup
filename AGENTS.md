# NextUp contributor instructions

Applies to this entire project. Follow the user's current instructions when they change an earlier product decision.

## Project and context

NextUp is a personal Android app for a Pixel 8 Pro, built with Kotlin, Jetpack Compose Material 3, CalendarContract, DataStore and WorkManager. Minimum Android API 34; compile/target API 35; JDK 17.

Read `README.md`, `PRODUCT.md`, and `DESIGN.md` before relevant changes. The original MVP is in `docs/spec.md`; later walking requirements are in `docs/walking-times-spec.md`. Some original restrictions (no network, wallpaper colors) were superseded by the approved Cobalt theme and optional openrouteservice integration. Use current code and later decisions to resolve these differences. Verification records in `docs/verification*.md` describe evidence and outstanding checks, not guarantees about the current device.

## Preserve product behavior

- Show ongoing and future timed events from visible calendars within seven days. Exclude cancelled, declined and all-day events. Sort by start time; overlaps count separately. Persist the selected count of 1–5 (default 3).
- Keep the notification silent. Use absolute times there and relative times in the app. Public lock-screen content must remain generic.
- Notification dismissal is intentional: stop background activity until the app is reopened, preserving dismissal across reboot.
- Use best-effort background scheduling. Do not promise exact refresh timing or introduce exact alarms or a continuously running foreground service without an explicit feature decision.
- Preserve the approved agenda-first layout and fixed Cobalt light/dark schemes. Consult `DESIGN.md` for actual roles and dimensions. Keep native controls, 48dp touch targets, wrapping text and scrolling.
- Walking estimates use actual foot-walking routes, never straight-line distance presented as a walking duration. Apply the 2,400-second limit before rounding up. Missing, online, ambiguous, stale or unavailable locations must not produce invented estimates.
- Movement refresh is approximately 200m, with a two-minute routing cooldown and fifteen-minute fallback/estimate lifetime. Android may defer delivery. Display estimate timestamps and preserve calendar functionality when routing fails.

## Architecture

All application Kotlin sources are under `app/src/main/java/com/adith/nextup/`.

- `Agenda.kt`, `TimeLabels.kt`: selection and formatting rules.
- `CalendarRepository.kt`: read-only CalendarContract Instances queries, including event locations.
- `SettingsStore.kt`: count, activation, dismissal and generation persistence.
- `RefreshCoordinator.kt`: calendar state, notification lifecycle and walking enrichment. Keep network work outside the calendar mutex. Reject late results after edits, permission changes, refresh or dismissal.
- `AgendaNotifications.kt`: notification styles, intents, privacy and Cobalt accent.
- `RefreshScheduler.kt`, `RefreshWorker.kt`, `AgendaReceiver.kt`: periodic, boundary and recovery work.
- `WalkingPolicy.kt`: pure cutoff, freshness, location-quality and cooldown rules.
- `WalkingSettings.kt`: device-local encrypted credential storage.
- `WalkingLocation.kt`, `WalkingReceiver.kt`: platform location fixes and proximity alerts.
- `WalkingRoutes.kt`, `WalkingService.kt`: HTTPS geocoding/routing, matching, caching and backoff.
- `MainActivity.kt`, `NextUpScreen.kt`, `WalkingSetup.kt`: lifecycle, permissions, agenda and secure setup dialog.
- `app/src/preview/`: synthetic fixtures in an isolated application; never include these in the distributed APK.

## Privacy and device safety

- Do not read, request in chat, log, commit or embed the user's routing key. The user enters it directly in the app. Preserve Android Keystore encryption, backup exclusions and secure key-entry screenshots.
- Send only the origin and destination information needed by openrouteservice, not event titles or descriptions. Avoid logging personal positions, addresses or calendar contents.
- Never create, edit or delete events in the user's personal calendars for testing. Do not grant/revoke personal app permissions through ADB to simplify tests.
- Never uninstall `com.adith.nextup`, clear its data or change its signing identity as an update workaround. Install with `adb install -r` and verify success. Removing the isolated preview/test packages after testing is acceptable.
- Do not bypass a locked phone. Request unlocking when a visual check requires it. Keep personal screenshots out of source packages and shared artifacts.
- The user cancelled the Drive delivery workflow. Use the existing USB/local APK workflow unless they request another destination.

## Build and tests

From the project root, with JDK 17 and Android SDK 35 configured:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

On this workstation, these tools were available; verify paths before relying on them:

```powershell
$env:JAVA_HOME='D:\sc\.tooling\jdk\jdk-17.0.20.1+1'
$env:GRADLE_USER_HOME='D:\calendar\.tooling\gradle-home'
$env:ANDROID_USER_HOME='D:\calendar\.tooling\android-user'
& 'D:\sc\.tooling\gradle-8.9\gradle-8.9\bin\gradle.bat' --offline --no-daemon '-Duser.home=D:\calendar\.tooling' :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Offline mode requires cached dependencies. Do not hide dependency failures or claim a build passed from a shell wrapper's exit code; inspect Gradle's result and test reports.

Use an empty emulator, with no personal calendar data, for the full `:app:connectedDebugAndroidTest` suite. CalendarProvider integration tests create/delete fixtures and assume no other events.

For a personal phone, run only explicitly selected isolated tests:

```powershell
.\gradlew.bat -PuiTestBuildType=preview -Pandroid.testInstrumentationRunnerArguments.class=com.adith.nextup.WalkingAndroidTest :app:connectedPreviewAndroidTest
```

`com.adith.nextup.preview` is separate from the production application. `NextUpScreenTest` can also be selected, but its prior Android 17 runs failed with “No compose hierarchies found”; do not report those checks as passing. Never accidentally omit the class filter on a personal phone.

Add meaningful regression tests for changed policy or behavior. Keep UI checks bounded: inspect native sample screens in relevant themes/font scales, fix material issues, then confirm. Do not repeat unrelated tests without a reason.

## Packaging and installation

- Increment versionCode/versionName for a delivered update; preserve the existing signing key.
- Copy the verified APK to `dist/NextUp-<version>-debug.apk`, verify its signature with `apksigner`, and generate an adjacent SHA-256 file.
- Inspect `adb devices -l` before installing. Previously used ADB: `D:\sc\.tooling\android-sdk\platform-tools\adb.exe`; Pixel serial: `39151FDJG001JQ`. Do not assume it remains connected or authorized.
- After installation, verify package version and activity launch. For visual claims such as icon color, inspect the rendered result instead of relying solely on resource values.
- Source archives should exclude `.tooling`, build/cache directories, `local.properties`, signing keys, credentials and personal screenshots.
- Update verification notes honestly. Separate build/unit/parser checks from live provider routing, real movement, permission flows and battery behavior.

## Last recorded handoff

At creation of this file, 0.4.0 was built and packaged: 16 JVM tests and four isolated walking key/parser device tests passed; lint had no errors. Native sample walking screens passed review. Installation was pending because ADB no longer detected the Pixel; the last confirmed installed version was 0.3.2. Real-key routing and actual movement remained unverified. Recheck these facts before reporting current status.
