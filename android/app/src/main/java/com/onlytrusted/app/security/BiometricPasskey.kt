// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Phase 2 of the authentication model: a biometric-gated AES-256 key living in the
 * Android Keystore — StrongBox-backed when the device supports it. The OS kernel
 * refuses to let any process use this key without a fresh BIOMETRIC_STRONG assertion,
 * enforced via BiometricPrompt.CryptoObject. See `docs/AUTH_MODEL.md`.
 *
 * This commit wires the enroll + unwrap plumbing but intentionally does NOT re-encrypt
 * the existing vault with the passkey — that migration is a follow-up once the dual
 * path has survived a release cycle in the wild. What ships today:
 *  1. On first unlock after biometric is enabled: generate the passkey + wrap a 32-byte
 *     session nonce. Stored in EncryptedSharedPreferences.
 *  2. On every subsequent unlock: BiometricPrompt.CryptoObject(Cipher) unwraps the nonce,
 *     proving the kernel-level biometric assertion succeeded.
 *  3. Session nonce is held in memory via [sessionKey] until explicit clear.
 */
object BiometricPasskey {

    private const val TAG = "Passkey"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "com.onlytrusted.app.passkey.v2"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_SIZE_BYTES = 12
    private const val SESSION_NONCE_SIZE = 32

    private const val PREFS_NAME = "only_trusted_prefs"
    private const val KEY_WRAPPED_SESSION = "passkey_session_wrapped_v2"

    @Volatile private var sessionKey: ByteArray? = null

    // MARK: - Availability

    /** True if the device has a Class 3 biometric enrolled (fingerprint, Face Unlock, etc.). */
    fun isAvailable(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /** True once the passkey has been generated for this install. */
    fun isEnrolled(): Boolean {
        return try {
            KeyStore.getInstance(KEYSTORE).apply { load(null) }.containsAlias(KEY_ALIAS)
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * True once the passkey is enrolled AND a wrapped session blob exists. We care
     * about both because enrollment can be interrupted (user cancels biometric prompt
     * after the Keystore key is generated but before the blob is wrapped), leaving the
     * install in a half-enrolled state that must restart the enrollment flow.
     */
    fun needsEnroll(ctx: Context): Boolean =
        !isEnrolled() || loadWrappedSession(ctx) == null

    /** True once the session nonce has been unwrapped and is held in memory for the session. */
    val isPrimed: Boolean get() = sessionKey != null

    // MARK: - Enroll (first-time biometric enrollment after unlock)

    /**
     * Generates the StrongBox-backed passkey (if missing) and wraps a fresh 32-byte
     * session nonce. Fires a biometric prompt on the activity's main thread. Safe to
     * call repeatedly — becomes a no-op after the first successful enrollment.
     */
    fun enrollIfNeeded(
        activity: FragmentActivity,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (isEnrolled() && loadWrappedSession(activity) != null) {
            Log.d(TAG, "enroll skipped — already enrolled")
            onDone(true)
            return
        }
        if (!isAvailable(activity)) {
            Log.d(TAG, "enroll skipped — biometric unavailable")
            onDone(false)
            return
        }

        try {
            getOrCreateKey()
        } catch (t: Throwable) {
            Log.e(TAG, "enroll — key generation failed", t)
            onDone(false)
            return
        }

        val sessionNonce = ByteArray(SESSION_NONCE_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, loadKey())
        }

        promptWithCrypto(
            activity = activity,
            title = "Set up your Passkey",
            subtitle = "Confirm with Face / fingerprint — hardware-bound, on-device.",
            cryptoObject = BiometricPrompt.CryptoObject(cipher),
            onSuccess = { authedCipher ->
                Log.d(TAG, "enroll — biometric OK, wrapping session nonce")
                try {
                    val iv = authedCipher.iv
                    Log.d(TAG, "enroll — IV length=${iv?.size}")
                    val ciphertext = authedCipher.doFinal(sessionNonce)
                    Log.d(TAG, "enroll — ciphertext length=${ciphertext.size}")
                    val combined = ByteArray(iv.size + ciphertext.size).also {
                        System.arraycopy(iv, 0, it, 0, iv.size)
                        System.arraycopy(ciphertext, 0, it, iv.size, ciphertext.size)
                    }
                    storeWrappedSession(activity, combined)
                    Log.d(TAG, "enroll — blob stored, verify: ${loadWrappedSession(activity)?.size ?: -1} bytes")
                    sessionKey = sessionNonce
                    Log.i(TAG, "enroll SUCCESS — session primed")
                    onDone(true)
                } catch (t: Throwable) {
                    Log.e(TAG, "enroll — wrap failed", t)
                    onDone(false)
                }
            },
            onFailure = { err ->
                Log.w(TAG, "enroll — biometric failed: $err")
                onDone(false)
            }
        )
    }

    /**
     * Unwraps the session nonce via BiometricPrompt.CryptoObject. Call this from
     * AppLockGate right after the lock-screen gate opens. On success, [isPrimed]
     * becomes true for the rest of the session. On failure, the app continues to
     * work via the legacy Keychain path — Phase 2 is additive, not required.
     */
    fun unlockSession(
        activity: FragmentActivity,
        onDone: (Boolean) -> Unit = {}
    ) {
        val wrapped = loadWrappedSession(activity)
        if (wrapped == null || wrapped.size <= IV_SIZE_BYTES) {
            Log.d(TAG, "unlockSession skipped — no wrapped blob")
            onDone(false)
            return
        }
        val key = loadKey() ?: run {
            Log.w(TAG, "unlockSession skipped — passkey missing from Keystore")
            onDone(false)
            return
        }

        val iv = wrapped.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = wrapped.copyOfRange(IV_SIZE_BYTES, wrapped.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }

        promptWithCrypto(
            activity = activity,
            title = "Unlock with your Passkey",
            subtitle = "Face / fingerprint — hardware-bound, phishing-proof.",
            cryptoObject = BiometricPrompt.CryptoObject(cipher),
            onSuccess = { authedCipher ->
                try {
                    sessionKey = authedCipher.doFinal(ciphertext)
                    Log.i(TAG, "unlockSession SUCCESS — session primed")
                    onDone(true)
                } catch (t: Throwable) {
                    Log.e(TAG, "unlockSession — unwrap failed", t)
                    onDone(false)
                }
            },
            onFailure = { err ->
                Log.w(TAG, "unlockSession — biometric failed: $err")
                onDone(false)
            }
        )
    }

    /** Clears the in-memory session nonce. Called when the user explicitly locks the app. */
    fun clearSession() {
        sessionKey?.fill(0)
        sessionKey = null
    }

    /**
     * Deletes the passkey + wrapped blob. Used when the user disables biometric unlock
     * from Settings — the app falls back to the legacy non-biometric path.
     */
    fun unenroll(ctx: Context) {
        try {
            KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        } catch (t: Throwable) {
            Log.w(TAG, "unenroll — keystore delete failed", t)
        }
        try {
            prefs(ctx).edit().remove(KEY_WRAPPED_SESSION).apply()
        } catch (t: Throwable) {
            Log.w(TAG, "unenroll — prefs clear failed", t)
        }
        clearSession()
    }

    // MARK: - Keystore key management

    private fun getOrCreateKey(): SecretKey {
        loadKey()?.let { return it }
        // Try StrongBox first; fall back to TEE-backed Keystore on devices without the
        // dedicated hardware chip (emulators, pre-2018 flagships). The fallback happens
        // at generateKey() time, not Builder time, so the try/catch has to wrap both.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return buildKey(strongBox = true)
            } catch (t: android.security.keystore.StrongBoxUnavailableException) {
                Log.i(TAG, "StrongBox unavailable, falling back to TEE Keystore")
            } catch (t: Throwable) {
                // Some devices throw a plain KeyStoreException wrapping the StrongBox
                // error rather than StrongBoxUnavailableException. Treat both the same.
                if (t.message?.contains("StrongBox", ignoreCase = true) == true) {
                    Log.i(TAG, "StrongBox unavailable (wrapped error), falling back to TEE Keystore")
                } else {
                    throw t
                }
            }
        }
        return buildKey(strongBox = false)
    }

