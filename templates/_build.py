#!/usr/bin/env python3
"""Generate the HTML email templates from a shared chrome + per-template
content data. Writes everything to templates/email/.

Each template is standalone — the sender (SES, SendGrid, Postmark, in-app
Mail composer) can plug placeholders without needing a template engine.

Run: python3 templates/_build.py
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT / 'email'

# ---------------------------------------------------------------------------
# Shared chrome: header + legal rail + footer rendered the same across every
# template so the brand reads identically. Only CONTENT varies per email.
# ---------------------------------------------------------------------------

HEAD = '''<!DOCTYPE html>
<!--
  OnlyTrusted email — {name}.
  Trigger: {trigger}
  Required placeholders: {placeholders}
-->
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{title}</title>
</head>
<body style="margin:0; padding:0; background:#0B1F2D; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color:#F4FAFC; -webkit-font-smoothing:antialiased;">
  <!-- Mountain-lake backdrop. JPG for max client compatibility (Gmail / Outlook
       Web / iOS Mail all render JPG; WebP is spotty). If an image blocker strips
       it, the dark gradient still looks correct over a flat #0B1F2D background. -->
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:#0B1F2D; background-image:linear-gradient(180deg, rgba(11,31,45,.72) 0%, rgba(11,31,45,.88) 60%, rgba(11,31,45,.96) 100%), url('https://onlytrusted.app/guide/assets/app_backdrop.jpg'); background-size:cover; background-position:center;">
    <tr><td align="center" style="padding:40px 16px;">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%; background:rgba(30,42,58,0.68); border:1px solid rgba(0,209,193,0.18); border-radius:18px; overflow:hidden;">
        <tr><td style="padding:28px 32px 20px 32px;">
          <!-- Brand lockup: 44×44 app icon + "OnlyTrusted" wordmark on the left,
               zero-cloud eyebrow on the right. Mirrors the .brand selector in
               docs/guide/styles.css so the website and the email share one
               identity. The <img> alt text degrades to text if images are blocked. -->
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0"><tr>
            <td align="left" valign="middle">
              <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                <td valign="middle" style="padding-right:12px;">
                  <img src="https://onlytrusted.app/guide/assets/brand_logo.png"
                       width="44" height="44" alt="OnlyTrusted"
                       style="display:block; width:44px; height:44px; border-radius:10px; box-shadow:0 4px 20px rgba(0,209,193,0.25);">
                </td>
                <td valign="middle" style="font-family:'Manrope', sans-serif; font-weight:800; font-size:22px; letter-spacing:-0.4px; color:#F4FAFC;">
                  <span style="color:#F4FAFC;">Only</span><span style="color:#00D1C1;">Trusted</span>
                </td>
              </tr></table>
            </td>
            <td align="right" valign="middle" style="font-family:'DM Sans', sans-serif; font-size:11px; color:#8FA5B2; letter-spacing:1.5px; text-transform:uppercase;">Zero cloud · Zero account</td>
          </tr></table>
          <div style="height:1px; background:rgba(0,209,193,0.28); margin-top:18px;"></div>
        </td></tr>
'''

CONDITIONS = '''        <tr><td style="padding:0 32px 20px 32px;">
          <div style="background:rgba(11,31,45,0.6); border:1px solid rgba(182,201,210,0.14); border-radius:14px; padding:18px 20px;">
            <div style="font-family:'Manrope', sans-serif; font-weight:800; font-size:11px; letter-spacing:1.5px; text-transform:uppercase; color:#8FA5B2; margin-bottom:10px;">Under all conditions</div>
            <p style="margin:0 0 10px 0; font-family:'DM Sans', sans-serif; font-size:13px; color:#B6C9D2; line-height:1.55;">
              OnlyTrusted stores all app data on your device. There is no cloud backup, no account recovery server, no profile. Losing the device loses the data unless you have exported it. This email is the only remote channel we use, and it is sent only in response to actions you took in the app.
            </p>
            <p style="margin:0; font-family:'DM Sans', sans-serif; font-size:13px; color:#B6C9D2; line-height:1.55;">
              iOS limitations prevent OnlyTrusted from monitoring other apps, your microphone, or your camera. Call blocking and message filtering use Apple's supported extensions. Features labeled Premium, Pro, or Elite require a subscription managed by Apple or Google.
            </p>
          </div>
        </td></tr>

        <tr><td style="padding:0 32px 24px 32px;" align="center">
          <table role="presentation" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td style="padding:4px 6px;"><a href="https://onlytrusted.app/legal/privacy" style="display:inline-block; padding:8px 14px; border:1px solid rgba(182,201,210,0.14); border-radius:50px; background:rgba(30,42,58,0.5); font-family:'DM Sans', sans-serif; font-size:12px; color:#B6C9D2; text-decoration:none;">Privacy Policy</a></td>
              <td style="padding:4px 6px;"><a href="https://onlytrusted.app/legal/terms" style="display:inline-block; padding:8px 14px; border:1px solid rgba(182,201,210,0.14); border-radius:50px; background:rgba(30,42,58,0.5); font-family:'DM Sans', sans-serif; font-size:12px; color:#B6C9D2; text-decoration:none;">Terms of Use</a></td>
              <td style="padding:4px 6px;"><a href="https://onlytrusted.app/legal/honest-claims" style="display:inline-block; padding:8px 14px; border:1px solid rgba(182,201,210,0.14); border-radius:50px; background:rgba(30,42,58,0.5); font-family:'DM Sans', sans-serif; font-size:12px; color:#B6C9D2; text-decoration:none;">Honest Claims</a></td>
            </tr>
            <tr>
              <td style="padding:4px 6px;"><a href="https://onlytrusted.app/guide" style="display:inline-block; padding:8px 14px; border:1px solid rgba(0,209,193,0.28); border-radius:50px; background:rgba(0,209,193,0.08); font-family:'DM Sans', sans-serif; font-size:12px; color:#4FE3DF; text-decoration:none; font-weight:600;">User Guide</a></td>
              <td style="padding:4px 6px;"><a href="https://apps.apple.com/app/onlytrusted?action=write-review" style="display:inline-block; padding:8px 14px; border:1px solid rgba(0,209,193,0.28); border-radius:50px; background:rgba(0,209,193,0.08); font-family:'DM Sans', sans-serif; font-size:12px; color:#4FE3DF; text-decoration:none; font-weight:600;">★ Rate on App Store</a></td>
              <td style="padding:4px 6px;"><a href="https://play.google.com/store/apps/details?id=com.onlytrusted.app&showAllReviews=true" style="display:inline-block; padding:8px 14px; border:1px solid rgba(0,209,193,0.28); border-radius:50px; background:rgba(0,209,193,0.08); font-family:'DM Sans', sans-serif; font-size:12px; color:#4FE3DF; text-decoration:none; font-weight:600;">★ Rate on Play Store</a></td>
            </tr>
          </table>
        </td></tr>
'''

FOOT_TEMPLATE = '''        <tr><td style="padding:20px 32px 28px 32px; background:rgba(11,31,45,0.75); border-top:1px solid rgba(182,201,210,0.08);">
          <p style="margin:0 0 8px 0; font-family:'DM Sans', sans-serif; font-size:12px; color:#8FA5B2; line-height:1.5;">
            {footer_reason}
          </p>
          <p style="margin:0 0 8px 0; font-family:'DM Sans', sans-serif; font-size:12px; color:#8FA5B2; line-height:1.5;">
            <a href="https://onlytrusted.app/unsubscribe?token={{{{unsubscribe_token}}}}" style="color:#8FA5B2; text-decoration:underline;">Unsubscribe from {unsubscribe_category}</a> ·
            <a href="mailto:admin@vanpaultek.com" style="color:#8FA5B2; text-decoration:underline;">Contact support</a>
          </p>
          <p style="margin:0; font-family:'DM Sans', sans-serif; font-size:11px; color:#6D808B; line-height:1.5;">
            VanPaulTek Inc · admin@vanpaultek.com<br>
            © 2026 VanPaulTek Inc. OnlyTrusted is a VanPaulTek product. Licensed under AGPL-3.0 — <a href="https://github.com/Vanpaultek/OnlyTrusted" style="color:#6D808B; text-decoration:underline;">source on GitHub</a>.
          </p>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>
'''


def content_block(eyebrow: str, heading: str, body_html: str,
                  cta_label: str, cta_url: str, note: str = '') -> str:
    """Render the per-template content card."""
    note_html = (
        f'<p style="margin:16px 0 0 0; font-size:13px; color:#8FA5B2;">{note}</p>'
        if note else ''
    )
    cta_html = (
        f'''<table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:24px 0 8px 0;"><tr>
            <td style="background:#00D1C1; border-radius:50px;">
              <a href="{cta_url}" style="display:inline-block; padding:14px 28px; font-family:'Manrope', sans-serif; font-weight:700; font-size:15px; color:#0B1F2D; text-decoration:none; letter-spacing:-0.2px;">
                {cta_label}
              </a>
            </td>
          </tr></table>'''
        if cta_label else ''
    )
    return f'''        <tr><td style="padding:8px 32px 32px 32px; font-family:'DM Sans', sans-serif; color:#B6C9D2; font-size:15px; line-height:1.6;">
          <div style="font-size:11px; letter-spacing:2px; font-weight:800; color:#00D1C1; text-transform:uppercase; margin-bottom:10px;">{eyebrow}</div>
          <h1 style="font-family:'Manrope', sans-serif; font-weight:800; font-size:30px; letter-spacing:-1px; color:#F4FAFC; margin:0 0 18px 0; line-height:1.2;">{heading}</h1>
          {body_html}
          {cta_html}
          {note_html}
        </td></tr>
'''


# ---------------------------------------------------------------------------
# Per-template content. Keep each tight — the content block is the only part
# that varies across emails.
# ---------------------------------------------------------------------------

TEMPLATES = {
    'welcome': {
        'trigger': 'Sent once after a user creates an account.',
        'placeholders': '{{owner_name}} {{unsubscribe_token}}',
        'title': 'Welcome to OnlyTrusted',
        'eyebrow': 'Welcome aboard',
        'heading': 'Welcome to OnlyTrusted, {{owner_name}}.',
        'body': '''<p style="margin:0 0 16px 0;">Your account is set up. The next 60 seconds inside the app set the three things that make it work: your Trusted Contacts list, your PIN, and your Passkey.</p>
<p style="margin:0 0 16px 0;">Everything stays on your device. We do not operate a backend that holds your contacts, calls, messages, or your PIN. This email is the only thing that lives on a server, and it exists because you created an account — not because we're keeping a profile.</p>''',
        'cta_label': 'Read the First-time setup guide',
        'cta_url': 'https://onlytrusted.app/guide#first-time-setup',
        'note': 'On the Free tier forever. Premium, Pro, and Elite add features — the full grid is on the Pricing Chart link below.',
        'footer_reason': 'You received this email because you created an OnlyTrusted account on this address.',
        'unsubscribe_category': 'account emails',
    },
    'subscription-confirmation': {
        'trigger': 'A user subscribes or upgrades via in-app purchase.',
        'placeholders': '{{owner_name}} {{plan_name}} {{billing_cadence}} {{price_label}} {{next_billing_date}} {{unsubscribe_token}}',
        'title': "You're on OnlyTrusted {{plan_name}}",
        'eyebrow': 'Subscription active',
        'heading': "You're on OnlyTrusted {{plan_name}}, {{owner_name}}.",
        'body': '''<p style="margin:0 0 16px 0;">Thanks for the upgrade. Every {{plan_name}} feature is unlocked immediately on this device, with no re-download required.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:18px 0 0 0; background:rgba(11,31,45,0.6); border:1px solid rgba(0,209,193,0.18); border-radius:14px;">
  <tr><td style="padding:14px 20px; border-bottom:1px solid rgba(182,201,210,0.08);"><div style="font-size:11px; color:#8FA5B2; text-transform:uppercase; letter-spacing:1.2px; margin-bottom:4px;">Plan</div><div style="font-family:'Manrope', sans-serif; font-weight:700; font-size:17px; color:#F4FAFC;">OnlyTrusted {{plan_name}}</div></td></tr>
  <tr><td style="padding:14px 20px; border-bottom:1px solid rgba(182,201,210,0.08);"><div style="font-size:11px; color:#8FA5B2; text-transform:uppercase; letter-spacing:1.2px; margin-bottom:4px;">Billing</div><div style="font-family:'Manrope', sans-serif; font-weight:700; font-size:17px; color:#F4FAFC;">{{price_label}} / {{billing_cadence}}</div></td></tr>
  <tr><td style="padding:14px 20px;"><div style="font-size:11px; color:#8FA5B2; text-transform:uppercase; letter-spacing:1.2px; margin-bottom:4px;">Next renewal</div><div style="font-family:'Manrope', sans-serif; font-weight:700; font-size:17px; color:#F4FAFC;">{{next_billing_date}}</div></td></tr>
</table>''',
        'cta_label': "See what's new in {{plan_name}}",
        'cta_url': 'https://onlytrusted.app/guide#pricing',
        'note': "Billing is handled by Apple (App Store) or Google (Play Store). Manage, pause, or cancel in your platform's subscription settings — not inside OnlyTrusted.",
        'footer_reason': "You received this email because your OnlyTrusted purchase succeeded on this address.",
        'unsubscribe_category': 'billing emails',
    },
    'subscription-renewal-reminder': {
        'trigger': 'Sent 7 days before auto-renewal of a yearly subscription (optional for monthly).',
        'placeholders': '{{owner_name}} {{plan_name}} {{price_label}} {{renewal_date}} {{card_last4}} {{unsubscribe_token}}',
        'title': 'Your OnlyTrusted {{plan_name}} renews on {{renewal_date}}',
        'eyebrow': 'Upcoming renewal',
        'heading': 'Your {{plan_name}} renews on {{renewal_date}}.',
        'body': '''<p style="margin:0 0 16px 0;">Hi {{owner_name}} — a quick heads-up so you can decide. Your OnlyTrusted {{plan_name}} subscription renews in about 7 days.</p>
<ul style="margin:0 0 16px 0; padding-left:20px; color:#B6C9D2;">
  <li style="margin:4px 0;">Amount: <strong style="color:#F4FAFC;">{{price_label}}</strong></li>
  <li style="margin:4px 0;">Date: <strong style="color:#F4FAFC;">{{renewal_date}}</strong></li>
  <li style="margin:4px 0;">Charged to: payment method ending {{card_last4}}</li>
</ul>
<p style="margin:0 0 16px 0;">Nothing to do if you want to keep it active. To stop the renewal, open your platform's subscription settings before the renewal date.</p>''',
        'cta_label': 'Manage subscription',
        'cta_url': 'https://support.apple.com/en-us/HT202039',
        'note': 'This reminder is a one-off per billing cycle. We never add anyone to a marketing cadence.',
        'footer_reason': 'You received this email because you have an active OnlyTrusted subscription on this address.',
        'unsubscribe_category': 'renewal reminders',
    },
    'subscription-cancelled': {
        'trigger': 'A user cancels their subscription (or the platform reports cancellation).',
        'placeholders': '{{owner_name}} {{plan_name}} {{access_until_date}} {{unsubscribe_token}}',
        'title': 'Your OnlyTrusted subscription is cancelled',
        'eyebrow': 'Cancellation confirmed',
        'heading': 'Your OnlyTrusted {{plan_name}} is cancelled.',
        'body': '''<p style="margin:0 0 16px 0;">Hi {{owner_name}} — your subscription is cancelled. You keep access to every {{plan_name}} feature until <strong style="color:#F4FAFC;">{{access_until_date}}</strong>; after that the app continues to work on the Free tier, forever.</p>
<p style="margin:0 0 16px 0;">All data on your device stays exactly where it is — Trusted Contacts, Blocked list, Events log, Settings. We don't erase anything. Nothing on our end to delete either, because we don't store anything on our end.</p>
<p style="margin:0 0 16px 0;">If this was a mistake, re-subscribe anytime from the Upgrade screen in the app.</p>''',
        'cta_label': 'Compare Free vs paid tiers',
        'cta_url': 'https://onlytrusted.app/pricing',
        'note': 'If you cancelled because something broke, please reply to this email — we read every one.',
        'footer_reason': "You received this email because your OnlyTrusted subscription on this address was cancelled.",
        'unsubscribe_category': 'billing emails',
    },
    'account-deleted': {
        'trigger': 'User deletes their account from Settings → Account → Delete.',
        'placeholders': '{{owner_name}} {{deletion_date}} {{unsubscribe_token}}',
        'title': 'Your OnlyTrusted account is deleted',
        'eyebrow': 'Account deletion confirmed',
        'heading': 'Your OnlyTrusted account is deleted.',
        'body': '''<p style="margin:0 0 16px 0;">Hi {{owner_name}} — your account record (email + any email-tied preferences) was deleted on {{deletion_date}}. We retained nothing.</p>
<p style="margin:0 0 16px 0;">The app itself is still on your device. All on-device data — Trusted Contacts, Blocked list, Events log, PIN, Passkey, exports — remains intact and under your control. Delete the app from your device if you want that gone too.</p>
<p style="margin:0 0 16px 0;">Under GDPR / CCPA, we treat this as a complete erasure of personal data we controlled. Since we never collected much, there wasn't much to erase.</p>''',
        'cta_label': '',
        'cta_url': '',
        'note': "If you did not request this deletion, reply to this email within 7 days and we'll help you investigate.",
        'footer_reason': 'You received this email because an OnlyTrusted account on this address was deleted.',
        'unsubscribe_category': 'account emails',
    },
    'weekly-digest': {
        'trigger': 'Weekly, every Monday — Premium feature. User opts in from Settings → Weekly Digest.',
        'placeholders': '{{owner_name}} {{week_range}} {{blocked_calls_count}} {{filtered_sms_count}} {{alerts_count}} {{unsubscribe_token}}',
        'title': 'Your OnlyTrusted protection week',
        'eyebrow': 'Weekly digest',
        'heading': 'Your protection week, {{week_range}}.',
        'body': '''<p style="margin:0 0 16px 0;">Hi {{owner_name}} — a one-paragraph summary of what OnlyTrusted did on your device this week. All counts come from your local Events log; nothing was uploaded.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:18px 0 0 0;">
  <tr>
    <td align="center" style="padding:14px 10px; background:rgba(11,31,45,0.6); border:1px solid rgba(0,209,193,0.18); border-radius:14px; width:33%;">
      <div style="font-family:'Manrope', sans-serif; font-weight:800; font-size:28px; color:#00D1C1; line-height:1;">{{blocked_calls_count}}</div>
      <div style="font-size:11px; color:#8FA5B2; text-transform:uppercase; letter-spacing:1.2px; margin-top:6px;">Calls blocked</div>
    </td>
    <td width="12"></td>
    <td align="center" style="padding:14px 10px; background:rgba(11,31,45,0.6); border:1px solid rgba(0,209,193,0.18); border-radius:14px; width:33%;">
      <div style="font-family:'Manrope', sans-serif; font-weight:800; font-size:28px; color:#00D1C1; line-height:1;">{{filtered_sms_count}}</div>
      <div style="font-size:11px; color:#8FA5B2; text-transform:uppercase; letter-spacing:1.2px; margin-top:6px;">SMS filtered</div>
    </td>
    <td width="12"></td>
    <td align="center" style="padding:14px 10px; background:rgba(11,31,45,0.6); border:1px solid rgba(0,209,193,0.18); border-radius:14px; width:33%;">
      <div style="font-family:'Manrope', sans-serif; font-weight:800; font-size:28px; color:#F59E0B; line-height:1;">{{alerts_count}}</div>
      <div style="font-size:11px; color:#8FA5B2; text-transform:uppercase; letter-spacing:1.2px; margin-top:6px;">Mic / cam alerts</div>
    </td>
  </tr>
</table>
<p style="margin:18px 0 0 0;">Open the app for the full breakdown by category, time, and sender.</p>''',
        'cta_label': 'Open the Events log',
        'cta_url': 'https://onlytrusted.app/guide#events-log',
        'note': 'This digest is counts only — never message contents, never caller IDs. Everything stays private.',
        'footer_reason': 'You received this email because you enabled the Weekly Digest in Settings.',
        'unsubscribe_category': 'the Weekly Digest',
    },
    'export-ready': {
        'trigger': 'User requested a data export (JSON, CSV, or evidence pack) and processing completed.',
        'placeholders': '{{owner_name}} {{export_type}} {{export_size}} {{download_link}} {{expiry_date}} {{unsubscribe_token}}',
        'title': 'Your OnlyTrusted data export is ready',
        'eyebrow': 'Export ready',
        'heading': 'Your {{export_type}} export is ready.',
        'body': '''<p style="margin:0 0 16px 0;">Hi {{owner_name}} — the {{export_type}} export you asked for from Settings → Export is ready. The file is {{export_size}} and can be downloaded until {{expiry_date}}.</p>
<p style="margin:0 0 16px 0;">The export was generated on your device and transferred directly from device to your chosen destination (iCloud Drive, email, Share Sheet). We never saw its contents.</p>''',
        'cta_label': 'Download export',
        'cta_url': '{{download_link}}',
        'note': 'The link expires in 7 days. After that, re-generate it from the app — the raw data on your device is unchanged.',
        'footer_reason': 'You received this email because you triggered a data export from the OnlyTrusted app.',
        'unsubscribe_category': 'export notifications',
    },
    'support-reply': {
        'trigger': 'Template for the support team to use when replying to admin@vanpaultek.com inbox.',
        'placeholders': '{{owner_name}} {{thread_subject}} {{support_reply_body}} {{support_agent_name}}',
        'title': 'Re: {{thread_subject}}',
        'eyebrow': 'Support reply',
        'heading': 'Re: {{thread_subject}}',
        'body': '''<p style="margin:0 0 16px 0;">Hi {{owner_name}},</p>
<div style="margin:0 0 16px 0;">{{support_reply_body}}</div>
<p style="margin:0 0 16px 0;">If this doesn't fully answer the question, just reply — the thread is live and I'll follow up.</p>
<p style="margin:0 0 16px 0;">— {{support_agent_name}}<br>OnlyTrusted support · VanPaulTek Inc</p>''',
        'cta_label': 'Browse the User Guide',
        'cta_url': 'https://onlytrusted.app/guide',
        'note': 'Replies to this address land in our support inbox and go to a human.',
        'footer_reason': 'You received this email because you contacted OnlyTrusted support.',
        'unsubscribe_category': 'support emails',
    },
    'legal-update': {
        'trigger': 'Privacy Policy, Terms, or Honest Claims updated in a user-visible way.',
        'placeholders': '{{owner_name}} {{updated_document}} {{effective_date}} {{summary_of_changes}} {{unsubscribe_token}}',
        'title': 'We updated our {{updated_document}}',
        'eyebrow': 'Legal update',
        'heading': 'We updated our {{updated_document}}.',
        'body': '''<p style="margin:0 0 16px 0;">Hi {{owner_name}} — we updated our {{updated_document}}. The new version takes effect on <strong style="color:#F4FAFC;">{{effective_date}}</strong>.</p>
<div style="margin:0 0 16px 0; padding:14px 18px; background:rgba(11,31,45,0.6); border:1px solid rgba(182,201,210,0.14); border-radius:14px;">
  <div style="font-family:'Manrope', sans-serif; font-weight:700; font-size:13px; color:#F4FAFC; margin-bottom:8px;">What changed</div>
  <div style="font-size:14px; color:#B6C9D2; line-height:1.55;">{{summary_of_changes}}</div>
</div>
<p style="margin:0 0 16px 0;">Nothing changed about where your data lives — it is still on your device, we still do not operate a backend that stores it. If you want to keep using OnlyTrusted after {{effective_date}}, you accept the updated {{updated_document}} by continuing to use the app.</p>''',
        'cta_label': 'Read the full {{updated_document}}',
        'cta_url': 'https://onlytrusted.app/legal/',
        'note': 'Prior versions remain available at onlytrusted.app/legal/history for comparison.',
        'footer_reason': 'You received this email because you have an active OnlyTrusted account on this address.',
        'unsubscribe_category': 'legal update notices',
    },
    'pin-reset': {
        'trigger': 'User triggered a PIN reset flow (requires device presence + Passkey).',
        'placeholders': '{{owner_name}} {{reset_time}} {{device_name}} {{unsubscribe_token}}',
        'title': 'Your OnlyTrusted PIN was reset',
        'eyebrow': 'Security event',
        'heading': 'Your PIN was reset.',
        'body': '''<p style="margin:0 0 16px 0;">Hi {{owner_name}} — your OnlyTrusted PIN was reset on {{reset_time}} from <strong style="color:#F4FAFC;">{{device_name}}</strong>.</p>
<p style="margin:0 0 16px 0;">The reset was authorized via Passkey biometric on the device itself. We cannot reset a PIN remotely — if you did not initiate this from your own device, your device is likely compromised and you should treat this as a security incident.</p>
<p style="margin:0 0 16px 0;">What to do now:</p>
<ul style="margin:0 0 16px 0; padding-left:20px; color:#B6C9D2;">
  <li style="margin:4px 0;">Open OnlyTrusted and sign in with the new PIN.</li>
  <li style="margin:4px 0;">If you did not reset the PIN: lock down the device immediately (iCloud Find My / Google Find Hub).</li>
  <li style="margin:4px 0;">Audit your Events log for anything unexpected after {{reset_time}}.</li>
</ul>''',
        'cta_label': 'Review the Events log',
        'cta_url': 'https://onlytrusted.app/guide#events-log',
        'note': "If this reset wasn't you, reply to this email and we'll help you investigate.",
        'footer_reason': 'You received this email because an OnlyTrusted account on this address had its PIN reset.',
        'unsubscribe_category': 'security alerts (we recommend keeping these on)',
    },
}


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, data in TEMPLATES.items():
        head = HEAD.format(name=name, trigger=data['trigger'],
                           placeholders=data['placeholders'], title=data['title'])
        content = content_block(
            eyebrow=data['eyebrow'],
            heading=data['heading'],
            body_html=data['body'],
            cta_label=data['cta_label'],
            cta_url=data['cta_url'],
            note=data['note'],
        )
        foot = FOOT_TEMPLATE.format(
            footer_reason=data['footer_reason'],
            unsubscribe_category=data['unsubscribe_category'],
        )
        html = head + content + CONDITIONS + foot
        (OUT / f'{name}.html').write_text(html, encoding='utf-8')
        print(f'  wrote email/{name}.html ({len(html)} bytes)')


if __name__ == '__main__':
    build()
