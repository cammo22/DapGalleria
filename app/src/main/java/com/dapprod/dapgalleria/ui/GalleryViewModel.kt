package com.dapprod.dapgalleria.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaRepository
import com.dapprod.dapgalleria.data.MediaSaver
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.data.PhotoEdit
import com.dapprod.dapgalleria.data.PhotoEditing
import com.dapprod.dapgalleria.data.SessionStore
import com.dapprod.dapgalleria.data.VideoEditing
import com.dapprod.dapgalleria.game.GameEngine
import com.dapprod.dapgalleria.game.Outcome
import com.dapprod.dapgalleria.game.Profile
import com.dapprod.dapgalleria.game.RoundSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

enum class Decision { KEEP, DELETE }

data class GalleryUiState(
    val loading: Boolean = true,
    val mode: MediaType = MediaType.PHOTO,
    /** Contenuti della modalità corrente ancora da vedere in questo giro, in ordine casuale (più quelli già visti, per l'annulla). */
    val deck: List<MediaEntry> = emptyList(),
    val index: Int = 0,
    /** Contenuti marchiati come "da eliminare", i più recenti per primi. */
    val pending: List<MediaEntry> = emptyList(),
    val canUndo: Boolean = false,
    val profile: Profile = Profile(),
    /** La combo in corso e quando scade (System.currentTimeMillis). */
    val combo: Int = 0,
    val multiplier: Int = 1,
    val comboExpiresAt: Long = 0,
    /** Il giro: quanti contenuti ci sono in tutto e quanti sono già stati visti. */
    val totalItems: Int = 0,
    val reviewedItems: Int = 0,
    val photosLeft: Int = 0,
    val videosLeft: Int = 0,
    /** Giro appena finito: si mostra la festa finché non se ne comincia un altro. */
    val roundSummary: RoundSummary? = null,
    /** Da che lato rientra la carta quando si annulla. */
    val lastUndone: Decision? = null,
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
) {
    val current: MediaEntry? get() = deck.getOrNull(index)
    val remaining: Int get() = (deck.size - index).coerceAtLeast(0)
    val roundProgress: Float get() = if (totalItems == 0) 0f else reviewedItems.toFloat() / totalItems
    val pendingBytes: Long get() = pending.sumOf { it.sizeBytes }

    fun isGolden(entry: MediaEntry): Boolean = GameEngine.isGolden(entry.key, profile.round)
}

/** Esito di un'eliminazione definitiva, per festeggiare. */
data class DeleteResult(val deletedKeys: Set<String>, val freedBytes: Long, val points: Long)

/** Quello che la UI deve festeggiare. */
sealed interface GameEvent {
    data class Swiped(val decision: Decision, val outcome: Outcome) : GameEvent
    data class Edited(val outcome: Outcome) : GameEvent
    data class Deleted(val count: Int, val freedBytes: Long, val streakMultiplier: Float, val outcome: Outcome) : GameEvent
    data class RoundDone(val summary: RoundSummary, val outcome: Outcome) : GameEvent
    data class Message(val text: String) : GameEvent
}

