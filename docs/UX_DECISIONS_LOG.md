# OnlyTrusted — UX Decisions Log

**Audience:** a UX designer or product reviewer coming in cold who needs to know what decisions were made, why, and what's still open.

Last updated: 2026-10-28 (v0.1.9 shipped, v0.1.10 in progress)

---

## Onboarding flow

```
Splash → Onboarding → Consent → (Android permission dialogs)
       → ProfileSetup (Email → Biometric optional) → Dashboard
```

### Decisions made

**Removed the mandatory PIN step** (v0.1.7)
- Tester reported "pin and passkey asking twice" + friction on first install
- PIN is now optional, set up via Settings → Duress Mode if the user wants it
- App Lock defaults to biometric (via `AppLockManager.setEnabled(true)` + `BiometricPrompter`)
- Tradeoff: fewer users will have a local lock on install → more exposed data if phone is picked up. We added the Security Nag (visits 3-5 show card, visit 6 shows modal) to recover this.

**Removed the in-app Permissions Overview screen** (v0.1.9)
- Tester perceived it as "being asked twice" when Android then shows its own system dialog per permission
- Replaced with the Consent screen's `PermissionHeadsUpCard` — one line of heads-up copy, no interaction
- Android fires its own system dialogs after Consent, driven by `MainActivity.requestCorePermissionsIfNeeded()`
- Honest limit: Android doesn't allow pre-granting; the system dialog is non-skippable by design

**Consent screen weighted layout** (v0.1.9)
- Hero image occupies 40% of screen height via `fillMaxHeight(0.40f)`
- Three toggle cards + CTA + legal footer fill the remaining 60% with no scroll
- Toggle cards use tighter paddings (`14 sp` title, `11 sp` detail, `40 dp` icon tile) so three fit on short screens
- Cropping: `Alignment` biased close to TopCenter so the shield + wordmark area is preserved; mountain / lake at bottom gets clipped

**Hero image orientation** (iterated many rounds)
- `splash_hero` composition has starry sky at top, shield + wordmark middle, mountain/lake bottom
- Bias `verticalBias = -0.85f` on the Image's `BiasAlignment` so the hero shows the branded middle region, not the empty sky
- 8 dp top margin keeps the hero clear of the camera cutout

**Chevron icons removed from Onboarding benefit tiles** (v0.1.9)
- `>` chevron implied "tap to open deeper screen" but the tiles were decorative only
- Removed to prevent misleading tap affordance

### Still open

- No design-review pass has been done. Every current layout decision is tester-driven.
- Short-screen devices (height < 640 dp) are untested — the weighted Consent layout might crowd.
- Tablets + foldables are untested beyond Compose Previews (`ui/previews/MultiDevicePreviews.kt`)
- Dark mode is the only shipping mode. Light mode has no design.

---

## Dashboard

### Decisions made

**Three stat tiles are tappable** (v0.1.9)
- Trusted Contacts → Vault (contact list)
- Blocked → BlockList
- Scam Contacts → ContactScan
- Previously decorative; tappability was a tester's natural expectation

