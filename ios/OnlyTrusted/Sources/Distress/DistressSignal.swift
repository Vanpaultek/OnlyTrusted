// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation

/// Core data types + message builder for the Distress Signal feature.
///
/// The honest claim OnlyTrusted makes — "no cloud, no server, no account, zero-network" —
/// holds through this feature: OnlyTrusted sends no traffic itself. On trigger we open
/// the OS Messages composer (user taps Send once) with a message pre-filled, and open
/// the Phone app via the `tel:` URL scheme with the first emergency contact pre-dialed.
/// Both intents use the user's own carrier channels; nothing phones home.
///
/// Trigger surfaces (Phase 1 ships only the in-app button; shake trigger follows):
///   • In-app Dashboard SOS FAB with 3-second cancel countdown.
///   • Shake-to-trigger — Phase 2, via CMMotionManager accelerometer.
///   • Volume-down 4× — not possible on iOS (volume keys are sandboxed outside the app).

/// A single emergency-group recipient — a phone number + display label.
struct DistressRecipient: Equatable, Hashable {
    let phoneNumber: String
    let displayName: String
}

/// Snapshot of the device's location at the moment the distress signal fired.
struct DistressLocation {
    let latitude: Double
    let longitude: Double
    let accuracyMeters: Double?
    let capturedAt: Date

    /// Google Maps web URL that opens the pin in any browser or the Maps app.
    var mapsUrl: String {
        "https://maps.google.com/?q=\(latitude),\(longitude)"
    }
}

/// Builds the pre-filled distress message body.
enum DistressMessageBuilder {

    /// Pre-filled message the user sees in the SMS composer before tapping Send. The
    /// emoji + "EMERGENCY" prefix is deliberate — recipients should recognise it as
    /// urgent at a glance even in notification previews.
    ///
    /// If `location` is nil (fetch timed out, permission denied, or no GPS fix), we
    /// append "Location: unavailable" instead of omitting silently — the recipient
    /// knows the owner is in distress but their current coordinates can't be shared.
    static func build(
        ownerName: String,
        location: DistressLocation?,
        now: Date = Date()
    ) -> String {
        let name = ownerName.isEmpty ? "an OnlyTrusted user" : ownerName
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "MMM d, h:mm a"
        let timestamp = formatter.string(from: now)
        let locationLine: String = {
            if let location {
                let acc = location.accuracyMeters.map { " (±\(Int($0)) m)" } ?? ""
                return "Location: \(location.mapsUrl)\(acc)"
            } else {
                return "Location: unavailable"
            }
        }()
        return "🚨 OnlyTrusted EMERGENCY from \(name) — I need help. "
            + "\(locationLine). Sent \(timestamp)."
    }
}
