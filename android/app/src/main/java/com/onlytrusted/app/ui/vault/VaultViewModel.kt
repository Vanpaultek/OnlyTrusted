// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.vault

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.data.db.entities.BlockedNumber
import com.onlytrusted.app.data.repository.MaskedContact
import com.onlytrusted.app.data.repository.RevealedContact
import com.onlytrusted.app.data.repository.TrustedContactDraft
import com.onlytrusted.app.scamreport.ReportedScamStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(private val app: OnlyTrustedApp) : AndroidViewModel(app) {

    val contacts: StateFlow<List<MaskedContact>> = app.trustedContacts.maskedList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _revealed = MutableStateFlow<RevealedContact?>(null)
    val revealed: StateFlow<RevealedContact?> = _revealed.asStateFlow()

    fun save(draft: TrustedContactDraft, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = app.trustedContacts.save(draft)
            onSaved(id)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            app.trustedContacts.delete(id)
            if (_revealed.value?.id == id) _revealed.value = null
        }
    }

    /**
     * Move a trusted contact to the Blocked list: writes every number on the
     * contact into blocked_numbers (calls + SMS blocked), then deletes the
     * contact from the vault. The contact's label is preserved as the
     * BlockedNumber.label so the Blocked list still shows "Scam Bank Alert"
     * rather than just the number.
     */
    fun moveToBlocked(contact: MaskedContact) {
        viewModelScope.launch {
            val dao = app.database.blockedNumberDao()
            contact.normalisedNumbers.forEach { number ->
                if (number.isBlank()) return@forEach
                dao.upsert(
                    BlockedNumber(
                        number = number,
                        label = contact.displayName,
                        blockCalls = true,
                        blockSms = true,
                    )
                )
            }
            app.trustedContacts.delete(contact.id)
            if (_revealed.value?.id == contact.id) _revealed.value = null
        }
    }

    /**
     * Record every number on a trusted contact as a user-reported scam, then
     * delete the contact from the vault. Uses the same ReportedScamStore the
     * dashboard "Report scam" dialog writes to — exports stay consistent.
     */
    fun markAsScam(contact: MaskedContact) {
        viewModelScope.launch {
            contact.normalisedNumbers.forEach { number ->
                if (number.isBlank()) return@forEach
                ReportedScamStore.add(
                    context = app,
                    normalisedNumber = number,
                    category = ReportedScamStore.Category.OTHER,
                )
            }
            app.trustedContacts.delete(contact.id)
            if (_revealed.value?.id == contact.id) _revealed.value = null
        }
    }

    /**
     * Bulk-move every contact in [victims] to Blocked, then delete all of
     * them from the vault. Used by the "Clean up nameless auto-imports"
     * card at the top of Trusted Contacts.
     */
    fun bulkMoveToBlocked(victims: List<MaskedContact>) {
        if (victims.isEmpty()) return
        viewModelScope.launch {
            val dao = app.database.blockedNumberDao()
            victims.forEach { contact ->
                contact.normalisedNumbers.forEach { number ->
                    if (number.isBlank()) return@forEach
                    dao.upsert(
                        BlockedNumber(
                            number = number,
                            label = contact.displayName,
                            blockCalls = true,
                            blockSms = true,
                        )
                    )
                }
                app.trustedContacts.delete(contact.id)
            }
            _revealed.value = null
        }
    }

    /** Bulk-remove without blocking — simply delete from vault. */
    fun bulkRemove(victims: List<MaskedContact>) {
        if (victims.isEmpty()) return
        viewModelScope.launch {
            victims.forEach { app.trustedContacts.delete(it.id) }
            _revealed.value = null
        }
    }

    fun reveal(id: Long) {
        viewModelScope.launch {
            _revealed.value = app.trustedContacts.reveal(id)
        }
    }

    fun hideRevealed() {
        _revealed.value = null
    }

    suspend fun loadForEdit(id: Long): RevealedContact? = app.trustedContacts.reveal(id)
}
