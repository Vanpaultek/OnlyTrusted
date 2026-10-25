// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import XCTest
@testable import OnlyTrusted

/// Pure-logic regression tests for the PIN lockout curve. Mirror of
/// `android/.../PINRateLimiterTest.kt`. The Keychain-backed recordFailure /
/// recordSuccess paths need device integration and are covered by the
/// manual verification step in `docs/RELEASE_GATES.md` Gate 5.
final class PINRateLimiterTests: XCTestCase {

    func testNoLockoutBefore5Failures() {
        for i in 1...4 {
            XCTAssertEqual(PINRateLimiter.penaltySeconds(for: i), 0, "at \(i)")
        }
    }

    func testFirstLockoutAt5FailuresIs60Seconds() {
        XCTAssertEqual(PINRateLimiter.penaltySeconds(for: 5), 60)
    }

    func test5To9FailuresAllMapTo60SecondLockout() {
        for i in 5...9 {
            XCTAssertEqual(PINRateLimiter.penaltySeconds(for: i), 60, "at \(i)")
        }
    }

    func test10To14FailuresEscalateTo5Minutes() {
        for i in 10...14 {
            XCTAssertEqual(PINRateLimiter.penaltySeconds(for: i), 300, "at \(i)")
        }
    }

    func test15To19FailuresEscalateTo15Minutes() {
        for i in 15...19 {
            XCTAssertEqual(PINRateLimiter.penaltySeconds(for: i), 900, "at \(i)")
        }
    }

    func test20AndAboveAllMapTo60Minutes() {
        for i in [20, 25, 50, 100, 1000] {
            XCTAssertEqual(PINRateLimiter.penaltySeconds(for: i), 3600, "at \(i)")
        }
    }

    func testCurveIsMonotonicNonDecreasing() {
        var previous: TimeInterval = 0
        for i in 1...30 {
            let current = PINRateLimiter.penaltySeconds(for: i)
            XCTAssertGreaterThanOrEqual(
                current, previous,
                "penalty should never shrink: at \(i) is \(current), was \(previous)"
            )
            previous = current
        }
    }
}
