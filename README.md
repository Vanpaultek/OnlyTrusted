# OnlyTrusted

A privacy-focused Android app that gives you visibility and control over who touches your microphone, camera, calls, and texts — with everything stored encrypted on your device.

**A product of VanPaulTek Inc.** Copyright © 2026 VanPaulTek Inc. Licensed under the **GNU Affero General Public License v3.0** — see [`LICENSE`](LICENSE). Support: `reddy@vanpaultek.com`.

> **If you deploy a modified version of this software — including over a network — you must publish the full source of your modifications to its users under the same license.** This is the AGPL-3.0 network clause. It prevents commercial forks from stripping the privacy guarantees without their users seeing the code.

**Security & release verification.** Report vulnerabilities privately via [`SECURITY.md`](SECURITY.md). Every tagged release ships with a CycloneDX SBOM + SLSA Build Provenance attestation — verify with `gh attestation verify OnlyTrusted-<tag>.aab -R Vanpaultek/OnlyTrusted` so you can confirm the published artifact was built by [this exact CI workflow](.github/workflows/release.yml) from a specific commit SHA.

## What it does (and what it doesn't)

**What it does**

- Alerts you (notification + vibration) whenever another app starts using the microphone or camera, and logs the event to an encrypted local database.
- Blocks incoming calls and silently drops SMS from any numbers in your personal block list (via `CallScreeningService` and a priority SMS receiver).
- Detects when the phone is in your pocket or face-down using the proximity sensor + accelerometer (pocket-dial protection).
- Protects this app's own screens from screenshots and screen recording via `FLAG_SECURE`.
- Keeps every event and the block list encrypted at rest with SQLCipher + an EncryptedSharedPreferences-managed key. No network calls, no analytics, no cloud sync.
- Shows a dashboard with 24-hour counts and a scrollable event history.

**What it cannot do** (do not advertise these — they are not technically possible from a third-party app)

- Prevent other apps from using the mic/camera at a hardware level without root.
- Stop a person on the other end of a video call from screen-recording their own device.
- Block screenshots of the home screen or other apps.
- Make the phone untrackable by your carrier, ISP, or OS vendor.

## Modules

| Module                                   | Role                                                           |
| ---------------------------------------- | -------------------------------------------------------------- |
| `services/SensorMonitorService.kt`       | Foreground service — audio & camera callbacks + 1.5s poller.   |
| `services/CallBlockerService.kt`         | `CallScreeningService` — blocks/silences numbers in DB.        |
| `services/SmsReceiver.kt`                | Priority SMS receiver — flags senders in DB.                   |
| `services/PocketDetectionService.kt`     | Proximity + accelerometer pocket detection.                    |
| `services/LockStateReceiver.kt`          | Logs screen on/off/unlock events.                              |
| `services/BootReceiver.kt`               | Restarts foreground services after reboot.                     |
| `data/db/AppDatabase.kt`                 | Room DB with SQLCipher factory; key stored in EncryptedPrefs.  |
| `ui/`                                    | Jetpack Compose + Material3 (Dashboard, Events, BlockList, Settings). |

## Repository layout

```
OnlyTrusted/
├── android/   ← Native Android app (Kotlin + Jetpack Compose)
├── ios/       ← Native iOS app (Swift + SwiftUI)
├── BUG_REPORT.md
├── LICENSE
└── README.md
```

## Build — Android

Requires Android Studio Hedgehog (or newer) and JDK 17.

```
cd android
./gradlew :app:assembleDebug
```

The Android app uses:

- Kotlin 2.0.21, Compose BOM 2024.12.01
- Room 2.6.1 + KSP
- SQLCipher 4.5.4
- androidx.security.crypto 1.1.0-alpha06
- `compileSdk` 36, `minSdk` 29

## Build — iOS (Simulator-only, Phase 2)

Requires Xcode 26 (or newer) and macOS 14+. XcodeGen is used to generate the Xcode project from `ios/project.yml`:

```
brew install xcodegen       # one time
cd ios
xcodegen generate
xcodebuild -scheme OnlyTrusted \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -configuration Debug \
  -derivedDataPath build \
  CODE_SIGNING_ALLOWED=NO \
  build
```

The iOS app uses:

- Swift 6, SwiftUI, Swift strict concurrency
- CryptoKit + Keychain for the trusted-contact vault
- LocalAuthentication (Face ID / Touch ID / device PIN) for App Lock
- CallKit `CXCallDirectoryProvider` extension for the user's block list
- IdentityLookup `ILMessageFilterQueryHandling` extension for unknown-sender SMS
- App Group `group.com.onlytrusted.app.shared` to share state with the two extensions
- Deployment target iOS 17

### iOS status (as of 2026-10-20)

**Simulator-only.** The app and both extensions build and install cleanly on the iPhone 17 Pro simulator. Real-device installation is not pursued today because:

1. `com.apple.developer.callkit` and `com.apple.developer.identitylookup.message-filter` entitlements require a paid Apple Developer Program membership for VanPaulTek Inc.
2. Signing those entitlements requires an Apple ID signed into Xcode (`Xcode → Settings → Accounts`).
3. Neither of the above is set up on this machine right now.

To resume real-device work later: sign in to Xcode with the Apple ID that owns the VanPaulTek Inc enrollment, confirm the Team ID in `Xcode → Settings → Accounts`, update `DEVELOPMENT_TEAM` in `ios/project.yml`, `xcodegen generate`, then build without `CODE_SIGNING_ALLOWED=NO`.

### iOS feature gaps vs Android (platform limitations, not TODOs)

- **Mic/camera monitoring** of other apps — iOS does not expose this to any third-party app.
- **Screen-overlay watermark** — iOS has no `SYSTEM_ALERT_WINDOW` equivalent. Not technically possible, ever.
- **Permission audit** of other apps — iOS does not expose this.
- **Call blocking in Simulator** — Simulator has no telephony stack. Extension builds and installs but cannot be exercised until a real-device build.
- **SMS filtering in Simulator** — Simulator does not deliver SMS. Same as above.

The in-app "Honest claims" screen states all of these to the user.

## Runtime permissions

On first launch the app requests: `RECORD_AUDIO`, `CAMERA`, `READ_PHONE_STATE`, `READ_CALL_LOG`, `RECEIVE_SMS`, `READ_SMS`, and (on API 33+) `POST_NOTIFICATIONS`.

Call screening also requires the user to pick OnlyTrusted as the default **Caller ID & spam** app in system settings — Settings → Apps → Default apps.

## Notes on honest marketing

Several claims commonly made by "security" apps are false and will get a listing rejected or taken down. The dashboard includes a visible "can / cannot" card so users set realistic expectations. Keep it.

## Non-interference pledge

The app is read-only against the system and never acts on anyone else's device:

- No system settings are flipped programmatically — we deep-link into system UI where the user flips the switch.
- No other apps are killed, modified, or stripped of permissions. We refuse Device Admin / Device Owner escalation.
- No third-party scanning, OSINT, "reverse lookup" or hack-back. If the user wants an attacker identified, they export the signed Evidence Pack and hand it to a carrier/bank/law-enforcement.
- No network at all — the manifest does not request internet permission.

The Non-interference tab inside the Legal screen states this to the user in plain language. Do not weaken it.
