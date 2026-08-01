# Theme context

## Compact token summary

### Product direction

- Calm, trustworthy and practical.
- Spacious cards and restrained motion.
- Native platform typography.
- Never financial-corporate, childish or accusatory.

### Light palette

| Token | Value | Use |
| --- | --- | --- |
| `foreground` | `#17231d` | Primary text |
| `background` | `#f2f5f2` | App background |
| `surface` | `#ffffff` | Cards |
| `border` | `#c8d3ca` | Card and divider border |
| `muted` | `#435248` | Supporting text |
| `brand` | `#3e654f` | Eyebrows and calm emphasis |
| `focus` | `#755c00` | Visible focus outline |

### Dark palette

| Token | Value | Use |
| --- | --- | --- |
| `foreground` | `#ecf4ee` | Primary text |
| `background` | `#111914` | App background |
| `surface` | `#19241d` | Cards |
| `border` | `#35453b` | Borders and separators |
| `muted` | `#b4c6ba` | Supporting text |
| `brand` | `#9ac9ac` | Calm emphasis |

### Typography and layout

- Font: Inter followed by platform system sans-serif.
- Mobile implementations use platform system typography.
- Hero/display tracking: compact; body line height approximately `1.65`.
- Card radius: `1rem`.
- Card shadow: `0 0.75rem 2rem rgb(27 54 39 / 6%)` in light mode; none in dark mode.
- Spacing rhythm: 4, 8, 12, 16, 24, 32, 48 and 64 logical pixels.
- Motion: short purposeful state transitions; disable or simplify for reduced motion.

### Status semantics

Every status combines icon, label and accessible description. Never use colour alone.

- Neutral: scheduled, draft, informational.
- Positive: recorded, confirmed, completed.
- Attention: due soon, pending request, estimated.
- Negative: overdue, rejected, failed sync, disputed.
- Disabled: waived, cancelled, expired.

## Raw source

### `apps/admin-web/src/styles.css`

```css
:root {
  color: #17231d;
  background: #f2f5f2;
  font-family:
    Inter,
    ui-sans-serif,
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif;
  font-synthesis: none;
  text-rendering: optimizeLegibility;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-width: 20rem;
  min-height: 100vh;
}

a {
  color: inherit;
}

.app-shell {
  min-height: 100vh;
}

.masthead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem clamp(1.25rem, 5vw, 4rem);
  border-bottom: 1px solid #cbd5cd;
  background: rgb(255 255 255 / 82%);
}

.brand {
  font-size: 1.125rem;
  font-weight: 750;
  text-decoration: none;
}

.brand:focus-visible {
  outline: 3px solid #755c00;
  outline-offset: 4px;
}

.environment,
.status {
  display: inline-flex;
  gap: 0.4rem;
  align-items: center;
  color: #385144;
  font-size: 0.875rem;
  font-weight: 650;
}

.content {
  width: min(72rem, calc(100% - 2.5rem));
  margin: 0 auto;
  padding: clamp(3rem, 8vw, 7rem) 0;
}

.hero {
  max-width: 48rem;
  margin-bottom: 4rem;
}

.eyebrow {
  margin: 0 0 0.75rem;
  color: #3e654f;
  font-size: 0.8rem;
  font-weight: 750;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  max-width: 15ch;
  margin-bottom: 1.25rem;
  font-size: clamp(2.5rem, 7vw, 5.5rem);
  line-height: 0.98;
  letter-spacing: -0.055em;
}

h2 {
  margin-bottom: 1.25rem;
  font-size: clamp(1.5rem, 3vw, 2rem);
}

h3 {
  margin-bottom: 0.75rem;
}

p {
  color: #435248;
  font-size: 1rem;
  line-height: 1.65;
}

.contract-status {
  display: flex;
  flex-wrap: wrap;
  gap: 2rem;
  margin: 2rem 0 0;
}

.contract-status div {
  display: grid;
  gap: 0.25rem;
}

.contract-status dt {
  color: #607068;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.contract-status dd {
  margin: 0;
  font-weight: 700;
}

.area-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 15rem), 1fr));
  gap: 1rem;
}

.area-card {
  min-height: 13rem;
  padding: 1.5rem;
  border: 1px solid #c8d3ca;
  border-radius: 1rem;
  background: #fff;
  box-shadow: 0 0.75rem 2rem rgb(27 54 39 / 6%);
}

@media (prefers-color-scheme: dark) {
  :root {
    color: #ecf4ee;
    background: #111914;
  }

  .masthead {
    border-color: #35453b;
    background: rgb(17 25 20 / 88%);
  }

  p,
  .environment,
  .status {
    color: #b4c6ba;
  }

  .eyebrow {
    color: #9ac9ac;
  }

  .area-card {
    border-color: #35453b;
    background: #19241d;
    box-shadow: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
```
