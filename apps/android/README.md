# SharedHouse Android application

This directory owns the Jetpack Compose application and future Android-only adapters for secure
storage, notifications, deep links, billing, photo picking, and accessibility.

The application uses application ID `com.sharedhouse.android`, minimum SDK 26 and target SDK 36.
Its current functional vertical supports registration, development email verification, sign-in,
access/refresh rotation, sign-out, household discovery, creation and version-checked editing. The
UI uses Navigation Compose and Material 3 with light, dark and compatible dynamic-colour themes,
plus matching English/Romanian resources. Unimplemented Home areas remain explicit empty or
unavailable states; they never fabricate household activity.

Access and refresh tokens currently remain in process memory. This avoids insecure persistence,
but process death requires sign-in again until the planned Android Keystore-backed refresh-token
store is implemented. Release builds require HTTPS and default to a deliberately invalid API host;
release identity, endpoint, signing and email delivery still require deployment configuration.

Portable rules belong in `shared/`; Android UI and platform integrations stay here. Build and test
the application with:

```powershell
.\gradlew.bat :apps:android:app:lintDebug :apps:android:app:testDebugUnitTest :apps:android:app:assembleDebug
```

The emulator debug build uses `http://10.0.2.2:3000`. A different local API can be selected with:

```powershell
.\gradlew.bat :apps:android:app:assembleDebug -PSHAREDHOUSE_DEBUG_API_BASE_URL=http://192.0.2.10:3000
```
