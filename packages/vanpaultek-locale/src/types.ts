// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later

/** ISO 639-1 language code (e.g. "en", "es", "hi"). */
export type LanguageCode = string;

/** ISO 3166-1 alpha-2 country code (e.g. "US", "IN", "DE"). */
export type CountryCode = string;

/** One of the five regional buckets in the directory. */
export type Region =
  | "Africa"
  | "Asia"
  | "Europe"
  | "North & South America"
  | "Oceania";

export interface Country {
  /** ISO 3166-1 alpha-2 code. */
  code: CountryCode;
  /** English display name. */
  name: string;
  /**
   * Languages the country advertises, in rough order of usefulness. English is
   * always included so the picker can offer it as a fallback.
   */
  languages: LanguageCode[];
  /** The language the picker proposes as default for this country. */
  suggested: LanguageCode;
}

/** Full directory: Region → Country[]. */
export type Directory = Record<Region, Country[]>;

/** Flat key→value string bundle for one language. */
export type StringBundle = Record<string, string>;

/** Map of language code → list of string keys still pending human review. */
export type ReviewIndex = Record<LanguageCode, string[]>;

/** Locales for which this package ships translated strings. */
export const SHIPPED_LANGUAGES: readonly LanguageCode[] = [
  "en",
  "es",
  "fr",
  "pt",
  "hi",
  "ar",
  "de",
  "id",
  "te",
] as const;
