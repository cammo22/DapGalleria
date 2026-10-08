package com.dapprod.dapgalleria

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dapprod.dapgalleria.data.MediaDeleter
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.ui.GalleryViewModel
import com.dapprod.dapgalleria.ui.components.ConfettiLayer
import com.dapprod.dapgalleria.ui.components.Feedback
import com.dapprod.dapgalleria.ui.components.GameHud
import com.dapprod.dapgalleria.ui.components.LocalFeedback
import com.dapprod.dapgalleria.ui.components.NeonBackdrop
import com.dapprod.dapgalleria.ui.components.SoundFx
import com.dapprod.dapgalleria.ui.components.rememberConfettiState
import com.dapprod.dapgalleria.ui.components.rememberHudState
import com.dapprod.dapgalleria.ui.screens.MediaViewer
import com.dapprod.dapgalleria.ui.screens.PermissionScreen
import com.dapprod.dapgalleria.ui.screens.PhotoEditorScreen
import com.dapprod.dapgalleria.ui.screens.ReviewScreen
import com.dapprod.dapgalleria.ui.screens.RoundCompleteScreen
import com.dapprod.dapgalleria.ui.screens.SwipeActions
import com.dapprod.dapgalleria.ui.screens.SwipeScreen
import com.dapprod.dapgalleria.ui.screens.TrophyScreen
import com.dapprod.dapgalleria.ui.screens.VideoEditorScreen
import com.dapprod.dapgalleria.ui.theme.DapGalleriaTheme

class MainActivity : ComponentActivity() {

    // va creato prima che l'Activity sia STARTED (registra un ActivityResult launcher)
    private val deleter = MediaDeleter(this)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            DapGalleriaTheme {
                AppRoot(deleter)
            }
        }
    }
}

private fun requiredPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    else ->
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
}

private fun hasPermissions(context: Context): Boolean = requiredPermissions().all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

private enum class Page { DECK, REVIEW, TROPHIES }

@Composable
private fun AppRoot(deleter: MediaDeleter, vm: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasPermissions(context)
    }
    var page by rememberSaveable { mutableStateOf(Page.DECK) }
    // il contenuto in modifica resta fisso anche se intanto il mazzo avanza
    var editEntry by remember { mutableStateOf<MediaEntry?>(null) }
    var viewerEntry by remember { mutableStateOf<MediaEntry?>(null) }

    // suoni e vibrazioni: uno solo per tutta l'app
    val view = LocalView.current
    val sound = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sound) { onDispose { sound.release() } }
    val feedback = remember(view) { Feedback(view, sound) }
    val confetti = rememberConfettiState()
    val hud = rememberHudState()

    // se l'utente concede il permesso dalle impostazioni di sistema, al ritorno si aggiorna da solo
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasPermissions(context) }

    CompositionLocalProvider(LocalFeedback provides feedback) {
        if (!granted) {
            PermissionScreen(
                onGrant = { permissionLauncher.launch(requiredPermissions()) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                },
            )
            return@CompositionLocalProvider
        }

        LaunchedEffect(Unit) { vm.ensureLoaded() }
        val state by vm.state.collectAsStateWithLifecycle()
        SideEffect {
            feedback.soundOn = state.soundOn
            feedback.hapticsOn = state.hapticsOn
        }

        BackHandler(enabled = page != Page.DECK) { page = Page.DECK }

        Box(Modifier.fillMaxSize()) {
            NeonBackdrop()

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(260)) { -it / 4 } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(320)) { -it / 3 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(260)) { it / 4 } + fadeOut(tween(200)))
                    }
                },
                label = "page",
            ) { p ->
                when (p) {
                    Page.REVIEW -> ReviewScreen(
                        pending = state.pending,
                        onBack = { page = Page.DECK },
                        onRestore = vm::restore,
                        onDelete = { items ->
                            val deleted = deleter.delete(items.map { it.uri }).map { it.toString() }.toSet()
                            vm.onDeleted(deleted)
                        },
                    )
                    Page.TROPHIES -> TrophyScreen(state, onBack = { page = Page.DECK })
                    Page.DECK -> SwipeScreen(
                        state = state,
                        actions = SwipeActions(
                            onMode = vm::setMode,
                            onDecision = vm::decide,
                            onUndo = vm::undo,
                            onEdit = { editEntry = state.current },
                            onOpenPhoto = { viewerEntry = it },
                            onOpenTrophies = { page = Page.TROPHIES },
                            onOpenReview = { page = Page.REVIEW },
                            onRestartRound = vm::restartRound,
                            onSound = vm::setSound,
                            onHaptics = vm::setHaptics,
                        ),
                    )
                }
            }

            val summary = state.roundSummary
            AnimatedVisibility(
                visible = summary != null && page != Page.REVIEW,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut(),
            ) {
                // durante l'uscita summary è già null: si tiene l'ultimo
                val last = remember { Holder(summary) }
                if (summary != null) last.value = summary
                last.value?.let { s ->
                    RoundCompleteScreen(
                        summary = s,
                        pendingCount = state.pending.size,
                        onNewRound = { vm.startNewRound(); page = Page.DECK },
                        onReview = { page = Page.REVIEW },
                    )
                }
            }

            viewerEntry?.let { entry ->
                MediaViewer(entry = entry, onDismiss = { viewerEntry = null })
            }

            AnimatedVisibility(
                visible = editEntry != null,
                enter = slideInVertically(tween(320)) { it / 2 } + fadeIn(tween(250)),
                exit = slideOutVertically(tween(260)) { it / 2 } + fadeOut(tween(200)),
            ) {
                val last = remember { Holder(editEntry) }
                editEntry?.let { last.value = it }
                last.value?.let { entry ->
                    if (entry.type == MediaType.PHOTO) {
                        PhotoEditorScreen(
                            entry = entry,
                            onDismiss = { editEntry = null },
                            onSave = { bitmap, edit, replace -> vm.savePhotoEdit(entry, bitmap, edit, replace) },
                        )
                    } else {
                        VideoEditorScreen(
                            entry = entry,
                            onDismiss = { editEntry = null },
                            onExported = { file, replace, length -> vm.saveVideoEdit(entry, file, replace, length) },
                            onSaveFrame = { ms -> vm.saveVideoFrame(entry, ms) },
                        )
                    }
                }
            }

            GameHud(vm.events, hud, confetti)
            ConfettiLayer(confetti)
        }
    }
}

/** Tiene l'ultimo valore mentre un pannello sta uscendo con l'animazione (non è uno stato: non ricompone). */
private class Holder<T>(var value: T?)
