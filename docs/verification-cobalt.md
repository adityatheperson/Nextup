# Cobalt 0.3.0 verification

Approved mockup B: fixed Cobalt light/dark colors, 220ms event crossfade/size transitions, 180ms count selection, refresh snackbar, blue calendar-arrow adaptive icon with monochrome support.

Final build: testDebugUnitTest, lintDebug, assembleDebug and assemblePreview succeeded. 10 JVM tests pass; lint has 0 errors and 15 warnings. Main foreground/background contrast pairs range from 5.24:1 to 15.14:1. APK signature verified. Installed via adb install -r successfully; package verified versionCode 3/versionName 0.3.0. User data preserved, preview app removed.

Source review found snackbar coroutine cancellation; fixed by using remembered scope. Native screenshots were black because phone was locked (confirmed mDreamingLockscreen=true); visual and motion verification remain pending. Prior Compose UI test harness failures remain unresolved; no claim of passing UI automation.
