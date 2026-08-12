# Superdesign

## Business Context
- **Type/Industry:** AI-powered design collaboration platform for creating and sharing UI/UX designs
- **What they do:** Cloud-based canvas for designing interfaces with component libraries, real-time collaboration, and asset management
- **Target audience:** Designers, design teams, product builders
- **Page goal:** Showcase a completed SharedHouse design system (multi-screen flow) with reusable components and invite cloning/remixing

# Page Layout & Structure

### Header / Navigation
Left: Superdesign logo (link home) + project title "Interfață SharedHous by Petrica andrei Dohot" with avatar thumbnail. Right: view count (1), favorite toggle (0), emoji reaction button (0 🤩), "Remix" CTA button in near-black with icon. Sticky, white background, near-black text. No accent color used here — navigation is functional, not decorative.

### Canvas / Prompt / Assets Tabs
Three horizontal tabs below header: **Canvas** (active, underlined in near-black), **Prompt**, **Assets**. Canvas tab shows the primary design output. White background, near-black text, near-black underline on active state.

### Primary CTA / Main Input
"Try it yourself" button: solid near-black background, white text, rounded corners (pill), positioned lower-left in a card ("Want to build on this? Clone this project to your account"). Secondary action: "Clone this" link text. Purpose: invite user to remix or fork the design.

### Design System Specification Block
- **Purpose:** Document all design constraints, color palette, component library, and pattern rules for the SharedHouse application
- **Layout:** Left column (stacked text), 2–3 paragraphs + bulleted lists describing: 5 reusable components (NavigationBar, HouseholdHeader, StatusBadge, MoneyCard, TaskCard), warm color palette (terracotta #C1673F, sage #7C8B6F, cream #F7F2EA), glassmorphism specs (20px blur, white 65% opacity, warm shadows), status badges (icon + text, no color-only status), dual light/dark modes. Tone: informational, technical, no decorative language.

### Flow Screens ×7 (Dashboard, Money Detail, Tasks, Calendar, Members, Settings User, Settings Admin)
Masonry grid of iPhone mockups + desktop/tablet previews, 2–3 columns varying width. Each mockup: rounded phone frame bezel (1.00:1 ratio thumbnail) showing screen design with warm palette (terracotta accents, cream backgrounds, sage text). Image positioning: alternates left-right placement across grid. Screens include:
1. **Dashboard (Light)** — household header, money summary card, due items, task list, calendar chips, bottom nav bar with central FAB
2. **Money Detail** — transaction list with status badges, member avatars, summary totals
3. **Tasks** — task cards with urgency states, assignee avatars, due dates
4. **Calendar** — month view with event chips in terracotta accent, week/month toggle
5. **Members** — household roster with avatars, invite code modal, household switcher
6. **Settings (User)** — preferences, notifications, profile edit
7. **Settings (Admin)** — household management, member roles, expense rules

All screens use **glassmorphism** (visible blur on cards, subtle borders), **warm accent colors** (terracotta for active states, calls-to-action, status highlights), and **calm typography** (no gamification, no neon, no corporate language).

### Component Library Callout
- **Purpose:** Highlight 5 named, reusable components with states and usage rules
- **Layout:** Bulleted list + checkmark (✓): NavigationBar (dynamic active states), HouseholdHeader (scroll effects), StatusBadge (6 status types: Confirmed/Reviewing/Overdue/etc., each with icon + text, no color-only indicator), MoneyCard (dynamic member data, amount + status), TaskCard (urgent/normal/scheduled states with visual hierarchy). One-line per component, no images embedded in this section — components appear in the full screens above.

### Color Palette Reference Block
- **Purpose:** Provide exact named-color tokens for implementation
- **Layout:** Short paragraph + inline color names (not hex): terracotta, sage, cream, gray, near-black, yellow, green, cyan. Notes: warm palette, no synthetic/electric tones, dual light + dark modes maintain warm coherence. Glassmorphism blur depth: 20px; white surface opacity: 65%; shadow tone: warm (terracotta tint, not pure gray).

### Interaction & Accessibility Notes
- **Purpose:** Document :focus-visible states, prefers-reduced-motion, and responsive breakpoints
- **Layout:** Short paragraph, bullet list: focus-visible present on all buttons/inputs, rounded corners (pills on primary CTAs), mobile-first responsive with bottom nav bar + central FAB (floating action button), dark mode toggle honor system preferences, status conveyed via icon + text (not color alone).

### End of Conversation / Feedback Section
Collapsed chat transcript ending with "End of conversation" marker. Below: star rating widget (1–5 stars) + "Add feedback" text button + "Don't show feedback widget" close. Right side: credit usage ("10.88 credits 17 minutes ago"), user avatar, menu buttons. Gray background, small text, optional/dismissible.

### Footer / Call-to-Action Card
Bottom: "Want to build on this? Clone this project to your account" — card with rocket icon, text, "Try it yourself →" button in near-black pill. Light gray background, soft border, white text on button.

**Notable patterns:**
- Masonry grid of mockups (iPhone + desktop previews, varying widths, 2–3 columns)
- Alternating image placement (mockups shift left ↔ right across rows)
- Sticky header (logo, project title, view count, Remix CTA)
- Warm color accent (terracotta) repeated across all UI surfaces (buttons, status badges, active nav states)
- No background alternation; white page background throughout
- Glassmorphism applied to all card surfaces in mockups (blur + transparency visible in previews)