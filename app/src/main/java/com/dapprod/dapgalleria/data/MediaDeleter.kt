package com.dapprod.dapgalleria.data

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Elimina definitivamente foto e video dalla galleria.
 *
 * - Android 11+: richiesta di sistema unica (`createDeleteRequest`) per tutto il gruppo.
 * - Android 10: conferma di sistema per ogni file non creato dall'app.
 * - Android 8-9: cancellazione diretta (permesso di scrittura).
 *
 * Va creato durante l'inizializzazione dell'Activity (registra un ActivityResult launcher).
 */
class MediaDeleter(private val activity: ComponentActivity) {

    private var waiting: CancellableContinuation<Boolean>? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        waiting?.resume(result.resultCode == Activity.RESULT_OK)
        waiting = null
    }

    /** Ritorna gli URI effettivamente eliminati. */
    suspend fun delete(uris: List<Uri>): Set<Uri> = when {
        uris.isEmpty() -> emptySet()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> deleteWithSystemRequest(uris)
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> deleteOnApi29(uris)
        else -> deleteLegacy(uris)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun deleteWithSystemRequest(uris: List<Uri>): Set<Uri> {
        val deleted = HashSet<Uri>()
        for (chunk in uris.chunked(CHUNK_SIZE)) {
            val sender = MediaStore.createDeleteRequest(activity.contentResolver, chunk).intentSender
            if (!launch(sender)) break // l'utente ha annullato: ci si ferma
            deleted += chunk
        }
        return deleted
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun deleteOnApi29(uris: List<Uri>): Set<Uri> {
        val deleted = HashSet<Uri>()
        for (uri in uris) {
            try {
                if (tryDelete(uri)) deleted += uri
            } catch (e: RecoverableSecurityException) {
                if (launch(e.userAction.actionIntent.intentSender) && tryDelete(uri)) deleted += uri
            } catch (_: SecurityException) {
                // file non eliminabile: lo saltiamo
            }
        }
        return deleted
    }

    private suspend fun deleteLegacy(uris: List<Uri>): Set<Uri> {
        val deleted = HashSet<Uri>()
        for (uri in uris) {
            try {
                if (tryDelete(uri)) deleted += uri
            } catch (_: SecurityException) {
                // file non eliminabile: lo saltiamo
            }
        }
        return deleted
    }

    private suspend fun tryDelete(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        activity.contentResolver.delete(uri, null, null) > 0
    }

    private suspend fun launch(sender: IntentSender): Boolean = suspendCancellableCoroutine { cont ->
        waiting = cont
        cont.invokeOnCancellation { waiting = null }
        launcher.launch(IntentSenderRequest.Builder(sender).build())
    }

    private companion object {
        const val CHUNK_SIZE = 500
    }
}
