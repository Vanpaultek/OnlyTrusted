// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation

/// Smart Link Guard — platform-agnostic URL safety rules engine (iOS half).
///
/// The Kotlin twin lives at
/// `android/app/src/main/java/com/onlytrusted/app/ui/smartlinkguard/SmartLinkGuardRules.kt`
/// and must stay in sync (same rule set, shortener/allowlist/suspicious-TLD data,
/// same verdict logic). Everything runs 100% on-device — no network calls.
///
/// Rules (shared with Android):
///   1. Scheme check — flag non-HTTPS (http://, ftp://, etc.) as "insecure".
///   2. Known shortener — 50+ domains that hide the real destination.
///   3. Suspicious TLD — notorious scammer TLDs (.zip, .mov, .xyz, .top, etc.).
///   4. Punycode / IDN — host containing `xn--` → possible homograph attack.
///   5. IP literal — host is an IPv4/IPv6 literal (no DNS).
///   6. Known-good allowlist — ~200 top legit domains → "verified allowlist".
///   7. Typo-squat — Levenshtein ≤ 2 vs. an allowlist entry → "possible typosquat".
///   8. Suspicious path keywords — /login, /verify, /secure, /account, /wallet,
///      /paypal, /signin on a non-allowlisted or shortened host.
///   9. Embedded user:password in URL authority → almost always phishing.
///  10. Non-Punycode Unicode homograph — Cyrillic / Greek lookalike chars
///      sneaking into a host (`danger` on their own; `caution` if the host is
///      otherwise fine so legit `münchen.de` isn't blocked).
///  11. Brand-as-subdomain — a well-known brand name appearing as a sub-label
///      under an unrelated eTLD+1 (`paypal.com.scam.ru`).
///  12. Weighted escalation — two or more independent `caution` reasons
///      aggregate to `danger`. One flag alone stays at its original level.
public enum URLVerdict: Int, Comparable, Sendable {
    case safe = 0
    case caution = 1
    case danger = 2

    public static func < (lhs: URLVerdict, rhs: URLVerdict) -> Bool { lhs.rawValue < rhs.rawValue }

    public var badgeText: String {
        switch self {
        case .safe: return "Safe"
        case .caution: return "Caution"
        case .danger: return "Danger"
        }
    }
}

public struct URLSafetyResult: Equatable, Sendable {
    public let url: String
    public let verdict: URLVerdict
    public let reasons: [String]
}

public enum SmartLinkGuardRules {

    /// Injection point for an external threat-intel source. The main-app target wires
    /// this to `ThreatIntelDatabase.shared.isPhishingHost` so a bundled APWG match
    /// bumps the verdict to `.danger`. Extensions that can't link ThreatIntelDatabase
    /// leave it nil and keep working with the built-in rules alone.
    public nonisolated(unsafe) static var phishingHostLookup: (@Sendable (String) -> Bool)?

    // MARK: - Public API

    public static func inspect(_ rawUrl: String) -> URLSafetyResult {
        let trimmed = rawUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return URLSafetyResult(url: rawUrl, verdict: .danger, reasons: ["Empty link."])
        }

        let normalised = trimmed.contains("://") ? trimmed : "http://" + trimmed

        var reasons: [String] = []
        var worst: URLVerdict = .safe
        // Weighted scoring: count caution-level reasons so N≥2 escalates to danger.
        var cautionCount = 0

        // 10a — Non-Punycode Unicode homograph on the raw input. Run BEFORE
        // URLComponents parse because Foundation silently normalizes or rejects
        // some non-ASCII hostnames and we'd miss the attack signal entirely.
        // Scope to the raw host portion so paths like /café/menu don't false-trigger.
        let rawHost = extractRawHost(trimmed)
        let rawHostHasHomoglyph = containsHomoglyph(rawHost)
        if rawHostHasHomoglyph {
            reasons.append("Host contains Unicode lookalike characters — possible homograph attack.")
            worst = bump(worst, .caution)
            cautionCount += 1
        }

        guard let components = URLComponents(string: normalised) else {
            return URLSafetyResult(
                url: rawUrl,
                verdict: worst == .safe ? .danger : worst,
                reasons: reasons + ["Can't parse as a URL."]
            )
        }

