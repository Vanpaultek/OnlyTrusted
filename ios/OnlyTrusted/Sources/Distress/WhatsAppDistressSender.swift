// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation
import UIKit

/// Opens WhatsApp with the distress message pre-filled, so the owner can broadcast to
/// their emergency contact GROUP in a few extra taps after the SMS fires.
///
/// Why we can't auto-send to a group:
///   WhatsApp has no third-party API that can create a group, add members, or
///   programmatically send to a specific group chat — those actions are reserved for
///   WhatsApp's own UI (Terms of Service). The ONLY integrations WhatsApp exposes to
///   third-party apps are:
///     * `whatsapp://send?phone=<num>&text=<text>` — opens a 1-on-1 chat with the
///       number, text pre-filled, user taps Send.
///     * `whatsapp://send?text=<text>` — opens WhatsApp with text pre-filled; user
///       picks which chat to send to (contact OR group).
///     * `https://chat.whatsapp.com/<invite-id>` — opens the group preview if the user
///       isn't yet a member, OR opens the group chat if they are.
///
/// The honest flow we ship:
///   1. Owner creates a WhatsApp group with their emergency contacts, manually, the
///      first time (OnlyTrusted guides them with a setup card). Pastes the group's
///      invite link into `WhatsAppDistressPrefs.setGroupInviteURL(_:)`.
///   2. On distress trigger, after the SMS composer opens, OnlyTrusted ALSO opens
///      WhatsApp via `whatsapp://send?text=<distress-msg>` with the full message
///      pre-filled + copies the group invite link to the clipboard so the owner can
///      paste it to open the group directly (or picks the group from WhatsApp's
///      chat selector).
///   3. From the group chat, the owner can one-tap the voice-call icon to start a
///      group call to every member — using WhatsApp's own native group-call feature.
///
/// This keeps the honest-claim intact (OnlyTrusted sends zero traffic itself; everything
/// flows through the user's own WhatsApp account via standard URL schemes) and respects
/// WhatsApp's Terms of Service.
///
/// `Info.plist` needs the `LSApplicationQueriesSchemes` entry for `whatsapp` so
/// `UIApplication.canOpenURL(_:)` can tell us whether WhatsApp is installed; added in
/// the same commit that ships this sender.
@MainActor
enum WhatsAppDistressSender {

    /// Returns true if WhatsApp (consumer app) is installed. We can't detect WhatsApp
    /// Business separately via URL scheme — the `whatsapp://` scheme is handled by
    /// whichever variant is installed, so a true from either counts.
    static var isInstalled: Bool {
        guard let url = URL(string: "whatsapp://send?text=test") else { return false }
        return UIApplication.shared.canOpenURL(url)
    }

    /// Opens WhatsApp with `body` pre-filled. If the user has saved their emergency
    /// group's invite link, we also copy it to the clipboard so the owner can paste
    /// it into WhatsApp's search bar to open the group directly.
    ///
    /// Returns true if WhatsApp was launched successfully.
    @discardableResult
    static func fire(body: String) async -> Bool {
        guard isInstalled else { return false }

        // Copy the group invite link to the clipboard if saved.
        if let link = WhatsAppDistressPrefs.groupInviteURL, !link.isEmpty {
            UIPasteboard.general.string = link
        }

        let encoded = body.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? body
        guard let url = URL(string: "whatsapp://send?text=\(encoded)") else { return false }
        return await UIApplication.shared.open(url)
    }

    /// Opens the saved group invite link directly (if the user has saved one).
    @discardableResult
    static func openGroupInviteIfSaved() async -> Bool {
        guard let link = WhatsAppDistressPrefs.groupInviteURL, !link.isEmpty,
              let url = URL(string: link) else { return false }
        return await UIApplication.shared.open(url)
    }
}

/// Per-user storage for the WhatsApp emergency group's invite URL. Not sensitive.
enum WhatsAppDistressPrefs {
    private static let key = "distress.whatsapp.group_invite_url"

    /// The saved WhatsApp group invite URL, or nil / blank if unset.
    static var groupInviteURL: String? {
        UserDefaults.standard.string(forKey: key)
    }

    /// Validates + saves the group invite URL. Accepts the two valid WhatsApp invite
    /// URL formats: `https://chat.whatsapp.com/<id>` and `https://wa.me/<id>`.
    /// Returns true on success, false if the URL doesn't match either shape.
    @discardableResult
    static func setGroupInviteURL(_ url: String) -> Bool {
        let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            UserDefaults.standard.removeObject(forKey: key)
            return true
        }
        if !trimmed.hasPrefix("https://chat.whatsapp.com/") &&
            !trimmed.hasPrefix("https://wa.me/") {
            return false
        }
        UserDefaults.standard.set(trimmed, forKey: key)
        return true
    }
}
