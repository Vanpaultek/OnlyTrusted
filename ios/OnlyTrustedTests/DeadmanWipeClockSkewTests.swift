// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import XCTest

/// Pure-logic verification of the Dead-Man Wipe clock-skew defense formula.
/// Mirror of `android/.../DeadmanWipeClockSkewTest.kt`. The real
/// `DeadmanWipeState.shouldWipeNow` depends on `UserDefaults` and the live
/// system clock, so we extract the pure policy here and verify it in
/// isolation. The code shape here MUST stay in sync with the production
/// `shouldWipeNow` — any divergence is a bug.
final class DeadmanWipeClockSkewTests: XCTestCase {

    /// Mirror of `DeadmanWipeState.shouldWipeNow` clock-skew logic.
    private func shouldWipe(
        now: TimeInterval,
        lastUnlockWall: TimeInterval,
        nowMonotonic: TimeInterval,
        lastUnlockMonotonic: TimeInterval,
        storedBoot: TimeInterval,
        intervalSeconds: TimeInterval
    ) -> Bool {
        let wallElapsed = now - lastUnlockWall
        let currentBoot = now - nowMonotonic
        let sameBoot = storedBoot > 0 && abs(currentBoot - storedBoot) < 60
        let monoElapsed = sameBoot ? (nowMonotonic - lastUnlockMonotonic) : 0
        let effectiveElapsed = max(wallElapsed, monoElapsed)
        return effectiveElapsed >= intervalSeconds
    }

    private let day: TimeInterval = 86_400

    func testFiresWhenWallClockCrossesIntervalNormally() {
        let now = 100 * day
        let lastWall = 93 * day
        let fired = shouldWipe(
            now: now,
            lastUnlockWall: lastWall,
            nowMonotonic: 10_000,
            lastUnlockMonotonic: 5_000,
            storedBoot: now - 10_000,
            intervalSeconds: 7 * day
        )
        XCTAssertTrue(fired, "wall-clock elapsed 7 days should fire 7-day timer")
    }

    func testFiresWhenUserRewindsClockButMonotonicMarchesOn() {
        let intervalSeconds = 7 * day
        let realNow: TimeInterval = 100 * day
        // Attacker set the device clock back by 10 days to delay the wipe
        let attackedWall = realNow - 10 * day
        // Monotonic elapsed is honest — 8 days of real uptime since last unlock
        let fired = shouldWipe(
            now: attackedWall,                       // the fake "now" the clock shows
            lastUnlockWall: attackedWall - day,      // wall-clock says only 1 day elapsed
            nowMonotonic: 8 * day,                   // 8 days of real uptime
            lastUnlockMonotonic: 0,
            storedBoot: attackedWall - 8 * day,      // boot time stays consistent on same boot
            intervalSeconds: intervalSeconds
        )
        XCTAssertTrue(fired, "monotonic 8 days should fire 7-day timer even if wall clock rewound")
    }

    func testDoesNotFireWithinIntervalOnNormalWallClock() {
        let now = 100 * day
        let fired = shouldWipe(
            now: now,
            lastUnlockWall: now - 3 * day,
            nowMonotonic: 3 * day,
            lastUnlockMonotonic: 0,
            storedBoot: now - 3 * day,
            intervalSeconds: 7 * day
        )
        XCTAssertFalse(fired, "3 days of elapsed time should not fire a 7-day timer")
    }

    func testAfterRebootFallsBackToWallClockOnly() {
        let now = 100 * day
        // Last unlock was 10 days ago wall-clock, but device rebooted since
        let fired = shouldWipe(
            now: now,
            lastUnlockWall: now - 10 * day,
            nowMonotonic: 1_000,                     // just booted, 1k seconds uptime
            lastUnlockMonotonic: 5 * day,            // irrelevant now
            storedBoot: now - 15 * day,              // old boot time, way out of 60s tolerance
            intervalSeconds: 7 * day
        )
        XCTAssertTrue(fired, "after reboot, wall clock 10 days should still fire 7-day timer")
    }

    func testBootTimeWithin60SecondToleranceIsConsideredSameBoot() {
        let now = 100 * day
        let fired = shouldWipe(
            now: now,
            lastUnlockWall: now - 3 * day,
            nowMonotonic: 8 * day,                   // monotonic says 8 days
            lastUnlockMonotonic: 0,
            // Boot time stored vs. computed differs by only 30 seconds — same boot
            storedBoot: (now - 8 * day) + 30,
            intervalSeconds: 7 * day
        )
        XCTAssertTrue(fired, "tolerance window should allow monotonic to win")
    }

    func testMissingStoredBootMeansWallClockOnlyMode() {
        let now = 100 * day
        let fired = shouldWipe(
            now: now,
            lastUnlockWall: now - 10 * day,
            nowMonotonic: 1_000,
            lastUnlockMonotonic: 0,
            storedBoot: 0,                           // never stored — pre-patch install
            intervalSeconds: 7 * day
        )
        XCTAssertTrue(fired, "pre-patch installs still fire via wall clock")
    }
}
