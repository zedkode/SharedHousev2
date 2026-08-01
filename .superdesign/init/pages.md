# Page dependency trees

## `/` — Administration foundation

Entry: `apps/admin-web/src/App.tsx`

Dependencies:

- `apps/admin-web/src/App.tsx`
  - `packages/contracts/src/index.ts` through `@sharedhouse/contracts`
- `apps/admin-web/src/main.tsx`
  - `apps/admin-web/src/App.tsx`
  - `apps/admin-web/src/styles.css`

The page renders a masthead, explanatory hero, contract/environment facts and three foundation
cards. There are no loading, authentication or responsive render branches.

## Mobile Home — new target

No entry file exists yet. The approved target should eventually depend on:

- native application shell;
- shared KMP presentation contract;
- household switcher;
- cycle selector;
- personal money summary;
- next due item;
- next assigned task;
- pending request summary;
- five-destination bottom navigation.
