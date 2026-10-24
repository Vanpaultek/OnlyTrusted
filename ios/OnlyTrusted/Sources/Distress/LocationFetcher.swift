// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import CoreLocation
import Foundation

/// One-shot best-effort location fetch for the Distress Signal flow.
///
/// Strategy:
///   1. If a cached fix from the system is < 30 s old, use it immediately (no new fix).
///   2. Otherwise start CLLocationManager with `.requestLocation()` which fires exactly
///      one update then stops. Wrap in a 3-second timeout — in a real emergency, waiting
///      30+ s for a perfect GPS fix defeats the purpose; a stale cached fix is better
///      than nothing, a current rough network fix is better than a precise one that
///      arrives after the user is incapacitated.
///   3. If authorisation hasn't been granted, return nil so the sender dispatches the
///      SMS with "Location: unavailable" — the signal still fires.
@MainActor
final class DistressLocationFetcher: NSObject {

    static let shared = DistressLocationFetcher()

    private let manager = CLLocationManager()
    private var continuation: CheckedContinuation<DistressLocation?, Never>?
    private var hasResumed = false
    private var timeoutTask: Task<Void, Never>?

    override init() {
        super.init()
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.delegate = self
    }

    /// Returns the best current location within `timeoutSeconds`, or nil if nothing came
    /// back / permission denied.
    func fetchOnce(timeoutSeconds: TimeInterval = 3.0) async -> DistressLocation? {
        // Short-circuit on a fresh cached fix.
        if let cached = manager.location,
           Date().timeIntervalSince(cached.timestamp) < 30 {
            return cached.toDistressLocation()
        }

        let status = manager.authorizationStatus
        if status == .denied || status == .restricted {
            return nil
        }
        if status == .notDetermined {
            manager.requestWhenInUseAuthorization()
            // Give the system prompt a brief moment to be answered; if the user says no
            // or the prompt is dismissed, requestLocation below will fail silently and
            // the timeout path will fire with nil.
            try? await Task.sleep(nanoseconds: 500_000_000)
        }

        return await withCheckedContinuation { cont in
            self.continuation = cont
            self.hasResumed = false
            self.manager.requestLocation()
            self.timeoutTask?.cancel()
            self.timeoutTask = Task { [weak self] in
                try? await Task.sleep(nanoseconds: UInt64(timeoutSeconds * 1_000_000_000))
                self?.resumeOnce(with: self?.manager.location?.toDistressLocation())
            }
        }
    }

    private func resumeOnce(with value: DistressLocation?) {
        guard !hasResumed else { return }
        hasResumed = true
        timeoutTask?.cancel()
        timeoutTask = nil
        continuation?.resume(returning: value)
        continuation = nil
    }
}

extension DistressLocationFetcher: @preconcurrency CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        resumeOnce(with: locations.last?.toDistressLocation())
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        resumeOnce(with: nil)
    }
}

private extension CLLocation {
    func toDistressLocation() -> DistressLocation {
        DistressLocation(
            latitude: coordinate.latitude,
            longitude: coordinate.longitude,
            accuracyMeters: horizontalAccuracy >= 0 ? horizontalAccuracy : nil,
            capturedAt: timestamp
        )
    }
}
