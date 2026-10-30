// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.data.repository

import com.onlytrusted.app.data.db.TrustedContactDao
import com.onlytrusted.app.data.db.TrustedContactWithNumbers
import com.onlytrusted.app.data.db.entities.TrustedContact
import com.onlytrusted.app.data.db.entities.TrustedNumber
import com.onlytrusted.app.privacy.FieldCrypto
import com.onlytrusted.app.privacy.PhoneUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TrustedContactDraft(
    val id: Long = 0,
    val displayName: String,
    val relation: String?,
    val isOwner: Boolean,
    val email: String?,
    val address: String?,
    val notes: String?,
    val phoneNumbers: List<Pair<String, String?>>, // raw number -> optional label
    /** ContactsContract LOOKUP_KEY when auto-imported from the system address book.
     *  Null for manually-entered contacts. Used by the auto-importer to detect
     *  which vault entries should be removed when they're deleted from device
     *  contacts (bi-directional sync). */
    val systemContactLookupKey: String? = null,
)

data class RevealedContact(
    val id: Long,
    val displayName: String,
    val relation: String?,
    val isOwner: Boolean,
    val emailPlain: String?,
    val addressPlain: String?,
    val notesPlain: String?,
    val numbers: List<TrustedNumber>
)

class TrustedContactsRepository(private val dao: TrustedContactDao) {

    val allContacts: Flow<List<TrustedContactWithNumbers>> = dao.observeAll()

    val count: Flow<Int> = dao.observeCount()

    fun maskedList(): Flow<List<MaskedContact>> = allContacts.map { list ->
        list.map { entry ->
            MaskedContact(
                id = entry.contact.id,
                displayName = entry.contact.displayName,
                relation = entry.contact.relation,
                isOwner = entry.contact.isOwner,
                maskedEmail = entry.contact.emailMasked,
                maskedNumbers = entry.numbers.map { it.maskedForDisplay },
                normalisedNumbers = entry.numbers.map { it.normalisedNumber }
            )
        }
    }

    suspend fun save(draft: TrustedContactDraft): Long {
        val now = System.currentTimeMillis()
        val entity = TrustedContact(
            id = draft.id,
            displayName = draft.displayName.trim(),
            relation = draft.relation?.trim()?.takeIf { it.isNotEmpty() },
            isOwner = draft.isOwner,
            emailMasked = PhoneUtils.maskEmail(draft.email),
            addressEncrypted = FieldCrypto.encrypt(draft.address?.trim()),
            notesEncrypted = FieldCrypto.encrypt(draft.notes?.trim()),
            createdAt = if (draft.id == 0L) now else now,
            updatedAt = now,
            systemContactLookupKey = draft.systemContactLookupKey,
        )
        val contactId = if (draft.id == 0L) {
            dao.insertContact(entity)
        } else {
            dao.updateContact(entity)
            draft.id
        }

        val numbers = draft.phoneNumbers
            .map { (raw, label) -> raw.trim() to label?.trim() }
            .filter { (raw, _) -> raw.isNotEmpty() }
            .map { (raw, label) ->
                TrustedNumber(
                    contactId = contactId,
                    label = label?.takeIf { it.isNotEmpty() },
                    normalisedNumber = PhoneUtils.normalise(raw),
                    maskedForDisplay = PhoneUtils.mask(raw)
                )
            }
        dao.replaceNumbers(contactId, numbers)
        return contactId
    }

    suspend fun delete(id: Long) {
        val existing = dao.loadById(id) ?: return
        dao.deleteNumbersForContact(id)
        dao.deleteContact(existing.contact)
    }

    suspend fun reveal(id: Long): RevealedContact? {
        val record = dao.loadById(id) ?: return null
        return RevealedContact(
            id = record.contact.id,
            displayName = record.contact.displayName,
            relation = record.contact.relation,
            isOwner = record.contact.isOwner,
            emailPlain = null, // email is only kept masked by default
            addressPlain = FieldCrypto.decrypt(record.contact.addressEncrypted),
            notesPlain = FieldCrypto.decrypt(record.contact.notesEncrypted),
            numbers = record.numbers
        )
    }

    suspend fun isTrusted(rawNumber: String): Boolean {
        if (rawNumber.isBlank()) return false
        return dao.isTrusted(PhoneUtils.normalise(rawNumber))
    }

    /** Synchronous — safe to call from a binder thread such as CallScreeningService. */
    fun isTrustedSync(rawNumber: String): Boolean {
        if (rawNumber.isBlank()) return false
        return dao.isTrustedSync(PhoneUtils.normalise(rawNumber))
    }

    suspend fun ownerNameFor(rawNumber: String): String? {
        if (rawNumber.isBlank()) return null
        return dao.ownerNameForNumber(PhoneUtils.normalise(rawNumber))
    }

    suspend fun wipeAll() {
        dao.deleteAllNumbers()
        dao.deleteAllContacts()
    }
}

data class MaskedContact(
    val id: Long,
    val displayName: String,
    val relation: String?,
    val isOwner: Boolean,
    val maskedEmail: String?,
    val maskedNumbers: List<String>,
    /** Normalised (E.164-like) numbers used by tel:/sms: deep links. Never displayed. */
    val normalisedNumbers: List<String> = emptyList()
)
