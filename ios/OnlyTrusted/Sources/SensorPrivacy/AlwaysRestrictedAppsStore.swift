// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation

/// User-maintained checklist of apps the user has personally denied mic
/// and / or camera access to. Nothing in iOS exposes the real cross-app
/// permission state to third-party apps — this list is **self-reported**.
///
/// The screen that reads from this store is required to label the content
/// as "apps you've told us are restricted" rather than claiming the OS has
/// enforced it. See docs/SENSOR_PRIVACY_ARCHITECTURE.md § "Tier 1 Consumer
/// (iOS)" for the honest-claims rationale.
///
/// Storage: UserDefaults with JSON encoding. No cloud sync. Simple on
/// purpose — if the user factory-resets or uninstalls, the list is gone,
/// which is correct given the data is informational, not security-grade.
struct RestrictedAppEntry: Codable, Identifiable, Equatable {
    let id: UUID
    var displayName: String
    /// True = user says they revoked mic access for this app.
    var micRestricted: Bool
    /// True = user says they revoked camera access for this app.
    var cameraRestricted: Bool
    var noteDate: Date

    init(displayName: String, micRestricted: Bool, cameraRestricted: Bool) {
        self.id = UUID()
        self.displayName = displayName
        self.micRestricted = micRestricted
        self.cameraRestricted = cameraRestricted
        self.noteDate = Date()
    }
}

@MainActor
final class AlwaysRestrictedAppsStore: ObservableObject {
    static let shared = AlwaysRestrictedAppsStore()
    @Published private(set) var entries: [RestrictedAppEntry] = []

    private let defaultsKey = "sensor_privacy.restricted_apps.v1"

    private init() {
        load()
    }

    func add(name: String, micRestricted: Bool, cameraRestricted: Bool) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        // Dedupe by case-insensitive display name — the user shouldn't be
        // able to add "Instagram" twice.
        if entries.contains(where: { $0.displayName.caseInsensitiveCompare(trimmed) == .orderedSame }) {
            return
        }
        let entry = RestrictedAppEntry(
            displayName: trimmed,
            micRestricted: micRestricted,
            cameraRestricted: cameraRestricted,
        )
        entries.append(entry)
        persist()
    }

    func remove(id: UUID) {
        entries.removeAll { $0.id == id }
        persist()
    }

    func toggleMic(id: UUID) {
        guard let i = entries.firstIndex(where: { $0.id == id }) else { return }
        entries[i].micRestricted.toggle()
        entries[i].noteDate = Date()
        persist()
    }

    func toggleCamera(id: UUID) {
        guard let i = entries.firstIndex(where: { $0.id == id }) else { return }
        entries[i].cameraRestricted.toggle()
        entries[i].noteDate = Date()
        persist()
    }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: defaultsKey),
              let decoded = try? JSONDecoder().decode([RestrictedAppEntry].self, from: data)
        else { return }
        self.entries = decoded
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(entries) else { return }
        UserDefaults.standard.set(data, forKey: defaultsKey)
    }
}
