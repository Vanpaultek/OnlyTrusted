// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import {
  getCountry,
  regionOf,
  suggestedLanguageFor,
} from "./directory.js";
import type { CountryCode, LanguageCode, Region } from "./types.js";

export interface RegionSnapshot {
  /** Two-letter country code (ISO-3166 alpha-2), uppercase. */
  country: CountryCode;
  /** Region bucket the country belongs to, if known. */
  region: Region | undefined;
  /** English display name of the country. */
  countryName: string;
  /** IANA time-zone id (e.g. "America/Los_Angeles"), empty when unavailable. */
  timeZoneId: string;
  /** Primary BCP-47 locale tag as reported by the environment. */
  languageTag: string;
  /** Suggested default language for the resolved country. */
  suggestedLanguage: LanguageCode;
}

interface DetectOptions {
  /**
   * Override the detected country. Useful when you already know it from a
   * server-side IP lookup, a phone SIM reading, or a user preference.
   */
  countryHint?: CountryCode;
  /**
   * Fallback country to use when detection yields nothing. Defaults to "US".
   */
  defaultCountry?: CountryCode;
}

/**
 * Browser-friendly region detector. Mirrors the Android `RegionState` object
 * in spirit: read-only, no network, best-effort. In Node / non-browser
 * environments it will fall back to `defaultCountry` unless a hint is given.
 */
export function detectRegion(opts: DetectOptions = {}): RegionSnapshot {
  const { countryHint, defaultCountry = "US" } = opts;

  const languageTag = pickLanguageTag();
  const timeZoneId = pickTimeZone();

  const detectedFromTag = languageTag.includes("-")
    ? languageTag.split("-")[1]?.toUpperCase()
    : undefined;

  const country = (
    countryHint ??
    detectedFromTag ??
    defaultCountry
  ).toUpperCase();

  const meta = getCountry(country);

  return {
    country,
    region: regionOf(country),
    countryName: meta?.name ?? country,
    timeZoneId,
    languageTag,
    suggestedLanguage: suggestedLanguageFor(country),
  };
}

function pickLanguageTag(): string {
  // Browser.
  const g = globalThis as unknown as {
    navigator?: { language?: string; languages?: readonly string[] };
  };
  const nav = g.navigator;
  if (nav?.language) return nav.language;
  if (nav?.languages && nav.languages.length > 0) return nav.languages[0] ?? "en-US";
  // Node.
  const env = (globalThis as unknown as { process?: { env?: Record<string, string> } })
    .process?.env;
  return env?.LANG?.split(".")[0]?.replace("_", "-") ?? "en-US";
}

function pickTimeZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone ?? "";
  } catch {
    return "";
  }
}
