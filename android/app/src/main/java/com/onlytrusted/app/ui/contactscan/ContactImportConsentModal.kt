// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.contactscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.MidnightCard
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark

/**
 * One-time dialog that fires on the user's first Dashboard mount after
 * READ_CONTACTS has been granted. Explains exactly what we're about to do
 * before silently reading their entire address book — avoiding the
 * "privacy-focused app that silently scraped my contacts" dark pattern.
 *
 * Dismiss behaviour:
 *  - "Import N contacts" → runs the auto-importer + scam scan
 *  - "Not now, I'll add manually" → marks the import flow done so this
 *    dialog never fires again; user can still add contacts via Vault
 *
 * Both actions are first-class, equally weighted. No deceptive labels.
 */
@Composable
fun ContactImportConsentModal(
    contactCount: Int,
    onImport: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = MidnightCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Contacts,
                    contentDescription = null,
                    tint = BrandAccentDark,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "We found $contactCount contacts",
                    color = PrimaryTextDark,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Add them all to your trusted-contacts list so OnlyTrusted " +
                        "knows who's safe to call and text? Any contact matching " +
                        "our offline scam database will be flagged automatically.",
                    color = PrimaryTextDark,
                    fontSize = 14.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BulletLine("Nothing leaves your device.")
                    BulletLine("You can un-trust any contact later.")
                    BulletLine("Numbers are stored encrypted on-device.")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandCyanDark,
                    contentColor = MidnightBackground,
                ),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    if (contactCount == 1) "Import 1 contact"
                    else "Import $contactCount contacts",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Not now — I'll add manually",
                    color = SecondaryTextDark,
                )
            }
        },
    )
}

@Composable
private fun BulletLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            "•",
            color = BrandAccentDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(text, color = PrimaryTextDark, fontSize = 13.sp)
    }
}
