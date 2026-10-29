// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Combine
import Foundation

/// iOS counterpart of Android's trusted-only toggles that live on the
/// Dashboard. Two independent switches:
///
///  - `silenceCallsNotInVault` — if a caller is not in the user's trusted
///    vault, the Call Blocking extension drops the ring (honest iOS limit:
///    we can't literally mute the OS ringer; the CallKit extension can
///    only mark the call as "blocked" so iOS routes it to voicemail).
///  - `flagSmsNotInVault` — if an SMS comes from a sender not in the vault,
///    the Messages Filter extension classifies it as junk so iOS files it
///    under Unknown Sender.
///
/// Both default OFF so a fresh install never silently filters anything the
/// user hasn't explicitly opted into.
///
/// Backed by `UserDefaults.standard` because these are tiny booleans + the
/// call / SMS extensions need to read them from their own process space
/// (App Group suite would be used in production; using standard for now
/// since the extensions aren't wired yet).
@MainActor
final class TrustedOnlyModeState: ObservableObject {
    static let shared = TrustedOnlyModeState()

    private enum Keys {
        static let silenceCallsNotInVault = "trustedOnly.silenceCallsNotInVault.v1"
        static let flagSmsNotInVault = "trustedOnly.flagSmsNotInVault.v1"
    }

    @Published var silenceCallsNotInVault: Bool {
        didSet { UserDefaults.standard.set(silenceCallsNotInVault, forKey: Keys.silenceCallsNotInVault) }
    }

    @Published var flagSmsNotInVault: Bool {
        didSet { UserDefaults.standard.set(flagSmsNotInVault, forKey: Keys.flagSmsNotInVault) }
    }

    /// `true` iff at least one of the two toggles is on. Used by the
    /// Dashboard to color the card header indicator.
    var isActive: Bool { silenceCallsNotInVault || flagSmsNotInVault }

    private init() {
        let d = UserDefaults.standard
        self.silenceCallsNotInVault = d.bool(forKey: Keys.silenceCallsNotInVault)
        self.flagSmsNotInVault = d.bool(forKey: Keys.flagSmsNotInVault)
    }
}
