# Route map

## Administration web

The Vite React application currently has no routing library. `apps/admin-web/src/main.tsx` renders
`App` at the root URL.

| URL | Entry | Layout |
| --- | --- | --- |
| `/` | `apps/admin-web/src/App.tsx` | Self-contained administration shell |

## Mobile

Android and iOS application routes are not implemented. The approved information architecture
defines five stable top-level destinations:

- Home
- Calendar
- Money
- Tasks
- House

The first mobile design target for EPIC-01-02 is the Home foundation state. It is a new target, not
an existing rendered screen.
