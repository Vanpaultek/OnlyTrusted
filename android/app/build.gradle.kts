plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version "2.0.21"
    // CycloneDX SBOM — adds the `cyclonedxBom` task that scans the release
    // runtime classpath and writes a CycloneDX-formatted JSON to
    // app/build/reports/bom.json. See the root build.gradle.kts for the
    // declared plugin version.
    id("org.cyclonedx.bom")
}

android {
    namespace = "com.onlytrusted.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.onlytrusted.app"
        minSdk = 29
        // Google Play (as of Nov 2025) requires new apps to target API 36
        // (Android 16 "Baklava"). Bumping also enables the latest security
        // + performance defaults on all shipping devices.
        targetSdk = 36
        versionCode = 13
        versionName = "0.1.12"
        // Note on native debug symbols: our dep .so files (SQLCipher, face
        // detector, androidx.graphics.path, ML Kit) are shipped stripped by
        // their publishers. There's nothing for AGP to copy into the AAB,
        // so Google Play's "no debug symbols" warning is unavoidable without
        // Firebase Crashlytics or similar. R8 mapping handles our own Kotlin
        // code symbolication automatically; see docs/RELEASE_GATES.md.

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Explicit allow-list of locales shipped with the APK. Must match
        // res/xml/locales_config.xml and the values-<lang>/strings.xml dirs.
        // Any locale not in this list is stripped from the final build regardless
        // of whether a sibling resource exists.
        resourceConfigurations += listOf("en", "es", "fr", "pt", "hi", "ar", "de", "id", "te")
    }

    // Release signing config. Only wired up when the four environment
    // variables below are populated — in local dev + on PR builds they're
    // unset so Gradle skips the block and `assembleRelease` fails cleanly
    // instead of silently signing with the debug key. In CI (release.yml)
    // we decode the keystore from a GitHub Secret into $RUNNER_TEMP and
    // export the paths before invoking `./gradlew :app:bundleRelease`.
    val releaseKeystorePath: String? = System.getenv("ONLYTRUSTED_RELEASE_KEYSTORE")
    val releaseKeystorePassword: String? = System.getenv("ONLYTRUSTED_RELEASE_KEYSTORE_PASSWORD")
    val releaseKeyAlias: String? = System.getenv("ONLYTRUSTED_RELEASE_KEY_ALIAS")
    val releaseKeyPassword: String? = System.getenv("ONLYTRUSTED_RELEASE_KEY_PASSWORD")
    val hasReleaseSigningEnv = !releaseKeystorePath.isNullOrBlank() &&
        !releaseKeystorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

    if (hasReleaseSigningEnv) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                // Enable v1 (JAR), v2 (APK), v3 (APK rotation), and v4 (fs-verity).
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigningEnv) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    // Auto Privacy Shield: ProcessLifecycleOwner so we can observe app-wide ON_PAUSE
    // / ON_STOP and tear down every live camera/mic capture OnlyTrusted owns.
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Room (encrypted via SQLCipher)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // SQLCipher for encrypted Room DB
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // Document files for user-chosen export location
    implementation("androidx.documentfile:documentfile:1.0.1")

    // JSON (bundled with Android, but kotlinx-serialization keeps us typed)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // --- Smart Link Guard (Pro) -----------------------------------------------
    // CameraX for the in-app QR scanner surface.
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    // ML Kit's on-device barcode scanner — no network calls.
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")

    // --- Hardware Security Key (Elite) ----------------------------------------
    // Credential Manager unified API; the play-services-auth provider adds the
    // FIDO2 / WebAuthn backend so YubiKey / Titan / SoloKey can register and
    // assert via the standard CreatePublicKeyCredentialRequest / GetCredentialRequest
    // flows. The core credentials module alone cannot talk to USB / NFC keys.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

    // --- Geo-fence Auto-Lock (Elite) -----------------------------------------
    // Google Play Services Location — provides GeofencingClient and the Geofence
    // builder. The feature is Elite-gated and self-disables on devices without
    // Google Play Services installed.
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // --- Face-Change Shoulder-Surfing Lock (Elite) ---------------------------
    // ML Kit's bundled on-device face detector drives both the one-time enrollment
    // flow and the runtime monitor. Zero network traffic — the model ships inside
    // the APK. Classifications + landmarks are needed so we can derive the owner
    // embedding from the 2D landmark geometry.
    implementation("com.google.mlkit:face-detection:16.1.6")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// --------------------------------------------------------------------------
// Launcher icon render smoke test.
//
// Extracts the compiled mipmap PNGs from the just-built APK so a reviewer
// can visually confirm the icon looks right at every density. Added after
// a long debug session where the bitmap `gravity="center"` attribute meant
// adaptive-icon inset changes had NO visible effect — hard to notice until
// you compare the rendered PNGs side by side.
//
// Usage:
//     ./gradlew extractLauncherIcons
//
// Output:
//     app/build/reports/launcher-icons/mipmap-<density>-<name>.png
//
// Depends on assembleDebug so the APK exists.
// --------------------------------------------------------------------------
tasks.register<Copy>("extractLauncherIcons") {
    group = "verification"
    description = "Extract rendered launcher-icon PNGs from the debug APK into build/reports/launcher-icons/ for visual review."
    dependsOn("assembleDebug")
    val apkFile = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
    val outDir = layout.buildDirectory.dir("reports/launcher-icons")

    from(zipTree(apkFile)) {
        include("res/mipmap*/ic_launcher*.png")
        eachFile {
            // Flatten res/mipmap-xxxhdpi-v4/ic_launcher.png →
            //          mipmap-xxxhdpi-v4-ic_launcher.png so a single
            //          Finder window shows every density side by side.
            path = relativePath.segments.drop(1).joinToString("-")
        }
        includeEmptyDirs = false
    }
    into(outDir)

    doLast {
        val listing = outDir.get().asFile.listFiles()?.sortedBy { it.name } ?: emptyList()
        println("Extracted ${listing.size} launcher-icon PNGs to:")
        println("  ${outDir.get().asFile.absolutePath}")
        listing.forEach { println("    ${it.name} (${it.length() / 1024} KB)") }
        println()
        println("Open any of the above PNGs to visually confirm the launcher icon.")
        println("If the icon looks wrong, the adaptive-icon XML change didn't take effect — ")
        println("most likely the bitmap wrapper has gravity=\"center\" instead of \"fill\".")
    }
}