        // 9 — embedded user:password (RFC-3986 userinfo). Legitimate modern sites
        // never do this; browsers warn aggressively. Treat as always-danger.
        if (components.user?.isEmpty == false) || (components.password?.isEmpty == false) {
            reasons.append("Contains embedded username/password — classic credential-harvest shape.")
            worst = bump(worst, .danger)
        }

        // 1 — scheme
        let scheme = components.scheme?.lowercased()
        switch scheme {
        case .none:
            reasons.append("Missing scheme — anyone could redirect you anywhere.")
            worst = bump(worst, .caution)
            cautionCount += 1
        case "https":
            break
        case "http":
            reasons.append("Insecure http:// — any Wi-Fi can tamper with the page.")
            worst = bump(worst, .caution)
            cautionCount += 1
        case "ftp", "telnet", "file":
            reasons.append("Legacy \(scheme!):// scheme — avoid.")
            worst = bump(worst, .caution)
            cautionCount += 1
        default:
            break
        }

        guard var host = components.host?.lowercased(), !host.isEmpty else {
            reasons.append("No host — not a real website address.")
            return URLSafetyResult(url: rawUrl, verdict: .danger, reasons: reasons)
        }
        if host.hasPrefix("www.") { host.removeFirst(4) }

        // 5 — IP literal
        if isIPLiteral(host) {
            reasons.append("Direct IP address — real sites use their domain name.")
            worst = bump(worst, .danger)
        }

        // 4 — punycode / IDN
        if host.contains("xn--") {
            reasons.append("Punycode detected — possible homograph (lookalike) attack.")
            worst = bump(worst, .danger)
        }

        let etldPlusOne = eTLDPlusOne(host)

        // 6 — allowlist
        let allowlistHit = allowlist.contains(etldPlusOne)
        if allowlistHit {
            reasons.append("Verified allowlist — matches a known legitimate domain.")
        }

        // 2 — shortener
        let shortenerHit = shorteners.contains(etldPlusOne)
        if shortenerHit {
            reasons.append("Shortened link — the real destination is hidden.")
            worst = bump(worst, .caution)
            cautionCount += 1
        }

        // 3 — suspicious TLD
        if let dotIdx = etldPlusOne.lastIndex(of: ".") {
            let tld = String(etldPlusOne[etldPlusOne.index(after: dotIdx)...])
            if !tld.isEmpty, suspiciousTLDs.contains(tld) {
                reasons.append(".\(tld) is a suspicious top-level domain often used by scammers.")
                worst = bump(worst, .caution)
                cautionCount += 1
            }
        }

        // 10b — Non-Punycode Unicode homograph on the parsed host. Only fires
        // when the raw-input check above didn't already flag it (avoids double-
        // counting the same attack).
        if !rawHostHasHomoglyph, containsHomoglyph(host) {
            reasons.append("Host contains Unicode lookalike characters — possible homograph attack.")
            worst = bump(worst, .caution)
            cautionCount += 1
        }

        // 11 — Brand-as-subdomain. If any well-known brand (first label of an
        // allowlisted eTLD+1) appears as a sub-label on an unrelated eTLD+1, the
        // user is almost certainly looking at a phishing shape like
        // `paypal.com.scam.ru`. Skip when the host itself is on the allowlist (we
        // don't want `support.paypal.com` to self-flag).
        if !allowlistHit, let impersonated = brandMisusedAsSubdomain(host: host, etldPlusOne: etldPlusOne) {
            reasons.append("Well-known brand \"\(impersonated)\" used as a subdomain of an unrelated domain — phishing shape.")
            worst = bump(worst, .danger)
        }

        // 7 — typosquat
        if !allowlistHit, let match = closestAllowlistMatch(etldPlusOne) {
            let plural = match.distance == 1 ? "" : "s"
            reasons.append("Possible typosquat of \(match.match) (\(match.distance) edit\(plural) away).")
            worst = bump(worst, .danger)
        }

        // 7b — Offline Threat Intel (Elite). Only consulted when the main-app target
        // has wired `phishingHostLookup`. Any hit forces .danger.
        if let lookup = phishingHostLookup, lookup(etldPlusOne) {
            reasons.append("Host appears in the bundled phishing-URL feed.")
            worst = bump(worst, .danger)
        }

