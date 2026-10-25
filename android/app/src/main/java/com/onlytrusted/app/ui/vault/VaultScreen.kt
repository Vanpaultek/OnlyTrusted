// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.vault

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import com.onlytrusted.app.ui.theme.AppBackdropScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.data.repository.MaskedContact
import androidx.compose.ui.res.stringResource
import com.onlytrusted.app.R
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.BrandPrimaryTeal
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.MidnightCard
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.OutlineDark
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.theme.TertiaryTextDark

private enum class ContactTab(val title: String) {
    All("All"),
    Family("Family"),
    Work("Work");

    fun includes(relation: String?): Boolean = when (this) {
        All -> true
        Family -> relation.equals("Family", ignoreCase = true)
        Work -> relation.equals("Work", ignoreCase = true) ||
            relation.equals("Colleague", ignoreCase = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OnlyTrustedApp
    val vm: VaultViewModel = viewModel(factory = viewModelFactory {
        initializer { VaultViewModel(app) }
    })
    val realContacts by vm.contacts.collectAsStateWithLifecycle()
    val duressActive by com.onlytrusted.app.ui.duress.DuressState.isDuressSession
        .collectAsStateWithLifecycle()
    val decoyContacts by com.onlytrusted.app.ui.duress.DuressVaultStore.contacts
        .collectAsStateWithLifecycle()
    // Pull the decoy snapshot into the StateFlow on first composition during a duress session
    // so switching tabs post-unlock shows seeded data even before any add/edit.
    androidx.compose.runtime.LaunchedEffect(duressActive) {
        if (duressActive) com.onlytrusted.app.ui.duress.DuressVaultStore.refresh(context)
    }
    val contacts = if (duressActive) decoyContacts else realContacts

    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(ContactTab.All) }
    var pendingDelete by remember { mutableStateOf<MaskedContact?>(null) }

    com.onlytrusted.app.ui.facelock.FaceLockShield {
    AppBackdropScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trusted Contacts",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    // Solid cyan circle "+" per mockup
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(BrandCyanDark)
                            .clickable(onClick = onAdd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add trusted contact",
                            tint = MidnightBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = PrimaryTextDark
                )
            )
        }
    ) { padding ->
        if (contacts.isEmpty() && query.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                onAdd = onAdd
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                SearchField(
                    value = query,
                    onValueChange = { query = it }
                )
                Spacer(Modifier.height(16.dp))
                TabChipRow(
                    selected = tab,
                    onSelected = { tab = it },
                    all = contacts.size,
                    family = contacts.count { ContactTab.Family.includes(it.relation) },
                    work = contacts.count { ContactTab.Work.includes(it.relation) }
                )
                Spacer(Modifier.height(16.dp))

                val visible = remember(contacts, query, tab) {
                    filter(contacts, query, tab)
                }

                if (visible.isEmpty()) {
                    EmptyFilterState(query = query)
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MidnightCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(visible, key = { it.id }) { contact ->
                                val isLast = contact.id == visible.last().id
                                ContactRow(
                                    contact = contact,
                                    onCall = { placeCall(context, contact) },
                                    onMessage = { sendSms(context, contact) },
                                    onEdit = { onEdit(contact.id) },
                                    onDelete = { pendingDelete = contact }
                                )
                                if (!isLast) {
                                    HorizontalDivider(
                                        color = OutlineDark,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 76.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = MidnightCard,
            titleContentColor = PrimaryTextDark,
            textContentColor = SecondaryTextDark,
            title = { Text(stringResource(R.string.vault_delete_title_fmt, target.displayName)) },
            text = { Text(stringResource(R.string.vault_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(target.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.vault_delete), color = androidx.compose.ui.graphics.Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.lock_cancel), color = BrandAccentDark)
                }
            }
        )
    }
    } // close FaceLockShield
}

private fun filter(
    all: List<MaskedContact>,
    query: String,
    tab: ContactTab
): List<MaskedContact> {
    val trimmed = query.trim()
    val digitsOnly = trimmed.filter { it.isDigit() || it == '+' }
    return all.filter { c ->
        if (!tab.includes(c.relation)) return@filter false
        if (trimmed.isEmpty()) return@filter true
        if (c.displayName.contains(trimmed, ignoreCase = true)) return@filter true
        if (digitsOnly.isNotEmpty() &&
            c.normalisedNumbers.any { it.contains(digitsOnly) }
        ) return@filter true
        c.normalisedNumbers.any { it.contains(trimmed, ignoreCase = true) } ||
            c.maskedNumbers.any { it.contains(trimmed, ignoreCase = true) }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier,
    onAdd: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MidnightCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(BrandAccentDark.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PeopleAlt,
                        contentDescription = null,
                        tint = BrandAccentDark,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Text(
                    stringResource(R.string.vault_no_contacts_yet),
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryTextDark,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.vault_tap_plus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryTextDark
                )
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAccentDark,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.vault_add_contact), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyFilterState(query: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidnightCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = TertiaryTextDark,
                modifier = Modifier.size(28.dp)
            )
            Text(
                "No matches",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryTextDark,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (query.isEmpty()) "No contacts in this tab yet."
                else "No contacts match “$query”.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryTextDark
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MidnightElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = TertiaryTextDark,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.vault_search_placeholder),
                        color = TertiaryTextDark,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    cursorBrush = SolidColor(BrandAccentDark),
                    textStyle = LocalTextStyle.current.copy(
                        color = PrimaryTextDark,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = TertiaryTextDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabChipRow(
    selected: ContactTab,
    onSelected: (ContactTab) -> Unit,
    all: Int,
    family: Int,
    work: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Chip(ContactTab.All, all, selected, onSelected)
        Chip(ContactTab.Family, family, selected, onSelected)
        Chip(ContactTab.Work, work, selected, onSelected)
    }
}

@Composable
private fun Chip(
    tab: ContactTab,
    count: Int,
    selected: ContactTab,
    onSelected: (ContactTab) -> Unit
) {
    val isSelected = tab == selected
    val background = if (isSelected) BrandAccentDark else MidnightElevated
    val textColor = if (isSelected) androidx.compose.ui.graphics.Color.Black else SecondaryTextDark
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background,
        modifier = Modifier
            .then(
                if (isSelected) Modifier
                else Modifier.border(
                    width = 1.dp,
                    color = TertiaryTextDark.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(999.dp)
                )
            )
            .clickable { onSelected(tab) }
    ) {
        Text(
            text = "${tab.title} ($count)",
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ContactRow(
    contact: MaskedContact,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val hasNumber = contact.normalisedNumbers.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(initial = contact.displayName.firstOrNull()?.uppercase() ?: "?")
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    contact.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryTextDark,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.isOwner) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = BrandAccentDark.copy(alpha = 0.18f)
                    ) {
                        Text(
                            stringResource(R.string.vault_owner_tag),
                            color = BrandAccentDark,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            val subtitleText = contact.relation?.takeIf { it.isNotBlank() }
                ?: contact.maskedNumbers.firstOrNull()
                ?: "No phone"
            val isCategory = contact.relation?.lowercase() in setOf("family", "work", "medical", "other", "colleague")
            Text(
                subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCategory) BrandCyanDark else SecondaryTextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        ActionIcon(
            icon = Icons.Filled.Call,
            label = "Call ${contact.displayName}",
            enabled = hasNumber,
            onClick = onCall
        )
        Spacer(Modifier.width(6.dp))
        ActionIcon(
            icon = Icons.AutoMirrored.Filled.Message,
            label = "Message ${contact.displayName}",
            enabled = hasNumber,
            onClick = onMessage
        )
        Spacer(Modifier.width(2.dp))

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More options for ${contact.displayName}",
                    tint = BrandCyanDark
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.vault_edit)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.vault_delete)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun Avatar(initial: String) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(BrandAccentDark.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            color = BrandAccentDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    // Cyan icon-only button per mockup — no background chip, just the icon in BrandCyanDark.
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) BrandCyanDark else TertiaryTextDark,
            modifier = Modifier.size(22.dp)
        )
    }
}

// Keep BrandPrimaryTeal reference to satisfy IDE unused warnings when colors are tuned.
@Suppress("unused")
private val KeepBrandPrimaryTealReachable = BrandPrimaryTeal

private fun placeCall(context: android.content.Context, contact: MaskedContact) {
    val target = contact.normalisedNumbers.firstOrNull() ?: return
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$target")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { context.startActivity(intent) }
}

private fun sendSms(context: android.content.Context, contact: MaskedContact) {
    val target = contact.normalisedNumbers.firstOrNull() ?: return
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$target")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { context.startActivity(intent) }
}
