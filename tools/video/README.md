# OnlyTrusted — Real-UI Video Walkthrough Kit

Produces a branded multi-chapter walkthrough MP4 using **real screenshots**
from the live OnlyTrusted app — never AI-generated or hand-drawn mockups.
Nothing goes to any cloud; the whole pipeline runs on your machine.

**Current build**: 13 chapters, ~55 seconds, 9 languages (en, es, fr, pt,
hi, ar, de, id, te) + WebVTT caption tracks for each.

**Last updated**: 2026-10-25 · covers release `v0.3.6+`

---

## What's in this folder

```
tools/video/
├── build_video.py        # Python generator — renders MP4 + VTT per language
├── capture_screens.sh    # Interactive capture helper for iOS Simulator
├── README.md             # (this file)
├── assets/               # Real app screenshots (PNG, one per chapter)
└── output/               # Generated MP4s + VTT caption tracks
```

The whole pipeline is reproducible: given the screenshots in `assets/`,
`build_video.py` deterministically produces identical MP4s on any machine
with the same Python + ffmpeg + fonts.

---

## Quick start

```bash
cd tools/video

# 1. One-time setup
pip install Pillow                      # anywhere Python 3.9+ is
brew install ffmpeg                     # macOS; apt install ffmpeg on Linux

# 2. Capture real app screens (interactive — see capture_screens.sh)
./capture_screens.sh

# 3. Produce the English video
python3 build_video.py --lang en --video

# 4. Produce all 9 languages
for lang in en es fr pt hi ar de id te; do
    python3 build_video.py --lang $lang --video
done
```

Outputs land at `output/OnlyTrusted_Real_UI_<lang>.mp4` + matching
`OnlyTrusted_<lang>.vtt` caption tracks.

---

## Chapter list

Mirrors the first ~half of `docs/USER_GUIDE_VIDEO_SCRIPT.md`. For the full
28-minute narrated tutorial, extend `SCENES` in `build_video.py` + add
per-chapter captures to `assets/`.

| # | Scene key | Duration | Caption summary |
|---|---|---|---|
| 1 | welcome | 4.0 s | OnlyTrusted — Zero cloud. Zero account. Zero compromise. |
| 2 | setup | 4.0 s | First-time setup — Email → PIN → Passkey on-device. |
| 3 | consent | 3.0 s | Honest consent — you confirm what we can / can't do. |
| 4 | dashboard | 5.0 s | Your dashboard — status + counts + quick actions. |
| 5 | protection | 4.0 s | Protection is active — only trusted contacts reach you. |
| 6 | contacts | 4.0 s | Trusted Contacts — the people who matter get through. |
| 7 | blocked | 4.0 s | Blocked numbers — one tap, logged on-device. |
| 8 | log | 3.5 s | Events log — every block + alert, auditable, local. |
| 9 | sos | 4.5 s | Distress Signal — 3-sec countdown → SMS + call + WhatsApp. |
| 10 | smartlink | 4.0 s | Smart Link Guard — 12 rules catch phishing before you tap. |
| 11 | language | 4.5 s | 9 languages shipped including Telugu (Telugu demo frame). |
| 12 | honest | 4.0 s | Honest Limits — we tell you what we can't do, in-app. |
| 13 | end | 3.5 s | OnlyTrusted — install today, Free tier forever yours. |

Total: ~52 seconds. Durations tuned so the full video fits inside the
60-second limit on TikTok / Instagram Reels / YouTube Shorts.

---

## How assets/ works

For each scene the generator looks for `assets/<scene_key>.png` first,
then falls back to a chapter-specific fallback (defined inline in
`SCENES`), then to `dashboard.png`, then to `splash.png`. This means
you can run the pipeline with just a dashboard screenshot and still get
a complete video — the dashboard just reuses across chapters until
you capture something more specific.

**Capturing with `capture_screens.sh`**: the script walks you through
each chapter, pauses for you to navigate the app to the right screen,
then captures via `xcrun simctl io <udid> screenshot`. See the script
header for prerequisites.

**Manual capture (iOS Simulator)**:
```bash
UDID=$(xcrun simctl list devices booted -j | python3 -c "import json,sys;print([d['udid'] for r,ds in json.load(sys.stdin)['devices'].items() for d in ds if d.get('state')=='Booted'][0])")
xcrun simctl io $UDID screenshot tools/video/assets/dashboard.png
```

