
package com.tk.quicksearch.search.contacts.models

import android.net.Uri

/**
 * Represents a customizable action on the contact card.
 * Can be assigned to primary (call) or secondary (message) positions.
 */
sealed class ContactCardAction {
    abstract val phoneNumber: String

    data class Phone(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class Sms(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class WhatsAppCall(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class WhatsAppMessage(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class WhatsAppVideoCall(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class TelegramMessage(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class TelegramCall(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class TelegramVideoCall(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class SignalMessage(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class SignalCall(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class SignalVideoCall(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class GoogleMeet(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class Email(
        override val phoneNumber: String,
    ) : ContactCardAction()

    data class VideoCall(
        override val phoneNumber: String,
        val packageName: String,
    ) : ContactCardAction()

    data class CustomApp(
        override val phoneNumber: String,
        val mimeType: String,
        val packageName: String?,
        val dataId: Long?,
        val displayLabel: String,
    ) : ContactCardAction()

    data class ViewInContactsApp(
        override val phoneNumber: String,
    ) : ContactCardAction()

    /**
     * Serializes the action to a string format: "TYPE:PHONE_NUMBER"
     */
    fun toSerializedString(): String {
        val type =
            when (this) {
                is Phone -> TYPE_PHONE
                is Sms -> TYPE_SMS
                is WhatsAppCall -> TYPE_WHATSAPP_CALL
                is WhatsAppMessage -> TYPE_WHATSAPP_MESSAGE
                is WhatsAppVideoCall -> TYPE_WHATSAPP_VIDEO_CALL
                is TelegramMessage -> TYPE_TELEGRAM_MESSAGE
                is TelegramCall -> TYPE_TELEGRAM_CALL
                is TelegramVideoCall -> TYPE_TELEGRAM_VIDEO_CALL
                is SignalMessage -> TYPE_SIGNAL_MESSAGE
                is SignalCall -> TYPE_SIGNAL_CALL
                is SignalVideoCall -> TYPE_SIGNAL_VIDEO_CALL
                is GoogleMeet -> TYPE_GOOGLE_MEET
                is Email -> TYPE_EMAIL
                is VideoCall -> TYPE_VIDEO_CALL
                is CustomApp -> TYPE_CUSTOM_APP
                is ViewInContactsApp -> TYPE_VIEW_CONTACT
            }
        val payload =
            when (this) {
                is VideoCall ->
                    listOf(
                        encodeField(phoneNumber),
                        encodeField(packageName),
                    ).joinToString(FIELD_SEPARATOR)
                is CustomApp ->
                    listOf(
                        encodeField(phoneNumber),
                        dataId?.toString().orEmpty(),
                        encodeField(mimeType),
                        encodeField(packageName.orEmpty()),
                        encodeField(displayLabel),
                    ).joinToString(FIELD_SEPARATOR)
                else -> encodeField(phoneNumber)
            }
        return "$type:$payload"
    }

    companion object {
        private const val FIELD_SEPARATOR = "|"
        private const val TYPE_PHONE = "PHONE"
        private const val TYPE_SMS = "SMS"
        private const val TYPE_WHATSAPP_CALL = "WHATSAPP_CALL"
        private const val TYPE_WHATSAPP_MESSAGE = "WHATSAPP_MESSAGE"
        private const val TYPE_WHATSAPP_VIDEO_CALL = "WHATSAPP_VIDEO_CALL"
        private const val TYPE_TELEGRAM_MESSAGE = "TELEGRAM_MESSAGE"
        private const val TYPE_TELEGRAM_CALL = "TELEGRAM_CALL"
        private const val TYPE_TELEGRAM_VIDEO_CALL = "TELEGRAM_VIDEO_CALL"
        private const val TYPE_SIGNAL_MESSAGE = "SIGNAL_MESSAGE"
        private const val TYPE_SIGNAL_CALL = "SIGNAL_CALL"
        private const val TYPE_SIGNAL_VIDEO_CALL = "SIGNAL_VIDEO_CALL"
        private const val TYPE_GOOGLE_MEET = "GOOGLE_MEET"
        private const val TYPE_EMAIL = "EMAIL"
        private const val TYPE_VIDEO_CALL = "VIDEO_CALL"
        private const val TYPE_CUSTOM_APP = "CUSTOM_APP"
        private const val TYPE_VIEW_CONTACT = "VIEW_CONTACT"

        /**
         * Deserializes a string back to a ContactCardAction
         */
        fun fromSerializedString(value: String): ContactCardAction? {
            val parts = value.split(":", limit = 2)
            if (parts.size != 2) return null

            val type = parts[0]
            val payload = parts[1]

            return when (type) {
                TYPE_PHONE -> Phone(decodeField(payload))
                TYPE_SMS -> Sms(decodeField(payload))
                TYPE_WHATSAPP_CALL -> WhatsAppCall(decodeField(payload))
                TYPE_WHATSAPP_MESSAGE -> WhatsAppMessage(decodeField(payload))
                TYPE_WHATSAPP_VIDEO_CALL -> WhatsAppVideoCall(decodeField(payload))
                TYPE_TELEGRAM_MESSAGE -> TelegramMessage(decodeField(payload))
                TYPE_TELEGRAM_CALL -> TelegramCall(decodeField(payload))
                TYPE_TELEGRAM_VIDEO_CALL -> TelegramVideoCall(decodeField(payload))
                TYPE_SIGNAL_MESSAGE -> SignalMessage(decodeField(payload))
                TYPE_SIGNAL_CALL -> SignalCall(decodeField(payload))
                TYPE_SIGNAL_VIDEO_CALL -> SignalVideoCall(decodeField(payload))
                TYPE_GOOGLE_MEET -> GoogleMeet(decodeField(payload))
                TYPE_EMAIL -> Email(decodeField(payload))
                TYPE_VIDEO_CALL -> {
                    val fields = payload.split(FIELD_SEPARATOR, limit = 2)
                    if (fields.size != 2) return null
                    VideoCall(
                        phoneNumber = decodeField(fields[0]),
                        packageName = decodeField(fields[1]),
                    )
                }
                TYPE_CUSTOM_APP -> {
                    val fields = payload.split(FIELD_SEPARATOR, limit = 5)
                    if (fields.size != 5) return null
                    CustomApp(
                        phoneNumber = decodeField(fields[0]),
                        dataId = fields[1].toLongOrNull(),
                        mimeType = decodeField(fields[2]),
                        packageName = decodeField(fields[3]).ifBlank { null },
                        displayLabel = decodeField(fields[4]),
                    )
                }
                TYPE_VIEW_CONTACT -> ViewInContactsApp(decodeField(payload))
                else -> null
            }
        }

        private fun encodeField(value: String): String = Uri.encode(value)

        private fun decodeField(value: String): String = Uri.decode(value)
    }
}
