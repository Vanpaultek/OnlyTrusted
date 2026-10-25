// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.json.JSONObject
import java.util.Locale

/**
 * App-wide language picker backed by [AppCompatDelegate.setApplicationLocales].
 *
 *  - Reads the 195-country directory from assets/countries.json (sourced from the
 *    OnlyTrusted Localization Kit; see docs/NEEDS_REVIEW.md).
 *  - Shipped (translated) locales are declared in build.gradle.kts
 *    `resourceConfigurations` and in res/xml/locales_config.xml. Users can PICK
 *    any language the country advertises, but if we don't have strings for it,
 *    the per-app locale is set anyway and Android transparently falls back to
 *    English per Resource Qualifier rules.
 *  - Language choice persists across process restarts via AppCompatDelegate's
 *    own storage; we don't duplicate it in SharedPreferences.
 *  - Country choice is informational only (determines the suggested languages
 *    shown in the picker); it does NOT force a language.
 */
object LocaleManager {

    /** Locales we actually ship translated strings for. English always first. */
    val SHIPPED_LANGUAGES: List<String> = listOf("en", "es", "fr", "pt", "hi", "ar", "de", "id")

    data class Country(
        val code: String,
        val name: String,
        val languages: List<String>,
        val suggested: String
    )

    data class Directory(val regions: Map<String, List<Country>>)

    @Volatile private var cached: Directory? = null

    /** Loads + caches the country/region directory from assets. */
    fun loadDirectory(ctx: Context): Directory {
        cached?.let { return it }
        val json = ctx.assets.open("countries.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val regions = linkedMapOf<String, List<Country>>()
        root.keys().forEach { region ->
            val arr = root.getJSONArray(region)
            val list = buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    val langs = c.getJSONArray("languages")
                    val langList = buildList(langs.length()) { for (j in 0 until langs.length()) add(langs.getString(j)) }
                    add(Country(
                        code = c.getString("code"),
                        name = c.getString("name"),
                        languages = langList,
                        suggested = c.getString("suggested")
                    ))
                }
            }
            regions[region] = list
        }
        val d = Directory(regions)
        cached = d
        return d
    }

    /** The current app-wide language tag (e.g. "en", "es", "hi"), falling back to "en". */
    fun currentLanguage(): String {
        val list = AppCompatDelegate.getApplicationLocales()
        if (list.isEmpty) return "en"
        val tag = list.get(0)?.toLanguageTag() ?: "en"
        // AppCompatDelegate returns BCP-47 (e.g. "pt-BR") but our resource folders
        // use language-only (values-pt). Strip the region qualifier for the lookup.
        return tag.substringBefore('-')
    }

    /** Changes the app-wide language. Takes effect immediately for most screens; some
     *  activities may recreate themselves. If the user chooses a language we don't ship
     *  strings for, Android falls back to English for the untranslated parts. */
    fun setLanguage(languageTag: String) {
        require(languageTag.isNotBlank()) { "languageTag must not be blank" }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    /** Pretty name for a language code in that language (e.g. "es" → "Español"). */
    fun displayName(code: String): String {
        val locale = Locale.forLanguageTag(code)
        return locale.getDisplayLanguage(locale).ifBlank { code }
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    /** True when the shipped-strings allow-list contains [code]. */
    fun hasTranslations(code: String): Boolean = code in SHIPPED_LANGUAGES
}
