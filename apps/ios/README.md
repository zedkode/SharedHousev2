# SharedHouse iOS application

This directory owns the future SwiftUI application and Apple-only adapters for Keychain, push
notifications, universal links, StoreKit, photo picking, and accessibility.

The first foundation slice intentionally does not create an Xcode project before the bundle
identifier, minimum runtime version, signing team, and app capabilities are approved. Portable
rules belong in `shared/`; iOS presentation and platform integrations stay here.
