// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.contactscan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.data.repository.TrustedContactDraft
import com.onlytrusted.app.privacy.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * One-shot importer that mirrors the user's device address book into the
 * [com.onlytrusted.app.data.repository.TrustedContactsRepository] on first
 * launch after READ_CONTACTS is granted — the WhatsApp-style "we found your
 * contacts" behaviour testers asked for.
 *
 * ### Guarantees
 *
 *   - **Local only.** The ContactsContract query runs on-device, results go
 *     into the local encrypted Room DB (same storage the manual Add Contact
 *     flow uses). Nothing is transmitted anywhere.
 *   - **Idempotent.** A persistent flag ([KEY_IMPORT_DONE]) is written after
 *     a successful run so we don't duplicate contacts on later launches.
 *   - **Non-blocking.** The query + inserts run on `Dispatchers.IO`. Caller
 *     fires-and-forgets from a `LaunchedEffect`.
 *   - **Opt-out of overwrite.** If a contact (by normalised phone number) is
 *     already saved — manually or from a prior import on another device —
 *     we skip it rather than clobber the user's existing notes/relation.
 *   - **Permission-gated.** [shouldRun] returns false unless READ_CONTACTS
 *     is granted and the flag is unset; the caller doesn't need its own
 *     permission check.
 *
 * ### What is read from each contact
 *
 *   `ContactsContract.CommonDataKinds.Phone`: display name, phone number,
 *   phone label. We don't read e-mail, address, notes, photo, or anything
 *   beyond the minimum needed to populate the trusted-contacts list.
 */
object TrustedContactsAutoImporter {

    private const val PREFS = "ot_contact_import_prefs"
    private const val KEY_IMPORT_DONE = "import_done_v1"

    /** True iff the device has granted READ_CONTACTS AND we haven't already
     *  completed an import. Call before launching [importAll]. */
    fun shouldRun(context: Context): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_IMPORT_DONE, false)
    }

    /** Mark the one-shot complete without actually importing anything. Used
     *  if the user explicitly disables auto-import later, or to re-trigger
     *  from a Settings "Re-import" button after clearing. */
    fun markDone(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IMPORT_DONE, true).apply()
    }

    /**
     * Reads every entry from `ContactsContract.CommonDataKinds.Phone` and
     * inserts each unique contact into the trusted-contacts repository.
     * Multiple phone numbers per contact are grouped into a single trusted
     * contact (via Contact ID).
     *
     * Returns the number of newly-inserted contacts (ignores already-existing
     * ones). Zero return is possible: empty address book, or every number was
     * already saved.
     */
    suspend fun importAll(context: Context): Int = withContext(Dispatchers.IO) {
        if (!shouldRun(context)) return@withContext 0
        val app = context.applicationContext as? OnlyTrustedApp
            ?: return@withContext 0
        val repo = app.trustedContacts

        // Build a map of contact-id → (displayName, [number→label]).
        val byContact = HashMap<Long, Pair<String, MutableList<Pair<String, String?>>>>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx)?.trim().orEmpty()
                val number = cursor.getString(numIdx)?.trim().orEmpty()
                if (number.isBlank()) continue
                val typeInt = cursor.getInt(typeIdx)
                val customLabel = cursor.getString(labelIdx)
                val label = labelForType(typeInt, customLabel)
                val entry = byContact.getOrPut(contactId) { name to mutableListOf() }
                entry.second += number to label
            }
        }

        if (byContact.isEmpty()) {
            markDone(context)
            return@withContext 0
        }

        // Load existing numbers once so we skip duplicates without hammering
        // the DB per-iteration.
        val existingNormalised = HashSet<String>()
        repo.allContacts.first().forEach { entry ->
            entry.numbers.forEach { tn -> existingNormalised += tn.normalisedNumber }
        }

        var inserted = 0
        for ((_, pair) in byContact) {
            val (displayName, numbers) = pair
            val normalisedForThisContact = numbers
                .map { (raw, _) -> PhoneUtils.normalise(raw) }
                .filter { it.isNotBlank() }
                .toSet()
            if (normalisedForThisContact.all { it in existingNormalised }) continue

            val draft = TrustedContactDraft(
                displayName = displayName.ifBlank { "Unknown" },
                relation = null,
                isOwner = false,
                email = null,
                address = null,
                notes = null,
                phoneNumbers = numbers,
            )
            runCatching { repo.save(draft) }
                .onSuccess {
                    inserted++
                    existingNormalised += normalisedForThisContact
                }
        }

        markDone(context)
        inserted
    }

    private fun labelForType(type: Int, customLabel: String?): String? = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Work fax"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "Home fax"
        ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "Pager"
        ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> null
        ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel?.trim()?.ifBlank { null }
        else -> null
    }
}

