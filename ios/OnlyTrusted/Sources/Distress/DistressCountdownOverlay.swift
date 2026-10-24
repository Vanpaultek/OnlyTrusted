// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

/// Full-screen 3-second cancel countdown shown before the distress signal fires.
///
/// UX rationale: the SOS FAB is a single tap, so accidental fires (pocket touch, child
/// tap, dropped phone bump) are a real risk. The countdown gives the owner a chance to
/// abort without punishing a real emergency with extra friction — three seconds is
/// fast enough that an owner in distress tolerates it + long enough that an accidental
/// tap can be reversed. The number counts down VERY visibly in red, dominating the
/// screen so there's no doubt about what's happening.
///
/// The cancel button is `.bordered` (less prominent) and the Fire-Now button is tinted
/// the brand danger red — if the owner wants to skip the countdown entirely they can
/// tap Send Now.
struct DistressCountdownOverlay: View {
    let totalSeconds: Int
    let onFire: () -> Void
    let onCancel: () -> Void

    @State private var remaining: Int
    @State private var firedOrCancelled = false

    init(
        totalSeconds: Int = 3,
        onFire: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.totalSeconds = totalSeconds
        self.onFire = onFire
        self.onCancel = onCancel
        self._remaining = State(initialValue: totalSeconds)
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.88).ignoresSafeArea()
            VStack(spacing: 20) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 72, weight: .bold))
                    .foregroundStyle(Color.appDanger)
                Text("Sending EMERGENCY alert")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.white)
                Text("SMS to your emergency contacts with your current location, then auto-open the dialer for the first one.")
                    .font(.system(size: 14))
                    .foregroundStyle(.white.opacity(0.8))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer().frame(height: 8)
                Text("\(remaining)")
                    .font(.system(size: 92, weight: .black, design: .rounded))
                    .foregroundStyle(Color.appDanger)
                    .contentTransition(.numericText())
                Spacer().frame(height: 8)
                Button(action: fireNow) {
                    Text("Send now")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: 52)
                        .background(RoundedRectangle(cornerRadius: 14).fill(Color.appDanger))
                }
                .buttonStyle(.plain)
                Button(action: cancelNow) {
                    Text("Cancel")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: 52)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(.white.opacity(0.5), lineWidth: 1.5)
                        )
                }
                .buttonStyle(.plain)
            }
            .padding(32)
        }
        .task {
            // Count down one second at a time; fire automatically when it hits zero
            // unless the user tapped Send Now or Cancel already.
            while remaining > 0, !firedOrCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                remaining -= 1
            }
            if !firedOrCancelled {
                firedOrCancelled = true
                onFire()
            }
        }
    }

    private func fireNow() {
        guard !firedOrCancelled else { return }
        firedOrCancelled = true
        onFire()
    }

    private func cancelNow() {
        guard !firedOrCancelled else { return }
        firedOrCancelled = true
        onCancel()
    }
}
