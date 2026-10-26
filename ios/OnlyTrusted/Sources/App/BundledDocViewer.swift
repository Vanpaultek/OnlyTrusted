// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI
@preconcurrency import WebKit

/// Loads a bundled static HTML site inside a WKWebView with offline file access
/// so relative paths (assets/, content.json, plans.json) resolve. Used by the
/// in-app User Guide and Pricing Chart — both ship as resource folders so they
/// work 100% offline (matches the zero-cloud ethos).
struct BundledDocViewer: UIViewRepresentable {
    /// Folder name under the app bundle root (e.g. "guide" or "pricing").
    let folder: String

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.defaultWebpagePreferences.allowsContentJavaScript = true
        let web = WKWebView(frame: .zero, configuration: config)
        web.isOpaque = false
        web.backgroundColor = UIColor(Color.appScreenBackground)
        web.scrollView.backgroundColor = UIColor(Color.appScreenBackground)
        return web
    }

    func updateUIView(_ web: WKWebView, context: Context) {
        guard let index = Bundle.main.url(forResource: "index",
                                          withExtension: "html",
                                          subdirectory: folder)
        else { return }
        // allowingReadAccessTo the folder root lets fetch() reach content.json /
        // plans.json / assets/*.webp from the WKWebView's file:// origin.
        web.loadFileURL(index, allowingReadAccessTo: index.deletingLastPathComponent())
    }
}

/// Settings → About → "User Guide" entry. Loads docs/guide bundled under
/// Resources/guide/index.html.
struct UserGuideScreen: View {
    var body: some View {
        BundledDocViewer(folder: "guide")
            .ignoresSafeArea(edges: .bottom)
            .navigationTitle("User Guide")
            .navigationBarTitleDisplayMode(.inline)
            .background(Color.appScreenBackground.ignoresSafeArea())
    }
}

/// Subscription screens → "See full pricing chart" link. Loads docs/pricing
/// bundled under Resources/pricing/index.html.
struct PricingChartScreen: View {
    var body: some View {
        BundledDocViewer(folder: "pricing")
            .ignoresSafeArea(edges: .bottom)
            .navigationTitle("Pricing Chart")
            .navigationBarTitleDisplayMode(.inline)
            .background(Color.appScreenBackground.ignoresSafeArea())
    }
}
