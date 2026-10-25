// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI
@preconcurrency import LocalAuthentication
import MessageUI

struct AppLockGate: View {
    let onUnlocked: () -> Void

    @EnvironmentObject private var gate: AppGate
    @StateObject private var duress = DuressState.shared
    @StateObject private var vault = VaultStore.shared

    @State private var error: String?
    @State private var canAuthenticate: Bool = false
    @State private var showPinSheet = false
    @State private var pin: String = ""
    @State private var pinError: String?
    @State private var showSOSComposer = false
    @State private var sosRecipients: [String] = []
    @State private var sosBody: String = ""

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 60))
                .foregroundStyle(Color.appSecondaryText)
                .decorativeAccessibility()

            Text("Locked")
                .font(.appLargeTitle)

            Text("Passkey · phishing-proof · on-device")
                .font(.caption)
                .foregroundStyle(Color.appSecondaryText)
                .padding(.bottom, 4)

            if let error {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(Color.appDanger)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }

            if canAuthenticate {
                Button {
                    attemptAuth()
                } label: {
                    Label("Unlock with Passkey", systemImage: "faceid")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity, minHeight: 32)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.appAccent)
                .controlSize(.large)
                .padding(.horizontal, 32)

                Button {
                    showPinSheet = true
                } label: {
                    Text("Use PIN instead")
                        .font(.footnote.weight(.medium))
                        .foregroundStyle(Color.appSecondaryText)
                }
                .buttonStyle(.plain)
            } else {
                let lockWasOn = gate.appLockEnabled
                VStack(spacing: 10) {
                    Text(lockWasOn
                         ? "This device no longer has a passcode or biometric set. Disable App Lock below to recover, then re-enable it after setting one up."
                         : "Panic lock engaged. Tap below to return to the app.")
                        .font(.footnote)
                        .foregroundStyle(Color.appSecondaryText)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                    Button {
                        if lockWasOn { gate.appLockEnabled = false }
                        onUnlocked()
                    } label: {
                        Text(lockWasOn ? "Disable App Lock and continue" : "Return to app")
                            .font(.body.weight(.semibold))
                            .frame(maxWidth: .infinity, minHeight: 32)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color.appAccent)
                    .controlSize(.large)
                    .padding(.horizontal, 32)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appScreenBackground)
        .onAppear(perform: setup)
        .sheet(isPresented: $showPinSheet) { pinSheet }
        .sheet(isPresented: $showSOSComposer) {
            if DuressSMSComposer.canSendText {
                DuressSMSComposer(
                    recipients: sosRecipients,
                    body: sosBody,
                    onFinish: { _ in
                        showSOSComposer = false
                        // Regardless of send result, continue into the decoy vault so coercion
                        // is not obvious.
                        onUnlocked()
                    }
                )
            } else {
                VStack(spacing: 16) {
                    Text("This device cannot send SMS.")
                        .font(.body)
                    Button("Continue") {
                        showSOSComposer = false
                        onUnlocked()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color.appAccent)
                }
                .padding(24)
            }
        }
    }

    // MARK: - PIN sheet (hosts duress unlock + eventually a real-PIN fallback)

    private var pinSheet: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text("Enter PIN")
                    .font(.title3.weight(.bold))
                    .foregroundStyle(Color.appPrimaryText)
                SecureField("PIN", text: $pin)
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .multilineTextAlignment(.center)
                    .padding(16)
                    .background(RoundedRectangle(cornerRadius: 10).fill(Color.appElevatedSurface))
                if let pinError {
                    Text(pinError)
                        .font(.footnote)
                        .foregroundStyle(Color.appDanger)
                }
                Button {
                    submitPin()
                } label: {
                    Text("Unlock")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.appAccent)
                Spacer()
            }
            .padding(24)
            .appBackdrop()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Cancel") { showPinSheet = false; pin = ""; pinError = nil }
                }
            }
        }
    }

    private func submitPin() {
        pinError = nil
        // Rate limit: refuse to even check the PIN if we're inside a lockout
        // window. Phase-2 audit flagged unlimited PIN attempts as HIGH severity.
        let lockout = PINRateLimiter.remainingLockout()
        if lockout > 0 {
            let mins = Int(ceil(lockout / 60.0))
            pinError = "Too many wrong PINs. Try again in \(mins) min."
            return
        }
        if duress.matches(pin: pin) {
            // Successful unlock (via duress path) resets the counter.
            PINRateLimiter.recordSuccess()
            // Duress unlock: swap to decoy, prepare SOS, dismiss PIN sheet, present composer.
            pin = ""
            showPinSheet = false
            duress.isDuressSession = true
            prepareSOS()
        } else {
            let penalty = PINRateLimiter.recordFailure()
            if penalty > 0 {
                let mins = Int(ceil(penalty / 60.0))
                pinError = "Too many wrong PINs. Locked for \(mins) min."
            } else {
                pinError = "Incorrect PIN."
            }
        }
    }

    private func prepareSOS() {
        guard
            let alertId = duress.alertContactId,
            let contact = VaultStore.shared.realContactsForSetup().first(where: { $0.id == alertId }),
            let number = contact.normalisedNumbers.first
        else {
            // No alert contact configured — still enter the decoy vault silently.
            onUnlocked()
            return
        }
        sosRecipients = [number]
        sosBody = DuressSOS.body(ownerName: duress.ownerName)
        showSOSComposer = true
    }

    // MARK: - Setup / normal auth

    private func setup() {
        var err: NSError?
        canAuthenticate = LAContext().canEvaluatePolicy(.deviceOwnerAuthentication, error: &err)
        if canAuthenticate { attemptAuth() }
    }

    private func attemptAuth() {
        let ctx = LAContext()
        // Reuse the same context for Secure Enclave key operations so the user isn't
        // prompted twice (once for the lock-screen gate, once for passkey unwrap).
        ctx.touchIDAuthenticationAllowableReuseDuration = 10
        ctx.evaluatePolicy(.deviceOwnerAuthentication,
                           localizedReason: "Unlock OnlyTrusted") { ok, err in
            Task { @MainActor in
                if ok {
                    // Real unlock — always clear duress session.
                    duress.isDuressSession = false
                    // Dead-Man Wipe (Elite): reset the countdown on every real unlock.
                    DeadmanWipeState.shared.markUnlockNow()
                    // Phase 2 passkey: enroll the Secure Enclave wrap on first run
                    // after upgrade, then prime the vault key for the session.
                    FieldCrypto.enrollPasskeyIfNeeded()
                    FieldCrypto.primeCache(from: ctx)
                    onUnlocked()
                } else if let err {
                    error = err.localizedDescription
                }
            }
        }
    }
}