        // 8 — suspicious path keywords
        let path = components.percentEncodedPath.lowercased()
        if !path.isEmpty {
            if let keyword = suspiciousPathKeywords.first(where: { path.contains("/" + $0) }),
               (!allowlistHit || shortenerHit) {
                reasons.append("Path contains \"\(keyword)\" on an unverified host — classic phishing shape.")
                worst = bump(worst, .caution)
                cautionCount += 1
            }
        }

        // 12 — Weighted escalation. Two or more independent caution-level flags
        // on the same URL (e.g. http + shortener + suspicious TLD) aggregate into
        // danger. One flag alone stays at its original level so legitimate
        // internationalized or http sites aren't blanket-blocked.
        if worst == .caution, cautionCount >= 2 {
            reasons.append("Multiple caution flags on the same link — escalated to Danger.")
            worst = .danger
        }

        if reasons.isEmpty {
            reasons.append("No red flags from on-device checks.")
        }

        return URLSafetyResult(url: rawUrl, verdict: worst, reasons: reasons)
    }

    public static func extractUrls(_ text: String) -> [String] {
        guard !text.isEmpty else { return [] }
        var out: [String] = []
        let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue)
        if let detector {
            let range = NSRange(text.startIndex..<text.endIndex, in: text)
            detector.enumerateMatches(in: text, options: [], range: range) { match, _, _ in
                if let match, let r = Range(match.range, in: text) {
                    out.append(String(text[r]))
                }
            }
        }
        return out
    }

    // MARK: - Internals

    private static func bump(_ current: URLVerdict, _ next: URLVerdict) -> URLVerdict {
        next.rawValue > current.rawValue ? next : current
    }

    /// Detects the common Cyrillic / Greek lookalikes attackers substitute for
    /// Latin ASCII to clone a brand name. Not an exhaustive Unicode confusables
    /// table — just the characters that are visually identical to common ASCII
    /// at every rendered font size, which is where the real phishing risk lives.
    private static let homoglyphCodepoints: Set<Character> = [
        // Cyrillic lookalikes for Latin a c e o p x y
        "а", "с", "е", "о", "р", "х", "у",
        // Greek lookalikes for Latin alpha / omicron / rho / etc.
        "α", "ο", "ρ", "ν", "κ", "τ",
        // Full-width ASCII (CJK rendering)
        "ａ", "ｂ", "ｃ", "ｇ", "ｏ", "ｐ"
    ]

    static func containsHomoglyph(_ host: String) -> Bool {
        host.contains(where: { homoglyphCodepoints.contains($0) })
    }

    /// Extract the host portion of a URL-ish string by hand, bypassing
    /// `URLComponents` (which can silently normalize non-ASCII hostnames away
    /// before our rules see them). Returns everything between the scheme
    /// separator and the next path / query / fragment / port delimiter.
    static func extractRawHost(_ rawInput: String) -> String {
        var s = rawInput.trimmingCharacters(in: .whitespacesAndNewlines)
        if let r = s.range(of: "://") {
            s = String(s[r.upperBound...])
        }
        if let r = s.range(of: "@") {
            s = String(s[r.upperBound...])
        }
        let cutIdxs = ["/", "?", "#", ":"].compactMap { delim in
            s.range(of: delim)?.lowerBound
        }
        if let earliest = cutIdxs.min() {
            s = String(s[..<earliest])
        }
        return s
    }

    /// Returns the brand name (first label of an allowlisted eTLD+1) that
    /// appears as a sub-label of [host] when the host's own eTLD+1 is NOT on
    /// the allowlist. Catches `paypal.com.scam.ru` → "paypal".
    static func brandMisusedAsSubdomain(host: String, etldPlusOne: String) -> String? {
        guard !allowlist.contains(etldPlusOne) else { return nil }
        let labels = host.split(separator: ".").map(String.init)
        guard labels.count > 2 else { return nil }
        let brandLabels: Set<String> = Set(allowlist.compactMap { domain in
            domain.split(separator: ".").first.map(String.init)
        })
        // Walk sub-labels (strip the last two which are the eTLD+1 of the host).
        for label in labels.dropLast(2) {
            if brandLabels.contains(label) { return label }
        }
        return nil
    }

    private static func isIPLiteral(_ host: String) -> Bool {
        if host.hasPrefix("[") && host.hasSuffix("]") { return true } // IPv6
        let parts = host.split(separator: ".")
        guard parts.count == 4 else { return false }
        return parts.allSatisfy { p in
            guard let i = Int(p) else { return false }
            return (0...255).contains(i)
        }
    }

    private static func eTLDPlusOne(_ host: String) -> String {
        let parts = host.split(separator: ".")
        if parts.count <= 2 { return host }
        return parts.suffix(2).joined(separator: ".")
    }

    private struct Candidate {
        let match: String
        let distance: Int
    }

    private static func closestAllowlistMatch(_ host: String) -> Candidate? {
        guard host.count >= 4 else { return nil }
        var best: Candidate?
        for entry in allowlist {
            if abs(entry.count - host.count) > 2 { continue }
            if entry == host { continue }
            let d = levenshtein(entry, host, cutoff: 2)
            if (1...2).contains(d), best == nil || d < best!.distance {
                best = Candidate(match: entry, distance: d)
                if d == 1 { break }
            }
        }
        return best
    }

    /// Levenshtein distance with early-exit cutoff.
    private static func levenshtein(_ a: String, _ b: String, cutoff: Int) -> Int {
        let aChars = Array(a)
        let bChars = Array(b)
        let m = aChars.count
        let n = bChars.count
        if abs(m - n) > cutoff { return cutoff + 1 }
        if m == 0 { return n }
        if n == 0 { return m }

        var prev = Array(0...n)
        var curr = Array(repeating: 0, count: n + 1)
        for i in 1...m {
            curr[0] = i
            var rowMin = curr[0]
            for j in 1...n {
                let cost = aChars[i - 1] == bChars[j - 1] ? 0 : 1
                curr[j] = min(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost
                )
                if curr[j] < rowMin { rowMin = curr[j] }
            }
            if rowMin > cutoff { return cutoff + 1 }
            swap(&prev, &curr)
        }
        return prev[n]
    }

    // MARK: - Data

    static let shorteners: Set<String> = [
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "buff.ly", "is.gd", "soo.gd",
        "s.id", "ow.ly", "cutt.ly", "rb.gy", "shorturl.at", "lnkd.in", "tiny.cc",
        "bl.ink", "clck.ru", "x.co", "db.tt", "qr.ae", "adf.ly", "mcaf.ee",
        "po.st", "u.to", "j.mp", "trib.al", "yourls.org", "shorte.st", "zpr.io",
        "chl.li", "fb.me", "youtu.be", "snip.ly", "sptfy.com", "amzn.to", "l.ead.me",
        "linktr.ee", "shortly.app", "surl.li", "mcaf.in", "v.gd", "shrunken.com",
        "rebrand.ly", "ouo.io", "shrtco.de", "4a.gs", "7.ly", "9nl.me", "a.gy",
        "aa.cx", "ad.vu", "ad7.biz", "al.ly", "bc.vc", "bitly.com", "bitly.ws",
        "cli.gs", "da.gd", "doi.co", "fff.to", "flic.kr", "hyperurl.co", "ity.im",
        "kck.st", "lnk.to", "mcaf.ly", "mz.cm", "n9.cl", "nyti.ms", "p.ost.im",
        "post.ly", "qpon.in", "redd.it", "reduceurl.com", "s2r.co", "sgb.gl",
        "shar.es", "shrten.io", "shrt.li", "sl.ink", "snurl.com", "spoti.fi",
        "spr.ly", "sq.ly", "ssu.rl", "swll.to", "sz.ag", "t.ly", "tcrn.ch",
        "tgr.ph", "tiny.pl", "tinylink.net", "tinyme.so", "tnw.co", "twt.si",
        "u.nu", "upfurl.com", "urlz.fr", "urly.it", "vk.cc", "vlst.co", "wa.me",
        "wp.me", "xoxo.gl", "y2u.be", "zc.vg", "zi.pe", "zws.im", "cs.pn",
        "nyp.st", "wapo.st", "ebay.us", "amzn.com", "apple.co", "msft.it"
    ]

    static let suspiciousTLDs: Set<String> = [
        "zip", "mov", "xyz", "top", "click", "loan", "party", "gq", "tk", "ml", "cf", "ga",
        "country", "stream", "download", "racing", "science", "work", "men", "date",
        "accountant", "faith", "review", "trade", "win", "cricket", "biz"
    ]

    static let suspiciousPathKeywords: [String] = [
        "login", "verify", "secure", "account", "wallet", "paypal", "signin",
        "sign-in", "update-password", "reset-password", "unlock"
    ]

    /// ~200 top legit eTLD+1s. Keep in sync with SmartLinkGuardRules.kt.
    static let allowlist: Set<String> = [
        // Big tech
        "google.com", "youtube.com", "apple.com", "icloud.com", "microsoft.com",
        "live.com", "outlook.com", "office.com", "office365.com", "bing.com",
        "amazon.com", "aws.amazon.com", "meta.com", "facebook.com", "instagram.com",
        "whatsapp.com", "threads.net", "x.com", "twitter.com", "linkedin.com",
        "github.com", "gitlab.com", "bitbucket.org", "stackoverflow.com", "reddit.com",
        "pinterest.com", "tiktok.com", "snapchat.com", "discord.com", "slack.com",
        "zoom.us", "dropbox.com", "box.com", "notion.so", "figma.com", "canva.com",
        "openai.com", "chatgpt.com", "anthropic.com", "claude.ai", "huggingface.co",
        "nvidia.com", "intel.com", "amd.com", "oracle.com", "ibm.com", "sap.com",
        "salesforce.com", "atlassian.com", "jira.com", "trello.com", "zendesk.com",
        "intercom.com", "hubspot.com", "mailchimp.com", "sendgrid.com", "twilio.com",
        "stripe.com", "square.com", "squareup.com", "shopify.com", "etsy.com",
        "ebay.com", "walmart.com", "target.com", "costco.com", "bestbuy.com",
        "homedepot.com", "lowes.com", "ikea.com", "wayfair.com",

        // Banks / finance
        "chase.com", "bankofamerica.com", "wellsfargo.com", "citibank.com", "citi.com",
        "capitalone.com", "usbank.com", "pnc.com", "truist.com", "ally.com",
        "discover.com", "americanexpress.com", "amex.com", "paypal.com", "venmo.com",
        "cashapp.com", "zellepay.com", "robinhood.com", "fidelity.com", "schwab.com",
        "vanguard.com", "coinbase.com", "kraken.com", "binance.com", "gemini.com",

        // Carriers / telco
        "att.com", "verizon.com", "verizonwireless.com", "t-mobile.com", "sprint.com",
        "uscellular.com", "cricketwireless.com", "metrobyt-mobile.com", "boostmobile.com",
        "mintmobile.com", "visible.com", "spectrum.net", "xfinity.com", "comcast.com",
        "cox.com", "optimum.com",

        // Shipping / travel
        "ups.com", "fedex.com", "usps.com", "dhl.com", "uber.com", "lyft.com",
        "doordash.com", "grubhub.com", "ubereats.com", "airbnb.com", "booking.com",
        "expedia.com", "kayak.com", "hotels.com", "united.com", "delta.com",
        "aa.com", "southwest.com", "jetblue.com", "alaskaair.com",

        // News / media
        "nytimes.com", "wsj.com", "washingtonpost.com", "bbc.com", "bbc.co.uk",
        "cnn.com", "reuters.com", "apnews.com", "theverge.com", "techcrunch.com",
        "arstechnica.com", "wired.com", "bloomberg.com", "ft.com", "economist.com",

        // Gov / health
        "irs.gov", "usa.gov", "ssa.gov", "medicare.gov", "cdc.gov", "nih.gov",
        "fda.gov", "state.gov", "whitehouse.gov",

        // Education / reference
        "wikipedia.org", "wikimedia.org", "khanacademy.org", "coursera.org", "edx.org",

        // Dev / tools
        "cloudflare.com", "digitalocean.com", "linode.com", "vercel.com", "netlify.com",
        "heroku.com", "render.com", "fly.io", "docker.com", "npmjs.com", "pypi.org",
        "rubygems.org", "gradle.org", "maven.org", "jetbrains.com", "android.com",
        "developer.apple.com", "mozilla.org", "firefox.com", "chromium.org",

        // Internal
        "vanpaultek.com", "57million.com", "onlytrusted.app", "splntr.app"
    ]
}
