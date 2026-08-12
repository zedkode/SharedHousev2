---
version: "superdesign-alpha"
name: "Light Editorial Grid"
description: "Near-white light-mode system led by a chat-style hero console, dense masonry card galleries, and a single circular black action button; motion is subtle and glass is used sparingly on a light frosted navbar."
colors:
  background: "#FFFFFF"
  surface: "#F2F2F2"
  surface-alt: "#EBEBEB"
  text-primary: "#18181B"
  text-secondary: "#71717A"
  text-tertiary: "#707070"
  border: "#EBEBEB"
  border-strong: "#E4E4E7"
  accent-ring: "#3C81F5"
  skeleton-bg: "#E8E8E8"
  skeleton-shimmer: "#F5F5F5"
typography:
  display-lg:
    fontFamily: "Inter Variable"
    fontSize: "30px"
    fontWeight: 600
    lineHeight: "1.2"
    letterSpacing: "-0.7px"
  headline-md:
    fontFamily: "Inter Variable"
    fontSize: "20px"
    fontWeight: 600
    lineHeight: "1.4"
    letterSpacing: "-0.4px"
  body-md:
    fontFamily: "Inter Variable"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "1.43"
  label-md:
    fontFamily: "Inter Variable"
    fontSize: "14px"
    fontWeight: 600
    lineHeight: "1.43"
    letterSpacing: "-0.2px"
spacing:
  base: "8px"
  gap: "16px"
  section-padding: "64px"
  section-gap: "48px"
rounded:
  control: "8px"
  card: "20px"
  chip: "12px"
  pill: "9999px"
  input-cluster: "6px"
components:
  navbar:
    background: "rgba(252, 252, 252, 0.8)"
    backdrop-filter: "blur(12px)"
    height: "57px"
    items: 7
  button-nav-cta:
    background: "#000000"
    text-color: "#FCFCFC"
    radius: "8px"
    height: "28px"
    padding: "6px 12px"
    hover: "transform matrix(1.02,0,0,1.02,0,0), opacity 0.9"
  button-nav-ghost:
    background: "transparent"
    text-color: "#000000"
    radius: "9999px"
    height: "32px"
    padding: "6px 14px"
  button-hero-primary:
    background: "#000000 (observed near-black solid)"
    text-color: "#FFFFFF"
    radius: "50%"
    height: "40px"
    padding: "0px"
    shadow: "rgba(0, 0, 0, 0.12) 0px 1px 3px 0px, rgba(0, 0, 0, 0.08) 0px 1px 2px 0px"
    hover: "boxShadow rgba(0, 0, 0, 0.15) 0px 2px 6px 0px, rgba(0, 0, 0, 0.1) 0px 2px 4px 0px, transform matrix(1.05,0,0,1.05,0,0)"
  button-utility-toolbar:
    background: "#F4F4F5"
    text-color: "#18181B"
    radius: "8px 0px 0px 8px"
    height: "28px"
    padding: "0px 10px 0px 12px"
    hover-background: "rgba(228, 228, 231, 0.8)"
  card-gallery-item:
    background: "transparent"
    radius: "0px"
    padding: "0px"
    anatomy: "media-top + heading + expandable + body-text"
  card-glass-footer:
    background: "rgba(235, 235, 235, 0.2)"
    radius: "20px"
    padding: "12px"
  card-media-scrim:
    background: "linear-gradient(to top, rgba(0, 0, 0, 0.8), rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0))"
    radius: "0px"
    padding: "0px"
  footer:
    background: "#FFFFFF"
    links: 3
---
# Light Editorial Grid
Source: https://superdesign.dev

## Overview
This is a light-mode-default, minimalism-leaning system built for scanning a large catalog of thumbnails rather than for atmospheric brand storytelling. The palette sits almost entirely in near-white and warm grays (`#FFFFFF`, `#F2F2F2`, `#EBEBEB`), with structural ink text in near-black (`#18181B`, `#707070`) and only two color events breaking the neutrality: a single blue focus/accent value (`#3C81F5`) and a black circular primary action. The aesthetic language is a hybrid of Swiss/International restraint (tight grid, generous negative space, one weight of sans throughout) and a contemporary AI-product console pattern — a centered chat-input hero flanked by pill toggles, sitting above a dense, scroll-heavy masonry of preview cards. Glassmorphism is present but rationed to a single sticky navbar. Nothing about this system is loud; hierarchy is carried by size, weight, and spacing, not color.

