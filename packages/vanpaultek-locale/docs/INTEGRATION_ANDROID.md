# Android integration — Kotlin

OnlyTrusted itself is the source of truth for the Android `strings.xml`
files. This section is for *other* Android apps that want to consume the
same directory + strings without re-translating.

## 1. Drop the JSON into assets

```
app/src/main/assets/
├── countries.json
└── locale_strings/
    ├── en.json
    ├── es.json
    └── …
```

Copy from `node_modules/@vanpaultek/locale/data/` or from a git submodule.

## 2. Data classes

```kotlin
package com.yourapp.locale

import org.json.JSONObject
import android.content.Context

data class Country(
    val code: String,
    val name: String,
    val languages: List<String>,
    val suggested: String
)

data class Directory(val regions: Map<String, List<Country>>) {
    val all: List<Country> get() = regions.values.flatten()
    fun getCountry(code: String): Country? =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) }
}

object LocaleDirectory {
    @Volatile private var cached: Directory? = null

    fun load(ctx: Context): Directory {
        cached?.let { return it }
        val json = ctx.assets.open("countries.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val regions = linkedMapOf<String, List<Country>>()
        root.keys().forEach { region ->
            val arr = root.getJSONArray(region)
            regions[region] = buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    val langs = c.getJSONArray("languages")
                    val langList = buildList(langs.length()) {
                        for (j in 0 until langs.length()) add(langs.getString(j))
                    }
                    add(Country(
                        code = c.getString("code"),
                        name = c.getString("name"),
                        languages = langList,
                        suggested = c.getString("suggested")
                    ))
                }
            }
        }
        return Directory(regions).also { cached = it }
    }
}
```

## 3. String bundles

```kotlin
object LocaleStrings {
    private val cache = mutableMapOf<String, Map<String, String>>()

    fun load(ctx: Context, lang: String): Map<String, String> =
        cache.getOrPut(lang) {
            val path = "locale_strings/$lang.json"
            runCatching {
                val json = ctx.assets.open(path).bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                buildMap {
                    obj.keys().forEach { put(it, obj.getString(it)) }
                }
            }.getOrDefault(emptyMap())
        }

    fun t(ctx: Context, lang: String, key: String, vararg args: Any?): String {
        val bundle = load(ctx, lang)
        val template = bundle[key] ?: load(ctx, "en")[key] ?: key
        // Delegate %1$s / %2$d substitution to Android's native String.format.
        return if (args.isEmpty()) template else template.format(*args)
    }
}
```

## 4. Region detection

Already in OnlyTrusted as `ui/region/RegionState.kt`. Copy it verbatim; it
reads `Locale.getDefault()` plus the SIM country and needs no extra deps.

## 5. If you already use OnlyTrusted's code

Don't vendor this package — you already have the source of truth. Use:

- `android/app/src/main/assets/countries.json` directly
- `com.onlytrusted.app.localization.LocaleManager`
- `com.onlytrusted.app.ui.settings.LanguageRegionScreen`

This npm package exists for apps that *don't* fork OnlyTrusted but still
want the directory and the 9 translation bundles.
