# Walking estimates — proposed implementation

## Approved behavior

Show an estimated walking duration beside calendar events in the app and private notification only when a resolved pedestrian route takes at most 2,400 seconds. Keep the event visible when an estimate is unavailable or longer. Preserve generic public lock-screen text.

Refresh in the background after moving approximately 200 metres from the last routing origin, using an exit geofence that is re-centered after a fresh location fix. Apply a two-minute request cooldown and retain the existing 15-minute WorkManager fallback. These are best-effort triggers, not real-time guarantees. Refresh on app open and manual refresh too. Stop location monitoring and routing when the notification is dismissed; restore when the app opens. Re-register after reboot when activated and not dismissed.

## Location and routing

Request foreground precise location first, then explain and request background access separately. Continue showing the calendar without walking estimates if permission, location services, or network access is unavailable. Reject old or inaccurate location fixes. Avoid a continuously running foreground location service.

Read CalendarContract event location. Ignore blank locations and online meeting URLs. Resolve physical destinations with a geocoder; do not select ambiguous matches silently. Cache destination coordinates by location text. Route only the currently displayed events, reuse duplicate destinations, and compare raw route seconds against the threshold before rounding up for display.

The proposed provider is openrouteservice for geocoding and foot-walking routes, subject to user selection and an API key. The provider receives origin coordinates and destination text/coordinates, never calendar titles or descriptions. Add Internet permission. Store the personal API key locally through an in-app setup field, excluded from backup; do not embed it in the APK or source. No custom backend is proposed.

## Freshness and failure behavior

Treat estimates as snapshots from the last usable origin. Hide estimates older than 15 minutes when rendered; schedule best-effort notification refresh at expiry. Android may retain a notification past expiry while work is deferred, so label its estimate timestamp. Clear estimates when significant movement is detected until routes are refreshed. Discard late results for changed events, locations, permissions, or dismissal state. Routing errors must not prevent calendar refresh. Use bounded timeouts, limited retries, and quota-aware backoff.

## Validation

Unit checks: 2,400-second cutoff, rounding, duplicate destinations, stale fixes, stale routes, cooldown, cancellation and late-result rejection. Fake location/routing integration checks: geofence exit, dismissal, permission loss, reboot recovery, offline and rate-limit states. Verify native UI and notification with sample events, then verify an actual walking route only after provider setup. Never insert fixture events into the personal calendar.

## Pending before implementation

- Confirm routing provider and obtain a user-owned API key through local setup.
- Review this design and the implementation plan before building the new location/routing subsystem.
