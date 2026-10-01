# NextUp Website Implementation Plan

> Use superpowers:executing-plans for inline implementation, followed by an independent review.

**Goal:** Build and publish the approved product website on GitHub Pages.
**Architecture:** Static HTML/CSS and two small ES modules under website/. Publish only this directory to the gh-pages branch; preserve Android main-branch contents.
**Tech stack:** HTML, CSS, browser JavaScript; Node's built-in test runner for demo behavior.
**Spec:** docs/superpowers/specs/2026-09-30-nextup-website-design.md

## Global constraints

- Preserve Cobalt identity; no user screenshots, credentials, analytics, runtime third-party service, or Android changes.
- Relative asset links must work beneath /Nextup/.
- Walking appears only on the next unstarted sample event; progress is explicitly illustrative.
- Direct APK links require verified hosted assets. Otherwise link source and build instructions honestly.
- User's subsequent instruction “publish it on github pages” authorizes implementation, push, and Pages configuration. Execute inline rather than request another process choice.

## Review focus

Mobile overflow, keyboard controls, disabled JavaScript, reduced motion, and correct project-path asset URLs are checked in browser verification. Missing release assets must not create a broken download button.

## Tasks

- [x] Write scripts/website-demo.test.mjs for all three state transitions and invalid-state fallback; run it before creating the demo module.
- [x] Build website/index.html, styles.css, demo.mjs, site.mjs, favicon.svg and .nojekyll. Implement accessible state buttons and FAQ disclosures; static content remains useful without JS.
- [x] Verify behavior with node --test and desktop/mobile browser checks; inspect screenshots together, fix material defects in one batch, and confirm. Obtain a fresh review.
- [ ] Document web-specific tokens in website/DESIGN.md and publish instructions in website/README.md. Preserve Android's DESIGN.md.
- [ ] Commit/push main, publish the website subtree to gh-pages, enable repository Pages using that branch, and verify the public URL and assets.

## Execution ledger

- Existing Android branch is clean at start. No checkout isolation is needed for this additive web directory.
- Ruling: explicit publishing instruction supersedes the specification's earlier no-deployment boundary. No APK release publication was requested.
