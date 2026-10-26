# OnlyTrusted — Email & Message Templates

All user-facing email and SMS/WhatsApp content the app can send, in one
place, matching the user guide's visual language (dark + mountain lake +
teal + Manrope / DM Sans).

Every template includes the same two anchors:

1. **"Under all conditions"** rail — a tightly-worded reminder that OnlyTrusted
   stores app data on-device, operates no backend, and that iOS limitations
   apply regardless of plan.
2. **User Guide + ★ Rate on App Store + ★ Rate on Play Store** pills in the
   footer so every email surfaces the written guide and the review flow.

Updates to the shared chrome (header, "Under all conditions" rail, link rail,
footer) should happen in `_build.py` so all 10 email templates regenerate
consistently. SMS templates are plain text — edit in place.

---

## Structure

```
templates/
├── README.md                 (this file)
├── _build.py                 (regenerates all 10 email templates — run after any chrome change)
├── _shared/
│   └── base.html             (reference skeleton — read this first to understand the design)
├── email/                    (10 HTML templates — ready to send via SES/SendGrid/Postmark)
│   ├── welcome.html
│   ├── subscription-confirmation.html
│   ├── subscription-renewal-reminder.html
│   ├── subscription-cancelled.html
│   ├── account-deleted.html
│   ├── weekly-digest.html
│   ├── export-ready.html
│   ├── support-reply.html
│   ├── legal-update.html
│   └── pin-reset.html
└── message/                  (6 plain-text SMS / WhatsApp templates)
    ├── sos-alert.txt
    ├── sos-whatsapp.txt
    ├── trusted-contact-invite.txt
    ├── duress-alert.txt
    ├── carrier-spam-report.txt
    └── pin-reset-code.txt
```

---

## Email templates — triggers + required placeholders

| File | Trigger | Placeholders |
|------|---------|--------------|
| `welcome.html` | Account created | `{{owner_name}}` `{{unsubscribe_token}}` |
| `subscription-confirmation.html` | In-app purchase succeeded | `{{owner_name}}` `{{plan_name}}` `{{billing_cadence}}` `{{price_label}}` `{{next_billing_date}}` `{{unsubscribe_token}}` |
| `subscription-renewal-reminder.html` | 7 days before yearly auto-renewal | `{{owner_name}}` `{{plan_name}}` `{{price_label}}` `{{renewal_date}}` `{{card_last4}}` `{{unsubscribe_token}}` |
| `subscription-cancelled.html` | User or platform cancels | `{{owner_name}}` `{{plan_name}}` `{{access_until_date}}` `{{unsubscribe_token}}` |
| `account-deleted.html` | User deletes account | `{{owner_name}}` `{{deletion_date}}` `{{unsubscribe_token}}` |
| `weekly-digest.html` | Weekly, every Monday (Premium opt-in) | `{{owner_name}}` `{{week_range}}` `{{blocked_calls_count}}` `{{filtered_sms_count}}` `{{alerts_count}}` `{{unsubscribe_token}}` |
| `export-ready.html` | Export completed | `{{owner_name}}` `{{export_type}}` `{{export_size}}` `{{download_link}}` `{{expiry_date}}` `{{unsubscribe_token}}` |
| `support-reply.html` | Support agent reply (template) | `{{owner_name}}` `{{thread_subject}}` `{{support_reply_body}}` `{{support_agent_name}}` |
| `legal-update.html` | Privacy / Terms / Honest Claims updated | `{{owner_name}}` `{{updated_document}}` `{{effective_date}}` `{{summary_of_changes}}` `{{unsubscribe_token}}` |
| `pin-reset.html` | User reset their PIN | `{{owner_name}}` `{{reset_time}}` `{{device_name}}` `{{unsubscribe_token}}` |

### Design tokens (match `docs/guide/styles.css`)

| Role | Value |
|------|-------|
| Background | `#0B1F2D` |
| Elevated card | `rgba(30,42,58,0.68)` |
| Accent | `#00D1C1` |
| Accent (bright) | `#4FE3DF` |
| Primary text | `#F4FAFC` |
| Muted text | `#B6C9D2` |
| Quiet text | `#8FA5B2` |
| Heading font | Manrope 800 |
| Body font | DM Sans |
| CTA button | 50px radius capsule, accent fill, dark text |
| Pills | 50px radius, 1px muted border, translucent dark fill |
| Backdrop image | `https://onlytrusted.app/email-backdrop.jpg` (host alongside the website) |

### Rendering the backdrop image

The dark gradient is applied via `background-image: linear-gradient(...), url(...)`.
Gmail and most clients will show the gradient even if they strip the remote
`url(...)` — the layout still reads correctly as a flat dark card. The lakescape
backdrop is a progressive enhancement, not load-bearing.

---

## Message templates — triggers + placeholders

| File | Trigger | Placeholders |
|------|---------|--------------|
| `sos-alert.txt` | SOS pressed, countdown completes → SMS to Trusted Contacts | `{{owner_name}}` `{{timestamp}}` `{{approx_location_url}}` |
| `sos-whatsapp.txt` | SOS pressed → WhatsApp Share Intent variant (richer) | `{{owner_name}}` `{{timestamp}}` `{{approx_location_url}}` `{{maps_coords}}` |
| `trusted-contact-invite.txt` | Owner adds a Trusted Contact and asks the app to notify them | `{{owner_name}}` `{{relation}}` `{{app_store_link}}` |
| `duress-alert.txt` | Duress PIN entered (covert channel) | `{{owner_name}}` `{{timestamp}}` |
| `carrier-spam-report.txt` | User reports a number from the Blocked tab | `{{spam_phone_number}}` `{{report_timestamp}}` |
| `pin-reset-code.txt` | In-app verification before PIN reset Passkey prompt | `{{reset_code}}` `{{expires_in_minutes}}` |

SMS templates intentionally skip the User Guide / Rate links in the bodies
(SMS budget is tight, and SOS / duress are safety-critical moments — not a
place for marketing). Where appropriate they include an unobtrusive
`onlytrusted.app/guide#<anchor>` link for recipients who need context.

---

## Regenerating

Run the generator after any change to the shared chrome (header / "Under all
conditions" / legal link rail / footer):

```bash
python3 templates/_build.py
```

This rewrites every file in `email/`. Content changes per template live inside
the `TEMPLATES` dict in `_build.py` — edit there, not in the generated HTML.

Message templates (`message/*.txt`) are plain text and are edited directly;
there is no generator for them.

---

## Where links go

| Pill | URL |
|------|-----|
| Privacy Policy | `https://onlytrusted.app/legal/privacy` |
| Terms of Use | `https://onlytrusted.app/legal/terms` |
| Honest Claims | `https://onlytrusted.app/legal/honest-claims` |
| User Guide | `https://onlytrusted.app/guide` |
| ★ Rate on App Store | `https://apps.apple.com/app/onlytrusted?action=write-review` |
| ★ Rate on Play Store | `https://play.google.com/store/apps/details?id=com.onlytrusted.app&showAllReviews=true` |
| Unsubscribe | `https://onlytrusted.app/unsubscribe?token={{unsubscribe_token}}` |
| Source on GitHub | `https://github.com/Vanpaultek/OnlyTrusted` |

All six resource URLs on the `onlytrusted.app` domain are placeholders — update
them to the production hosts when the website ships. The App Store / Play Store
review URLs follow the official write-review deep-link format and work as-is
once the apps are live under those IDs.

---

Maintained by: VanPaulTek Inc · admin@vanpaultek.com
