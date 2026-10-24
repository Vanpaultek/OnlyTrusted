// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import CoreMotion
import Foundation

/// Shake-to-trigger for the Distress Signal.
///
/// Pattern: detects at least `requiredShakes` discrete shakes within `windowSeconds`.
///   • A "shake" fires when total acceleration minus gravity exceeds `thresholdGforce`.
///   • Successive shakes must be at least `cooldownSeconds` apart so a single violent
///     motion isn't counted as several shakes (prevents false positives from drops).
///   • If the window elapses without reaching `requiredShakes`, the counter resets.
///
/// Scope: foreground-only. iOS does not allow third-party apps to receive accelerometer
/// events in the background (Background Modes `audio` / `location` work differently;
/// motion events don't). This matches the Android implementation's foreground-only
/// scope — in both cases the shake trigger only works while the user has OnlyTrusted
/// open on screen.
///
/// Honest-claim note: accelerometer data NEVER leaves the device, is NEVER logged,
/// and is only sampled while the user has OnlyTrusted open on screen. We don't retain
/// any motion history past the rolling 3-second window.
@MainActor
final class ShakeDetector {

    private let motion = CMMotionManager()
    private let queue: OperationQueue
    private var shakeTimestamps: [TimeInterval] = []
    private var lastShakeAt: TimeInterval = 0
    private let requiredShakes: Int
    private let windowSeconds: TimeInterval
    private let cooldownSeconds: TimeInterval
    private let thresholdGforce: Double
    private var onShakePatternDetected: (() -> Void)?

    init(
        requiredShakes: Int = 5,
        windowSeconds: TimeInterval = 3.0,
        cooldownSeconds: TimeInterval = 0.15,
        thresholdGforce: Double = 2.5
    ) {
        self.requiredShakes = requiredShakes
        self.windowSeconds = windowSeconds
        self.cooldownSeconds = cooldownSeconds
        self.thresholdGforce = thresholdGforce
        self.queue = OperationQueue()
        self.queue.name = "com.onlytrusted.shake-detector"
        self.queue.qualityOfService = .userInteractive
    }

    /// Start listening. Returns true if the device actually has an accelerometer and the
    /// updates were successfully started.
    @discardableResult
    func start(onShakePatternDetected: @escaping () -> Void) -> Bool {
        guard motion.isAccelerometerAvailable else { return false }
        self.onShakePatternDetected = onShakePatternDetected
        motion.accelerometerUpdateInterval = 0.03   // ~33 Hz
        motion.startAccelerometerUpdates(to: queue) { [weak self] data, _ in
            guard let self, let data else { return }
            // Hop to MainActor so we can safely touch the shared counters.
            let x = data.acceleration.x
            let y = data.acceleration.y
            let z = data.acceleration.z
            Task { @MainActor in
                self.ingest(x: x, y: y, z: z)
            }
        }
        return true
    }

    /// Stop listening. Idempotent.
    func stop() {
        motion.stopAccelerometerUpdates()
        shakeTimestamps.removeAll()
        lastShakeAt = 0
        onShakePatternDetected = nil
    }

    private func ingest(x: Double, y: Double, z: Double) {
        // CMAcceleration is already in units of g (1.0 ≈ one gravity). Subtracting 1
        // isolates the "shake" force. Magnitude handles any axis.
        let gforce = (x * x + y * y + z * z).squareRoot() - 1.0
        let now = Date().timeIntervalSince1970
        guard gforce >= thresholdGforce else { return }
        guard (now - lastShakeAt) >= cooldownSeconds else { return }
        lastShakeAt = now
        shakeTimestamps.append(now)
        // Drop timestamps that have fallen outside the rolling window.
        shakeTimestamps.removeAll { (now - $0) > windowSeconds }
        if shakeTimestamps.count >= requiredShakes {
            shakeTimestamps.removeAll()
            lastShakeAt = 0
            onShakePatternDetected?()
        }
    }
}

/// Per-user toggle persisted in UserDefaults (not sensitive).
enum ShakeTriggerPrefs {
    private static let key = "distress.shake.enabled"

    /// Default ON — foreground-only, zero battery impact when the app is backgrounded.
    static var isEnabled: Bool {
        if UserDefaults.standard.object(forKey: key) == nil { return true }
        return UserDefaults.standard.bool(forKey: key)
    }

    static func setEnabled(_ enabled: Bool) {
        UserDefaults.standard.set(enabled, forKey: key)
    }
}
