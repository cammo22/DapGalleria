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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dapprod.dapgalleria.data.MediaDeleter
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.ui.GalleryViewModel
import com.dapprod.dapgalleria.ui.screens.CropDialog
import com.dapprod.dapgalleria.ui.screens.PermissionScreen
import com.dapprod.dapgalleria.ui.screens.ReviewScreen
import com.dapprod.dapgalleria.ui.screens.SwipeActions
import com.dapprod.dapgalleria.ui.screens.StatsDialog
import com.dapprod.dapgalleria.ui.screens.SwipeScreen
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

@Composable
private fun AppRoot(deleter: MediaDeleter, vm: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasPermissions(context)
    }
    var showReview by rememberSaveable { mutableStateOf(false) }
    var showStats by rememberSaveable { mutableStateOf(false) }
    // la foto in ritaglio resta fissa anche se intanto il mazzo avanza
    var cropEntry by remember { mutableStateOf<MediaEntry?>(null) }

    // se l'utente concede il permesso dalle impostazioni di sistema, al ritorno si aggiorna da solo
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasPermissions(context) }

    if (!granted) {
        PermissionScreen(
            onGrant = { permissionLauncher.launch(requiredPermissions()) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
        )
        return
    }

    LaunchedEffect(Unit) { vm.ensureLoaded() }

    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler(enabled = showReview) { showReview = false }

    if (showReview) {
        ReviewScreen(
            pending = state.pending,
            onBack = { showReview = false },
            onRestore = vm::restore,
            onDelete = { items ->
                val deleted = deleter.delete(items.map { it.uri }).map { it.toString() }.toSet()
                vm.onDeleted(deleted)
            },
        )
    } else {
        SwipeScreen(
            state = state,
            actions = SwipeActions(
                onMode = vm::setMode,
                onDecision = vm::decide,
                onUndo = vm::undo,
                onCrop = { cropEntry = state.current?.takeIf { it.type == MediaType.PHOTO } },
                onOpenStats = { showStats = true },
                onOpenReview = { showReview = true },
                onShowReviewed = vm::setShowReviewed,
                onResetKept = vm::resetKept,
            ),
        )
    }

    if (showStats) StatsDialog(state.stats, onDismiss = { showStats = false })

    cropEntry?.let { entry ->
        CropDialog(
            entry = entry,
            onDismiss = { cropEntry = null },
            onSave = { bitmap, rect -> vm.cropCurrent(bitmap, rect) },
        )
    }
}
