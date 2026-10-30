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

    /** ITU E.164: minimum 7 digits (shortest valid country-code + subscriber),
     *  maximum 15 digits. Anything outside that range is almost certainly not
     *  a real phone number — reject rather than import. */
    private const val MIN_PHONE_DIGITS = 7
    private const val MAX_PHONE_DIGITS = 15
    private const val MAX_NAME_LENGTH = 120
    private const val MAX_LABEL_LENGTH = 32

    /** True iff [raw] looks like a plausible phone number: between 7 and 15
     *  digits (ignoring +, -, spaces, parens) and contains no control chars.
     *  Rejects obviously-corrupt rows from the system address book. */
    private fun isPlausiblePhoneNumber(raw: String): Boolean {
        if (raw.isBlank()) return false
        if (raw.length > 64) return false // defensive upper bound
        if (raw.any { it.isISOControl() }) return false
        val digits = raw.count { it.isDigit() }
        return digits in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS
    }

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

    /** Result of a bi-directional sync pass. */
    data class SyncResult(
        val added: Int,
        val removed: Int,
        val totalInDevice: Int,
        val permissionDenied: Boolean,
    )

    /**
     * Bi-directional sync between device contacts and the trusted vault.
     *  - Adds device contacts that aren't in the vault yet (same as [importAll]).
     *  - Removes vault contacts whose `systemContactLookupKey` no longer
     *    matches any current device contact (handles cases where the user
     *    deleted a contact on the phone after importing).
     *  - Leaves manually-added vault contacts (lookupKey == null) alone.
     *  - Idempotent — safe to call on every Dashboard mount.
     *
     * Returns [SyncResult] with per-pass counts. If permission is missing,
     * returns `permissionDenied = true` and performs no work.
     */
    suspend fun syncAll(context: Context): SyncResult = withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext SyncResult(0, 0, 0, permissionDenied = true)

        val app = context.applicationContext as? OnlyTrustedApp
            ?: return@withContext SyncResult(0, 0, 0, permissionDenied = false)
        val repo = app.trustedContacts

        val deviceContacts = readDeviceContacts(context)
        val vaultEntries = repo.allContacts.first()
        val vaultByLookupKey = vaultEntries.associateBy { it.contact.systemContactLookupKey ?: "" }

        // ADDS: device contacts whose lookupKey isn't in vault yet.
        //
        // We DELIBERATELY skip device contacts with a blank display name.
        // Those are usually missed-call auto-saves or scam numbers that got
        // added to the address book without the user ever typing a name.
        // Auto-trusting them flooded the vault with nameless entries that
        // the user then couldn't distinguish from real contacts (reported by
        // tester: 614 nameless entries in a 1,423-contact vault). A trusted
        // contact should be someone the user intentionally identified.
        var added = 0
        for ((lookupKey, contact) in deviceContacts) {
            if (vaultByLookupKey.containsKey(lookupKey)) continue
            if (contact.displayName.isBlank()) continue
            val draft = TrustedContactDraft(
                displayName = contact.displayName,
                relation = null,
                isOwner = false,
                email = null,
                address = null,
                notes = null,
                phoneNumbers = contact.numbers,
                systemContactLookupKey = lookupKey,
            )
            runCatching { repo.save(draft) }.onSuccess { added++ }
        }

        // REMOVALS: vault entries that WERE auto-imported but whose lookupKey
        // is no longer in the device contacts. Leaves lookupKey==null (manual)
        // entries alone.
        var removed = 0
        val deviceKeys = deviceContacts.keys
        for (entry in vaultEntries) {
            val lookupKey = entry.contact.systemContactLookupKey ?: continue
            if (lookupKey !in deviceKeys) {
                runCatching { repo.delete(entry.contact.id) }.onSuccess { removed++ }
            }
        }

        // Mark the one-shot flag so the first-time consent modal doesn't
        // re-fire after a successful sync.
        if (added > 0 || removed > 0 || !shouldRun(context)) markDone(context)

        SyncResult(
            added = added,
            removed = removed,
            totalInDevice = deviceContacts.size,
            permissionDenied = false,
        )
    }

    /** Snapshot of device contacts keyed by LOOKUP_KEY, with display name +
     *  all phone numbers (validated via [isPlausiblePhoneNumber]). */
    private data class DeviceContact(
        val displayName: String,
        val numbers: List<Pair<String, String?>>,
    )

    private fun readDeviceContacts(context: Context): Map<String, DeviceContact> {
        val byLookupKey = HashMap<String, Pair<String, MutableList<Pair<String, String?>>>>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null, null,
        )?.use { cursor ->
            val keyIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
            while (cursor.moveToNext()) {
                val lookupKey = cursor.getString(keyIdx)?.takeIf { it.isNotBlank() } ?: continue
                val name = cursor.getString(nameIdx)?.trim().orEmpty().take(MAX_NAME_LENGTH)
                val rawNumber = cursor.getString(numIdx)?.trim().orEmpty()
                if (!isPlausiblePhoneNumber(rawNumber)) continue
                val typeInt = cursor.getInt(typeIdx)
                val customLabel = cursor.getString(labelIdx)?.take(MAX_LABEL_LENGTH)
                val label = labelForType(typeInt, customLabel)
                val entry = byLookupKey.getOrPut(lookupKey) { name to mutableListOf() }
                entry.second += rawNumber to label
            }
        }
        return byLookupKey.mapValues { (_, v) -> DeviceContact(v.first, v.second) }
    }

    /**
     * Reads the number of distinct contacts (by CONTACT_ID) that WOULD be
     * imported if [importAll] ran now. Used to populate the "We found N
     * contacts" prompt before the user confirms. Returns 0 if permission
     * is missing or the query fails.
     */
    suspend fun countAvailable(context: Context): Int = withContext(Dispatchers.IO) {
        if (!shouldRun(context)) return@withContext 0
        val ids = HashSet<Long>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
                null, null, null,
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                while (c.moveToNext()) ids.add(c.getLong(idIdx))
            }
        }
        ids.size
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
                val name = cursor.getString(nameIdx)?.trim().orEmpty().take(MAX_NAME_LENGTH)
                val rawNumber = cursor.getString(numIdx)?.trim().orEmpty()
                // Defensive validation — reject malformed entries rather than
                // trusting the system address book blindly. A corrupted
                // ContactsContract row (long garbage string, control chars,
                // non-phone-like data) could otherwise end up in our vault
                // and poison downstream blocking + scam-match comparisons.
                if (!isPlausiblePhoneNumber(rawNumber)) continue
                val typeInt = cursor.getInt(typeIdx)
                val customLabel = cursor.getString(labelIdx)?.take(MAX_LABEL_LENGTH)
                val label = labelForType(typeInt, customLabel)
                val entry = byContact.getOrPut(contactId) { name to mutableListOf() }
                entry.second += rawNumber to label
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
            // Same rationale as syncAll(): skip blank-name device contacts
            // so missed-call auto-saves and nameless scam numbers don't get
            // auto-trusted.
            if (displayName.isBlank()) continue
            val normalisedForThisContact = numbers
                .map { (raw, _) -> PhoneUtils.normalise(raw) }
                .filter { it.isNotBlank() }
                .toSet()
            if (normalisedForThisContact.all { it in existingNormalised }) continue

            val draft = TrustedContactDraft(
                displayName = displayName,
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

