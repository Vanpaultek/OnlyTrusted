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
  SENSOR_PRIVACY_ARCHITECTURE.md       (this file)

ios/OnlyTrusted/Sources/SensorPrivacy/
  SensorPrivacyController.swift        (protocol + SensorState)
  ConsumerSensorPrivacyController.swift (iOS consumer impl)
  SensorPrivacyScreen.swift            (SwiftUI UI)

android/app/src/main/java/com/onlytrusted/app/ui/sensorprivacy/
  SensorPrivacyController.kt           (interface + SensorState)
  ConsumerSensorPrivacyController.kt   (Android consumer impl)
  DeviceOwnerSensorPrivacyController.kt (Device Owner impl + provisioning helper)
  PrivilegedSensorPrivacyController.kt (OEM-only skeleton)
  OTDeviceAdminReceiver.kt             (DeviceAdminReceiver for Device Owner)
  SensorPrivacyScreen.kt               (Compose UI)
  ScreenStateReceiver.kt               (BroadcastReceiver for lock/unlock)
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
