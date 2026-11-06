// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import type {
  Country,
  CountryCode,
  Directory,
  LanguageCode,
  Region,
} from "./types.js";

import directoryJson from "../data/countries.json" with { type: "json" };

/** The full country directory, keyed by region. */
export const directory: Directory = directoryJson as Directory;

/** Flat list of all 195 countries, region-independent. */
export const countries: readonly Country[] = Object.values(directory).flat();

const BY_CODE: Record<CountryCode, Country> = Object.fromEntries(
  countries.map((c) => [c.code.toUpperCase(), c])
);

/** Look up a country by ISO-3166 alpha-2 code (case-insensitive). */
export function getCountry(code: CountryCode): Country | undefined {
  return BY_CODE[code.toUpperCase()];
}

/** Which region does a given country belong to? */
export function regionOf(code: CountryCode): Region | undefined {
  const upper = code.toUpperCase();
  for (const [region, list] of Object.entries(directory) as [
    Region,
    Country[]
  ][]) {
    if (list.some((c) => c.code === upper)) return region;
  }
  return undefined;
}

/** All languages advertised by a country, or [] if unknown. */
export function languagesFor(code: CountryCode): LanguageCode[] {
  return getCountry(code)?.languages ?? [];
}

/** Suggested default language for a country, or "en" if unknown. */
export function suggestedLanguageFor(code: CountryCode): LanguageCode {
  return getCountry(code)?.suggested ?? "en";
}
