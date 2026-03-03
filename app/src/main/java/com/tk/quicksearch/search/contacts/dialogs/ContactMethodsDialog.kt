package com.tk.quicksearch.search.contacts.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.contacts.components.ContactActionButton
import com.tk.quicksearch.search.contacts.components.ContactAvatar
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.contacts.utils.TelegramContactUtils
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import kotlin.reflect.KClass

// ============================================================================
// Contact Methods Dialog
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactMethodsDialog(
    contactInfo: ContactInfo,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    onDismiss: () -> Unit,
    getLastShownPhoneNumber: (Long) -> String? = { null },
    setLastShownPhoneNumber: (Long, String) -> Unit = { _, _ -> },
) {
    val viewInContactsLabel = stringResource(R.string.contact_method_view_in_contacts_label)
    val hasMultipleNumbers = contactInfo.phoneNumbers.size > 1
    val maxCardHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f
    val context = LocalContext.current
    val addToHomeHandler = remember(context) { AddToHomeHandler(context) }

    // Reorder phone numbers to show last shown number first (only for multiple numbers)
    val reorderedPhoneNumbers =
        remember(contactInfo.phoneNumbers, contactInfo.contactId, hasMultipleNumbers) {
            reorderPhoneNumbersForDisplay(contactInfo, hasMultipleNumbers, getLastShownPhoneNumber)
        }

    // State for phone number selection (always start at 0 since we reordered)
    var selectedPhoneIndex by remember { mutableStateOf(0) }
    val selectedPhoneNumber =
        reorderedPhoneNumbers.getOrNull(selectedPhoneIndex)
            ?: contactInfo.primaryNumber

    // Save the selected number when it changes (only for multiple numbers)
    LaunchedEffect(selectedPhoneIndex, reorderedPhoneNumbers, hasMultipleNumbers) {
        if (hasMultipleNumbers &&
            reorderedPhoneNumbers.isNotEmpty() &&
            selectedPhoneIndex >= 0 &&
            selectedPhoneIndex < reorderedPhoneNumbers.size
        ) {
            val number = reorderedPhoneNumbers[selectedPhoneIndex]
            if (number.isNotBlank()) {
                setLastShownPhoneNumber(contactInfo.contactId, number)
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties =
            androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // Header with contact info (photo and name outside the card)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ContactAvatar(
                            photoUri = contactInfo.photoUri,
                            displayName = contactInfo.displayName,
                            onClick = {
                                onContactMethodClick(contactInfo, ContactMethod.ViewInContactsApp(viewInContactsLabel))
                                onDismiss()
                            },
                            modifier = Modifier.size(48.dp),
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = contactInfo.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        // Close button
                        IconButton(
                            onClick = {
                                onDismiss()
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.dialog_cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Card encompassing options with black background
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                 .heightIn(max = maxCardHeight),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color.Black,
                            ),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        val optionsScrollState = rememberScrollState()
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(optionsScrollState)
                                    .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Phone number with navigation arrows
                            selectedPhoneNumber?.let { phoneNumber ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Left arrow (only show if there are multiple numbers and not at first)
                                    if (reorderedPhoneNumbers.size > 1 && selectedPhoneIndex > 0) {
                                        IconButton(
                                            onClick = {
                                                selectedPhoneIndex = (selectedPhoneIndex - 1).coerceAtLeast(0)
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronLeft,
                                                contentDescription = stringResource(R.string.contacts_action_previous_number),
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    } else {
                                        // Spacer to maintain alignment
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }

                                    // Phone number
                                    Text(
                                        text = PhoneNumberUtils.formatPhoneNumberForDisplay(phoneNumber),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )

                                    // Right arrow (only show if there are multiple numbers and not at last)
                                    if (reorderedPhoneNumbers.size > 1 && selectedPhoneIndex < reorderedPhoneNumbers.size - 1) {
                                        IconButton(
                                            onClick = {
                                                selectedPhoneIndex = (selectedPhoneIndex + 1).coerceAtMost(reorderedPhoneNumbers.size - 1)
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = stringResource(R.string.contacts_action_next_number),
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    } else {
                                        // Spacer to maintain alignment
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            // First row: call, message, google meet (filtered by selected phone number)
                            val firstRowMethods = mutableListOf<ContactMethod>()

                            // Filter methods by selected phone number (using phone number normalization)
                            val methodsForSelectedNumber =
                                filterMethodsByPhoneNumber(
                                    contactInfo.contactMethods,
                                    selectedPhoneNumber,
                                    context,
                                )

                            // Always add call if available for selected number
                            methodsForSelectedNumber.find { it is ContactMethod.Phone }?.let { firstRowMethods.add(it) }

                            // Always add message if available for selected number
                            methodsForSelectedNumber.find { it is ContactMethod.Sms }?.let { firstRowMethods.add(it) }

                            // Add Google Meet if available for selected number
                            methodsForSelectedNumber.find { it is ContactMethod.GoogleMeet }?.let { firstRowMethods.add(it) }

                            if (firstRowMethods.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    firstRowMethods.forEach { method ->
                                        ContactActionButton(
                                            method = method,
                                            onClick = {
                                                onContactMethodClick(contactInfo, method)
                                                onDismiss()
                                            },
                                            onLongClick = {
                                                val action = contactMethodToCardAction(method, selectedPhoneNumber)
                                                val actionDisplayName = methodShortcutLabel(context, method)
                                                if (action != null && actionDisplayName != null) {
                                                    addToHomeHandler.addContactActionToHome(
                                                        contact = contactInfo,
                                                        contactAction = action,
                                                        actionDisplayName = actionDisplayName,
                                                    )
                                                }
                                            },
                                        )
                                    }
                                }
                            }

                            // Render method rows for different app types
                            renderMethodRow(
                                methods = methodsForSelectedNumber,
                                methodTypes = listOf(
                                    ContactMethod.WhatsAppCall::class,
                                    ContactMethod.WhatsAppMessage::class,
                                    ContactMethod.WhatsAppVideoCall::class,
                                ),
                                onMethodClick = { method ->
                                    onContactMethodClick(contactInfo, method)
                                    onDismiss()
                                },
                                onMethodLongClick = { method ->
                                    val action = contactMethodToCardAction(method, selectedPhoneNumber)
                                    val actionDisplayName = methodShortcutLabel(context, method)
                                    if (action != null && actionDisplayName != null) {
                                        addToHomeHandler.addContactActionToHome(
                                            contact = contactInfo,
                                            contactAction = action,
                                            actionDisplayName = actionDisplayName,
                                        )
                                    }
                                },
                            )

                            renderMethodRow(
                                methods = methodsForSelectedNumber,
                                methodTypes = listOf(
                                    ContactMethod.TelegramMessage::class,
                                    ContactMethod.TelegramCall::class,
                                    ContactMethod.TelegramVideoCall::class,
                                ),
                                onMethodClick = { method ->
                                    onContactMethodClick(contactInfo, method)
                                    onDismiss()
                                },
                                onMethodLongClick = { method ->
                                    val action = contactMethodToCardAction(method, selectedPhoneNumber)
                                    val actionDisplayName = methodShortcutLabel(context, method)
                                    if (action != null && actionDisplayName != null) {
                                        addToHomeHandler.addContactActionToHome(
                                            contact = contactInfo,
                                            contactAction = action,
                                            actionDisplayName = actionDisplayName,
                                        )
                                    }
                                },
                            )

                            renderMethodRow(
                                methods = methodsForSelectedNumber,
                                methodTypes = listOf(
                                    ContactMethod.SignalMessage::class,
                                    ContactMethod.SignalCall::class,
                                    ContactMethod.SignalVideoCall::class,
                                ),
                                onMethodClick = { method ->
                                    onContactMethodClick(contactInfo, method)
                                    onDismiss()
                                },
                                onMethodLongClick = { method ->
                                    val action = contactMethodToCardAction(method, selectedPhoneNumber)
                                    val actionDisplayName = methodShortcutLabel(context, method)
                                    if (action != null && actionDisplayName != null) {
                                        addToHomeHandler.addContactActionToHome(
                                            contact = contactInfo,
                                            contactAction = action,
                                            actionDisplayName = actionDisplayName,
                                        )
                                    }
                                },
                            )

                            // Show message if no methods available
                            if (contactInfo.contactMethods.filterNot { it is ContactMethod.Email }.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.contacts_no_methods_available),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}