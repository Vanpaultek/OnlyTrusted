# OnlyTrusted — Firebase Test Lab setup

**Why:** manual testing on Pixel 7 emulator catches ~70% of regressions. Firebase Test Lab adds coverage across real Samsung / Xiaomi / OnePlus hardware without us owning them.

Last updated: 2026-10-28

---

## One-time setup

1. **Enable Firebase** on the Google Cloud project backing your Play Console
   - Go to [console.firebase.google.com](https://console.firebase.google.com)
   - Add project → link to the existing GCP project that owns `com.onlytrusted.app`
2. **Enable the Cloud Testing API** + **Cloud Tool Results API** in GCP
3. **Install `gcloud` CLI** locally:
   ```bash
   brew install --cask google-cloud-sdk
   gcloud auth login
   gcloud config set project <your-project-id>
   ```

## Running the matrix locally

```bash
cd android
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest

gcloud firebase test android run \
  --type instrumentation \
  --app       app/build/outputs/apk/debug/app-debug.apk \
  --test      app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device    model=a24,version=34,locale=en,orientation=portrait \
  --device    model=redmi12_global,version=34,locale=en,orientation=portrait \
  --device    model=cph2451,version=34,locale=en,orientation=portrait \
  --device    model=panther,version=34,locale=en,orientation=portrait \
  --timeout   15m
```

Devices used above:
- `a24` — Samsung Galaxy A24 (One UI 6)
- `redmi12_global` — Xiaomi Redmi 12 (MIUI 14)
- `cph2451` — OnePlus Nord 3 (OxygenOS 14)
- `panther` — Google Pixel 7 (stock Android, regression baseline)

List all currently available devices: `gcloud firebase test android models list`.

---

## Adding instrumentation tests

As of v0.1.10 the project has unit tests under `app/src/test/` but **no** instrumentation tests under `app/src/androidTest/`. For the matrix above to do anything useful, add instrumentation tests that at minimum:

- Launch `MainActivity`, walk through Onboarding → Consent → Email → Dashboard
- Verify the three Dashboard tiles navigate correctly
- Verify the Mic Lock service starts when the user toggles it on
- Verify screen rotation preserves state (`activity.recreate()`)

Starter location: `android/app/src/androidTest/java/com/onlytrusted/app/onboarding/OnboardingSmokeTest.kt`.

---

## CI integration (GitHub Actions)

A workflow file at `.github/workflows/firebase-test-lab.yml` would:

1. Trigger on `push` to `main` + weekly `schedule`
2. Build the debug + androidTest APKs
3. Authenticate to GCP via a `FIREBASE_SA_KEY` repo secret (JSON service-account key with `Firebase Test Admin` role)
4. Shell out to `gcloud firebase test android run ...`
5. Upload the results URL as a PR comment

Example workflow:

```yaml
name: Firebase Test Lab
on:
  push: { branches: [main] }
  schedule:
    - cron: "0 7 * * 1"   # weekly, Mon 07:00 UTC
jobs:
  ftl:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: 17, distribution: temurin }
      - run: ./android/gradlew -p android :app:assembleDebug :app:assembleDebugAndroidTest
      - uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.FIREBASE_SA_KEY }}
      - uses: google-github-actions/setup-gcloud@v2
      - name: Run FTL matrix
        run: |
          gcloud firebase test android run \
            --type instrumentation \
            --app     android/app/build/outputs/apk/debug/app-debug.apk \
            --test    android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
            --device  model=a24,version=34 \
            --device  model=redmi12_global,version=34 \
            --device  model=cph2451,version=34 \
            --timeout 20m
```

---

## Cost ballpark

Firebase Test Lab has a free tier of 15 physical-device test minutes + 60 virtual-device minutes per day. Beyond that, physical-device minutes are billed at ~$5/hr. The 4-device matrix above runs ~15 min per device = 60 min total = **~$5 per full matrix run**. Weekly schedule = ~$20/month.

Cheaper alternative: run **Samsung Remote Test Lab** (free) + physical device for Xiaomi + physical for OnePlus. See `docs/REAL_DEVICE_TESTING_MATRIX.md` (if ever written) for the hybrid approach.

---

## Related

- `docs/ICON_DESIGN_BRIEF.md` — hand to a designer for proper launcher icon
- `docs/UX_DECISIONS_LOG.md` — context for anyone reviewing the current UX
- In-app diagnostic: `Settings → Device compatibility` — testers can self-report their device state