## Composition
The first screen is a centered, single-column hero: a short headline, a subhead line, a segmented pill toggle, and — the clear focal element — a large rounded input console with a circular black submit button anchored bottom-right. Below the console, a quiet row of ghost/ text links. This hero occupies roughly the top third of the page; everything below is priced for density. From there the page becomes a long, uninterrupted scroll-driven masonry of preview-card tiles (a 3-across grid, repeating for dozens of rows), broken only by a filter/tab row ("style," "animation," etc., referenced only structurally) and, at the very end, a 3-up feature strip and a flat white footer. The deliberate choice here is breadth-over-hierarchy: rather than a handful of hero-sized feature bands, the page commits almost its entire body to one repeating card unit at consistent scale, trusting grid rhythm and thumbnail imagery to carry visual interest instead of varied section types. The rejected alternative is a bento/magazine layout with mixed card sizes — this system instead holds every gallery card to the same footprint and lets density, not asymmetry, create energy.

## Colors
`#FFFFFF` (~54% of pixels) is the page background — the deepest layer, with no gradient or texture beneath it. `#F0F0F0`/`#F2F2F2` (~22%) and `#EBEBEB` (~25% combined with borders) form the surface and border layer: card hover fills, dividers, and the segmented-toggle track. `#000000` (~9%) is reserved for ink-heavy content and the two solid black controls (nav CTA, hero circular button) — it is a punctuation color, not a background. `#181818` (~5%) appears in the dark thumbnail imagery embedded inside gallery cards (screenshots of dark-themed designs), not in the site's own chrome. `#FFF0F0` and `#D8D8D8` are trace tones living inside thumbnail images, not part of the authored UI palette. Text ink runs `#18181B` for primary copy, `#707070`/`#71717A` for secondary/meta labels. The one saturated color in the entire system is the ring/focus blue `#3C81F5`, used only on focus states — an accent so rationed it never appears as a fill, only as an outline. Nothing else is colored: no brand hue, no semantic red/green/amber anywhere in the chrome.

