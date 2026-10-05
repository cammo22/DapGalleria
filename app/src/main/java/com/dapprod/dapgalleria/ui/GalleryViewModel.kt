package com.dapprod.dapgalleria.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaRepository
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.data.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Decision { KEEP, DELETE }

enum class SortOrder { RECENT, RANDOM }

data class GalleryUiState(
    val loading: Boolean = true,
    val mode: MediaType = MediaType.PHOTO,
    val sort: SortOrder = SortOrder.RECENT,
    val showReviewed: Boolean = false,
    /** Contenuti della modalità corrente ancora da valutare (più quelli già visti, per l'annulla). */
    val deck: List<MediaEntry> = emptyList(),
    val index: Int = 0,
    /** Contenuti marchiati come "da eliminare" nella sessione, i più recenti per primi. */
    val pending: List<MediaEntry> = emptyList(),
    val canUndo: Boolean = false,
    val keptCount: Int = 0,
) {
    val current: MediaEntry? get() = deck.getOrNull(index)
    val remaining: Int get() = (deck.size - index).coerceAtLeast(0)
}

private class HistoryEntry(val key: String, val decision: Decision, val wasKept: Boolean, val index: Int)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app.contentResolver)
    private val store = SessionStore(app)

    private val _state = MutableStateFlow(
        GalleryUiState(
            mode = if (store.photoMode) MediaType.PHOTO else MediaType.VIDEO,
            sort = if (store.randomOrder) SortOrder.RANDOM else SortOrder.RECENT,
            showReviewed = store.showReviewed,
        ),
    )
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    private var allItems: List<MediaEntry> = emptyList()
    private var byKey: Map<String, MediaEntry> = emptyMap()
    private val kept = HashSet<String>(store.loadKept())
    private val pendingKeys = LinkedHashSet<String>(store.loadPending())
    private val history = ArrayDeque<HistoryEntry>()
    private var shuffleSeed = System.nanoTime()
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
            allItems = items
            byKey = items.associateBy { it.key }
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

    fun setSort(sort: SortOrder) {
        if (sort == _state.value.sort) return
        store.randomOrder = sort == SortOrder.RANDOM
        shuffleSeed = System.nanoTime()
        _state.update { it.copy(sort = sort) }
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

    fun undo() {
        val e = history.removeLastOrNull() ?: return
        when (e.decision) {
            Decision.DELETE -> {
                pendingKeys.remove(e.key)
                if (e.wasKept) kept.add(e.key)
            }
            Decision.KEEP -> if (!e.wasKept) kept.remove(e.key)
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

    /** Chiamato dopo che il sistema ha eliminato davvero i file. */
    fun onDeleted(keys: Set<String>) {
        if (keys.isEmpty()) return
        pendingKeys.removeAll(keys)
        kept.removeAll(keys)
        allItems = allItems.filter { it.key !in keys }
        byKey = allItems.associateBy { it.key }
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
            )
        }
        persistNow()
    }

    private fun rebuildDeck() {
        val s = _state.value
        var list = allItems.filter {
            it.type == s.mode && it.key !in pendingKeys && (s.showReviewed || it.key !in kept)
        }
        if (s.sort == SortOrder.RANDOM) list = list.shuffled(Random(shuffleSeed))
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
