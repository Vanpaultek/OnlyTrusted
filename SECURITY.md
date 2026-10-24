# Security policy

OnlyTrusted is a privacy & security app with a **zero-cloud, zero-server,
zero-network** architecture. If you find a vulnerability — including anything
that would break that promise — please report it to us privately so we can fix
it before it's publicly disclosed.

## Reporting a vulnerability

- **Preferred channel**: email `security@vanpaultek.com` (or `admin@vanpaultek.com`
  if the first bounces). Use PGP if you need confidentiality — key fingerprint
  will be published at `.well-known/security.txt` on the vanpaultek.com site
  once that site is live. If you don't have PGP, email anyway in plain text;
  we'll triage either way.
- **Please include**: a reproduction, the OnlyTrusted version + commit SHA
  (visible in Settings → About), the platform (Android / iOS) + OS version,
  and if the issue involves a bundled threat-intel feed: which file / line.
- **Please do NOT**: publicly disclose, open a public issue, or post to
  social media before we've confirmed receipt and had a reasonable chance
  to fix it. The embargo is 90 days from first contact; we'll usually be
  faster.

## What we treat as in-scope

- Any code path that violates the zero-network / zero-cloud / no-account
  promise — e.g. an unintended outbound request, a bundled dependency that
  tries to phone home, a build-time pipeline that leaks user data.
- Data-at-rest flaws — key handling, Keychain / EncryptedSharedPreferences
  misuse, SQLCipher key derivation, encrypted-backup format weaknesses.
- Authentication / unlock bypasses — PIN hashing (PBKDF2 100 k), biometric
  gate, duress-mode leaks, dead-man wipe not firing.
- Scam-detection evasion that's exploitable at scale (not a single
  false-positive report — those are regular bugs, open an issue for them).
- Supply-chain issues in release artifacts — unsigned where it shouldn't be,
  reproducibility failures, SLSA attestation mismatches with the published
  artifact SHA.

## What's out of scope

- iOS platform limitations we explicitly document in `LegalContent.swift`
  (e.g. we cannot read SMS inbox on iOS; we cannot auto-block Wangiri
  callers on iOS because CallKit doesn't expose numbers). These are OS
  constraints, not bugs we can fix.
- Threat-intel feed coverage gaps — the current `scam_numbers.v1.txt` is
  a known 41-entry placeholder (see the `README.md` in that directory
  for the honest status). Please help us fix this by contributing a
  public, free, bulk source — don't file it as a vuln.
- Social-engineering risks the owner can only mitigate behaviourally
  (e.g. "scammer convinced owner to disable the app").

## Our commitments

- Acknowledge receipt of your report within **3 working days**.
- Confirm the vulnerability or explain why it's out-of-scope within
  **14 days**.
- Ship a fix on the main branch and a tagged release within **90 days**
  of first contact, unless we've agreed in writing with you to extend.
- Credit you in the release notes (or anonymously, if you prefer).
- Never pursue legal action against a researcher who complies with this
  policy. Good-faith research is explicitly welcomed.

## Public verification of releases

Every tagged release on GitHub (`v*`) ships with:

- **CycloneDX SBOM** — the full list of included dependencies + versions.
- **SLSA Build Provenance attestation** — cryptographic proof that the
  artifact was built by our GitHub Actions `.github/workflows/release.yml`
  pipeline from a specific commit SHA. Verify with:

  ```
  gh attestation verify OnlyTrusted-<tag>.aab -R Vanpaultek/OnlyTrusted
  gh attestation verify OnlyTrusted-<tag>-unsigned.ipa -R Vanpaultek/OnlyTrusted
  ```

- A SHA-256 checksum manifest.

If a release artifact fails attestation verification, **do not install it**
and please report it immediately via the channel above.

## Reproducible builds (work in progress)

The current release workflow produces deterministic Android AABs modulo:
- The Gradle wrapper + Android Gradle Plugin + Kotlin compiler versions
  (pinned, but major versions do change behaviour).
- Dependency resolution timing (versions, not transitive-fetch ordering).

We're working toward byte-level reproducible AABs; follow progress in the
`reproducible-build` label on GitHub issues. iOS reproducibility is a
longer road because Xcode build inputs are harder to pin — contributions
welcome.

---

Last updated: 2026-10-24.
