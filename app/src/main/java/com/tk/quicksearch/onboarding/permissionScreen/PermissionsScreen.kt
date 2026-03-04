package com.tk.quicksearch.onboarding.permissionScreen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.OnboardingHeader
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Main permissions screen that allows users to grant optional permissions for enhanced functionality.
 * Displays cards for usage access, contacts, and files permissions with toggle controls.
 * Shows a reminder dialog if user tries to continue without granting all permissions.
 *
 * @param onPermissionsComplete Callback invoked when user wants to proceed (with or without permissions)
 * @param modifier Modifier for the composable
 */
@Composable
fun PermissionsScreen(
    onPermissionsComplete: () -> Unit,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appsRepository = remember { AppsRepository(context) }
    val contactRepository = remember { ContactRepository(context) }
    val fileRepository = remember { FileSearchRepository(context) }

    var usagePermissionState by remember {
        mutableStateOf(createInitialPermissionState(appsRepository.hasUsageAccess()))
    }
    var contactsPermissionState by remember {
        mutableStateOf(createInitialPermissionState(contactRepository.hasPermission()))
    }
    var filesPermissionState by remember {
        mutableStateOf(createInitialPermissionState(fileRepository.hasPermission()))
    }
    var callingPermissionState by remember {
        mutableStateOf(
            createInitialPermissionState(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED,
            ),
        )
    }

    var showPermissionReminderDialog by remember { mutableStateOf(false) }

    val totalSteps = if (!contactsPermissionState.isGranted && !filesPermissionState.isGranted) 2 else 3

    val multiplePermissionsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            permissions[Manifest.permission.READ_CONTACTS]?.let { contactsGranted ->
                contactsPermissionState =
                    updatePermissionState(
                        isGranted = contactsGranted,
                        isEnabled = contactsGranted,
                        wasDenied = !contactsGranted,
                    )
            }

            permissions[Manifest.permission.CALL_PHONE]?.let { callingGranted ->
                callingPermissionState =
                    updatePermissionState(
                        isGranted = callingGranted,
                        isEnabled = callingGranted,
                        wasDenied = !callingGranted,
                    )
            }

            // Handle files permission for pre-R Android
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE]?.let { filesGranted ->
                    filesPermissionState =
                        updatePermissionState(
                            filesGranted,
                            filesGranted,
                            wasDenied = !filesGranted,
                        )
                }
            }
        }

    val allFilesAccessLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            val filesGranted = PermissionHelper.checkFilesPermission(context)
            filesPermissionState = updatePermissionState(filesGranted, filesGranted)
        }

    // Refresh permissions when activity resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val hasUsageAccess = appsRepository.hasUsageAccess()
                    val hasContactsPermission = contactRepository.hasPermission()
                    val hasFilesPermission = fileRepository.hasPermission()

                    usagePermissionState = updatePermissionState(hasUsageAccess, hasUsageAccess)

                    if (hasContactsPermission) {
                        contactsPermissionState = updatePermissionState(hasContactsPermission, true)
                    }

                    if (hasFilesPermission) {
                        filesPermissionState = updatePermissionState(hasFilesPermission, true, wasDenied = false)
                    }

                    val hasCallingPermission =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                    callingPermissionState =
                        if (hasCallingPermission) {
                            updatePermissionState(isGranted = true, isEnabled = true, wasDenied = false)
                        } else {
                            callingPermissionState.copy(isGranted = false)
                        }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .padding(horizontal = DesignTokens.OnboardingHorizontalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        OnboardingHeader(
            title = stringResource(R.string.permissions_screen_title),
            currentStep = currentStep,
            totalSteps = totalSteps,
        )

        Text(
            text = stringResource(R.string.permissions_screen_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
        )

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
        ) {
            val permissionItems =
                listOf(
                    PermissionCardItem(
                        title = stringResource(R.string.permissions_usage_title),
                        description = stringResource(R.string.permissions_usage_desc),
                        permissionState = usagePermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            if (enabled && !usagePermissionState.isGranted) {
                                PermissionHelper.launchUsageAccessRequest(context)
                            }
                        },
                    ),
                    PermissionCardItem(
                        title = stringResource(R.string.permissions_contacts_title),
                        description = stringResource(R.string.permissions_contacts_desc),
                        permissionState = contactsPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            contactsPermissionState = contactsPermissionState.copy(isEnabled = enabled)
                            if (enabled && !contactsPermissionState.isGranted) {
                                PermissionHelper.requestRuntimePermissionOrOpenSettings(
                                    context = context,
                                    permission = Manifest.permission.READ_CONTACTS,
                                    wasPreviouslyDenied = contactsPermissionState.wasDenied,
                                    runtimeLauncher = multiplePermissionsLauncher,
                                )
                            }
                        },
                    ),
                    PermissionCardItem(
                        title = stringResource(R.string.permissions_files_title),
                        description = stringResource(R.string.permissions_files_desc),
                        permissionState = filesPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            filesPermissionState = filesPermissionState.copy(isEnabled = enabled)
                            if (enabled && !filesPermissionState.isGranted) {
                                PermissionHelper.requestFilesPermission(
                                    context = context,
                                    wasPreviouslyDenied = filesPermissionState.wasDenied,
                                    runtimeLauncher = multiplePermissionsLauncher,
                                    allFilesLauncher = allFilesAccessLauncher,
                                )
                            }
                        },
                    ),
                    PermissionCardItem(
                        title = stringResource(R.string.permissions_calling_title),
                        description = stringResource(R.string.permissions_calling_desc),
                        permissionState = callingPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            callingPermissionState = callingPermissionState.copy(isEnabled = enabled)
                            if (enabled && !callingPermissionState.isGranted) {
                                PermissionHelper.requestRuntimePermissionOrOpenSettings(
                                    context = context,
                                    permission = Manifest.permission.CALL_PHONE,
                                    wasPreviouslyDenied = callingPermissionState.wasDenied,
                                    runtimeLauncher = multiplePermissionsLauncher,
                                )
                            }
                        },
                    ),
                )

            PermissionCard(
                items = permissionItems,
                modifier = Modifier.fillMaxWidth(),
                cardContainer = { cardModifier, content ->
                    Card(
                        modifier = cardModifier,
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        shape = RoundedCornerShape(DesignTokens.OnboardingPermissionCardCornerRadius),
                    ) {
                        content()
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingCompactSpacing))

        Button(
            onClick = {
                val hasUngrantedPermissions =
                    !usagePermissionState.isGranted ||
                        !contactsPermissionState.isGranted ||
                        !filesPermissionState.isGranted ||
                        !callingPermissionState.isGranted

                if (hasUngrantedPermissions) {
                    showPermissionReminderDialog = true
                } else {
                    onPermissionsComplete()
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.OnboardingButtonOuterHorizontalPadding),
            shape = RoundedCornerShape(DesignTokens.OnboardingButtonCornerRadius),
            contentPadding =
                PaddingValues(
                    horizontal = DesignTokens.OnboardingButtonHorizontalPadding,
                    vertical = DesignTokens.OnboardingButtonVerticalPadding,
                ),
        ) {
            Text(
                text = stringResource(R.string.setup_action_next),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))
    }

    // Permission reminder dialog
    if (showPermissionReminderDialog) {
        PermissionReminderDialog(
            usagePermissionState = usagePermissionState,
            contactsPermissionState = contactsPermissionState,
            filesPermissionState = filesPermissionState,
            callingPermissionState = callingPermissionState,
            onDismiss = { showPermissionReminderDialog = false },
            onContinue = {
                showPermissionReminderDialog = false
                onPermissionsComplete()
            },
        )
    }
}

