# OnlyTrusted — Launcher Icon Design Brief

**Audience:** a professional icon designer hired to replace the current internal-testing icon with a polished shipping asset.

## Current state (what we have)

Shield-only crop from the "Neon Mountain Shield" composition at `/Users/vinreddy/Downloads/OnlyTrusted Neon Mountain Shield.png`, centred inside Android's 108×108 dp adaptive canvas with asymmetric insets (top 18 dp, bottom 6 dp, sides 12 dp) that shift the shield slightly down. Dark midnight (`#06131D`) adaptive background. Monochrome layer added for Android 13+ Themed Icons.

Extracted PNGs for visual review: run `./gradlew extractLauncherIcons` and open `app/build/reports/launcher-icons/`.

**Why it works for internal testing but not launch:**
- Shield reads clearly at launcher sizes
- No clipping on circular / squircle masks
- Monochrome theme-icon support

**Why it's not launch-ready:**
- Hand-cropped from a larger composition, not purpose-designed for the icon canvas
- No pixel-level grid alignment (edges look slightly fuzzy at low densities)
- Checkmark proportion is OK but not iconic — reads as "yet another shield-check" instead of distinctive
- "OnlyTrusted" wordmark not in the icon (clipped by circular masks in previous iteration); competitors recognise brands via logo-mark alone and we don't yet have a strong, trademark-worthy mark

---

## What the icon needs to communicate

Primary meaning (in priority order):
1. **Trust / protection** — this is a safety app
2. **Privacy / discretion** — not loud or alarmist
3. **Local / on-device** — not cloud-connected, not corporate
4. **Modern / mobile-native** — doesn't look like a 2015 icon

Secondary associations we're OK with:
- Shield metaphor (used across the category — Signal, 1Password, Authy)
- Check-mark (approval, verified)
- Cyan / teal accent (brand colour, see below)

Associations to **avoid**:
- Locks (overused, implies "walled garden" rather than privacy)
- Eyes (surveillance connotation — opposite of our positioning)
- Human silhouettes (we are not a chat app)
- Government / law-enforcement iconography (badges, stars)
- Red / orange / yellow (alarm colours, we are calming)

---

## Brand palette

```
Primary Cyan   #00D1FF   (BrandCyanDark in theme)
Teal Accent    #1BA6B8   (BrandAccentDark)
Midnight BG    #06131D   (MidnightBackground)
Card BG        #0F2030   (MidnightCard)
Elevated BG    #182B3D   (MidnightElevated)
Primary Text   #E8F4FB   (PrimaryTextDark)
Secondary Text #9BB4C7   (SecondaryTextDark)
Tertiary Text  #6B839A   (TertiaryTextDark)
Danger         #E55353   (DangerDark)
Warning        #F59E0B   (WarningDark)
```

Icon should feel at home against the midnight background used throughout the app.

---

## Technical constraints

### Adaptive icon (Android 8+)
- Canvas: **108 × 108 dp** total
- Safe zone (inner guaranteed-visible area): **66 × 66 dp** circle, centred
- Outer ~22 dp per side may be clipped by circular / squircle / teardrop / rounded-square masks
- Two layers: `foreground` + `background`; both rendered edge-to-edge; mask applied over both
- Monochrome layer (Android 13+ themed icons): a flat silhouette, fully opaque where the shape exists, transparent elsewhere; launcher tints it dynamically

### Static mipmap PNGs (fallback for pre-API 26 and some surfaces)
- mdpi: 48 × 48 px
- hdpi: 72 × 72 px
- xhdpi: 96 × 96 px
- xxhdpi: 144 × 144 px
- xxxhdpi: 192 × 192 px
- (Round variants: same sizes, pre-circled)

### Common pitfalls (learned the hard way)
1. Any artwork outside the 66 dp safe zone WILL be clipped on Pixel and OnePlus launchers
2. Wordmark text in the icon gets clipped by circular masks every single time
3. `android:gravity="center"` on a bitmap layer ignores `<inset>` wrappers and draws at native size — see `ic_launcher_foreground_bitmap.xml` for the fix
4. Launchers cache icons aggressively; test by uninstall + reinstall + `pm clear com.google.android.apps.nexuslauncher`
5. Icon packs may skin the icon regardless of our design — the monochrome layer gives us some control back

---

## Deliverables wanted

1. **Vector source** (SVG, Figma, or Illustrator) — authoritative artwork, exports to any density
2. **Adaptive icon layers**:
   - `ic_launcher_foreground.xml` or 432 × 432 PNG for the foreground
   - `ic_launcher_background.xml` or 432 × 432 PNG for the background
   - `ic_launcher_monochrome.xml` — flat silhouette for Themed Icons
3. **Static mipmap PNGs** at all five densities (mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi), both regular + round
4. **Store listing assets**:
   - 512 × 512 PNG for Google Play (hi-res icon)
   - 1024 × 1024 PNG for Apple App Store (iOS marketing icon)
5. **Marketing / splash**:
   - 1024 × 1024 PNG with wordmark baked in (for splash, email templates, landing page)
   - Horizontal lockup (icon + "OnlyTrusted" wordmark side by side) for horizontal headers

---

## Reference icons we admire (for mood, not plagiarism)

- **Signal** — clean, confident, instantly recognisable as a privacy app
- **ProtonMail** — teal accent, geometric
- **1Password** — authoritative without being aggressive
- **Bitwarden** — simple shield that doesn't look dated

## Reference icons we do NOT want to look like

- Avast / Norton / Kaspersky — loud, alarmist, enterprise-security vibe
- Random utility apps with generic shields from icon packs
