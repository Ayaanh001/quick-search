package com.tk.quicksearch.app

import android.content.Intent
import android.os.Bundle
import android.os.Trace
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.app.navigation.MainContent
import com.tk.quicksearch.app.navigation.NavigationRequest
import com.tk.quicksearch.app.navigation.RootDestination
import com.tk.quicksearch.app.navigation.SettingsNavigationMemory
import com.tk.quicksearch.app.startup.StartupCoordinator
import com.tk.quicksearch.app.startup.StartupMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.StartupPhase
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.searchScreen.SearchScreenBackground
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.FeedbackUtils
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.widgets.searchWidget.SearchWidget
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.searchWidget.VoiceSearchHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private data class PendingContactActionPickerRequest(
        val contactId: Long,
        val isPrimary: Boolean,
        val serializedAction: String?,
    )

    companion object {
        const val ACTION_VOICE_SEARCH_SHORTCUT = "com.tk.quicksearch.action.VOICE_SEARCH_SHORTCUT"
        const val ACTION_SEARCH_TARGET_SHORTCUT = "com.tk.quicksearch.action.SEARCH_TARGET_SHORTCUT"
        const val EXTRA_SHORTCUT_QUERY = "com.tk.quicksearch.extra.SHORTCUT_QUERY"
        const val EXTRA_SHORTCUT_TARGET_ENGINE = "com.tk.quicksearch.extra.SHORTCUT_TARGET_ENGINE"

        private const val EXTRA_CONTACT_ACTION_PICKER = "overlay_contact_action_picker"
        private const val EXTRA_CONTACT_ACTION_PICKER_ID = "overlay_contact_action_picker_id"
        private const val EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY = "overlay_contact_action_picker_primary"
        private const val EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION =
            "overlay_contact_action_picker_serialized_action"
        private const val ENABLE_LAUNCH_SHELL_SWAP = true
        private const val TRACE_ON_CREATE_ENTRY = "QS.Startup.MainActivity.OnCreate"
        private const val TRACE_SET_CONTENT = "QS.Startup.MainActivity.SetContent"
        private const val TRACE_PHASE_0_SHELL = "QS.Startup.Phase0.Shell"
        private const val TRACE_FIRST_FRAME_CALLBACK = "QS.Startup.MainActivity.FirstFrameCallback"
        private const val TRACE_SEARCH_SURFACE_FIRST_COMPOSE =
            "QS.Startup.MainActivity.SearchSurfaceFirstCompose"
    }

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences
    private lateinit var startupCoordinator: StartupCoordinator
    private lateinit var voiceSearchHandler: VoiceSearchHandler
    private val voiceInputLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            voiceSearchHandler.processVoiceInputResult(result, searchViewModel::onQueryChange)
        }
    private val showReviewPromptDialog = mutableStateOf(false)
    private val showFeedbackDialog = mutableStateOf(false)
    private val navigationRequest = mutableStateOf<NavigationRequest?>(null)
    private var pendingSearchTargetShortcut: Pair<String, SearchTarget>? = null
    private var pendingContactActionPickerRequest: PendingContactActionPickerRequest? = null
    private var hasMainUiActivated = false
    private var hasSearchSurfaceComposeTraced = false
    private var hasFirstFrameTraced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection(TRACE_ON_CREATE_ENTRY)
        try {
            // Must be called before super.onCreate for edge-to-edge to work correctly on all versions
            val statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            val navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            enableEdgeToEdge(statusBarStyle, navigationBarStyle)

            super.onCreate(savedInstanceState)

            // Disable activity opening animation for instant appearance
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)

            initializePreferences()
            if (launchOverlayIfNeeded(intent)) {
                return
            }

            installFirstFrameTrace()

            Trace.beginSection(TRACE_SET_CONTENT)
            try {
                Trace.beginSection(TRACE_PHASE_0_SHELL)
                try {
                    setupContent()
                } finally {
                    Trace.endSection()
                }
            } finally {
                Trace.endSection()
            }

            startupCoordinator =
                StartupCoordinator(
                    context = this,
                    activity = this,
                    lifecycleScope = lifecycleScope,
                    viewModel = searchViewModel,
                    userPreferences = userPreferences,
                    mode = StartupMode.MAIN,
                    onReviewPromptEligible = { showReviewPromptDialog.value = true },
                )
            startupCoordinator.scheduleAfterFirstFrame(window)

            initializeVoiceSearchHandler()
            handleIntent(intent)
        } finally {
            Trace.endSection()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (launchOverlayIfNeeded(intent)) {
            return
        }
        handleIntent(intent)
        if (hasMainUiActivated) {
            maybeExecutePendingSearchTargetShortcut()
            maybeExecutePendingContactActionPickerRequest()
        }
    }

    override fun onStop() {
        super.onStop()
        searchViewModel.handleOnStop()
    }

    private fun initializePreferences() {
        userPreferences = UserAppPreferences(this)
    }

    private fun launchOverlayIfNeeded(intent: Intent?): Boolean {
        val forceNormalLaunch =
            intent?.getBooleanExtra(OverlayModeController.EXTRA_FORCE_NORMAL_LAUNCH, false)
                ?: false
        if (!forceNormalLaunch && userPreferences.isOverlayModeEnabled()) {
            val isVoiceShortcutLaunch = intent?.action == ACTION_VOICE_SEARCH_SHORTCUT
            val isAssistantLaunch = intent?.action == Intent.ACTION_ASSIST
            val startVoiceForAssistant =
                isAssistantLaunch && userPreferences.isAssistantLaunchVoiceModeEnabled()
            val startVoiceFromShortcut = isVoiceShortcutLaunch
            val startVoiceFromWidget =
                intent?.getBooleanExtra(SearchWidget.EXTRA_START_VOICE_SEARCH, false) ?: false
            val micAction =
                intent
                    ?.getStringExtra(SearchWidget.EXTRA_MIC_ACTION)
                    ?.let { actionString ->
                        MicAction.entries.find { it.value == actionString }
                    }
                    ?: MicAction.DEFAULT_VOICE_SEARCH
            OverlayModeController.startOverlay(
                context = this,
                startVoiceSearch =
                    startVoiceForAssistant || startVoiceFromShortcut || startVoiceFromWidget,
                micAction = micAction,
            )
            finish()
            return true
        }
        return false
    }

    private fun initializeVoiceSearchHandler() {
        voiceSearchHandler = VoiceSearchHandler(this, voiceInputLauncher)
    }

    private fun setupContent() {
        setContent {
            val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
            val showLaunchShell =
                ENABLE_LAUNCH_SHELL_SWAP && uiState.startupPhase == StartupPhase.PHASE_0_SHELL
            QuickSearchTheme(fontScaleMultiplier = uiState.fontScaleMultiplier) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                ) {
                    if (showLaunchShell) {
                        LaunchShell(uiState = uiState)
                    } else {
                        LaunchedEffect(Unit) {
                            if (!hasSearchSurfaceComposeTraced) {
                                hasSearchSurfaceComposeTraced = true
                                Trace.beginSection(TRACE_SEARCH_SURFACE_FIRST_COMPOSE)
                                Trace.endSection()
                            }
                            if (!hasMainUiActivated) {
                                hasMainUiActivated = true
                                maybeExecutePendingSearchTargetShortcut()
                                maybeExecutePendingContactActionPickerRequest()
                            }
                        }

                        MainContent(
                            context = this@MainActivity,
                            userPreferences = userPreferences,
                            searchViewModel = searchViewModel,
                            onSearchBackPressed = { moveTaskToBack(true) },
                            navigationRequest = navigationRequest.value,
                            onNavigationRequestHandled = { navigationRequest.value = null },
                            onFinishActivity = {
                                if (userPreferences.isOverlayModeEnabled()) {
                                    OverlayModeController.startOverlay(this@MainActivity)
                                }
                                finish()
                            },
                        )
                        if (showReviewPromptDialog.value) {
                            EnjoyingAppDialog(
                                onYes = {
                                    showReviewPromptDialog.value = false
                                    ReviewHelper.requestReviewIfEligible(
                                        this@MainActivity,
                                        userPreferences,
                                    )
                                },
                                onNo = {
                                    showReviewPromptDialog.value = false
                                    showFeedbackDialog.value = true
                                    userPreferences.recordReviewPromptTime()
                                    userPreferences.recordAppOpenCountAtPrompt()
                                    userPreferences.incrementReviewPromptedCount()
                                },
                                onDismiss = { showReviewPromptDialog.value = false },
                            )
                        }
                        if (showFeedbackDialog.value) {
                            SendFeedbackDialog(
                                onSend = { feedbackText ->
                                    FeedbackUtils.launchFeedbackEmail(
                                        this@MainActivity,
                                        feedbackText,
                                    )
                                },
                                onDismiss = { showFeedbackDialog.value = false },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun installFirstFrameTrace() {
        val decorView = window.decorView
        val observer = decorView.viewTreeObserver
        if (!observer.isAlive) return
        observer.addOnDrawListener(
            object : ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    if (hasFirstFrameTraced) return
                    hasFirstFrameTraced = true
                    Trace.beginSection(TRACE_FIRST_FRAME_CALLBACK)
                    Trace.endSection()
                    decorView.post {
                        if (decorView.viewTreeObserver.isAlive) {
                            decorView.viewTreeObserver.removeOnDrawListener(this)
                        }
                    }
                }
            },
        )
    }

    private fun handleIntent(intent: Intent?) {
        if (isExplicitLauncherLaunch(intent)) {
            navigationRequest.value = NavigationRequest(destination = RootDestination.Search)
        }

        if (intent?.action == ACTION_SEARCH_TARGET_SHORTCUT) {
            val query = intent.getStringExtra(EXTRA_SHORTCUT_QUERY)?.trim().orEmpty()
            val engineName = intent.getStringExtra(EXTRA_SHORTCUT_TARGET_ENGINE)
            val engine = engineName?.let { runCatching { SearchEngine.valueOf(it) }.getOrNull() }
            if (query.isNotBlank() && engine != null) {
                pendingSearchTargetShortcut = query to SearchTarget.Engine(engine)
            }
        }
        if (intent?.action == ACTION_VOICE_SEARCH_SHORTCUT) {
            voiceSearchHandler.handleMicAction(MicAction.DEFAULT_VOICE_SEARCH)
        }
        if (
            intent?.action == Intent.ACTION_ASSIST &&
                userPreferences.isAssistantLaunchVoiceModeEnabled()
        ) {
            voiceSearchHandler.handleMicAction(MicAction.DEFAULT_VOICE_SEARCH)
        }

        if (intent?.getBooleanExtra(OverlayModeController.EXTRA_OPEN_SETTINGS, false) == true) {
            val requestedDetail =
                intent.getStringExtra(OverlayModeController.EXTRA_OPEN_SETTINGS_DETAIL)
                    ?.let { name ->
                        runCatching { SettingsDetailType.valueOf(name) }.getOrNull()
                    }
                    ?: SettingsNavigationMemory.getLastOpenedSettingsDetail()
            navigationRequest.value =
                NavigationRequest(
                    destination = RootDestination.Settings,
                    settingsDetailType = requestedDetail,
                )
            intent.removeExtra(OverlayModeController.EXTRA_OPEN_SETTINGS)
            intent.removeExtra(OverlayModeController.EXTRA_OPEN_SETTINGS_DETAIL)
        }
        val contactActionIntent = intent
        if (contactActionIntent?.getBooleanExtra(
                EXTRA_CONTACT_ACTION_PICKER,
                false,
            ) == true
        ) {
            val contactId =
                contactActionIntent.getLongExtra(
                    EXTRA_CONTACT_ACTION_PICKER_ID,
                    -1L,
                )
            val isPrimary =
                contactActionIntent.getBooleanExtra(
                    EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY,
                    true,
                )
            val serializedAction =
                contactActionIntent.getStringExtra(
                    EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION,
                )
            if (contactId != -1L) {
                pendingContactActionPickerRequest =
                    PendingContactActionPickerRequest(
                        contactId = contactId,
                        isPrimary = isPrimary,
                        serializedAction = serializedAction,
                    )
            }
            contactActionIntent.removeExtra(EXTRA_CONTACT_ACTION_PICKER)
            contactActionIntent.removeExtra(EXTRA_CONTACT_ACTION_PICKER_ID)
            contactActionIntent.removeExtra(
                EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY,
            )
            contactActionIntent.removeExtra(
                EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION,
            )
        }

        // Handle voice search from widget
        val shouldStartVoiceSearch =
            intent?.getBooleanExtra(SearchWidget.EXTRA_START_VOICE_SEARCH, false) ?: false
        if (shouldStartVoiceSearch) {
            intent?.removeExtra(SearchWidget.EXTRA_START_VOICE_SEARCH)
            val micActionString = intent?.getStringExtra(SearchWidget.EXTRA_MIC_ACTION)
            val micAction =
                micActionString?.let { actionString ->
                    MicAction.entries.find { it.value == actionString }
                }
                    ?: MicAction.DEFAULT_VOICE_SEARCH
            voiceSearchHandler.handleMicAction(micAction)
        }
        // ACTION_ASSIST can optionally start voice typing based on user preference.
    }

    private fun isExplicitLauncherLaunch(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_MAIN) return false
        return intent.hasCategory(Intent.CATEGORY_LAUNCHER)
    }

    private fun maybeExecutePendingSearchTargetShortcut() {
        val pending = pendingSearchTargetShortcut ?: return
        lifecycleScope.launch {
            repeat(30) {
                val dispatched =
                    runCatching {
                        searchViewModel.openSearchTarget(pending.first, pending.second)
                    }.isSuccess
                if (dispatched) {
                    pendingSearchTargetShortcut = null
                    return@launch
                }
                delay(50)
            }
            pendingSearchTargetShortcut = null
        }
    }

    private fun maybeExecutePendingContactActionPickerRequest() {
        val pending = pendingContactActionPickerRequest ?: return
        pendingContactActionPickerRequest = null
        searchViewModel.requestContactActionPicker(
            contactId = pending.contactId,
            isPrimary = pending.isPrimary,
            serializedAction = pending.serializedAction,
        )
    }
}

@Composable
private fun LaunchShell(uiState: SearchUiState) {
    val cachedWallpaperBitmap = remember { WallpaperUtils.getCachedWallpaperBitmap() }
    val wallpaperImage = remember(cachedWallpaperBitmap) { cachedWallpaperBitmap?.asImageBitmap() }
    val shellApps =
        remember(uiState.pinnedApps, uiState.recentApps, uiState.searchResults) {
            buildList {
                addAll(uiState.pinnedApps)
                addAll(uiState.recentApps)
                addAll(uiState.searchResults)
            }
                .distinctBy { it.packageName }
                .take(8)
        }

    Box(modifier = Modifier.fillMaxSize()) {
        SearchScreenBackground(
            showWallpaperBackground = wallpaperImage != null,
            wallpaperBitmap = wallpaperImage,
            wallpaperBackgroundAlpha = uiState.wallpaperBackgroundAlpha,
            wallpaperBlurRadius = uiState.wallpaperBlurRadius,
            backgroundTransitionDurationMillis = 0,
            animateBlurRadius = false,
            fallbackBackgroundAlpha =
                if (uiState.backgroundSource == BackgroundSource.THEME) {
                    0.6f
                } else {
                    1f
                },
            useGradientFallback = uiState.backgroundSource == BackgroundSource.THEME,
            overlayGradientTheme = uiState.overlayGradientTheme,
            overlayThemeIntensity = uiState.overlayThemeIntensity,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(
                        start = DesignTokens.SpacingXLarge,
                        top = DesignTokens.SpacingLarge,
                        end = DesignTokens.SpacingXLarge,
                    ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.ShapeXXLarge,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = DesignTokens.SpacingLarge,
                                vertical = DesignTokens.SpacingMedium,
                            ),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .width(18.dp)
                                .height(18.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    shape = DesignTokens.ShapeFull,
                                ),
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(0.58f)
                                .height(14.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                    shape = DesignTokens.ShapeSmall,
                                ),
                    )
                }
            }

            if (shellApps.isNotEmpty()) {
                val rows = shellApps.chunked(4).take(2)
                rows.forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        rowApps.forEach { app ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = DesignTokens.ShapeLarge,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                            ) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier =
                                        Modifier.padding(
                                            horizontal = DesignTokens.SpacingMedium,
                                            vertical = DesignTokens.SpacingSmall,
                                        ),
                                )
                            }
                        }
                        repeat(4 - rowApps.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