/**
 * Dialog that reminds users they can grant permissions later from app settings.
 */
@Composable
private fun PermissionReminderDialog(
    usagePermissionState: PermissionState,
    contactsPermissionState: PermissionState,
    filesPermissionState: PermissionState,
    callingPermissionState: PermissionState,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val permissionsList =
        listOfNotNull(
            stringResource(R.string.permissions_usage_title).takeIf { !usagePermissionState.isGranted },
            stringResource(R.string.permissions_contacts_title).takeIf { !contactsPermissionState.isGranted },
            stringResource(R.string.permissions_files_title).takeIf { !filesPermissionState.isGranted },
            stringResource(R.string.permissions_calling_title).takeIf { !callingPermissionState.isGranted },
        ).joinToString(", ")

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(AppColors.OverlayVeryHigh)
                    .blur(radius = DesignTokens.OnboardingDialogBlurRadius),
            contentAlignment = Alignment.Center,
        ) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = stringResource(R.string.permissions_reminder_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Text(
                        text =
                            stringResource(
                                R.string.permissions_reminder_dialog_message,
                                permissionsList,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                confirmButton = {
                    Button(onClick = onContinue) {
                        Text(stringResource(R.string.dialog_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }
    }
}

/**
 * Creates initial permission state. If permission is already granted, creates a granted state.
 * Otherwise creates an initial state (not granted, not enabled).
 */
private fun createInitialPermissionState(isGranted: Boolean): PermissionState =
    if (isGranted) {
        PermissionState.granted()
    } else {
        PermissionState.initial()
    }

/**
 * Creates a new permission state with the given parameters.
 * Used to update permission states after permission checks or user interactions.
 */
private fun updatePermissionState(
    isGranted: Boolean,
    isEnabled: Boolean,
    wasDenied: Boolean = false,
): PermissionState =
    PermissionState(
        isGranted = isGranted,
        isEnabled = isEnabled,
        wasDenied = wasDenied,
    )

