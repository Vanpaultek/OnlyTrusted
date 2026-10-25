import SwiftUI
import WebKit
import StoreKit

// Add index.html, styles.css, app.js and assets/logo.svg as folder resources.
// This sample uses StoreKit 2. Confirm the Product IDs and enable IAP in App Store Connect.
struct OnlyTrustedPaywallView: UIViewRepresentable {
    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let controller = WKUserContentController()
        controller.add(context.coordinator, name: "onlyTrustedPurchase")
        let configuration = WKWebViewConfiguration()
        configuration.userContentController = controller
        let view = WKWebView(frame: .zero, configuration: configuration)
        context.coordinator.webView = view
        if let page = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "PricingPages") {
            view.loadFileURL(page, allowingReadAccessTo: page.deletingLastPathComponent())
        }
        return view
    }
    func updateUIView(_ uiView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKScriptMessageHandler {
        weak var webView: WKWebView?
        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            guard message.name == "onlyTrustedPurchase",
                  let payload = message.body as? [String: Any],
                  let productID = payload["appleProductId"] as? String,
                  productID.hasPrefix("com.onlytrusted.app.") else { return }
            Task { @MainActor in
                do {
                    let matches = try await Product.products(for: [productID])
                    guard let product = matches.first else { throw PurchaseError.productUnavailable }
                    let outcome = try await product.purchase()
                    switch outcome {
                    case .success(let result):
                        switch result {
                        case .verified(let transaction):
                            // IMPORTANT: send signed transaction info to backend for validation
                            // and grant entitlements based on a validated subscription state.
                            await transaction.finish()
                            self.alert("Purchase verified on device. Sync membership with your server.")
                        case .unverified: self.alert("Purchase could not be verified.")
                        }
                    case .userCancelled: break
                    case .pending: self.alert("Purchase pending approval.")
                    @unknown default: self.alert("Unknown purchase outcome.")
                    }
                } catch { self.alert("Unable to complete purchase: \(error.localizedDescription)") }
            }
        }
        @MainActor private func alert(_ text: String) {
            // JSON serialization avoids interpolating untrusted text into JS.
            guard let data = try? JSONSerialization.data(withJSONObject: [text]),
                  let json = String(data: data, encoding: .utf8) else { return }
            webView?.evaluateJavaScript("window.alert(\(json)[0])")
        }
        enum PurchaseError: Error { case productUnavailable }
    }
}
