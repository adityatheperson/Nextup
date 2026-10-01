# NextUp MVP — approved September 24, 2026

Personal Kotlin/Compose Material 3 Android app for Pixel 8 Pro. Read Android CalendarContract instances; no network, OAuth, backend, write access, analytics, ads, widgets, or exact alarms.

Display ongoing and future timed events from visible synced calendars in the next seven days. Exclude cancelled, declined and all-day events. Sort by start time; overlaps consume separate slots. Event limit 1–5, default 3, persisted using DataStore. Show event titles and localized times; relative times appear only in the app.

One silent notification, generic public lock-screen version, absolute times. Tap opens first event in Calendar, falling back to NextUp; empty state opens NextUp. No events: “No events in the next 7 days”. Swiping hides it until app reopened; dismissal survives reboot. Calendar revocation clears in-memory event details when detected and displays a permission notice.

Refresh on open, manual refresh, count change, event boundaries, and reboot/unlock using best-effort scheduling. WorkManager periodic backup, no exact-alarm permission. Background work may be delayed by Android. Restore on reboot only if not dismissed. Low battery usage takes priority over exact freshness.

Minimal single screen: permissions, count, preview, refresh. Light/dark/dynamic colors. Source and locally installable APK; no Play Store release.
