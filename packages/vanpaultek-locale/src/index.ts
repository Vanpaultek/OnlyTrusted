// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later

export type {
  Country,
  CountryCode,
  Directory,
  LanguageCode,
  Region,
  ReviewIndex,
  StringBundle,
} from "./types.js";
export { SHIPPED_LANGUAGES } from "./types.js";

export {
  countries,
  directory,
  getCountry,
  languagesFor,
  regionOf,
  suggestedLanguageFor,
} from "./directory.js";

export { detectRegion, type RegionSnapshot } from "./region.js";

export { hasTranslations, loadStrings, reviewPending, t } from "./strings.js";
