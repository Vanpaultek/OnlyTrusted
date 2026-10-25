// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.R
import com.onlytrusted.app.localization.LocaleManager

/**
 * Settings → Language & Region picker. Region → Country → Language cascading
 * dropdowns, English always present as a fallback option. Shows a review-badge
 * caption when the chosen language's strings carry the [REVIEW] sentinel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageRegionScreen(onBack: () -> Unit = {}) {
    val ctx = LocalContext.current
    val directory = remember { LocaleManager.loadDirectory(ctx) }

    val regions = remember { directory.regions.keys.toList() }
    var region by remember { mutableStateOf(regions.firstOrNull { it == "Asia" } ?: regions.first()) }

    val countriesInRegion = directory.regions[region] ?: emptyList()
    var countryCode by remember(region) {
        mutableStateOf(countriesInRegion.firstOrNull()?.code ?: "")
    }
    val country = countriesInRegion.firstOrNull { it.code == countryCode }

    var language by remember { mutableStateOf(LocaleManager.currentLanguage()) }

    // Choices for the Language dropdown: country-advertised languages + English always.
    val choices = remember(country) {
        val base = (country?.languages ?: emptyList()).toMutableList()
        if ("en" !in base) base.add(0, "en")
        base.distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_language_region)) },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.settings_language_region_subtitle),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                stringResource(R.string.language_english_always),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8CA0B0)
            )

            Dropdown(
                label = stringResource(R.string.language_region_header),
                options = regions,
                selected = region,
                onSelect = { region = it },
                display = { it }
            )
            Dropdown(
                label = stringResource(R.string.language_country_header),
                options = countriesInRegion.map { it.code },
                selected = countryCode,
                onSelect = { countryCode = it },
                display = { code -> countriesInRegion.firstOrNull { it.code == code }?.name ?: code }
            )
            Dropdown(
                label = stringResource(R.string.language_language_header),
                options = choices,
                selected = language,
                onSelect = {
                    language = it
                    LocaleManager.setLanguage(it)
                },
                display = { LocaleManager.displayName(it) }
            )

            Text(
                stringResource(R.string.language_selected_locale, language),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8CA0B0)
            )
            if (!LocaleManager.hasTranslations(language)) {
                Text(
                    "⚠ ${stringResource(R.string.language_pending_review)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF2B347),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                stringResource(R.string.language_restart_note),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8CA0B0),
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Dropdown(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    display: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = display(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(display(opt)) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
