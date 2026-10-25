# Translation review checklist

Last updated: 2026-10-25 · covers release `v0.3.5+`

OnlyTrusted ships 8 UI languages: **English, Spanish, French, Portuguese, Hindi,
Arabic, German, Indonesian**. The non-English translations in `v0.3.5` are
**machine translations produced by Claude Opus** and have not been reviewed by
native speakers. Every safety-critical string carries a `[REVIEW]` prefix in the
translated resource file; the `ReviewBadge` UI component on both platforms strips
the prefix at render time and shows a small amber **"⚠ translation pending review"**
caption beneath the translated text so users know the string hasn't been signed
off.

**Do not promote this release to the Play Store / App Store language metadata
until a native speaker has signed off on the strings listed below.** Shipping a
badly-translated SOS message could delay rescue in a real emergency.

---

## Strings flagged `[REVIEW]`

These are the 32 safety-critical string keys that MUST get a native-speaker
review per locale. Priority order: SOS first, duress second, legal third,
system alerts last.

### Priority 1 — Distress Signal (SOS)
Botched translations here could delay emergency rescue. Verify the exact
wording triggers urgency in the target culture — "HELP" in English becomes
different words in different languages depending on register (formal vs
colloquial).

| Key                | English source                                      |
|--------------------|-----------------------------------------------------|
| `sos_title`        | Distress Signal                                     |
| `sos_send`         | Send Distress Signal                                |
| `sos_countdown`    | Sending in %1$d…                                    |
| `sos_cancel`       | Cancel                                              |
| `sos_sent`         | SOS fired to %1$d contact(s).                       |
| `sos_body_prefix`  | HELP — I need urgent assistance.                    |
| `sos_body_location`| My current location: %1$s                           |

**Review question for the native speaker**: if you received the translated
`sos_body_prefix` as an SMS from a family member, would you know immediately
it was an emergency? Reject anything that reads like a project-management
request.

### Priority 2 — Duress

| Key                   | English source                                   |
|-----------------------|--------------------------------------------------|
| `duress_decoy_opened` | Decoy vault opened. Silent SOS sent.             |

**Review question**: the whole duress mechanism depends on the user NOT
reading this message, because it's shown only to the attacker viewing the
decoy vault. Verify the translation feels natural enough to not raise
suspicion.

### Priority 3 — Legal / pricing

| Key                | English source                                                                   |
|--------------------|----------------------------------------------------------------------------------|
| `legal_auto_renew` | Auto-renews at the price shown. Cancel anytime in Google Play Subscriptions.     |
| `legal_no_server`  | OnlyTrusted has no server. Everything stays on this device.                      |

**Review question**: in your country's regulatory environment, is the renewal
disclosure sufficient? Spanish (es-MX vs es-ES) and Portuguese (pt-BR vs pt-PT)
in particular may need country-specific variants.

### Priority 4 — System alerts (push notifications)

| Key                              | English source                                            |
|----------------------------------|-----------------------------------------------------------|
| `alert_mic_title`                | Microphone is in use                                      |
| `alert_mic_body_generic`         | Something on your phone is using the microphone.          |
| `alert_mic_body_with_pkg`        | %1$s is using the microphone.                             |
| `alert_camera_title`             | Camera is in use                                          |
| `alert_camera_body`              | Camera %1$s was just opened.                              |
| `alert_call_blocked_title`       | Call blocked                                              |
| `alert_call_blocked_body`        | Blocked an incoming call from %1$s.                       |
| `alert_sms_flagged_title`        | Spam SMS flagged                                          |
| `alert_sms_flagged_body`         | A message from %1$s matched your block list.              |
| `alert_untrusted_call_title`     | Untrusted call silenced                                   |
| `alert_untrusted_call_body`      | %1$s is not in your family vault. Call was silenced.      |
| `alert_untrusted_sms_title`      | Untrusted SMS                                             |
| `alert_untrusted_sms_body`       | %1$s is not in your family vault.                         |
| `alert_robocall_title`           | Likely robocall silenced                                  |
| `alert_robocall_warn_title`      | Suspicious caller                                         |
| `alert_lockscreen_title`         | OnlyTrusted alert                                         |
| `alert_lockscreen_body`          | Unlock the phone to see details.                          |

---

## File locations per locale

- **Android**: `android/app/src/main/res/values-<lang>/strings.xml` where
  `<lang>` is `es`, `fr`, `pt`, `hi`, `ar`, `de`, or `id`.
- **iOS**: `ios/OnlyTrusted/Resources/Localization/Localizable.xcstrings` —
  single JSON file with every locale nested under `strings.<key>.localizations.<lang>`.

To remove the `[REVIEW]` tag after a native speaker signs off:
1. Open the file for the reviewed locale.
2. Find the string value starting with `[REVIEW] `.
3. Replace with the reviewed translation (strip `[REVIEW] `).
4. The `ReviewBadge` UI auto-hides for strings that no longer carry the sentinel.
5. Commit with a `review:` prefix in the subject so git log makes it obvious which
   strings in which locale have been signed off.

---

## What this release does NOT yet translate

The ~500 remaining user-facing strings — Dashboard body copy, Vault editing
flows, Blocked list detail, Events log entries, every per-feature Settings
screen, every Premium / Elite feature screen (Breach Watch, Shamir Split,
Border Mode, Panic Lockdown, etc.) — are **still hardcoded in English in
Swift and Kotlin source**.

Converting them is mechanical but large: `Text("Set up your Passkey")` on
every call site becomes `Text(LocalizedStringKey("passkey_setup_title"))` on
iOS and `stringResource(R.string.passkey_setup_title)` on Android. The
infrastructure to add new keys and translations is in place; what remains is
the extraction + replacement work. Follow-up commit `v0.3.6+` will tackle
feature areas one at a time (Vault → Blocked → Events → Settings sections).

For languages the user picks that we don't ship, the picker still accepts the
choice but the UI falls back to English per OS resource-qualifier rules + the
Settings screen renders the "⚠ translation pending review" badge next to the
selection to tell the user why.

---

## Review sign-off log

Add a row per locale per review session. The git commit hash goes in the
"Reviewed in" column so we can trace exactly which translation set a native
speaker signed off on.

| Locale | Reviewer                 | Date       | Reviewed in commit | Notes                                  |
|--------|--------------------------|------------|--------------------|----------------------------------------|
| es     | _pending native review_  | —          | —                  | ES-MX / ES-ES variant decision pending |
| fr     | _pending native review_  | —          | —                  | FR-FR / FR-CA variant decision pending |
| pt     | _pending native review_  | —          | —                  | PT-BR / PT-PT variant decision pending |
| hi     | _pending native review_  | —          | —                  |                                        |
| ar     | _pending native review_  | —          | —                  | RTL layout tested; copy still pending  |
| de     | _pending native review_  | —          | —                  |                                        |
| id     | _pending native review_  | —          | —                  |                                        |

---

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
