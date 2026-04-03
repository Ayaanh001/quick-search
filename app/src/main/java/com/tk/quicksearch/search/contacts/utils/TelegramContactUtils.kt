package com.tk.quicksearch.search.contacts.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.tk.quicksearch.search.models.ContactMethodMimeTypes

/**
 * Utility functions for matching Telegram/Cherrygram contact methods to phone numbers.
 * Based on the approach from: https://stackoverflow.com/questions/63637693/make-telegram-video-call-with-specific-contact-with-android-programmatically
 *
 * The key insight is to:
 * 1. Get the contact ID from the phone number using PhoneLookup.CONTENT_FILTER_URI
 * 2. Query ContactsContract.Data with that contact ID and Telegram/Cherrygram MIME types
 * 3. This gives us the data IDs for entries associated with that specific phone number
 *
 * Cherrygram (uz.unnarsx.cherrygram) registers its own MIME types in the Contacts provider,
 * so we query both the official Telegram MIME types and Cherrygram MIME types.
 */
object TelegramContactUtils {
    /**
     * All MIME types that represent a Telegram-compatible message/call entry.
     * Includes both official Telegram and Cherrygram variants.
     */
    private val ALL_TELEGRAM_MIME_TYPES = listOf(
        ContactMethodMimeTypes.TELEGRAM_MESSAGE,
        ContactMethodMimeTypes.TELEGRAM_CALL,
        ContactMethodMimeTypes.TELEGRAM_VIDEO_CALL,
        ContactMethodMimeTypes.CHERRYGRAM_MESSAGE,
        ContactMethodMimeTypes.CHERRYGRAM_CALL,
        ContactMethodMimeTypes.CHERRYGRAM_VIDEO_CALL,
    )

    /**
     * Gets the contact ID for a specific phone number using PhoneLookup.
     * This is important because the same contact can have multiple phone numbers,
     * and each phone number lookup returns the contact ID specifically associated with it.
     *
     * @param context The application context
     * @param phoneNumber The phone number to look up
     * @return The contact ID associated with this phone number, or null if not found
     */
    private fun getContactIdByPhoneNumber(
        context: Context,
        phoneNumber: String,
    ): Long? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val contentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber),
        )

        val cursor: Cursor? = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null,
        )

        cursor?.use { c ->
            val idIndex = c.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID)
            if (c.moveToFirst()) {
                return c.getLong(idIndex)
            }
        }

        return null
    }

    /**
     * Finds Telegram/Cherrygram data IDs that are associated with a specific phone number.
     *
     * Queries all known Telegram-compatible MIME types (official Telegram + Cherrygram) so
     * that contacts synced by either client are matched correctly.
     *
     * @param context The application context
     * @param phoneNumber The phone number to match
     * @return Set of data IDs for Telegram/Cherrygram entries that match the phone number
     */
    fun findTelegramDataIdsForPhoneNumber(
        context: Context,
        phoneNumber: String,
    ): Set<Long> {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptySet()
        }

        val contactId = getContactIdByPhoneNumber(context, phoneNumber) ?: return emptySet()

        val dataIds = mutableSetOf<Long>()
        val contentResolver = context.contentResolver

        // Build a dynamic selection that covers all known Telegram-compatible MIME types.
        // This ensures contacts synced by Cherrygram are matched just as reliably as
        // those synced by the official Telegram app.
        val mimeTypePlaceholders = ALL_TELEGRAM_MIME_TYPES.joinToString(" OR ") {
            "${ContactsContract.Data.MIMETYPE} = ?"
        }
        val selection =
            "${ContactsContract.Data.CONTACT_ID} = ? AND ($mimeTypePlaceholders)"
        val selectionArgs = (listOf(contactId.toString()) + ALL_TELEGRAM_MIME_TYPES).toTypedArray()

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            selection,
            selectionArgs,
            null,
        )

        cursor?.use { c ->
            val dataIdIndex = c.getColumnIndexOrThrow(ContactsContract.Data._ID)
            while (c.moveToNext()) {
                dataIds.add(c.getLong(dataIdIndex))
            }
        }

        return dataIds
    }

    /**
     * Checks if a Telegram/Cherrygram ContactMethod is associated with a specific phone number.
     *
     * @param context The application context
     * @param phoneNumber The phone number to check
     * @param telegramMethod The Telegram/Cherrygram method to check
     * @return True if the method is associated with the phone number
     */
    fun isTelegramMethodForPhoneNumber(
        context: Context,
        phoneNumber: String,
        telegramMethod: com.tk.quicksearch.search.models.ContactMethod,
    ): Boolean {
        val methodDataId = telegramMethod.dataId ?: return false
        val matchingDataIds = findTelegramDataIdsForPhoneNumber(context, phoneNumber)
        return matchingDataIds.contains(methodDataId)
    }
}
