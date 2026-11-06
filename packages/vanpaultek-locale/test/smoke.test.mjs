// Smoke test for the built package. Run with: npm test
import { strict as assert } from "node:assert";
import { test } from "node:test";

const pkg = await import("../dist/index.js");

test("directory contains 195 countries across 5 regions", () => {
  const regions = Object.keys(pkg.directory);
  assert.equal(regions.length, 5);
  assert.equal(pkg.countries.length, 195);
});

test("getCountry is case-insensitive", () => {
  assert.equal(pkg.getCountry("us")?.name, "United States");
  assert.equal(pkg.getCountry("IN")?.suggested, "hi");
});

test("suggestedLanguageFor falls back to en for unknown codes", () => {
  assert.equal(pkg.suggestedLanguageFor("XX"), "en");
});

test("regionOf locates countries correctly", () => {
  assert.equal(pkg.regionOf("DE"), "Europe");
  assert.equal(pkg.regionOf("BR"), "North & South America");
  assert.equal(pkg.regionOf("NG"), "Africa");
});

test("loadStrings returns a non-empty bundle for shipped languages", () => {
  for (const code of pkg.SHIPPED_LANGUAGES) {
    const bundle = pkg.loadStrings(code);
    assert.ok(Object.keys(bundle).length > 50, `${code} bundle too small`);
    assert.equal(typeof bundle.app_name, "string");
  }
});

test("t() substitutes Android-style positional tokens", () => {
  // Pick a known key with %1$s; alert_mic_body_with_pkg is "%1$s opened the microphone at %2$s."
  const out = pkg.t("en", "alert_mic_body_with_pkg", ["Zoom", "10:14"]);
  assert.equal(out, "Zoom opened the microphone at 10:14.");
});

test("hasTranslations is true for shipped and false for anything else", () => {
  assert.equal(pkg.hasTranslations("en"), true);
  assert.equal(pkg.hasTranslations("te"), true);
  assert.equal(pkg.hasTranslations("ja"), false);
});

test("detectRegion uses countryHint when provided", () => {
  const snap = pkg.detectRegion({ countryHint: "jp" });
  assert.equal(snap.country, "JP");
  assert.equal(snap.region, "Asia");
  assert.equal(snap.suggestedLanguage, "ja");
});
