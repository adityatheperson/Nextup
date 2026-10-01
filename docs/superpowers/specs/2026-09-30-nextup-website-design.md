# NextUp website design

## Purpose and scope

Create a responsive, single-page product website for the existing NextUp Android app. Help a visitor understand its quiet calendar notification, try an illustrative demo, and find installation instructions and source. This is a new web surface alongside the Android project; it does not change the app.

The user approved a Cobalt website with the headline “Your next class. One glance away.”, an interactive Upcoming / Now / Finished notification demo, feature and setup information, privacy details, and GitHub/download navigation.

## Page and visual direction

- Compact header: NextUp calendar-arrow mark, How it works, Setup, GitHub.
- Hero: large editorial headline and short explanation, next to an interactive notification illustration. A clear “Get NextUp” link goes to installation instructions; a secondary link opens the source repository.
- Demo: three keyboard-accessible buttons select Upcoming, Now, and Finished. Sample classes and walking estimates are explicitly illustrative. Upcoming shows the walk to the first class. Now shows its half-complete progress bar and the walking estimate for the next class only. Finished removes the ended class and promotes the next event. Changes use a short transition, disabled for reduced-motion preferences.
- Features: concise, varied sections explaining the silent agenda, ongoing-event progress, and optional walking estimates. Avoid generic grids of repeated decorative cards.
- Setup/download: Android 14+ compatibility, local APK installation steps, calendar and notification permissions, and optional walking-key/location setup. Link to the existing repository. Show a direct APK download only if its remote asset is verified to exist; otherwise offer build/source instructions and explain that a packaged download is not yet hosted. Do not invent a Play Store listing or upload a binary as a side effect.
- Privacy/FAQ: calendar access is read-only; no ads or analytics; optional routing sends position and destination to openrouteservice; the key is entered in the app and encrypted locally; notification dismissal stops background work until reopening; Android may delay updates.
- Footer: project source, version 0.5.0, Android compatibility, and a short personal-project credit without invented endorsements or usage statistics.

Extend the existing Cobalt identity: blue #284FB9, pale surface #F7F9FE, ink #18243D, and a dark notification illustration using #222E46 / #B3C5FF. Use oversized clean typography, generous spacing, thin rules, and a compact calendar-arrow logo adapted from the existing icon. Avoid stock photography and personal screenshots. Draw the notification with HTML/CSS so it remains sharp and responsive. Label it as an illustration; actual Android styling varies.

## Implementation boundaries

Use static HTML, CSS, and small vanilla JavaScript under `website/`, with no framework, backend, tracking, or runtime third-party service. Use local assets and a system font stack. Keep assets and links relative so the site works at the GitHub Pages project path `/Nextup/` and in a local preview. Include a concise website README with local preview and GitHub Pages deployment instructions. Preserve the Android design documents; document web-specific tokens separately.

Prepare the website for GitHub Pages, but do not enable hosting, publish a GitHub release, or change repository settings as part of this design approval. Those actions require an explicit publishing request. The source link is https://github.com/adityatheperson/Nextup.

## Acceptance and verification

- The first screen explains the Android app and presents a working next action.
- All three demo states behave correctly; the next unstarted class is the only one with a walking estimate.
- Mobile and desktop layouts have readable content, no horizontal overflow, and visible focus states. Navigation and controls work by keyboard and touch.
- Use semantic landmarks, accessible button state, sufficient contrast, 44px minimum web control targets, and reduced-motion support. Content and setup remain readable without JavaScript.
- Verify links, the actual availability of downloadable assets, and demo behavior. Inspect desktop and mobile browser renders in one batch, fix material issues together, and confirm once.
- Do not describe simulated progress as evidence of exact background refresh or claim real-key walking routes have been verified.

## Review status

Direction approved in chat. Written specification prepared for user review. No website implementation or deployment has started.
