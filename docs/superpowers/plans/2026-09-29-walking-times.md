# Walking Times Implementation Plan

**Goal:** Show nearby event walking durations with movement-triggered background updates.
**Architecture:** Pure walking policy and route engine, Android location/proximity adapter, HTTPS openrouteservice client, device-local encrypted key, coordinator integration and native setup dialog. Platform proximity alerts provide an approximately 200m exit fence without a continuous foreground service.
**Tech Stack:** Existing Kotlin/Compose/WorkManager, Android LocationManager/Keystore, HTTPS; no extra SDK.
**Spec:** docs/walking-times-spec.md; user selected openrouteservice and obtained their key.

## Constraints
- Raw route duration <=2400 seconds; round up only for display.
- Two-minute routing cooldown; fifteen-minute estimate lifetime and periodic fallback.
- Require precise and background permission for background feature. Dismissal stops monitoring.
- Calendar must remain useful without location/network/key. Never log credentials or locations.
- Do not touch personal calendar data in tests.

## Tasks
- [ ] Pure duration, location quality, matching and cache policy with JVM boundary/failure tests.
- [ ] Encrypted key storage, platform location/proximity, openrouteservice geocoding/walking and bounded errors.
- [ ] Read event locations; enrich outside calendar mutex; reject late results after dismissal/refresh; schedule expiry.
- [ ] Native key/permission setup, status and timestamps, event/notification walking labels, public privacy.
- [ ] JVM and isolated Android checks, lint/build, native fixtures, review, package/install and leave key entry to user.

## Review focus
Permission revocation and late results; route duration 2400 vs 2401; movement during cooldown; ambiguous room names; rate-limit/offline behavior; stale notification timing under Android deferral.
