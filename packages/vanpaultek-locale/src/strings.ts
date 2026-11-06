// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import type { LanguageCode, ReviewIndex, StringBundle } from "./types.js";
import { SHIPPED_LANGUAGES } from "./types.js";

import ar from "../data/strings/ar.json" with { type: "json" };
import de from "../data/strings/de.json" with { type: "json" };
import en from "../data/strings/en.json" with { type: "json" };
import es from "../data/strings/es.json" with { type: "json" };
import fr from "../data/strings/fr.json" with { type: "json" };
import hi from "../data/strings/hi.json" with { type: "json" };
import id from "../data/strings/id.json" with { type: "json" };
import pt from "../data/strings/pt.json" with { type: "json" };
import te from "../data/strings/te.json" with { type: "json" };
import review from "../data/strings/_review.json" with { type: "json" };

const BUNDLES: Record<LanguageCode, StringBundle> = {
  ar: ar as StringBundle,
  de: de as StringBundle,
  en: en as StringBundle,
  es: es as StringBundle,
  fr: fr as StringBundle,
  hi: hi as StringBundle,
  id: id as StringBundle,
  pt: pt as StringBundle,
  te: te as StringBundle,
};

const REVIEW: ReviewIndex = review as ReviewIndex;

/** True when we ship human-translatable strings for this language. */
export function hasTranslations(code: LanguageCode): boolean {
  return (SHIPPED_LANGUAGES as readonly string[]).includes(code);
}

const EN_BUNDLE = BUNDLES.en as StringBundle;

/** Fetch the full string bundle for a language, falling back to English. */
export function loadStrings(code: LanguageCode): StringBundle {
  return BUNDLES[code] ?? EN_BUNDLE;
}

/** Keys in a given language that are machine-translated and awaiting review. */
export function reviewPending(code: LanguageCode): readonly string[] {
  return REVIEW[code] ?? [];
}

/**
 * Translate a single key. Falls back to the English string, then to the key
 * itself. Supports Android-style positional tokens: pass `vars` as an array
 * and `%1$s` / `%2$s` get replaced in order, which keeps the JSON files
 * interchangeable with the Android resource files they came from.
 */
export function t(
  code: LanguageCode,
  key: string,
  vars?: readonly (string | number)[]
): string {
  const bundle = loadStrings(code);
  const template = bundle[key] ?? EN_BUNDLE[key] ?? key;
  if (!vars || vars.length === 0) return template;
  return template.replace(/%(\d+)\$[sd]/g, (_, n) => {
    const idx = Number.parseInt(n, 10) - 1;
    return idx >= 0 && idx < vars.length ? String(vars[idx]) : "";
  });
}
