# OnlyTrusted — Beta Testers Guide

Thank you for helping test OnlyTrusted before the public release.

This document is self-contained — you don't need to read anything else
in the repo first. If you're an engineer and want the deeper picture,
start from `README.md` and `docs/SENSOR_PRIVACY_ARCHITECTURE.md` instead.

---

## 1. What OnlyTrusted is

A **quieter, safer phone for the people who matter.**

- Only your trusted contacts' calls + texts come through. Everyone
  else is silenced or blocked at the OS level.
- Every block is logged on-device so you have an auditable record.
- Mic & camera auto-lock when the phone screen locks (Android).
- Zero cloud, zero account, zero compromise — nothing leaves your
  phone.
- Open-source under AGPL-3.0 — every claim is verifiable in the code.

Owner: **VanPaulTek Inc** · support: **admin@vanpaultek.com**

---

## 2. What we need you to test

The current build (**internal test 1, v0.1.1**) is focused on the
new **Mic & Camera Lock** feature. Real-world testing matters because
some phone manufacturers (Samsung, Xiaomi, OnePlus, Huawei) layer
aggressive battery-killer policies on top of Android that we can
only find by watching the service survive on real devices.

### Priority test paths

1. **Install → onboarding → first screen.** Let us know if anything
   is confusing, broken, or looks wrong.
2. **Mic & Camera Lock** (Android only — iOS has honest-limits-only
   version):
   - Settings → **Mic & Camera Lock** → flip **Lock-watcher service** ON.
   - Lock your phone. Check if the notification stays visible.
   - Wait 10-30 min with screen off, unlock, check that your mic still
     works (open a voice recorder or make a WhatsApp voice note).
   - If it got killed, tell us your phone model + Android version.
3. **Battery exemption prompt** — tap it, grant it, repeat the lock
   test. Should survive longer on OEM battery-killer phones.
4. **Device Admin** (optional) — tap "Enable Device Admin" in the
   Mic & Camera Lock detail screen. Grant it. Lock the phone, try
   to open the camera via the lockscreen shortcut — should be blocked.
   Unlock, camera works again.
5. **Safety Override pause** — tap a 5-minute pause chip. During
   that window the lock-watcher should NOT mute mic/camera on lock.
   After the window expires, enforcement resumes automatically.
6. **Dashboard "Security Lock" sheet** — tap the Security Lock card
   under Quick Actions. Toggle the Mic & Camera auto-lock switch.
   Confirm it reflects + controls the same state as the Settings page.
7. **Quick Settings tile** — pull down the system quick-settings
   panel → Edit → add "OnlyTrusted mic/cam" → confirm one-tap mute
   works.
8. **Launcher shortcut** — long-press the OnlyTrusted app icon on
   your home screen → tap "Lock + Mute Now".
9. **Volume-down double-press trigger** — enable in Settings →
   Mic & Camera Lock → More options → Accessibility. Double-press
   volume-down → mic/cam should cut immediately.
10. **Reactive mic alerts** — flip "Reactive mic alerts" ON in
    Mic & Camera Lock. Open Instagram / WhatsApp / voice recorder.
    You should get a notification: "<app> is using your mic."

### Secondary (worth a smoke test)

- Trusted Contacts (Vault / Contacts tab) — add a contact, confirm
  calls from that number ring through, calls from unknown numbers
  silenced.
- Blocked list — add a number, confirm calls / SMS blocked.
- Events log — confirm every block + alert gets a timestamped entry.
- Weekly Digest banner on the Dashboard (fires Mondays if ≥1 event).
- Honest Limits callout — tap it, confirm the sheet opens.

### Where to focus your energy

- **Any crash.** Even a one-liner "it crashed when I did X" helps.
- **OEM-specific weirdness.** Samsung One UI, Xiaomi MIUI, Oppo
  ColorOS, Vivo FuntouchOS, OnePlus OxygenOS all have their own
  background-kill behaviours.
- **Battery drain over 24h.** Expected is 1-3% / day with the
  lock-watcher running. Report anything higher.
- **Permissions prompts that don't make sense.**
- **Localised strings that look wrong** — we ship 9 languages
  (en, es, fr, pt, hi, ar, de, id, te) and auto-detect from your
  phone locale.

---

## 3. How to join the test

### Android (available now)

1. You should receive an invitation email with a link that looks
   like `https://play.google.com/apps/internaltest/<ID>`.