**Manual capture (Android emulator)**:
```bash
adb -s emulator-5554 exec-out screencap -p > tools/video/assets/dashboard.png
```

**Video capture (for a future screen-recording-based cut)**:
```bash
# iOS Simulator — record until Ctrl+C
xcrun simctl io $UDID recordVideo tools/video/raw/ios_walkthrough.mov

# Android emulator — 120-sec cap, then pull
adb -s emulator-5554 shell screenrecord --time-limit 120 /sdcard/ot.mp4
adb -s emulator-5554 pull /sdcard/ot.mp4 tools/video/raw/android_walkthrough.mp4
```

---

## Localization

All 9 shipped languages have caption dicts in `build_video.py`'s `LANG`.
Fonts resolve per-language via `FONT_CANDIDATES` with a fallback chain
so the pipeline works on macOS, Linux, and (with Pillow + ffmpeg
installed) Windows.

Translations are machine-generated for the 8 non-English languages —
same policy as the in-app strings (`docs/NEEDS_REVIEW.md`). Any paid-
media use of a non-English video should go through native-speaker
sign-off per the `NEEDS_REVIEW.md` checklist first.

**Font resolution per language** (first existing wins):
- `en, es, fr, pt, de, id` → system default (Helvetica on macOS,
  DejaVu Sans on Linux)
- `hi` → DevanagariMT.ttc → Kohinoor.ttc → Noto Sans Devanagari
- `ar` → GeezaPro.ttc → ArabicUIText.ttc → Noto Sans Arabic
- `te` → KohinoorTelugu.ttc → Telugu Sangam MN.ttc → SFIndia.ttc →
  Noto Sans Telugu

If none exist, Pillow's default bitmap font is used — glyphs for
complex scripts will render as empty boxes. Add the appropriate font
to one of the search paths and re-run.

---

## What this pipeline does NOT do

Honest limits (keeping with the app's own ethos):

- **No voice narration.** The videos are silent with on-screen captions
  only. For the full narrated tutorial, follow the chapter beats in
  `docs/USER_GUIDE_VIDEO_SCRIPT.md` and record voiceover separately,
  then overlay in a video editor.
- **No animation between chapters.** Each scene is a static frame held
  for its duration — the cut hard-switches to the next scene. For
  animated transitions, add ffmpeg's `xfade` filter in a follow-up
  commit.
- **No tap indicators on the device screenshot.** The real screen is
  shown without overlaid cursor / finger graphics. For a tutorial that
  highlights specific gestures, use a video editor to add call-outs
  after the pipeline runs.
- **No live screen-recording.** The current pipeline is static-image
  based. A screen-recording-based cut needs a different pipeline
  (ffmpeg concat of MOV / MP4 captures instead of PNG scenes). The
  capture commands for that live in the README above but the stitcher
  is a future addition.

These are deliberate limits — the kit is a reliable, reproducible,
on-brand way to produce localized walkthrough videos quickly. For the
full 28-min narrated tutorial, use this as the b-roll generator and
cut it into a bigger project in Final Cut / Premiere / Resolve.

---

## Website embed

The `output/` folder is self-contained — you can copy any produced
video + its VTT track into your website / docs host:

```html
<video controls width="720">
    <source src="OnlyTrusted_Real_UI_en.mp4" type="video/mp4">
    <track kind="subtitles" src="OnlyTrusted_en.vtt" srclang="en" label="English" default>
    <track kind="subtitles" src="OnlyTrusted_es.vtt" srclang="es" label="Español">
    <track kind="subtitles" src="OnlyTrusted_te.vtt" srclang="te" label="Telugu">
    <!-- add more tracks here -->
</video>
```

Full working player at `docs/guide/videos/` once copied — see
`docs/USER_GUIDE_VIDEO_SCRIPT.md` for the planned video layout on
the website.

---

## Related docs

- `USER_GUIDE.md` — the written user guide
- `docs/USER_GUIDE_VIDEO_SCRIPT.md` — the full 28-min narrated-video
  script (filmable chapter beats, voiceover copy, honest-limits fine
  print)
- `docs/MARKETING_SHOWCASE.md` — ad creative brief (15s / 30s / 60s cuts)
- `docs/NEEDS_REVIEW.md` — per-locale translation review log; must be
  consulted before any paid-media use of a non-English video
- `docs/screenshots/` — the shared screenshot library some assets
  were copied from

---

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
