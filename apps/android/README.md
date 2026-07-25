# SharedHouse Android application

This directory owns the future Jetpack Compose application and Android-only adapters for secure
storage, notifications, deep links, billing, photo picking, and accessibility.

The first foundation slice intentionally does not create an installable application before the
design tokens, application identifier, minimum runtime version, and signing environments are
approved. Portable rules belong in `shared/`; Android UI and platform integrations stay here.
