# OnlyTrusted — Sensor Privacy Architecture

**What this document covers:** the four-tier architecture OnlyTrusted uses to
read and (where the OS allows) enforce microphone + camera privacy around
device lock events, plus the honest platform limits that apply to each tier.

**Audience:** engineers extending OnlyTrusted. Users should read the Honest
Claims page in the app instead.

---

## The honest headline

**On a normal consumer install from the App Store or Play Store, OnlyTrusted
cannot globally disable the microphone or camera for other apps when the
screen locks.** Both platforms reserve that privilege for OS-managed systems:

- **iOS**: No public API lets a third-party app flip global mic/camera
  privacy. Apple's privacy model is strictly per-app authorization. MDM
  profiles can do more, but a consumer App Store install cannot.
- **Android**: The underlying mechanism exists — `SensorPrivacyManager` +
  `MANAGE_SENSOR_PRIVACY` — but the mutating methods are gated behind a
  signature-level permission. A Play Store APK can read the state, cannot
  change it. Device Owner enrollment unlocks `DevicePolicyManager`-based
  enforcement for the camera, but Device Owner requires provisioning on a
  reset/fresh device.

What OnlyTrusted **does** ship on both platforms:

1. Reads the current mic/camera privacy state and surfaces it in UI.
2. Observes screen-lock / unlock transitions and writes them to the local
   Events log so the user has an auditable timeline.
3. Shuts down OnlyTrusted's own captures (if any) on lock.
4. Deep-links the user to the exact Settings page where they can revoke
   mic/camera permission per-app.
5. Reminds the user about iOS Focus modes and Android's Private Space (API
   35+) which can approximate "no mic / no camera while locked" without
   leaving OnlyTrusted's trust boundary.

---

## The four tiers