    private fun buildKey(strongBox: Boolean): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            // Kernel-enforced biometric gate — every Cipher init requires a fresh
            // BIOMETRIC_STRONG assertion via BiometricPrompt.CryptoObject.
            .setUserAuthenticationRequired(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(
                        0, // require fresh auth for every use (no timeout window)
                        KeyProperties.AUTH_BIOMETRIC_STRONG
                    )
                }
                if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setIsStrongBoxBacked(true)
                }
            }
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun loadKey(): SecretKey? = try {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        ks.getKey(KEY_ALIAS, null) as? SecretKey
    } catch (t: Throwable) {
        null
    }

    // MARK: - Wrapped-blob storage (EncryptedSharedPreferences)

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx,
        PREFS_NAME,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun loadWrappedSession(ctx: Context): ByteArray? {
        val b64 = prefs(ctx).getString(KEY_WRAPPED_SESSION, null) ?: return null
        return runCatching { Base64.decode(b64, Base64.NO_WRAP) }.getOrNull()
    }

    private fun storeWrappedSession(ctx: Context, data: ByteArray) {
        prefs(ctx).edit().putString(KEY_WRAPPED_SESSION, Base64.encodeToString(data, Base64.NO_WRAP)).apply()
    }

    // MARK: - BiometricPrompt wrapper that returns the authenticated Cipher

    private fun promptWithCrypto(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cryptoObject: BiometricPrompt.CryptoObject,
        onSuccess: (Cipher) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authedCipher = result.cryptoObject?.cipher
                    if (authedCipher != null) {
                        onSuccess(authedCipher)
                    } else {
                        onFailure("Authenticator returned no Cipher.")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure(errString.toString())
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            // CryptoObject-based prompts require BIOMETRIC_STRONG specifically; device
            // credential (PIN/pattern) can't satisfy the Keystore's biometric ACL.
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info, cryptoObject)
    }
}
