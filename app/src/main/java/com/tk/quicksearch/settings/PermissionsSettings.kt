package com.tk.quicksearch.settings.settingsDetailScreen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.permissionScreen.PermissionCard
import com.tk.quicksearch.onboarding.permissionScreen.PermissionCardItem
import com.tk.quicksearch.onboarding.permissionScreen.PermissionState
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Permissions settings screen with permission status and request options.
 */
@Composable
fun PermissionsSettings(
    onRequestUsagePermission: () -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
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
                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED,
            ),
        )
    }

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
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
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

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.permissions_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = DesignTokens.SpacingLarge),
        )

        val permissionItems =
            listOf(
                PermissionCardItem(
                    title = stringResource(R.string.settings_usage_access_title),
                    description = stringResource(R.string.permissions_usage_desc),
                    permissionState = usagePermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        if (enabled && !usagePermissionState.isGranted) {
                            usagePermissionState = usagePermissionState.copy(isEnabled = true)
                            PermissionHelper.launchUsageAccessRequest(context)
                            onRequestUsagePermission()
                        }
                    },
                ),
                PermissionCardItem(
                    title = stringResource(R.string.settings_contacts_permission_title),
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
                            onRequestContactPermission()
                        }
                    },
                ),
                PermissionCardItem(
                    title = stringResource(R.string.settings_files_permission_title),
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
                            onRequestFilePermission()
                        }
                    },
                ),
                PermissionCardItem(
                    title = stringResource(R.string.settings_call_permission_title),
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
                            onRequestCallPermission()
                        }
                    },
                ),
            )

        PermissionCard(
            items = permissionItems,
            modifier = Modifier.fillMaxWidth(),
            cardContainer = { cardModifier, content ->
                ElevatedCard(
                    modifier = cardModifier,
                    shape = DesignTokens.ExtraLargeCardShape,
                ) {
                    content()
                }
            },
        )
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