| Tier | Platforms | Installable by | Can enforce? | Shipped? |
|---|---|---|---|---|
| **Consumer** | iOS, Android | Any user via App/Play Store | Read-only + deep-links + log | ✅ Yes |
| **Device Owner** | Android | Power user / enterprise (requires reset + `adb shell dpm set-device-owner`) | Yes (camera + mic policy via DevicePolicyManager) | ✅ Yes, behind a provisioning gate |
| **Privileged system component** | Android | OEM / custom ROM build only | Yes (signature-permission access to `SensorPrivacyManager`) | 📄 Documented skeleton only — not shippable without an OEM deal |
| **MDM / Managed Device** | iOS | MDM-enrolled device only | Partial (per Apple's managed-device matrix) | ❌ Not in scope; separate enterprise product |

---

## The shared controller contract

Both platforms implement the same conceptual interface. The impls differ in
what `enforceLock()` + `restoreAfterUnlock()` actually do.

### iOS (Swift)

```swift
struct SensorState {
    let microphoneBlocked: Bool?    // nil = unknown / platform doesn't expose it
    let cameraBlocked: Bool?
}

protocol SensorPrivacyController {
    func readState() async -> SensorState
    func enforceLock() async -> SensorState
    func restoreAfterUnlock() async -> SensorState
    var canEnforceGlobally: Bool { get }   // false for Consumer impl
}
```

### Android (Kotlin)

```kotlin
data class SensorState(
    val microphoneBlocked: Boolean?,
    val cameraBlocked: Boolean?,
)

interface SensorPrivacyController {
    suspend fun readState(): SensorState
    suspend fun enforceLock(): SensorState
    suspend fun restoreAfterUnlock(): SensorState
    val canEnforceGlobally: Boolean   // false for Consumer, true for DeviceOwner+
}
```

Returning `null` for one of the two sensors means "we have no reliable
read for this value on this install tier" — the UI should render it as
"unknown" rather than guessing.

---

## Pre-lock state preservation

The naive implementation blindly restores both sensors to ON after unlock.
This is wrong — it would silently override the user's intentional mic-off
setting. The controller **must** snapshot pre-lock state and only reverse
changes it was responsible for.

```
Before lock:   mic=OFF,  cam=ON    (user had mic deliberately off)
During lock:   mic=OFF,  cam=OFF   (OnlyTrusted enforced cam-off)
After unlock:  mic=OFF,  cam=ON    (OnlyTrusted reverses ONLY the cam change)
```

Pseudocode:

```
onLock:
    snapshot := currentState()
    for sensor in [mic, cam]:
        if sensor is currently ON and user policy says lock-off:
            enforceOff(sensor)
            markAsOnlyTrustedChange(sensor)

onUnlock:
    for sensor in [mic, cam]:
        if we marked this sensor as our change at lock time:
            restoreTo(sensor, snapshot[sensor])
        else:
            leave untouched
```

---

## Consumer-tier auto-lock mechanics (Android)

The Play Store install cannot touch `MANAGE_SENSOR_PRIVACY`, but it can
still achieve a credible "lock-when-screen-locks" behaviour by combining
these levers — each honest about what it does and doesn't do:

1. **Foreground service** (`SensorPrivacyForegroundService`) — hosts the
   dynamic `ACTION_SCREEN_OFF` / `ACTION_USER_PRESENT` receiver. Required
   because Android does not deliver screen-state broadcasts to manifest-
   declared receivers. Shows a persistent, user-visible notification per
   Android law. `foregroundServiceType="specialUse"` with the rationale
   `Lock-watcher: observe screen state ...` declared in-manifest for Play
   review.

2. **`AudioManager.setMicrophoneMute(true)` on lock** — the one real
   consumer-app lever. Mutes the mic input level system-wide so other
   apps that acquire mic access receive silence. **Not** a security-grade
   cutoff (apps still see a "mic active" handle), but materially useful.
   Requires `MODIFY_AUDIO_SETTINGS` (normal permission).

3. **Battery optimisation exemption** (`BatteryOptimizationHelper`) —
   OEM Android builds (Samsung, Xiaomi, Huawei, OnePlus) aggressively
   kill background services. The exemption is user-granted via
   `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Google Play requires
   justification; ours is "lock-watcher must stay alive during long
   screen-off periods to toggle sensor restrictions on unlock."

4. **Quick Settings tile** (`SensorPrivacyTileService`) — one-tap toggle
   from the system pulldown. Permission `BIND_QUICK_SETTINGS_TILE` ensures
   only the system can bind the service. User manually adds the tile via
   the pulldown's edit flow.

5. **Volume-key trigger** (`SensorPrivacyAccessibilityService`) —
   optional. Observes volume-down double-press via a scoped
   AccessibilityService (`canRequestFilterKeyEvents=true`, nothing else).
   Compliance for Play Store's Accessibility Service Policy:
   - No `canRetrieveWindowContent` — we don't read UI text.
   - No `canPerformGestures` — we don't inject touches.
   - `onAccessibilityEvent` is intentionally empty — proof of
     non-exfiltration during Play review.
   - User enables it manually in Settings → Accessibility.

**Why NOT register the power button?** Both Android and iOS reserve the
power button for OS-level actions (shutdown, emergency SOS, lock).
Third-party apps cannot intercept it, and attempts via Accessibility
Services violate Play policy. The volume-down double-press is the
Play-approved alternative trigger.

---

## Tier 1 — Consumer (iOS + Android)

### iOS implementation

File: `ios/OnlyTrusted/Sources/SensorPrivacy/ConsumerSensorPrivacyController.swift`

What it does:
- Observes `UIApplication.willResignActiveNotification` as a proxy for
  screen-lock (iOS doesn't give consumer apps a direct lock callback).
- On resign-active: shuts down any OnlyTrusted-owned AVCaptureSession and
  AVAudioEngine instances, writes a `screenLock` event to EventStore.
- On `willEnterForegroundNotification`: writes a `screenUnlock` event,
  restarts captures the user had running.
- `readState()`: returns the app's own mic/camera authorization status via
  `AVCaptureDevice.authorizationStatus`. Returns `nil` for the global
  (cross-app) sensor state — iOS does not expose it.
- Deep-links to:
  - `App-prefs:Privacy&path=MICROPHONE` → iOS Settings → Privacy → Microphone
  - `App-prefs:Privacy&path=CAMERA` → iOS Settings → Privacy → Camera

Honest UX:
- Shows the orange/green mic/camera dot explanation at the top of the screen.
- Links to Focus modes documentation — Focus can be configured to silence
  calls and limit notifications when a Focus is active during sleep hours,
  which partially approximates "no interruption while screen is locked."

### Android implementation

File: `android/app/src/main/java/com/onlytrusted/app/ui/sensorprivacy/ConsumerSensorPrivacyController.kt`

What it does:
- Reads `SensorPrivacyManager.isSensorPrivacyEnabled()` (API 31+) for both
  sensors. On older APIs, returns `null`.
- Registers a `BroadcastReceiver` for `Intent.ACTION_SCREEN_OFF` /
  `ACTION_SCREEN_ON` to log lock/unlock events.
- Does NOT attempt to call the mutating methods — they will throw
  `SecurityException` without `MANAGE_SENSOR_PRIVACY`.
- Deep-links to:
  - `Settings.ACTION_PRIVACY_SETTINGS` → Android Settings → Privacy
  - `Settings.ACTION_MANAGE_APP_PERMISSIONS` → per-app permission manager

Honest UX:
- Shows the mic/camera indicator explanation (API 31+ shows these in the
  status bar automatically).
- Links to Private Space docs (API 35+) which gives true app isolation
  behind a separate authentication.

---

## Tier 2 — Android Device Owner

File: `android/app/src/main/java/com/onlytrusted/app/ui/sensorprivacy/DeviceOwnerSensorPrivacyController.kt`

### What Device Owner unlocks

- `DevicePolicyManager.setCameraDisabled(componentName, true/false)` —
  globally disables the camera for all apps while active.
- `DevicePolicyManager.addUserRestriction(componentName, DISALLOW_UNMUTE_MICROPHONE)` —
  locks the microphone muted; user cannot unmute via Quick Settings.
- These calls are enforced by the OS and persist across process death.

### Provisioning

Device Owner can be set **once per device**, only on a device that has no
existing accounts. The provisioning flow:

```bash
# 1. Factory reset the Android device (or use a brand-new device).
# 2. Skip through setup without adding any account.
# 3. Enable Developer Options → USB Debugging.
# 4. Connect to a computer and run:
adb shell dpm set-device-owner com.onlytrusted.app/.sensorprivacy.OTDeviceAdminReceiver
# 5. Verify:
adb shell dpm list-owners
```

Once set, Device Owner cannot be removed except via another factory reset.
Users should understand the commitment before proceeding.

### Why this isn't a Play Store "feature"

- Play Store install cannot elevate itself to Device Owner.
- Factory reset is a significant commitment.
- Enterprises typically provision Device Owner at scale via EMM solutions
  (Microsoft Intune, Google Workspace, Jamf). OnlyTrusted's Device Owner
  variant is for individuals who want hardware-level enforcement on a
  dedicated device.

### What the Device Owner impl does

- On screen lock: snapshot current camera + mic state, call
  `setCameraDisabled(true)` + `addUserRestriction(DISALLOW_UNMUTE_MICROPHONE)`.
- On unlock: `setCameraDisabled(false)` + `clearUserRestriction(...)` —
  only reverting changes the controller made, not user-initiated restrictions.
- `canEnforceGlobally` returns `true`.
- Fails loudly if the app is not actually Device Owner (defensive check via
  `DevicePolicyManager.isDeviceOwnerApp(packageName)`).

---

## Tier 3 — Privileged system component (Android)

File: `android/app/src/main/java/com/onlytrusted/app/ui/sensorprivacy/PrivilegedSensorPrivacyController.kt`

### What it would unlock

Direct `SensorPrivacyManager.setSensorPrivacy(SENSOR, true/false)` calls on
both the microphone and camera, without needing Device Owner. Faster + more
granular than DevicePolicyManager (works on secondary users, doesn't require
full device policy).

### Why it isn't shippable to end users

- The `MANAGE_SENSOR_PRIVACY` permission is `signature|privileged`.
- Only apps signed with the platform signing key or pre-installed in
  `/system/priv-app` can hold it.
- Requires either an OEM to include OnlyTrusted as a system app or a
  custom ROM build.

### Status

Skeleton only. The class is defined with its intended API but the
implementation throws `UnsupportedOperationException` on the mutating
methods when the required permission isn't granted. If OnlyTrusted
ever ships as a pre-installed OEM app, this is where the enforcement
lives.

---

## Tier 4 — iOS Managed Device

Not implemented. iOS MDM profiles (via Apple Business Manager / Apple
School Manager) have a strict API surface published in Apple's
Configuration Profile Reference. If OnlyTrusted ever ships an enterprise
MDM variant, it would use payloads like `allowCamera=false` /
`allowAssistant=false` — and would be a separate product with its own
licensing, enrollment, and support model.

Consumer iOS App Store installs cannot access any of this.

---

## iOS Honest-UX Sensor Privacy Center

Because iOS forbids every consumer-tier auto-enforcement path Android
has, the iOS build takes the opposite philosophy: **show the user
exactly what they can and cannot control, deep-link into Apple's
authoritative Settings, and never pretend to block when the OS won't
let us.** Core components:

### `ConsumerSensorPrivacyController.swift`

- Reads *this app's own* mic + camera authorization via
  `AVCaptureDevice.authorizationStatus(for:)`. Returns `nil` for
  cross-app state because iOS does not expose it.
- Observes `UIApplication.willResignActiveNotification` (lock proxy) +
  `.willEnterForegroundNotification` (unlock proxy).
- On lock: shuts down any OnlyTrusted-owned `AVCaptureSession` /
  `AVAudioEngine`. Logs `screenLock` to EventStore.
- On unlock: logs `screenUnlock`. Does **not** restart captures the
  user had running — leaves that to the owning feature's own policy.
- `canEnforceGlobally` returns `false` always. The UI uses this flag
  to render the honest "iOS limits apply" badge rather than fake-controls.

### `AlwaysRestrictedAppsStore.swift`

- User-maintained personal checklist of apps the user has deliberately
  denied mic / camera access to in iOS Settings.
- **Self-reported** — OnlyTrusted does not verify what iOS actually
  enforces. The UI explicitly labels this as "self-reported, not
  OS-verified" so there's no implied enforcement.
- Backed by `UserDefaults` (JSON-encoded `[RestrictedAppEntry]`),
  on-device only, never synced.

### `SensorPrivacyScreen.swift` — the iPhone Sensor Privacy Center

Four cards + a collapsible disclosure, in this order:

1. **Status** — OnlyTrusted's own mic/camera authorization chips.
2. **Shield apps on lock** (Pro) — FamilyControls path, see next section.
3. **Always Restricted** — self-reported checklist with:
   - Browseable sheet (~50 curated suggested apps, searchable, select-all)
   - Custom TextField entry for anything not in the suggestions
   - Toggle defaults for mic / cam revoked state
4. **Learn more about iOS limits** (collapsible) — Apple indicator-dot
   education, "Our own captures" status, Feedback Assistant link,
   explicit honest-limits bullets.
5. **Review in iOS Settings** — one-tap deep links to:
   - `App-prefs:Privacy&path=MICROPHONE`
   - `App-prefs:Privacy&path=CAMERA`
   On return (observed via `willEnterForegroundNotification` within
   5 min of tap), shows a **PostReturnPromptSheet** asking the user
   to tap-add any apps they just revoked — still honest ("iOS doesn't
   tell us what you changed, so this is self-reported").

Why no "fake block" buttons: a toggle labelled "Block mic for
Instagram" that silently fails because iOS forbids cross-app sensor
control would be lying to the user. The checklist model stores an
audit; it does not claim to enforce.

---

## iOS "Shield apps on lock" — FamilyControls + Shortcuts (Pro)

The one path on iOS that gets credibly close to the Android
auto-enforcement behaviour. Combines three Apple frameworks + a
user-created iOS Shortcuts Automation.

### Prerequisites

- `com.apple.developer.family-controls` entitlement — declared in
  `ios/Entitlements/OnlyTrusted.entitlements`. **Apple reviews and
  grants this entitlement individually per developer team** via
  <https://developer.apple.com/contact/request/family-controls-distribution/>.
  Simulator builds work; real-device builds fail code-signing until
  the request is approved on the real team ID.
- User grants Family Controls authorization in-app via
  `AuthorizationCenter.shared.requestAuthorization(for: .individual)`.
- User picks apps via Apple's `FamilyActivityPicker` (we never see the
  names — only opaque `ApplicationToken` instances).

### Runtime stack

| File | Role |
|---|---|
| `ios/.../AppShield/AppShieldState.swift` | ObservableObject holding `authorization`, `selection: FamilyActivitySelection`, `enabled`, `shieldedCount`. Persists selection to `UserDefaults` as JSON. |
| `ios/.../AppShield/AppShieldManager.swift` | `requestAuthorization()` / `refreshAuthorization()` / `applyShield()` / `clearShield()` / `rehydrate()`. Wraps `ManagedSettingsStore` with a scoped namespace. Called at launch from `OnlyTrustedApp` to re-apply the shield if it was active before a device restart. |
| `ios/.../AppShield/AppShieldSetupScreen.swift` | SwiftUI setup flow — auth request, `FamilyActivityPicker` embed, enable/disable toggle. |
| `ios/.../SensorPrivacy/SensorPrivacyAppIntents.swift` | `ShieldAppsOnLockIntent` + `UnshieldAppsOnUnlockIntent`. Returns `ProvidesDialog` with specific status strings for Shortcuts / Siri. |

### Shortcuts automation setup

OnlyTrusted cannot programmatically create a Shortcuts Automation —
Apple intentionally restricts that API. The user wires it themselves,
following the 5-step guide rendered in-app:

1. iOS Shortcuts → Automation tab.
2. **+** → Create Personal Automation → **When iPhone is Locked** → Next.
3. Add Action → search "OnlyTrusted" → tap **Shield apps**.
4. Toggle **Ask Before Running** OFF → Done.
5. Repeat with trigger **When iPhone is Unlocked** → action **Unshield apps**.

Once set up, the system fires the two intents automatically on screen
state transitions.

### Enforcement model

- `applyShield()` sets
  `ManagedSettingsStore(named: "OnlyTrustedAppShield").shield.applications`
  to the user's selection. iOS blocks every token in that set from
  opening — the user sees a system shield screen instead.
- `clearShield()` sets the field back to `nil`.
- Scoped `ManagedSettingsStore` namespace isolates us from any other
  app that might use the framework. Clearing our store doesn't clear
  theirs.
- `applyShield()` returns `false` and triggers `clearShield()` when
  selection is empty, so the UI doesn't claim coverage falsely.
- On launch, `rehydrate()` re-applies the shield if the user had it
  active before the device restart. If authorization was revoked
  meanwhile, we silently clear the "enabled" bit.

### Honest trade-offs documented in-app

- Shielded apps are blocked **entirely**, not just mic/cam —
  notifications, calls, foreground UI all stop. The "What Family
  Controls does" legal transparency banner on the shield card calls
  this out explicitly before the user enables.
- The suggested-apps list uses opaque `ApplicationToken`s from Apple's
  `FamilyActivityPicker`. OnlyTrusted never sees which apps the user
  picked — not even to log. This is Apple's design, not ours.
- Family Controls authorization also exposes aggregate Screen Time
  signals. OnlyTrusted's AGPL-3.0 source is cited as code-path proof
  that we don't read, store, or transmit any of it.

### App Intents + Siri phrases

Registered via `OnlyTrustedAppShortcutsProvider`:

| Intent | Siri phrases |
|---|---|
| `LockMicAppIntent` | "Lock mic in OnlyTrusted" / "OnlyTrusted mic off" |
| `RestoreMicAppIntent` | "Restore mic in OnlyTrusted" / "OnlyTrusted mic on" |
| `OpenMicrophoneSettingsAppIntent` | "Open mic settings via OnlyTrusted" |
| `OpenCameraSettingsAppIntent` | "Open camera settings via OnlyTrusted" |
| `ShieldAppsOnLockIntent` | "Shield my apps with OnlyTrusted" / "OnlyTrusted shield on" |
| `UnshieldAppsOnUnlockIntent` | "Unshield my apps with OnlyTrusted" / "OnlyTrusted shield off" |

Each intent returns a `ProvidesDialog` result so Shortcuts shows the
actual outcome ("Shield applied to 7 apps" / "Not authorized — grant
Family Controls first") rather than a silent success/failure.

---

## Event log integration

Every tier logs the following events to the on-device Events log, same
schema as calls/texts:

| Event kind | Fired on | Payload |
|---|---|---|
| `screenLock` | Lock detected | `{mic:bool?, cam:bool?, controller:"consumer"\|"deviceOwner"\|"privileged"}` |
| `screenUnlock` | Unlock detected | same |
| `sensorPrivacyEnforced` | Controller changed a sensor state | `{sensor, from, to, reason}` |
| `sensorPrivacyRestored` | Controller reverted a change | `{sensor, from, to}` |
| `sensorPrivacyRefused` | Controller lacked permission | `{sensor, attempted:true, succeeded:false, reason:"MANAGE_SENSOR_PRIVACY not granted"}` |

The user can audit these in Settings → Events, filter by `security`.

---

## File map

```
docs/
  SENSOR_PRIVACY_ARCHITECTURE.md         (this file)

ios/OnlyTrusted/Sources/SensorPrivacy/
  SensorPrivacyController.swift          (protocol + SensorState)
  ConsumerSensorPrivacyController.swift  (consumer impl — read + log + stop
                                          OnlyTrusted-owned captures on lock)
  AlwaysRestrictedAppsStore.swift        (self-reported checklist store)
  SensorPrivacyScreen.swift              (iPhone Sensor Privacy Center UI)
  SensorPrivacyAppIntents.swift          (6 App Intents + AppShortcutsProvider
                                          — Lock Mic, Restore Mic, Open Mic/Cam
                                          Settings, Shield Apps On Lock,
                                          Unshield Apps On Unlock)

ios/OnlyTrusted/Sources/AppShield/
  AppShieldState.swift                   (@MainActor ObservableObject;
                                          authorization + selection +
                                          enabled + shieldedCount; JSON
                                          persistence via UserDefaults)
  AppShieldManager.swift                 (requestAuthorization / applyShield /
                                          clearShield / rehydrate; wraps
                                          ManagedSettingsStore)
  AppShieldSetupScreen.swift             (auth prompt + FamilyActivityPicker
                                          embed + enable/disable toggle)
  AppShieldUpsellSheet.swift             (Pro-tier gate for non-subscribers)

ios/Entitlements/
  OnlyTrusted.entitlements               (declares
                                          com.apple.developer.family-controls —
                                          requires Apple approval per team ID)

android/app/src/main/java/com/onlytrusted/app/ui/sensorprivacy/
  SensorPrivacyController.kt             (interface + SensorState)
  ConsumerSensorPrivacyController.kt     (consumer impl — AudioManager mic
                                          mute on lock)
  DeviceAdminSensorPrivacyController.kt  (user-dialog Device Admin tier)
  DeviceOwnerSensorPrivacyController.kt  (ADB-provisioned Device Owner tier)
  PrivilegedSensorPrivacyController.kt   (OEM-only skeleton)
  OTDeviceAdminReceiver.kt               (DeviceAdminReceiver)
  SensorPrivacyScreen.kt                 (Compose UI)
  SensorPrivacyForegroundService.kt      (specialUse FGS hosting the receiver)
  ScreenStateReceiver.kt                 (dynamic SCREEN_OFF / USER_PRESENT)
  SensorPrivacyLog.kt                    (EncryptedSharedPreferences log)
  SensorPrivacyTileService.kt            (Quick Settings tile)
  SensorPrivacyAccessibilityService.kt   (volume-down double-press trigger)
  BatteryOptimizationHelper.kt           (ignore-battery-optimisation prompt)
  SafetyOverride.kt                      (5/15/60-min bounded pause)
  AudioRecordingWatcher.kt               (reactive mic-usage alerts)
  AudioRecordingWatcherState.kt          (opt-in gate for the watcher)
  LockAndMuteActivity.kt                 (launcher-shortcut target)
```

---

## Testing matrix

| Scenario | Consumer iOS | Consumer Android | Device Owner Android | Privileged |
|---|---|---|---|---|
| App opens, read state | Returns app auth status; global = `nil` | Returns `SensorPrivacyManager` state (API 31+) | Returns `DevicePolicyManager.getCameraDisabled()` + user restriction | Returns `SensorPrivacyManager` state (writable) |
| Screen locks | Logs `screenLock`; stops app captures | Logs `screenLock` | Logs; enforces cam-off + mic-mute | Enforces via SensorPrivacyManager |
| Screen unlocks | Logs `screenUnlock`; restores captures | Logs `screenUnlock` | Restores pre-lock snapshot | Restores pre-lock snapshot |
| Attempt enforce without privilege | n/a (iOS has no path) | Returns `sensorPrivacyRefused` event | Fails loudly if not Device Owner | Throws if `MANAGE_SENSOR_PRIVACY` not granted |

---

Maintained by: VanPaulTek Inc · admin@vanpaultek.com