private class HistoryEntry(
    val key: String,
    val decision: Decision,
    val wasKept: Boolean,
    val index: Int,
    val points: Long,
    val golden: Boolean,
    /** Se era una modifica: il contenuto nuovo creato (da rimuovere se si annulla). */
    val createdUri: Uri? = null,
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app.contentResolver)
    private val store = SessionStore(app)
    private val engine = GameEngine(store.loadProfile())

    private val _state = MutableStateFlow(
        GalleryUiState(
            mode = if (store.photoMode) MediaType.PHOTO else MediaType.VIDEO,
            profile = engine.profile,
            soundOn = store.soundOn,
            hapticsOn = store.hapticsOn,
        ),
    )
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var allItems: List<MediaEntry> = emptyList()
    private var byKey: Map<String, MediaEntry> = emptyMap()
    private val kept = HashSet<String>(store.loadKept())
    private val pendingKeys = LinkedHashSet<String>(store.loadPending())
    private val history = ArrayDeque<HistoryEntry>()
    private var persistJob: Job? = null
    private var started = false

    private fun today(): Long = LocalDate.now().toEpochDay()

    init {
        engine.refreshDay(today())
        _state.update { it.copy(profile = engine.profile) }
    }

    /** Carica la galleria una sola volta (sopravvive alle rotazioni). Da chiamare con i permessi concessi. */
    fun ensureLoaded() {
        if (started) return
        started = true
        load()
    }

    /** (Ri)carica la galleria da MediaStore: le foto nuove entrano nel giro. */
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

    fun setSound(on: Boolean) {
        store.soundOn = on
        _state.update { it.copy(soundOn = on) }
    }

    fun setHaptics(on: Boolean) {
        store.hapticsOn = on
        _state.update { it.copy(hapticsOn = on) }
    }

    fun decide(decision: Decision) {
        val s = _state.value
        val item = s.current ?: return
        val golden = s.isGolden(item)
        val outcome = engine.swipe(golden, System.currentTimeMillis(), today())
        history.addLast(HistoryEntry(item.key, decision, wasKept = item.key in kept, index = s.index, points = outcome.points + outcome.bonusPoints, golden = golden))
        when (decision) {
            Decision.KEEP -> kept.add(item.key)
            Decision.DELETE -> {
                kept.remove(item.key)
                pendingKeys.add(item.key)
            }
        }
        publish(index = s.index + 1)
        _events.tryEmit(GameEvent.Swiped(decision, outcome))
        persistSoon()
        checkRoundComplete()
    }

    /**
     * Salva la foto modificata. Con [replace] l'originale va tra i da eliminare, altrimenti si tengono tutte e due.
     * Ritorna false se il salvataggio non riesce.
     */
    suspend fun savePhotoEdit(original: MediaEntry, source: Bitmap, edit: PhotoEdit, replace: Boolean): Boolean {
        val uri = PhotoEditing.save(getApplication(), original, source, edit) ?: return false
        return applyEdit(original, MediaType.PHOTO, uri, replace, durationHint = 0)
    }

    /** Salva il video esportato dall'editor (file temporaneo). */
    suspend fun saveVideoEdit(original: MediaEntry, file: File, replace: Boolean, lengthMs: Long): Boolean {
        val uri = VideoEditing.saveExport(getApplication(), original, file) ?: return false
        return applyEdit(original, MediaType.VIDEO, uri, replace, durationHint = lengthMs)
    }

    /** Salva un fotogramma del video come foto nuova (tenuta). Il mazzo non avanza. */
    suspend fun saveVideoFrame(original: MediaEntry, timeMs: Long): Boolean {
        val uri = VideoEditing.saveFrame(getApplication(), original, timeMs) ?: return false
        val created = repository.loadOne(MediaType.PHOTO, uri) ?: fallbackEntry(MediaType.PHOTO, uri, original, 0)
        setItems(allItems + created)
        kept.add(created.key)
        val outcome = engine.edited(today())
        publish(index = _state.value.index)
        _events.tryEmit(GameEvent.Edited(outcome))
        persistSoon()
        return true
    }

    private suspend fun applyEdit(original: MediaEntry, type: MediaType, uri: Uri, replace: Boolean, durationHint: Long): Boolean {
        val created = repository.loadOne(type, uri) ?: fallbackEntry(type, uri, original, durationHint)
        val s = _state.value
        val isCurrent = s.current?.key == original.key
        setItems(allItems + created)
        kept.add(created.key)

        val decision = if (replace) Decision.DELETE else Decision.KEEP
        val golden = s.isGolden(original)
        val swipe = engine.swipe(golden, System.currentTimeMillis(), today())
        val edit = engine.edited(today())
        val outcome = edit.copy(
            points = swipe.points + edit.points,
            combo = swipe.combo,
            multiplier = swipe.multiplier,
            golden = golden,
            quests = swipe.quests + edit.quests,
            chest = swipe.chest || edit.chest,
            achievements = swipe.achievements + edit.achievements,
            levelUp = edit.levelUp ?: swipe.levelUp,
            bonusPoints = swipe.bonusPoints + edit.bonusPoints,
        )
        history.addLast(
            HistoryEntry(
                original.key, decision, wasKept = original.key in kept, index = s.index,
                points = outcome.points + outcome.bonusPoints, golden = golden, createdUri = uri,
            ),
        )
        if (replace) {
            kept.remove(original.key)
            pendingKeys.add(original.key)
        } else {
            kept.add(original.key)
        }
        publish(index = if (isCurrent) s.index + 1 else s.index)
        _events.tryEmit(GameEvent.Edited(outcome))
        persistSoon()
        checkRoundComplete()
        return true
    }

    /** Se MediaStore non ha ancora la riga del file nuovo, la si costruisce da quello che si sa. */
    private fun fallbackEntry(type: MediaType, uri: Uri, original: MediaEntry, durationMs: Long) = MediaEntry(
        id = try { android.content.ContentUris.parseId(uri) } catch (_: Exception) { -1L },
        type = type,
        uri = uri,
        name = MediaSaver.editedName(original.name, if (type == MediaType.VIDEO) "mp4" else "jpg"),
        sizeBytes = 0,
        dateMillis = original.dateMillis,
        durationMs = durationMs,
    )

    fun undo() {
        val e = history.removeLastOrNull() ?: return
        when (e.decision) {
            Decision.DELETE -> {
                pendingKeys.remove(e.key)
                if (e.wasKept) kept.add(e.key)
            }
            Decision.KEEP -> if (!e.wasKept) kept.remove(e.key)
        }
        if (e.createdUri != null) {
            // annullare una modifica elimina anche la copia creata
            val key = e.createdUri.toString()
            kept.remove(key)
            setItems(allItems.filter { it.key != key })
            viewModelScope.launch { MediaSaver.deleteOwn(getApplication(), e.createdUri) }
        }
        engine.undoSwipe(e.points, e.golden)
        _state.update { it.copy(lastUndone = e.decision) }
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
        val base = removed.sumOf { GameEngine.pointsForDelete(it.sizeBytes) }
        val streak = engine.profile.streakMultiplier
        val outcome = engine.deleted(removed.size, freed, base, today())

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
            )
        }
        publish(index = _state.value.index)
        persistNow()
        _events.tryEmit(GameEvent.Deleted(removed.size, freed, streak, outcome))
        return DeleteResult(keys, freed, outcome.points)
    }

    /** Dopo la festa di fine giro: si rilegge la galleria e tutto torna nel mazzo. */
    fun startNewRound() {
        kept.clear()
        history.clear()
        _state.update { it.copy(roundSummary = null) }
        persistNow()
        load()
    }

    /** Ricomincia subito il giro (dal menu), senza premio. */
    fun restartRound() {
        engine.restartRound()
        startNewRound()
    }

    /** Si è visto tutto (foto e video): si chiude il giro col suo premio. */
    private fun checkRoundComplete() {
        val s = _state.value
        if (s.roundSummary != null || allItems.isEmpty()) return
        if (s.photosLeft > 0 || s.videosLeft > 0) return
        // un giro vale solo se in questo giro hai valutato qualcosa (niente premi a ripetizione)
        if (engine.profile.roundReviewed == 0) return
        val (summary, outcome) = engine.completeRound(today())
        // da qui il giro nuovo è già cominciato: se l'app si chiude ora, alla riapertura il mazzo è di nuovo pieno
        kept.clear()
        history.clear()
        _state.update { it.copy(roundSummary = summary, canUndo = false, profile = engine.profile) }
        persistNow()
        _events.tryEmit(GameEvent.RoundDone(summary, outcome))
    }

    private fun setItems(items: List<MediaEntry>) {
        allItems = items
        byKey = items.associateBy { it.key }
    }

    private fun isReviewed(key: String) = key in kept || key in pendingKeys

    /** Il mazzo è sempre in ordine casuale: ogni volta che viene ricostruito si rimescola. */
    private fun rebuildDeck() {
        val s = _state.value
        val list = allItems
            .filter { it.type == s.mode && !isReviewed(it.key) }
            .shuffled()
        history.clear()
        _state.update { it.copy(loading = false, deck = list, index = 0) }
        publish(index = 0)
        if (_state.value.roundSummary == null && allItems.isNotEmpty()) checkRoundComplete()
    }

    private fun publish(index: Int) {
        val now = System.currentTimeMillis()
        var photosLeft = 0
        var videosLeft = 0
        var reviewed = 0
        for (it in allItems) {
            if (isReviewed(it.key)) {
                reviewed++
            } else if (it.type == MediaType.PHOTO) {
                photosLeft++
            } else {
                videosLeft++
            }
        }
        _state.update {
            it.copy(
                index = index,
                pending = pendingList(),
                canUndo = history.isNotEmpty(),
                profile = engine.profile,
                combo = engine.combo,
                multiplier = engine.multiplier,
                comboExpiresAt = now + engine.comboTimeLeft(now),
                totalItems = allItems.size,
                reviewedItems = reviewed,
                photosLeft = photosLeft,
                videosLeft = videosLeft,
            )
        }
    }

    private fun pendingList(): List<MediaEntry> = pendingKeys.mapNotNull { byKey[it] }.asReversed()

    private fun persistSoon() {
        val keptSnapshot = HashSet(kept)
        val pendingSnapshot = pendingKeys.toList()
        val profile = engine.profile
        persistJob?.cancel()
        persistJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            store.save(keptSnapshot, pendingSnapshot)
            store.saveProfile(profile)
        }
    }

    private fun persistNow() {
        persistJob?.cancel()
        store.save(kept, pendingKeys.toList())
        store.saveProfile(engine.profile)
    }

    override fun onCleared() {
        persistNow()
    }
}
