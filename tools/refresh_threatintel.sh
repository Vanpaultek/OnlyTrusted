#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 VanPaulTek Inc
# SPDX-License-Identifier: AGPL-3.0-or-later
#
# OnlyTrusted — Offline Threat Intel refresher.
#
# Downloads up-to-date phishing-domain + phishing-URL feeds from public sources
# (no API key required, zero-cost), normalises + deduplicates them, trims to a
# shippable size, and writes the resulting feeds to BOTH platforms' bundled
# asset paths:
#   • android/app/src/main/assets/threatintel/phishing_urls.v1.txt
#   • ios/OnlyTrusted/Data/phishing_urls.v1.txt
#
# Honest-claim note: this script is a BUILD-TIME tool. The runtime app never
# phones home — the fetched data is baked into the APK/IPA at build. Users can
# audit this script to see exactly what public feeds are included and re-run it
# themselves to verify the bundled data matches.
#
# Usage:
#   bash tools/refresh_threatintel.sh           # fetches + writes feeds
#   DRY_RUN=1 bash tools/refresh_threatintel.sh # fetches but doesn't write
#
# Sources (as of 2026-10-24, all public + unauthenticated):
#   • Mitchell Krogza's Phishing.Database — community-maintained aggregator of
#     phishing domains. GitHub repo, MIT license.
#     https://github.com/mitchellkrogza/Phishing.Database
#   • PhishTank — verified-phishing URL feed (free CSV, no API key tier).
#     https://phishtank.com/developer_info.php
#
# Sources CONSIDERED but not used (and why — avoids silent dependency on access):
#   • URLhaus (abuse.ch) — now requires free Auth-Key as of 2026. Omitted to
#     keep this script runnable in zero-setup CI.
#   • OpenPhish free feed — now behind registration wall as of 2026.
#   • APWG phishing feed — commercial / membership-only.
#
# Scam-number feed: scam_numbers.v1.txt is NOT touched by this script. There is
# no public, free, bulk source for globally-reported scam phone numbers (the
# Hiya / Nomorobo / Truecaller feeds are commercial). Scam numbers remain a
# manually-curated list of publicly-documented government-notice-reported
# scam ranges — see the file header for the current source list.

set -euo pipefail

cd "$(dirname "$0")/.."
WORK=$(mktemp -d)
trap "rm -rf '$WORK'" EXIT
echo "Workdir: $WORK"

# Shippable size cap. Each entry averages ~25 chars = ~25 bytes; 50k entries
# gives ~1.3 MB uncompressed, ~400 KB after APK zip. Adjust if the APK budget
# allows more. Trimming is RANDOM across the sorted dedup set, not top-N —
# phishing domains don't have a reliable public severity score.
MAX_ENTRIES=${MAX_ENTRIES:-50000}

MKP_DOMAINS_URL="https://raw.githubusercontent.com/mitchellkrogza/Phishing.Database/master/phishing-domains-ACTIVE.txt"
PHISHTANK_URL="https://data.phishtank.com/data/online-valid.csv.gz"

echo "→ Fetching Mitchell Krogza phishing-domains feed..."
curl -sfL "$MKP_DOMAINS_URL" --max-time 90 -o "$WORK/mkp_domains.txt" || {
  echo "FATAL: could not fetch MKP domains feed"
  exit 1
}
MKP_LINES=$(wc -l < "$WORK/mkp_domains.txt" | tr -d ' ')
echo "   got $MKP_LINES domains"

echo "→ Fetching PhishTank CSV..."
curl -sfL "$PHISHTANK_URL" --max-time 60 -o "$WORK/phishtank.csv.gz" || {
  echo "WARN: PhishTank fetch failed (not fatal — proceeding without it)"
  touch "$WORK/phishtank.csv.gz"
}
if [[ -s "$WORK/phishtank.csv.gz" ]]; then
  gunzip -c "$WORK/phishtank.csv.gz" | tail -n +2 | awk -F, '{print $2}' > "$WORK/phishtank_urls.txt"
  # Extract host portion from each URL
  python3 - "$WORK/phishtank_urls.txt" > "$WORK/phishtank_hosts.txt" <<'PY'
import sys, urllib.parse
with open(sys.argv[1]) as f:
    for line in f:
        line = line.strip().strip('"')
        if not line: continue
        try:
            host = urllib.parse.urlparse(line).hostname
            if host: print(host.lower())
        except Exception: pass
