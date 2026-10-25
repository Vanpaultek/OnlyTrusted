# Custom-domain setup for GitHub Pages

Right now the site is live at **https://vanpaultek.github.io/OnlyTrusted/**.
This doc walks through moving it to a custom domain you own — e.g.
`vanpaultek.com`, `onlytrusted.app`, or a subdomain. **Do not commit the
`CNAME` file until DNS is working** — doing so causes GitHub Pages to
redirect the github.io URL to a domain that doesn't resolve, breaking the
live site during the gap.

The `CNAME.example` file in this directory holds the recommended content.
Rename it to `CNAME` as the final step.

---

## Three domain options

| Option | URL after setup | DNS records needed | Trade-offs |
|---|---|---|---|
| **A — Apex of your main brand** | `https://vanpaultek.com/` | 4 × A records to GitHub Pages IPs | Shortest URL. One domain to maintain. Everything under VanPaulTek. |
| **B — Dedicated product domain** | `https://onlytrusted.app/` | 4 × A records on apex OR 1 CNAME on www | Stronger product identity. Needs a second domain registration (~$12/yr for `.app`). |
| **C — Subdomain of your main brand** | `https://onlytrusted.vanpaultek.com/` | 1 × CNAME record | Simplest DNS. Keeps apex free for other uses. |

**Recommendation if you're only running OnlyTrusted:** Option A.
**Recommendation if VanPaulTek will ship multiple apps:** Option C.
**Recommendation if OnlyTrusted becomes the product brand:** Option B.

The `CNAME.example` in this folder is pre-filled with `vanpaultek.com` (Option A). Edit if you pick a different route.

---

## DNS setup — Option A (apex: `vanpaultek.com`)

Add these records at your DNS registrar (Cloudflare, Namecheap, Google Domains, etc.):

### A records (point apex to GitHub Pages)

```
Type: A    Name: @    Value: 185.199.108.153    TTL: 3600
Type: A    Name: @    Value: 185.199.109.153    TTL: 3600
Type: A    Name: @    Value: 185.199.110.153    TTL: 3600
Type: A    Name: @    Value: 185.199.111.153    TTL: 3600
```

### Optional AAAA records (IPv6)

```
Type: AAAA    Name: @    Value: 2606:50c0:8000::153    TTL: 3600
Type: AAAA    Name: @    Value: 2606:50c0:8001::153    TTL: 3600
Type: AAAA    Name: @    Value: 2606:50c0:8002::153    TTL: 3600
Type: AAAA    Name: @    Value: 2606:50c0:8003::153    TTL: 3600
```

### Optional www redirect

```
Type: CNAME   Name: www   Value: vanpaultek.github.io   TTL: 3600
```

GitHub Pages will auto-redirect `www.vanpaultek.com` → `vanpaultek.com`.

---

## DNS setup — Option B (dedicated: `onlytrusted.app`)

Same 4 A records as Option A at your `onlytrusted.app` registrar. Change the `CNAME.example` content to `onlytrusted.app` before renaming.

---

## DNS setup — Option C (subdomain: `onlytrusted.vanpaultek.com`)

```
Type: CNAME   Name: onlytrusted   Value: vanpaultek.github.io   TTL: 3600
```

That's it — single record. Change the `CNAME.example` content to `onlytrusted.vanpaultek.com` before renaming.

---

## Rollout sequence (do these IN ORDER)

```
Step 1   Add DNS records at your registrar
Step 2   Wait for DNS propagation (5–60 min; check with
         `dig vanpaultek.com` or https://dnschecker.org)
Step 3   `git mv docs/CNAME.example docs/CNAME`
         Edit the file if you chose Option B or C
Step 4   git add docs/CNAME + git commit + git push
Step 5   GitHub Pages detects the new CNAME + requests a Let's Encrypt
         certificate (takes 5–15 min)
Step 6   Verify https://vanpaultek.com/ loads the landing page
Step 7   Verify https://vanpaultek.com/pricing/ + /guide/ both load
Step 8   Verify https://vanpaultek.com/guide/#feature-sos lands + highlights
```

---

## Enforce HTTPS (post-rollout)

GitHub Pages auto-enables HTTPS once the Let's Encrypt cert is provisioned (usually within 15 min of the CNAME commit). Verify via:

```
gh api /repos/Vanpaultek/OnlyTrusted/pages --jq '.https_enforced'
# expect: true
```

If `false`, force-enable:

```
gh api --method PUT /repos/Vanpaultek/OnlyTrusted/pages \
  -F 'https_enforced=true'
```

---

## Verify after rollout

```
# A records resolved to GitHub?
dig +short vanpaultek.com @1.1.1.1
# Expected (all four): 185.199.108.153, 109.153, 110.153, 111.153

# Site responds with 200 at the custom domain?
curl -sI https://vanpaultek.com/ | head -3
curl -sI https://vanpaultek.com/pricing/ | head -3
curl -sI https://vanpaultek.com/guide/ | head -3

# Cert is Let's Encrypt?
echo | openssl s_client -connect vanpaultek.com:443 -servername vanpaultek.com 2>/dev/null \
  | openssl x509 -noout -issuer -subject -dates
# issuer should start with "C = US, O = Let's Encrypt"
```

---

## Rolling back

If something breaks + you want to revert to the `vanpaultek.github.io/OnlyTrusted/` URL temporarily:

```
git rm docs/CNAME
git commit -m "revert: drop custom-domain CNAME while investigating"
git push origin main
```

Pages will drop the custom-domain binding within ~1 min + the github.io URL
starts serving again.

If you also need to remove the custom-domain setting at the API level:

```
gh api --method PUT /repos/Vanpaultek/OnlyTrusted/pages -F 'cname='
```

---

## Shareable URLs after custom-domain is live

Deep links update predictably:

```
Before:  https://vanpaultek.github.io/OnlyTrusted/guide/#feature-sos
After:   https://vanpaultek.com/guide/#feature-sos

Before:  https://vanpaultek.github.io/OnlyTrusted/pricing/#plans
After:   https://vanpaultek.com/pricing/#plans
```

The `html_url` in the GitHub Pages API response updates automatically — no code changes needed on either kit.

---

Last updated: 2026-10-25.
Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
