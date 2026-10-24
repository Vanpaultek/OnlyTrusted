# Offline Threat Intel bundle (Elite)

Zero-network, bundle-only threat-intel feeds. Every file in this directory is loaded on
app launch and consulted from Elite feature flows (Smart Link Guard, Wangiri auto-block,
and the Offline Threat Intel screen). No request ever leaves the device.

| File | Purpose | Current size | Current source |
| --- | --- | --- | --- |
| `scam_numbers.v1.txt` | Known scam / premium-rate phone numbers (E.164). | ~1 KB (41 placeholder lines) | Hand-curated from public government notices. **See status below.** |
| `phishing_urls.v1.txt` | Phishing hosts (lowercase, no scheme). Loaded as an in-memory set. | ~1.4 MB (50 000 real entries) | Mitchell Krogza Phishing.Database + PhishTank — refreshed via `tools/refresh_threatintel.sh`. |
| `breach_hashes.v1.txt` | SHA-256 prefixes of breached email addresses. | ~1 KB (37 placeholder lines) | HaveIBeenPwned public pwned-emails (not yet wired — see status below). |
| `pegasus_iocs.v1.json` | Pegasus / Candiru / Predator IoCs — filenames, plist keys, dylib SHA-256, process names, domains. | ~1.5 KB | Citizen Lab reports + Amnesty Tech MVT indicator sets. |

## Status (2026-10-24)

**`phishing_urls.v1.txt` — REAL FEED (50 000 entries).**
Regenerated via `tools/refresh_threatintel.sh` from two public,
unauthenticated sources: Mitchell Krogza's Phishing.Database (MIT-licensed,
community-maintained aggregator of ~390k active phishing domains) and
PhishTank's verified-online CSV (free, no API key required). The script
deterministically shuffle-samples 50k of the ~412k unique deduped entries so
the shipped feed isn't alphabetically biased, and the sample is reproducible
(fixed seed) — anyone can re-run the script and verify the bundled file
matches.

**`scam_numbers.v1.txt` — still a 41-entry placeholder.**
There is NO public, free, bulk source for globally-reported scam phone numbers
— the comprehensive feeds (Nomorobo, Hiya, YouMail, Truecaller) are all
commercial / API-key-only. The current list is hand-curated from publicly
documented government notices + the audit-safe +18XXX5551212 test ranges. A
Phase 2 refresher would need either a commercial feed license or a
crowdsourced reporting pipeline inside the app (user reports a scam call →
anonymized hash contributes to the next release's feed).

**`breach_hashes.v1.txt` — still a 37-entry placeholder.**
HIBP's full dataset is ~17 GB uncompressed — too big to ship. The honest
option is to ship the top-N most-breached hashes (first 5 bytes of SHA-1,
HIBP's K-Anonymity format) for ~500 KB APK bloat — not yet wired into the
refresh script.

## Format conventions

Every file is UTF-8. Lines beginning with `#` are comments. Empty lines are ignored.
Numeric values are E.164 (digits after `+`, no spaces). Hashes are lowercase hex.

## Refresh cadence

Daily rebuilds of this bundle will land via normal app updates. The runtime never
reaches out to any server for these feeds — there is no live-fetch path, by design.