2. On the **same Google account the invite was sent to**, open the
   link in Chrome.
3. Tap **Accept invitation** → **Download it on Google Play**.
4. Install OnlyTrusted from the Play Store. The app name may show
   as "**com.onlytrusted.app (unreviewed)**" for the first few days —
   that's Google still processing the first-ever upload. The real
   name shows once review completes.

### iPhone (not yet available)

TestFlight invitations will go out when the iOS build enters beta.
If you want iPhone + Android, tell us and we'll add you to both lists.

---

## 4. Troubleshooting the invite link

If the invite link gives you **"We're sorry, the requested URL was
not found on this server"**:

- You may need to wait **15-30 min** after you accept — Google Play
  can take that long to make a first-time listing live.
- Confirm you're signed into the **same Google account** on your
  phone's Play Store as the one the invite went to (Play Store → top
  right profile icon).
- If still no luck after a few hours, email **admin@vanpaultek.com**
  with the Google account email you accepted the invite with and
  we'll check your tester list membership.

---

## 5. How to report feedback

### Fast path: email

**admin@vanpaultek.com** — one line is fine. Format:

```
Subject: OnlyTrusted v0.1.1 — <short description>

Phone model: Samsung Galaxy S23 (Android 14, One UI 6.1)
Steps to reproduce: 1) open mic & camera lock 2) ... 3) ...
What you expected: ...
What actually happened: ...
Screenshot: (attach if helpful)
```

### Longer feedback / discussion

Reply to the Google Play tester-feedback form (sent after you
install), or open a GitHub issue:
<https://github.com/Vanpaultek/OnlyTrusted/issues>

### Crash reports

Google Play Console auto-collects crash + ANR data from internal
testers. We'll see them. Our own code symbolicates fine; crashes
inside system libraries (SQLCipher, ML Kit, etc.) will show as hex
addresses in Play Console but we can still tell they happened.

---

## 6. Privacy — what we collect from testers

**Nothing beyond what Google Play Console sends us automatically.**
Specifically:

- Crash + ANR stack traces (from Google Play Console — automatic for
  all internal/closed/open/production tracks).
- Install / uninstall counts (aggregate, no email addresses).
- Optional feedback you type into Google Play's tester feedback form.

**We do not operate a backend.** OnlyTrusted sends no telemetry, no
analytics, no usage data to any VanPaulTek server. The AGPL-3.0
source is proof — see `docs/PRIVACY_ARCHITECTURE.md` for the
complete data-flow audit. If you want to be extra sure, uninstall
Google Play Services telemetry too (your call — unrelated to us).

---

## 7. Known issues in v0.1.1

- **Google Play shows "contains native code without debug symbols"
  warning.** Non-blocking. Our own crash reports still symbolicate;
  only dep-library crashes (SQLCipher internals etc.) show as hex.
- **App Shield "Shield apps on lock" (iOS Elite)** requires
  Apple-approved Family Controls entitlement. Simulator shows the UI
  but shielding is a no-op; real-device builds need the approval.
- **Weekly Digest** notification scheduler is Android-only in v0.1.1.
- Narrated video + text subtitles for 9 languages are generated via
  macOS `say` (local, zero-cloud) — natural-sounding only for en, es,
  fr, pt, de (Sandy voice); hi/ar/id/te use classic voices and sound
  more robotic. We can swap to higher-quality TTS before public
  launch if feedback is strong.

See `BUG_REPORT.md` for the full audit log.

---

## 8. How to leave the test

Any time:

1. Open the invite link (same one you joined with) → **Leave the program**.
2. Uninstall OnlyTrusted from your phone.
3. Reinstall from the public Play Store once we launch (your settings
   won't carry over — a fresh install starts from the onboarding flow).

No hard feelings. We appreciate every tester, including ones who
try it and bounce after a day.

---

## 9. Legal

- OnlyTrusted is licensed under **AGPL-3.0-or-later**. See `LICENSE`
  in the repo root.
- Source: <https://github.com/Vanpaultek/OnlyTrusted>
- Owner: VanPaulTek Inc · support: admin@vanpaultek.com
- You're testing a pre-release build. Expect bugs. Don't rely on
  OnlyTrusted as your sole safety tool during the test period — keep
  your existing phone-safety habits in place.

---

Thanks for testing. Honest feedback — even brutal — is what makes
this app actually safe to ship.
