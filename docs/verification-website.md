# NextUp website verification

Date: 2026-09-30.

- Static site under website/; no application changes. HTML/CSS, local SVG, and two small JavaScript modules. No browser analytics, credentials, custom backend, or runtime third-party request.
- Four Node demo tests pass. Initial run failed with the demo module missing; implemented module passes Upcoming, Now, Finished, invalid-state fallback, and independent returned-state tests.
- Headless Edge checks passed at 1440, 390 and 320px: demo transitions, walking placement, progress, keyboard navigation, FAQ disclosure, relative project-path assets, no horizontal overflow, and no page/asset errors. No-JavaScript check retains sample and setup content and hides unavailable demo controls.
- Inspected desktop/mobile screenshots. Raised small UI text to at least 11px and replaced tinted shadow with neutral elevation. Rechecked behavior and screenshots in a bounded second pass. System fonts are deliberate per approved spec; Upcoming/Now labels belong to the app illustration.
- Independent visual and source review disposition: Ship; no required fixes. Web tokens documented separately from the Android design system.
- GitHub releases API returned no releases. The website therefore offers truthful source/build instructions and no unverified APK download link.
- Publishing is explicitly authorized. Only the website subtree will be served from gh-pages; Android sources, caches, APKs and local screenshots remain outside the Pages branch.

## Deployment verified 2026-10-01

- Published https://adityatheperson.github.io/Nextup/ from gh-pages commit c8ebbbe914045ca683b948eb6b6a88a1c6f12e03. GitHub Pages reports built with HTTPS enforced.
- Public HTML, CSS, both JavaScript modules and favicon returned HTTP 200 and matched the locally verified source.
- Live mobile Edge check passed: Now progress is 50%, exactly one walking estimate appears, Finished promotes Physics lab, no horizontal overflow or page errors.

