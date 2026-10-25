plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    // CycloneDX SBOM generator — produces app/build/reports/bom.json listing
    // every dependency + version in the release AAB. Consumed by the release
    // workflow (.github/workflows/release.yml) which uploads bom.json alongside
    // the AAB so downstream auditors can inspect the full dependency tree.
    id("org.cyclonedx.bom") version "2.3.1" apply false
}