## Typography
A single family, Inter Variable, carries the entire system — there is no serif or mono accent visible in the UI chrome itself (any serif/mono appearance is confined to imagery inside thumbnail cards, not the site's own type). Hierarchy is built through size/weight/tracking alone: `display-lg` at 30px/600/-0.7px letter-spacing for the hero headline, `headline-md` at 20px/600/-0.4px for sub-section headings, `label-md` at 14px/600/-0.2px for card titles and button labels, and `body-md` at 14px/400 for supporting copy and meta text (secondary tone `#71717A`). Negative tracking on both display and label sizes gives the small-scale UI a tightened, engineered feel rather than an airy editorial one — appropriate to a dense card-catalog page rather than a slow-read article.

## Layout
The gallery is a 3-column card grid (measured at both 16px and 24px gap, rows repeating at roughly even thirds — i.e. rows of [3][3][3] uniform-width cards throughout), which reads as a uniform card grid rather than bento or masonry: every tile in a row shares the same width, and variety comes only from each card's internal image aspect ratio, not from column-span. Content is held to a 720px max-width column for the hero text block, while the gallery grid runs wider, contained by generous outer margins. Section padding is 64px with 48px gaps between major bands; internal spacing steps through 16/12/10/8/6/4px, giving a tight, componentized rhythm rather than a loose editorial one. Corner radii scale by role: 20px for larger glass/feature cards, 12px for chips, 8–6px for controls, and 9999px pill for toggles and the ghost nav button — a consistent "the bigger the surface, the softer the corner" logic.

## Components
- **Navbar** — top of page, sticky, full width, 57px tall, 7 nav items (logo + items + auth actions), fill `rgba(252, 252, 252, 0.8)` with `backdrop-filter: blur(12px)` — the system's only glass surface. Its CTA is solid `#000000` fill, text `#FCFCFC`, radius 8px, height 28px, padding 6px 12px, hover lifts via `transform matrix(1.02,0,0,1.02,0,0)` and `opacity 0.9`. A secondary ghost button (transparent fill, black text, 9999px pill radius, 32px height, 6px 14px padding) sits beside it for a lower-emphasis auth action. Logo is a small wordmark/icon lockup at the far left.
- **Button — hero primary (circular)** — the single most emphasized control on the first screen, docked bottom-right inside the input console. Observed near-black solid fill (`#000000`), white icon/text, fully circular (50% radius), 40px height, no padding, resting shadow `rgba(0,0,0,0.12) 0px 1px 3px 0px, rgba(0,0,0,0.08) 0px 1px 2px 0px`; on hover it scales up (`matrix(1.05,0,0,1.05,0,0)`) with a deepened shadow. This is the page's true primary — not the nav CTA, which is a smaller utility rectangle.
- **Toolbar/utility buttons** — a large repeating cluster (measured ×45) beneath the hero input and used across the toolbar row for mode toggles/pill segments; fill `#F4F4F5`, text `#18181B`, asymmetric radius `8px 0px 0px 8px` (left-rounded only, implying grouped/segmented placement), 28px height, padding `0px 10px 0px 12px`, hover fill `rgba(228,228,231,0.8)`.
- **Gallery card (×dozens, repeating)** — the dominant component family, arranged 3-per-row across the full scroll body. Surface is fully transparent with 0px radius and 0px padding — the "card" is really just a stacked anatomy: a media block on top (a screenshot thumbnail filling most of the card's height), a heading label beneath in `label-md`, an expandable affordance, and a line of `body-md` supporting text (remix/view counts) at the bottom. No border or shadow separates cards from the page; separation comes purely from spacing and the imagery itself.
- **Glass footer-feature card (×3, near page end)** — a 3-up row (measured grid: 3 columns, 24px gap, even thirds) of small translucent tiles, fill `rgba(235, 235, 235, 0.2)`, radius 20px, padding 12px, each holding a short heading and one line of body copy — a soft, glassy counterpoint to the flat gallery cards above it.
- **Media-scrim card (×2, mid-page and near end)** — image/video-backed tiles with a bottom-anchored gradient scrim `linear-gradient(to top, rgba(0,0,0,0.8), rgba(0,0,0,0.4), rgba(0,0,0,0))`, 0px radius, 0px padding, used where a caption needs to sit legibly over a busier thumbnail image.
- **Footer** — flat `#FFFFFF` band, 3 links plus small social icon glyphs, no border or shadow separating it from the page above — the flattest, quietest element in the system.

## Graphics & Effects
The only gradient in the system is the media scrim, `linear-gradient(to top, rgba(0, 0, 0, 0.8), rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0))`, applied exclusively as a bottom-up darkening overlay on individual thumbnail/media cards (each instance covering only that one card, roughly 2.4% of total page area) so a caption stays legible over imagery — this is never a page-level background wash. Two live video surfaces exist inside gallery thumbnails; when rebuilding, substitute a static dark gradient or single representative frame. Shadow use is minimal and utility-grade: a near-invisible `rgba(0,0,0,0.05) 0px 1px 2px 0px` resting elevation for subtle card lift, a soft wide `rgba(0,0,0,0.08) 0px 2px 20px -4px` for larger panels, and the firmer `rgba(0,0,0,0.12) 0px 1px 3px 0px, rgba(0,0,0,0.08) 0px 1px 2px 0px` reserved for the circular hero button. Backdrop blur appears at two strengths — `blur(12px)` for the navbar glass, `blur(4px)` for lighter frosted moments (e.g. modal backdrops) — both light-on-light frosting, never dark glass.

## Motion
Interaction timing is fast and utilitarian: color/background/border transitions run at `150ms` with `cubic-bezier(0.4, 0, 0.2, 1)`, general property and opacity transitions at `300ms` on the same curve, and box-shadow eases with `cubic-bezier(0, 0, 0.2, 1)` — all standard "ease-out" deceleration curves, nothing springy or overshooting. Hover feedback is conveyed through small scale transforms (`matrix(1.02...)` on the nav CTA, `matrix(1.05...)` on the hero circular button) paired with shadow deepening, rather than color inversion. Keyframe animations (`rotate`, `dash`, modal fade-ins, skeleton shimmer/opacity pulses) drive loading and modal-entry states; CSS scroll-driven animations are present for on-scroll reveals in the gallery. The net motion character is crisp, short-duration, and confidence-signaling — feedback is immediate, never lingering.

## Guardrails
- Never paint the hero background with the media-scrim gradient — that gradient belongs only to individual thumbnail cards, at card scale.
- Never substitute the glass navbar's `rgba(252,252,252,0.8)` + `blur(12px)` fill for the hero primary button — the hero button is solid black, fully opaque, no blur.
- Keep the gallery grid uniform-width 3-across; do not introduce bento-style mixed column spans not present in the evidence.
- Reserve `#3C81F5` strictly for focus rings — it must never appear as a fill or heading color.
- Do not darken the overall page toward black — the system is light-dominant (~76% white/near-white pixels); black is a punctuation ink, not a base.
- Keep card corners at 0px for gallery tiles; only glass/feature cards and controls receive the 20px/pill radii.