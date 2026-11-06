# Releasing @vanpaultek/locale

## One-time setup (do this first)

1. **Create an npm automation token.**
   - Log in at https://www.npmjs.com → **Access Tokens** → **Generate New Token**.
   - Choose **Automation** (not "Publish") so it bypasses 2FA on CI.
   - Copy the token; you'll only see it once.

2. **Add the token to GitHub.**
   - Repo → **Settings** → **Secrets and variables** → **Actions** → **New
     repository secret**.
   - Name: `NPM_TOKEN`. Value: the token from step 1.

3. **Create the `npm-publish` environment** (protects the token).
   - Repo → **Settings** → **Environments** → **New environment**.
   - Name it exactly `npm-publish` (matches `publish.yml`'s `environment:`).
   - Add optional approval rule: "Required reviewers" → yourself, so every
     publish needs a human click.

4. **Enable npm provenance on the registry.**
   - The `publish.yml` workflow passes `--provenance`, which uses GitHub OIDC
     to sign the package. No extra config on npm; it just works once the
     repo is public and `id-token: write` is granted (already in the file).

## Cutting a release

From the package directory (`packages/vanpaultek-locale/` while it's still
in this repo, or the repo root after you split it out):

```bash
# 1. Make your changes, commit them.
git add . && git commit -m "feat: ..."

# 2. Bump version (patch / minor / major). This also writes the git tag.
npm version patch        # 0.1.0 → 0.1.1
# or: npm version minor  # 0.1.0 → 0.2.0
# or: npm version major  # 0.1.0 → 1.0.0

# 3. Push the commit AND the tag. The tag triggers publish.yml.
git push && git push --tags
```

That's it. Watch the **Actions** tab:

- **CI** runs on the commit push → tests on Node 20/22/24.
- **Publish to npm** runs on the tag push → builds, tests, publishes with
  provenance, and creates a GitHub Release with auto-generated notes.

## Manual publish (break-glass)

If you need to republish without cutting a new tag:

- Actions tab → **Publish to npm** → **Run workflow** → optionally supply a
  tag name, or leave blank to publish `main`'s current HEAD.

The workflow still needs `package.json` version to match the git tag when
triggered by a tag push; manual runs skip that check.

## Pre-release channels (optional)

For betas, use a dist-tag so stable consumers don't auto-upgrade:

```bash
npm version 0.2.0-beta.1
git push && git push --tags
```

Edit `publish.yml` once to add `--tag beta` to the `npm publish` step, or
override via workflow_dispatch input. Users opt in with
`npm install @vanpaultek/locale@beta`.

## Rolling back a bad release

npm forbids re-publishing the same version. Options:

- **Deprecate the broken version** so new installs skip it:
  ```bash
  npm deprecate @vanpaultek/locale@0.1.1 "contains bug XYZ, use 0.1.2+"
  ```
- **Publish a fix as a new patch** (`0.1.2`). This is the usual path.
- `npm unpublish` only works within 72 hours and is strongly discouraged.
