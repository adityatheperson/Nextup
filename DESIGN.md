---
name: NextUp
description: A compact native Material 3 calendar agenda.
colors:
  cobalt-light-primary: "#284FB9"
  cobalt-light-on-primary: "#FFFFFF"
  cobalt-light-container: "#D3DFFC"
  cobalt-light-on-container: "#182F68"
  cobalt-light-surface: "#F7F9FE"
  cobalt-light-on-surface: "#18243D"
  cobalt-light-surface-high: "#E5EBF9"
  cobalt-light-on-surface-variant: "#51617D"
  cobalt-light-outline: "#71809B"
  cobalt-light-outline-variant: "#C3CEE2"
  cobalt-dark-primary: "#B3C5FF"
  cobalt-dark-on-primary: "#092B78"
  cobalt-dark-primary-container: "#233F80"
  cobalt-dark-secondary-container: "#2C3E64"
  cobalt-dark-on-container: "#DDE5FF"
  cobalt-dark-surface: "#111724"
  cobalt-dark-on-surface: "#E6ECFA"
  cobalt-dark-surface-high: "#222E46"
  cobalt-dark-on-surface-variant: "#BCC7DE"
  cobalt-dark-outline: "#8795B1"
  cobalt-dark-outline-variant: "#44516A"
rounded:
  agenda-surface: "16dp"
spacing:
  compact: "4dp"
  inline: "8dp"
  control: "12dp"
  row: "16dp"
  section: "24dp"
---

# Design System: NextUp

## Overview

NextUp is a quiet, compact Android utility. Native Material 3 typography, the approved Cobalt palette and restrained tonal surfaces make calendar content easy to scan. The main-screen composition is recorded in `.impeccable/surface-nextup.md`; `NextUpScreen.kt` is the implementation source.

## Colors

`NextUpTheme` selects the fixed Cobalt `lightColorScheme` or `darkColorScheme` from system mode. Frontmatter records its explicit overrides; other roles retain Material defaults. Light primary/secondary containers share the light-container token, and both on-container roles share their respective scheme's on-container token. Background and on-background match surface and on-surface.

The page uses `surface`; featured events and agenda messages use `surfaceContainerHigh`. Main text inherits `onSurface`; supporting text uses `onSurfaceVariant`; featured countdowns and the connected check use `primary`. Dividers use `outlineVariant` at 60% alpha. Selected count segments use `secondaryContainer`, unselected segments use `surface`. System-bar icon appearance follows theme mode.

## Typography

Use default Material 3 typography and the system font, with scalable sp roles:

| Role | Use | Explicit weight override |
| --- | --- | --- |
| `headlineMedium` | Next-event title | Medium |
| `headlineSmall` | Agenda-state titles | Medium |
| `titleLarge` | App name | SemiBold |
| `titleMedium` | Later titles/heading, featured countdown | None |
| `titleSmall` | Count and notification-access headings | None |
| `bodyLarge` | Featured date/time, count numerals | None |
| `bodyMedium` | Header date, later countdowns, message details | None |
| `bodySmall` | Freshness and notification guidance | None |
| `labelLarge` | Later clock times | None |
| `labelMedium` | Connected status | None |

Titles wrap without truncation. Supporting text stays subordinate through role and color.

## Layout

Header and scrollable body are centered and capped at 600dp, with section-sized horizontal gutters. Header vertical padding is row-sized and includes status-bar insets. Body respects Scaffold insets, with 12dp top and 28dp bottom padding. Main sections use section-sized gaps.

Featured surfaces use section-sized padding, with 18dp event gaps or 14dp message gaps. Later rows have 12dp vertical padding, an 84dp clock column and row-sized column gap; dividers start at 100dp to align with content. Title/countdown pairs have 5dp gaps. Settings use control-sized gaps; footer lines use 10dp gaps.

Refresh is a 48dp target. Count segments and calendar-access buttons have 48dp minimum height. Preserve scrolling and wrapping at large font scales.

## Elevation & Depth

Hierarchy comes from tonal surfaces and spacing. Agenda surfaces retain default zero elevation without custom shadows. Later events sit directly on the page with subtle dividers.

## Shapes

Agenda and message surfaces share the agenda-surface corner token. Tonal buttons and the five-part segmented control retain native Material shapes.

## Components

- **Refresh:** header `FilledTonalIconButton`; disabled while loading, when a 20dp progress indicator with 2dp stroke replaces the 22dp icon. User-requested refresh completion shows a native snackbar: “Agenda updated” or “Couldn't refresh. Try again.”
- **Featured event:** tonal surface, prominent wrapping title, absolute time with an 18dp clock, then primary-colored relative time. Changed featured events crossfade over 220ms; body content-size changes use a 220ms tween.
- **Later event:** aligned clock/content columns with unboxed titles and relative times.
- **Event count:** full-width `SingleChoiceSegmentedButtonRow`, five numeric choices, native selected semantics and singular/plural accessible labels; disabled while loading. Container selection color transitions use a 180ms tween.
- **Agenda states:** shared tonal surface for loading, empty, error and denied calendar access. Calendar recovery uses `FilledTonalButton`; notification recovery uses inline `TextButton`.
- **Connected status:** compact row with a 16dp check, followed by supporting freshness and dismissal guidance.
- **Vectors:** refresh, clock and check use 24-unit Android vector viewports, rounded caps/joins and 1.8-unit strokes. Compose supplies theme tints; white asset strokes are tintable sources, not fixed UI colors. Decorative icons have no redundant content description.
- **Launcher:** a white outlined calendar with a right-pointing arrow on the light-primary Cobalt background, using a 108-unit adaptive-icon viewport, rounded caps/joins, 4-unit calendar strokes and 4.5-unit arrow strokes. The foreground also supplies the themed monochrome icon.

## Do's and Don'ts

- Do use native Material color and typography roles in both themes.
- Do retain full titles, accessible labels and scrollable large text.
- Do use actual calendar content to establish hierarchy.
- Don't introduce decorative imagery, gradients or custom shadows.
- Don't add navigation or extra settings to this single-screen utility.

Walking estimates use bodyMedium in primary, with bodySmall onSurfaceVariant timestamps and provider attribution. Long destination names wrap. Walking times setup uses a scrollable Material AlertDialog, password-masked key field, native permission actions, and screenshot protection. This feature adds no new color tokens.

## Notification layout (0.5.0)

The expanded notification features the first event, an Upcoming/Now label, absolute start/end times, and a Cobalt progress bar while that event is ongoing. Progress is a snapshot of the latest refresh, labeled with its timestamp; background refresh remains best effort. Only the earliest displayed event that has not started may show a fresh walking estimate. Ongoing and later events have no walking details. An unavailable estimate is omitted, never borrowed from a later event.

Android owns the notification surface and header. Custom RemoteViews provide the compact expanded content for up to three short-titled events at normal font scale. Larger text, narrow screens, longer titles and four/five-event agendas use native BigText layout. Public lock-screen content stays generic. The main Compose agenda is unchanged.
