package com.tk.quicksearch.settings.searchEnginesScreen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.shared.util.withoutWhitespaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AddSearchEngineDialog(
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var urlInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    var nameInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    var iconBase64 by remember { mutableStateOf<String?>(null) }
    var metadataLoadedTemplate by remember { mutableStateOf<String?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    val validation = remember(urlInput.text) { validateCustomSearchTemplate(urlInput.text) }
    val validTemplate = (validation as? CustomSearchTemplateValidation.Valid)?.normalizedTemplate
    val showValidationError =
        urlInput.text.isNotBlank() && validation is CustomSearchTemplateValidation.Invalid

    val pickIconLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val encoded = loadCustomIconAsBase64(context, uri) ?: return@rememberLauncherForActivityResult
            iconBase64 = encoded
        }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(validTemplate) {
        if (validTemplate == null) {
            metadataLoadedTemplate = null
            isEditingName = false
            iconBase64 = null
            nameInput = nameInput.copy(text = "", selection = TextRange(0))
            return@LaunchedEffect
        }

        val inferredName = withContext(Dispatchers.IO) { inferCustomSearchEngineName(validTemplate) }
        val fetchedIcon = withContext(Dispatchers.IO) { fetchFaviconAsBase64(validTemplate) }

        if (!inferredName.isNullOrBlank() && !fetchedIcon.isNullOrBlank()) {
            iconBase64 = fetchedIcon
            nameInput =
                TextFieldValue(
                    text = inferredName,
                    selection = TextRange(inferredName.length),
                )
            metadataLoadedTemplate = validTemplate
            isEditingName = false
        }
    }

    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            nameFocusRequester.requestFocus()
        }
    }

    val trimmedName = nameInput.text.trim()
    val isNameValid = trimmedName.isNotBlank()
    val hasCurrentTemplateMetadata = validTemplate != null && metadataLoadedTemplate == validTemplate
    val canSave = hasCurrentTemplateMetadata && isNameValid
    val iconBitmap =
        remember(iconBase64) {
            val encoded = iconBase64 ?: return@remember null
            val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                ?: return@remember null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_add_search_engine_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (metadataLoadedTemplate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(38.dp)
                                    .offset(y = (-4).dp)
                                    .clickable {
                                        pickIconLauncher.launch(arrayOf("image/*"))
                                    },
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(34.dp)
                                        .align(androidx.compose.ui.Alignment.Center),
                            ) {
                                if (iconBitmap != null) {
                                    Image(
                                        bitmap = iconBitmap,
                                        contentDescription = trimmedName,
                                        modifier = Modifier.size(34.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Public,
                                            contentDescription = trimmedName,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier =
                                    Modifier
                                        .align(androidx.compose.ui.Alignment.BottomEnd)
                                        .offset(x = 2.dp, y = (-1).dp)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = androidx.compose.ui.Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (isEditingName) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .focusRequester(nameFocusRequester),
                                label = {
                                    Text(text = stringResource(R.string.settings_edit_search_engine_name_label))
                                },
                                singleLine = true,
                                maxLines = 1,
                                isError = !isNameValid,
                            )
                        } else {
                            Text(
                                text = nameInput.text,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .offset(y = (-2).dp)
                                        .clickable { isEditingName = true },
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settings_add_search_engine_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                TextField(
                    value = urlInput,
                    onValueChange = { newValue -> urlInput = newValue.withoutWhitespaces() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    singleLine = true,
                    maxLines = 1,
                    isError = showValidationError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (canSave) {
                                    onSave(trimmedName, validTemplate!!, iconBase64.orEmpty())
                                    onDismiss()
                                }
                            },
                        ),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                )

                when {
                    showValidationError -> {
                        when ((validation as CustomSearchTemplateValidation.Invalid).reason) {
                            CustomSearchTemplateValidation.Reason.EMPTY -> {
                                Text(
                                    text = stringResource(R.string.settings_add_search_engine_error_required),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }

                            CustomSearchTemplateValidation.Reason.MISSING_QUERY_PLACEHOLDER -> {
                                val fullMessage = stringResource(R.string.settings_add_search_engine_error_placeholder)
                                val messageText =
                                    buildAnnotatedString {
                                        val placeholderStart = fullMessage.indexOf(CUSTOM_QUERY_PLACEHOLDER)
                                        if (placeholderStart < 0) {
                                            append(fullMessage)
                                            return@buildAnnotatedString
                                        }
                                        val placeholderEnd = placeholderStart + CUSTOM_QUERY_PLACEHOLDER.length
                                        append(fullMessage.substring(0, placeholderStart))
                                        pushStringAnnotation(tag = "placeholder", annotation = CUSTOM_QUERY_PLACEHOLDER)
                                        withStyle(
                                            style =
                                                SpanStyle(
                                                    color = MaterialTheme.colorScheme.primary,
                                                ),
                                        ) {
                                            append(CUSTOM_QUERY_PLACEHOLDER)
                                        }
                                        pop()
                                        append(fullMessage.substring(placeholderEnd))
                                    }
                                ClickableText(
                                    text = messageText,
                                    style =
                                        MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.error,
                                        ),
                                    onClick = { offset ->
                                        val hasPlaceholderLink =
                                            messageText
                                                .getStringAnnotations(
                                                    tag = "placeholder",
                                                    start = offset,
                                                    end = offset,
                                                ).isNotEmpty()
                                        if (hasPlaceholderLink) {
                                            val appendedText = "${urlInput.text}$CUSTOM_QUERY_PLACEHOLDER"
                                            urlInput =
                                                TextFieldValue(
                                                    text = appendedText,
                                                    selection = TextRange(appendedText.length),
                                                )
                                        }
                                    },
                                )
                            }

                            CustomSearchTemplateValidation.Reason.MULTIPLE_QUERY_PLACEHOLDERS -> {
                                Text(
                                    text =
                                        stringResource(
                                            R.string.settings_add_search_engine_error_multiple_placeholders,
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(trimmedName, validTemplate!!, iconBase64.orEmpty())
                        onDismiss()
                    }
                },
                enabled = canSave,
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}