# NextUp 0.2.0 verification

- Debug build succeeds; APK identifies versionCode 2 / versionName 0.2.0.
- All 10 JVM tests pass. Android lint: 0 errors, 15 warnings.
- APK signature verified with apksigner (v2, one signer).
- Native Pixel 8 Pro screenshots reviewed for light, dark, 1.3x text with a long title, denied permission, and empty agenda. Fixtures use synthetic events in an isolated preview application. Fresh independent visual review: ship, no material visual blockers.
- Three Compose UI tests on Android 17 failed with `No compose hierarchies found`. Interaction verification remains incomplete; do not describe these tests as passing. Earlier test infrastructure also needed Espresso 3.7.0 for Android 17 compatibility.
- Captured test log shows the host ComponentActivity reaching RESUMED, then PAUSED and STOPPED within about 50 ms; its window becomes invisible before assertions. Further device diagnosis is needed to establish why it lost visibility.
- No calendar integration tests were run on the personal phone, and no personal calendar records were modified.
- September 29: after USB authorization, installed the APK with adb install -r (Success), launched MainActivity (Status: ok), and verified installed versionCode 2 / versionName 0.2.0. Existing app data preserved. No preview package remained before the final UI test rerun.

Screenshots: `.impeccable/review/phone-*.png`. Packaged APK: `dist/NextUp-0.2.0-debug.apk` with adjacent SHA-256 file. Existing app has not been uninstalled or its data cleared.


Final authorized-device rerun: all three Compose tests still fail with No compose hierarchies found. Production MainActivity reopened successfully afterward. UI automation remains unresolved.
