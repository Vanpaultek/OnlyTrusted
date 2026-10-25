// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI
import LocalAuthentication

/// Mandatory first-launch profile setup. Runs after OnboardingScreen + ConsentScreen,
/// before AppLockGate / MainTabs. Three sequential steps:
///   1. Email (stored locally only; prefills Breach Watch, personalises copy)
///   2. PIN (4-6 digits, hashed with PBKDF2 before storing)
///   3. Biometric opt-in (Face ID / Touch ID via LocalAuthentication)
/// All three write to `ProfileStore.shared`. On completion, calls `onComplete()`.
struct ProfileSetupScreen: View {

    let onComplete: () -> Void

    @StateObject private var store = ProfileStore.shared
    @State private var step: Step = .email

    @State private var emailDraft = ""
    @State private var pinDraft = ""
    @State private var pinConfirm = ""
    @State private var errorMessage: String?

    private enum Step { case email, pin, biometric, done }

    var body: some View {
        VStack(spacing: 24) {
            header
            switch step {
            case .email:     emailStep
            case .pin:       pinStep
            case .biometric: biometricStep
            case .done:      Color.clear
            }
            if let errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundStyle(Color.appDanger)
                    .padding(.top, 4)
            }
        }
        .padding(.horizontal, 24)
        // Centers the VStack inside the full-screen frame (horizontal + vertical).
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        .background(AppBackdrop())
    }

    // MARK: - Header

    private var header: some View {
        VStack(spacing: 10) {
            Image(systemName: iconName)
                .font(.system(size: 48))
                .foregroundStyle(Color.appAccent)
            Text(titleText)
                .font(.title.weight(.bold))
                .foregroundStyle(Color.appPrimaryText)
                .multilineTextAlignment(.center)
            Text(subtitleText)
                .font(.subheadline)
                .foregroundStyle(Color.appSecondaryText)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var iconName: String {
        switch step {
        case .email:     return "envelope.badge"
        case .pin:       return "lock.rectangle.stack"
        case .biometric: return "faceid"
        case .done:      return "checkmark.seal.fill"
        }
    }

    private var titleText: String {
        switch step {
        case .email:     return "Your email"
        case .pin:       return "Set a 4-6 digit PIN"
        case .biometric: return "Set up your Passkey"
        case .done:      return "All set"
        }
    }

    private var subtitleText: String {
        switch step {
        case .email:     return "Stored on this device only. OnlyTrusted does not have a server, so your email is never transmitted anywhere."
        case .pin:       return "Backup unlock method. PBKDF2-hashed before storing — we can't read it back. Only used if your Passkey can't be presented."
        case .biometric: return "Face ID / Touch ID becomes your primary unlock. Phishing-proof, no password to type, no email code to wait for. Uses Secure Enclave — your biometric never leaves this device."
        case .done:      return ""
        }
    }

    // MARK: - Step: Email

    private var emailStep: some View {
        VStack(spacing: 14) {
            TextField("you@example.com", text: $emailDraft)
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .autocapitalization(.none)
                .disableAutocorrection(true)
                .padding(14)
                .background(Color.appElevatedSurface, in: RoundedRectangle(cornerRadius: 10))
                .foregroundStyle(Color.appPrimaryText)

            Button(action: handleEmailNext) {
                Text("Continue")
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(Color.appAccent, in: RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(Color.appScreenBackground)
            }
            .buttonStyle(.plain)
            .disabled(emailDraft.trimmingCharacters(in: .whitespaces).isEmpty)
        }
    }

    private func handleEmailNext() {
        let normalised = emailDraft.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard normalised.contains("@"), normalised.contains(".") else {
            errorMessage = "That doesn't look like an email address."
            return
        }
        store.setEmail(normalised)
        errorMessage = nil
        step = .pin
    }

    // MARK: - Step: PIN

    private var pinStep: some View {
        VStack(spacing: 14) {
            SecureField("4-6 digit PIN", text: $pinDraft)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .padding(14)
                .background(Color.appElevatedSurface, in: RoundedRectangle(cornerRadius: 10))
                .foregroundStyle(Color.appPrimaryText)

            SecureField("Confirm PIN", text: $pinConfirm)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .padding(14)
                .background(Color.appElevatedSurface, in: RoundedRectangle(cornerRadius: 10))
                .foregroundStyle(Color.appPrimaryText)

            Button(action: handlePINNext) {
                Text("Set PIN")
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(Color.appAccent, in: RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(Color.appScreenBackground)
            }
            .buttonStyle(.plain)
            .disabled(pinDraft.count < 4)
        }
    }

    private func handlePINNext() {
        let digits = pinDraft.filter(\.isNumber)
        guard digits.count >= 4, digits.count <= 6 else {
            errorMessage = "PIN must be 4 to 6 digits."
            return
        }
        guard digits == pinConfirm.filter(\.isNumber) else {
            errorMessage = "PINs don't match."
            return
        }
        Task {
            await store.setPIN(digits)
            pinDraft = ""
            pinConfirm = ""
            errorMessage = nil
            step = .biometric
        }
    }

    // MARK: - Step: Biometric

    private var biometricStep: some View {
        VStack(spacing: 14) {
            // Primary action — enable Passkey (biometric) as the default unlock.
            Button(action: { enableBiometric(true) }) {
                Text("Set up Passkey")
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(Color.appAccent, in: RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(Color.appScreenBackground)
            }
            .buttonStyle(.plain)

            // Secondary — users on devices without biometric sensors or those who
            // prefer PIN-only can skip. PIN was set in the previous step so unlock
            // still works either way.
            Button(action: { enableBiometric(false) }) {
                Text("Use PIN only")
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(Color.appElevatedSurface, in: RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(Color.appAccent)
            }
            .buttonStyle(.plain)
        }
    }

    private func enableBiometric(_ enabled: Bool) {
        if enabled {
            let ctx = LAContext()
            var err: NSError?
            guard ctx.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &err) else {
                errorMessage = "Face ID / Touch ID isn't available on this device — continuing with PIN only."
                store.setBiometricEnabled(false)
                finish()
                return
            }
            ctx.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics,
                               localizedReason: "Set up your OnlyTrusted Passkey") { ok, _ in
                DispatchQueue.main.async {
                    store.setBiometricEnabled(ok)
                    finish()
                }
            }
        } else {
            store.setBiometricEnabled(false)
            finish()
        }
    }

    private func finish() {
        store.markSetupComplete()
        onComplete()
    }
}
