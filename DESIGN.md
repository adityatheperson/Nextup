# NextUp web design

Mode: Persuade. Extend the Android Cobalt identity into a quiet editorial product page. The main Android DESIGN.md remains authoritative for native UI; these rules apply only to website/.

## Tokens

- Paper: #F7F9FE; ink: #18243D; supporting text: #51617D.
- Primary: #284FB9; pale stage: #E5EBF9; dividers: #C3CEE2.
- Notification: #222E46; text: #E6ECFA; supporting text: #BCC7DE; accent: #B3C5FF.
- Typography: local Arial/Helvetica/sans-serif stack as specified, no external font requests. Desktop hero up to 90px, mobile 46–76px; tracking no tighter than -0.04em. Body 14–18px with 1.65–1.8 line height.
- Content max-width 1232px. Gutters 56px desktop, 32px at widths up to 1050px, 20px up to 780px, and 16px up to 390px. Main content sections use 82–105px vertical spacing on desktop and 58–65px on mobile. The hero uses 86px top / 80px bottom, 100px above 1500px, 60px up to 1050px, and 49px top / 45px bottom up to 780px.
- Notification and demo stage radius 16px; action radius 7px; a single soft shadow lifts the notification from the pale stage.

## Composition and behavior

Large left-aligned hero beside a dark interactive notification. A narrow editorial strip separates the hero from a two-column explanatory section. A full-width Cobalt installation section changes scroll rhythm; native FAQ disclosures and a restrained footer close the page.

Upcoming, Now, and Finished are button states with aria-pressed, keyboard support and a polite explanation. The notification progress bar is explicitly illustrative. One short content transition follows user input; reduced-motion disables it. The default sample and all informational content work without JavaScript. Focus, selection, and scrollbar colors use the same palette.

No stock photos, personal screenshots, decorative gradients, or fabricated endorsements. The app icon is an authored SVG adaptation of NextUp's existing calendar-arrow mark. The notification is HTML/CSS rather than a raster mockup.
