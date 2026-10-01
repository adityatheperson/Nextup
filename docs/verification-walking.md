# Walking feature verification — 0.4.0

- Final `testDebugUnitTest`, `lintDebug`, `assembleDebug` succeeded. 16 JVM tests pass (10 existing, 6 walking-policy tests).
- Four WalkingAndroidTest instrumentation tests passed on Pixel 8 Pro Android 17 in the separate preview package: encrypted fixture-key roundtrip/removal, coordinate/label parsing, ambiguous/missing-confidence rejection, invalid coordinate rejection.
- Native light/dark sample screenshots inspected. Independent source/visual review: ship with verification limits. Fixed observer registration before initial network work and added a calendar requery before route results publish.
- APK signature verified (v2); `dist/NextUp-0.4.0-debug.apk` and SHA-256 prepared. Lint: 0 errors, 16 warnings (pinned dependencies, older version checks and unused previous icon).
- Key is entered through a secure password-masked dialog, encrypted using Android Keystore AES-GCM, and excluded from cloud/device backup. Tests used a fake key only. No real key was read, entered, logged or used by the assistant.
- Remaining: user-key live geocoding/routing, permission flow on the personal app, actual 200m movement and reboot/dismissal recovery with location enabled. No claim of live walking-route success. Existing Compose UI harness failures are unrelated and unresolved; only the four new non-Compose instrumentation tests were run.
- Installation pending USB reconnection; current installed production version remains 0.3.2 until adb confirms success.