PY
  PT_LINES=$(wc -l < "$WORK/phishtank_hosts.txt" | tr -d ' ')
  echo "   got $PT_LINES PhishTank hosts"
else
  touch "$WORK/phishtank_hosts.txt"
  echo "   skipped PhishTank"
fi

# Normalise + dedupe + trim.
echo "→ Normalising + deduplicating..."
cat "$WORK/mkp_domains.txt" "$WORK/phishtank_hosts.txt" \
  | tr '[:upper:]' '[:lower:]' \
  | sed -E 's/^www\.//' \
  | sed -E 's/\.$//' \
  | grep -E '^[a-z0-9.-]+\.[a-z]{2,}$' \
  | sort -u > "$WORK/all_domains_sorted.txt"
TOTAL=$(wc -l < "$WORK/all_domains_sorted.txt" | tr -d ' ')
echo "   $TOTAL unique domains after normalisation"

if [[ $TOTAL -gt $MAX_ENTRIES ]]; then
  echo "→ Trimming to $MAX_ENTRIES entries (shuffle-sample)..."
  # Shuffle-sample so trimming isn't alphabetically biased (don't want
  # all "aa..." domains to dominate the shipped feed). Use python for
  # portability — macOS doesn't ship `shuf` by default.
  python3 - "$WORK/all_domains_sorted.txt" "$MAX_ENTRIES" > "$WORK/final.txt" <<'PY'
import random, sys
random.seed(0xBAD5CA3)  # deterministic — same input → same output for reproducible builds
with open(sys.argv[1]) as f:
    lines = [l.strip() for l in f if l.strip()]
n = int(sys.argv[2])
if len(lines) > n:
    lines = random.sample(lines, n)
for l in sorted(lines):
    print(l)
PY
else
  cp "$WORK/all_domains_sorted.txt" "$WORK/final.txt"
fi
FINAL_COUNT=$(wc -l < "$WORK/final.txt" | tr -d ' ')
FINAL_SIZE_KB=$(( $(wc -c < "$WORK/final.txt") / 1024 ))

# Build the output file with a stable header.
STAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)
cat > "$WORK/phishing_urls.v1.txt" <<EOF
# OnlyTrusted — Offline Threat Intel: phishing hosts v1.
#
# Format: one lowercase hostname per line (no scheme, no path, no port). Lines
# starting with '#' are comments and ignored by the loader.
#
# This file is REGENERATED by tools/refresh_threatintel.sh. Do not hand-edit
# individual rows — your changes will be clobbered on the next refresh. To add
# a permanent allow / deny override, edit the loader's static override set in
#   android/.../ui/threatintel/ThreatIntelDatabase.kt
#   ios/OnlyTrusted/Sources/ThreatIntel/ThreatIntelDatabase.swift
#
# Sources merged into this feed (as of regen time below):
#   • Mitchell Krogza phishing-domains-ACTIVE.txt (MIT)
#   • PhishTank verified-online phishing URLs (host portion extracted)
#
# Regenerated: $STAMP
# Total entries: $FINAL_COUNT (random shuffle-sample of $TOTAL unique)
EOF
cat "$WORK/final.txt" >> "$WORK/phishing_urls.v1.txt"

if [[ "${DRY_RUN:-}" == "1" ]]; then
  echo
  echo "DRY_RUN=1 — not writing feeds. Final file at: $WORK/phishing_urls.v1.txt"
  ls -la "$WORK/phishing_urls.v1.txt"
  exit 0
fi

echo "→ Writing feeds to both platform asset paths..."
cp "$WORK/phishing_urls.v1.txt" android/app/src/main/assets/threatintel/phishing_urls.v1.txt
cp "$WORK/phishing_urls.v1.txt" ios/OnlyTrusted/Data/phishing_urls.v1.txt

echo
echo "Done. Shipped feed:"
echo "  Entries:   $FINAL_COUNT  (sampled from $TOTAL unique)"
echo "  File size: ${FINAL_SIZE_KB} KB (+ header)"
echo
echo "Next steps:"
echo "  • git diff android/app/src/main/assets/threatintel/phishing_urls.v1.txt"
echo "  • git diff ios/OnlyTrusted/Data/phishing_urls.v1.txt"
echo "  • commit with a sources-cited message"
