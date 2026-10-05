package com.dapprod.dapgalleria.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dapprod.dapgalleria.data.CropRect
import com.dapprod.dapgalleria.data.ImageCropper
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaRepository
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.data.SessionStore
import com.dapprod.dapgalleria.data.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Decision { KEEP, DELETE }

data class GalleryUiState(
    val loading: Boolean = true,
    val mode: MediaType = MediaType.PHOTO,
    val showReviewed: Boolean = false,
    /** Contenuti della modalità corrente, in ordine casuale (più quelli già visti, per l'annulla). */
    val deck: List<MediaEntry> = emptyList(),
    val index: Int = 0,
    /** Contenuti marchiati come "da eliminare" nella sessione, i più recenti per primi. */
    val pending: List<MediaEntry> = emptyList(),
    val canUndo: Boolean = false,
    val keptCount: Int = 0,
    val stats: Stats = Stats(),
) {
    val current: MediaEntry? get() = deck.getOrNull(index)
    val remaining: Int get() = (deck.size - index).coerceAtLeast(0)
}

/** Esito di un'eliminazione definitiva, per mostrare i punti guadagnati. */
data class DeleteResult(val deletedKeys: Set<String>, val freedBytes: Long, val points: Long)

private class HistoryEntry(
    val key: String,
    val decision: Decision,
    val wasKept: Boolean,
    val index: Int,
    /** Se lo swipe era un ritaglio: la copia ritagliata creata (da rimuovere se si annulla). */
    val croppedUri: Uri? = null,
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app.contentResolver)
    private val store = SessionStore(app)

    private val _state = MutableStateFlow(
        GalleryUiState(
            mode = if (store.photoMode) MediaType.PHOTO else MediaType.VIDEO,
            showReviewed = store.showReviewed,
            stats = store.loadStats(),
        ),
    )
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    private var allItems: List<MediaEntry> = emptyList()
    private var byKey: Map<String, MediaEntry> = emptyMap()
    private val kept = HashSet<String>(store.loadKept())
    private val pendingKeys = LinkedHashSet<String>(store.loadPending())
    private val history = ArrayDeque<HistoryEntry>()
    private var persistJob: Job? = null
    private var started = false

    /** Carica la galleria una sola volta (sopravvive alle rotazioni). Da chiamare con i permessi concessi. */
    fun ensureLoaded() {
        if (started) return
        started = true
        load()
    }

    /** (Ri)carica la galleria da MediaStore. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val items = try {
                repository.loadAll()
            } catch (_: SecurityException) {
                emptyList()
            }
            setItems(items)
            if (items.isNotEmpty()) {
                // dimentica le decisioni su file che non esistono più
                pendingKeys.retainAll(byKey.keys)
                kept.retainAll(byKey.keys)
                persistNow()
            }
            rebuildDeck()
        }
    }

    fun setMode(mode: MediaType) {
        if (mode == _state.value.mode) return
        store.photoMode = mode == MediaType.PHOTO
        _state.update { it.copy(mode = mode) }
        rebuildDeck()
    }

    fun setShowReviewed(show: Boolean) {
        store.showReviewed = show
        _state.update { it.copy(showReviewed = show) }
        rebuildDeck()
    }

    /** Dimentica i contenuti "tenuti": torneranno a essere proposti. */
    fun resetKept() {
        kept.clear()
        persistNow()
        rebuildDeck()
    }

    fun decide(decision: Decision) {
        val s = _state.value
        val item = s.current ?: return
        history.addLast(HistoryEntry(item.key, decision, wasKept = item.key in kept, index = s.index))
        when (decision) {
            Decision.KEEP -> kept.add(item.key)
            Decision.DELETE -> {
                kept.remove(item.key)
                pendingKeys.add(item.key)
            }
        }
        publish(index = s.index + 1)
        persistSoon()
    }

    /**
     * Ritaglia la foto in cima al mazzo: la versione ritagliata viene salvata e tenuta,
     * l'originale finisce nella lista "da eliminare". Ritorna false se il salvataggio fallisce.
     */
    suspend fun cropCurrent(source: Bitmap, crop: CropRect): Boolean {
        val s = _state.value
        val original = s.current ?: return false
        if (original.type != MediaType.PHOTO) return false
        val context = getApplication<Application>()
        val newUri = ImageCropper.saveCropped(context, original, source, crop) ?: return false
        val created = repository.loadPhoto(newUri)
        if (created == null) {
            ImageCropper.deleteOwn(context, newUri)
            return false
        }

        // la copia ritagliata entra nella galleria come "tenuta"; l'originale va tra i da eliminare
        setItems(allItems + created)
        kept.add(created.key)
        history.addLast(HistoryEntry(original.key, Decision.DELETE, wasKept = original.key in kept, index = s.index, croppedUri = newUri))
        kept.remove(original.key)
        pendingKeys.add(original.key)
        publish(index = s.index + 1)
        persistSoon()
        return true
    }

    fun undo() {
        val e = history.removeLastOrNull() ?: return
        when (e.decision) {
            Decision.DELETE -> {
                pendingKeys.remove(e.key)
                if (e.wasKept) kept.add(e.key)
            }
            Decision.KEEP -> if (!e.wasKept) kept.remove(e.key)
        }
        if (e.croppedUri != null) {
            // annullare un ritaglio elimina anche la copia ritagliata
            val key = e.croppedUri.toString()
            kept.remove(key)
            setItems(allItems.filter { it.key != key })
            viewModelScope.launch { ImageCropper.deleteOwn(getApplication(), e.croppedUri) }
        }
        publish(index = e.index)
        persistSoon()
    }

    /** Toglie dei contenuti dalla lista "da eliminare" (swipe fatto per sbaglio): restano nella galleria. */
    fun restore(keys: Collection<String>) {
        if (keys.isEmpty()) return
        val set = keys.toSet()
        pendingKeys.removeAll(set)
        kept.addAll(set)
        history.removeAll { it.key in set }
        publish(index = _state.value.index)
        persistSoon()
    }

    /** Chiamato dopo che il sistema ha eliminato davvero i file: assegna i punti per lo spazio liberato. */
    fun onDeleted(keys: Set<String>): DeleteResult {
        if (keys.isEmpty()) return DeleteResult(emptySet(), 0L, 0L)
        val removed = keys.mapNotNull { byKey[it] }
        val freed = removed.sumOf { it.sizeBytes }
        val points = removed.sumOf { Stats.pointsFor(it.sizeBytes) }
        val stats = _state.value.stats.let {
            Stats(it.score + points, it.freedBytes + freed, it.deletedCount + removed.size)
        }
        store.saveStats(stats)

        pendingKeys.removeAll(keys)
        kept.removeAll(keys)
        setItems(allItems.filter { it.key !in keys })
        history.clear()
        _state.update { s ->
            // i file eliminati erano tutti già stati swipati, quindi stanno prima di `index`
            val removedBefore = s.deck.take(s.index).count { it.key in keys }
            s.copy(
                deck = s.deck.filter { it.key !in keys },
                index = s.index - removedBefore,
                pending = pendingList(),
                canUndo = false,
                keptCount = kept.size,
                stats = stats,
            )
        }
        persistNow()
        return DeleteResult(keys, freed, points)
    }

    private fun setItems(items: List<MediaEntry>) {
        allItems = items
        byKey = items.associateBy { it.key }
    }

    /** Il mazzo è sempre in ordine casuale: ogni volta che viene ricostruito si rimescola. */
    private fun rebuildDeck() {
        val s = _state.value
        val list = allItems
            .filter { it.type == s.mode && it.key !in pendingKeys && (s.showReviewed || it.key !in kept) }
            .shuffled()
        history.clear()
        _state.update {
            it.copy(
                loading = false,
                deck = list,
                index = 0,
                canUndo = false,
                pending = pendingList(),
                keptCount = kept.size,
            )
        }
    }

    private fun publish(index: Int) {
        _state.update {
            it.copy(
                index = index,
                pending = pendingList(),
                canUndo = history.isNotEmpty(),
                keptCount = kept.size,
            )
        }
    }

    private fun pendingList(): List<MediaEntry> = pendingKeys.mapNotNull { byKey[it] }.asReversed()

    private fun persistSoon() {
        val keptSnapshot = HashSet(kept)
        val pendingSnapshot = pendingKeys.toList()
        persistJob?.cancel()
        persistJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            store.save(keptSnapshot, pendingSnapshot)
        }
    }

    private fun persistNow() {
        persistJob?.cancel()
        store.save(kept, pendingKeys.toList())
    }

    override fun onCleared() {
        persistNow()
    }
}