**Contact auto-import consent modal** (v0.1.10)
- After READ_CONTACTS granted, Dashboard counts available contacts and shows a modal confirming the import before actually reading anything
- Modal: "We found N contacts. Add them all as trusted?" with [Import N] and [Not now — I'll add manually]
- "Not now" marks the flow done so the modal doesn't re-fire
- Honest: avoids the "privacy app that silently scraped my contacts" perception

**Security Nag card + modal** (v0.1.10)
- Dashboard visits 3–5: inline card ("Lock OnlyTrusted when you close it")
- Visit 6: one-time modal with [Enable App Lock], [Not now], [Never ask again]
- 7-day snooze per dismissal; 3 snoozes auto-stops; "Never ask again" opts out permanently
- Settings → "Suggest security setup" toggle is the master kill switch
- Policy logic extracted to `SecurityNagPolicy.decide(State)` and unit-tested

### Still open

- Scam Contacts count will almost always be 0 for real users — the bundled `scam_numbers.v1.txt` has only ~34 placeholder entries. Needs real feed (FTC Consumer Sentinel, FCC Do-Not-Call) before public launch, or hide the tile when the matched count is 0 and no real feed is loaded.
- Dashboard shield icon could show a ⚠️ badge when `!AppLockManager.isEnabled` — not yet added.
- "Protection Active" card iconography is a leftover from earlier UX; probably should be redesigned once the nag is in place.

---

## Launcher icon

**Current state** (v0.1.10):
- Shield-only crop from the "Neon Mountain Shield" composition at `/Users/vinreddy/Downloads/`
- Asymmetric insets shift shield down inside the 108 dp canvas
- Static mipmap PNGs regenerated at all five densities with midnight background + 50-75% shield
- Monochrome adaptive-icon layer for Android 13+ Themed Icons

**Debugging note**: the bitmap wrapper originally used `gravity="center"` which drew at native size and silently ignored every `<inset>` adjustment. The fix (`gravity="fill"`) was found on round 10 of iteration; the smoke-test Gradle task (`./gradlew extractLauncherIcons`) exists so this class of bug surfaces on round 2 next time.

**Still open**: see `ICON_DESIGN_BRIEF.md` — the current icon is acceptable for internal testing, not launch-ready.

---

## iOS parity with Android — architectural limits

After the 2026-10-28 parity audit, iOS reached dashboard-level parity with Android on these items:

- **Dashboard existed already** (home tab was always the Dashboard; it was mislabeled as "Vault" in an earlier internal note that proved wrong).
- **Added v0.1.10:** `TrustedOnlyModeCard` (iOS counterpart of Android's `TrustedModeContent`) with silence-calls + flag-SMS toggles backed by `TrustedOnlyModeState` singleton.
- **Added v0.1.10:** `SensorOwnershipCard` (iOS counterpart of Android's `SensorOwnershipContent`) reading from `ConsumerSensorPrivacyController.readState()`.
- **Account-first reorder** on the Settings Group 1 landed on both platforms simultaneously.

**iOS cannot catch up on these — architectural limits**, not product gaps:

- **Scam SMS Scan** — iOS does not let third-party apps read the SMS inbox. The only honest iOS path is an `ILMessageFilterExtension` (Messages Filter) that classifies individual messages at delivery time. We ship the Android version of Scam SMS Scan today; iOS equivalent would be a separate target + review process.
- **Shield watermark** — iOS does not let apps draw outside their own window. We cannot overlay an owner tag on the system screenshot the way Android can with `FLAG_SECURE` + a drawn overlay.
- **Four-tier Mic & Camera Lock** — iOS has no equivalent of Android's Device Owner / Device Admin / Privileged roles. iOS's `AMFI` doesn't allow user-space delegation; we ship the honest-UX Consumer tier only.
- **System-wide call rejection** — iOS CallKit extensions can mark a call as "blocked" (routing to voicemail) but cannot literally mute the OS ringer. Documented in the TrustedOnlyModeCard subtitle so users know what the toggle actually does.

## What NOT to repeat

- **Don't ship UI without a design pass.** Every onboarding screen in this app converged after many tester-driven rounds. A professional designer could've shortcut most of that.
- **Don't modify adaptive-icon XML without extracting the rendered PNG and visually comparing.** The caching + `gravity="center"` combination ate hours.
- **Don't add 4-step forced onboarding.** Users bounce. We're down to 2 required steps (Consent + Email) + optional biometric.
- **Don't put wordmark text in the launcher icon.** Circular masks will clip it. Save wordmarks for splash + email + store listing.
- **Don't offer features we can't deliver.** Scam Contacts tile exists but has no real feed; this is a known embarrassment and will be fixed before public launch.
