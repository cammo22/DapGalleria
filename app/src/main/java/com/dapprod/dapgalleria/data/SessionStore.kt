package com.dapprod.dapgalleria.data

import android.content.Context
import androidx.core.content.edit
import com.dapprod.dapgalleria.game.Profile
import org.json.JSONArray
import org.json.JSONObject

/** Salva su disco le decisioni dell'utente e la partita, cosi' non si perdono se l'app viene chiusa. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("dapgalleria_session", Context.MODE_PRIVATE)

    /** Contenuti che hai deciso di tenere nel giro in corso (non vengono più riproposti). */
    fun loadKept(): Set<String> = prefs.getStringSet(KEPT, emptySet())?.toSet() ?: emptySet()

    /** Contenuti marchiati "da eliminare", in ordine di swipe. */
    fun loadPending(): List<String> =
        prefs.getString(PENDING, "").orEmpty().split('\n').filter { it.isNotEmpty() }

    fun save(kept: Set<String>, pending: List<String>) {
        prefs.edit {
            putStringSet(KEPT, HashSet(kept))
            putString(PENDING, pending.joinToString("\n"))
        }
    }

    var photoMode: Boolean
        get() = prefs.getBoolean(PHOTO_MODE, true)
        set(value) = prefs.edit { putBoolean(PHOTO_MODE, value) }

    var soundOn: Boolean
        get() = prefs.getBoolean(SOUND, true)
        set(value) = prefs.edit { putBoolean(SOUND, value) }

    var hapticsOn: Boolean
        get() = prefs.getBoolean(HAPTICS, true)
        set(value) = prefs.edit { putBoolean(HAPTICS, value) }

    /** La partita. Dalla 1.x si recuperano punti, spazio liberato ed eliminati. */
    fun loadProfile(): Profile {
        val raw = prefs.getString(PROFILE, null)
        if (raw == null) {
            return Profile(
                score = prefs.getLong(SCORE, 0L),
                freedBytes = prefs.getLong(FREED, 0L),
                deletedCount = prefs.getInt(DELETED, 0),
            )
        }
        return try {
            val o = JSONObject(raw)
            Profile(
                score = o.optLong("score"),
                freedBytes = o.optLong("freedBytes"),
                deletedCount = o.optInt("deletedCount"),
                reviewedCount = o.optInt("reviewedCount"),
                bestCombo = o.optInt("bestCombo"),
                goldenFound = o.optInt("goldenFound"),
                edits = o.optInt("edits"),
                roundsCompleted = o.optInt("roundsCompleted"),
                round = o.optInt("round", 1),
                roundReviewed = o.optInt("roundReviewed"),
                roundDeleted = o.optInt("roundDeleted"),
                roundFreed = o.optLong("roundFreed"),
                roundPoints = o.optLong("roundPoints"),
                roundBestCombo = o.optInt("roundBestCombo"),
                streakDays = o.optInt("streakDays"),
                bestStreak = o.optInt("bestStreak"),
                lastActiveDay = o.optLong("lastActiveDay", -1),
                day = o.optLong("day", -1),
                dayReviewed = o.optInt("dayReviewed"),
                dayDeleted = o.optInt("dayDeleted"),
                dayFreed = o.optLong("dayFreed"),
                dayBestCombo = o.optInt("dayBestCombo"),
                dayEdits = o.optInt("dayEdits"),
                dayGolden = o.optInt("dayGolden"),
                questsClaimed = o.optJSONArray("questsClaimed").toStringSet(),
                questDaysCompleted = o.optInt("questDaysCompleted"),
                achievements = o.optJSONArray("achievements").toStringSet(),
            )
        } catch (_: Exception) {
            Profile()
        }
    }

    fun saveProfile(p: Profile) {
        val o = JSONObject()
            .put("score", p.score)
            .put("freedBytes", p.freedBytes)
            .put("deletedCount", p.deletedCount)
            .put("reviewedCount", p.reviewedCount)
            .put("bestCombo", p.bestCombo)
            .put("goldenFound", p.goldenFound)
            .put("edits", p.edits)
            .put("roundsCompleted", p.roundsCompleted)
            .put("round", p.round)
            .put("roundReviewed", p.roundReviewed)
            .put("roundDeleted", p.roundDeleted)
            .put("roundFreed", p.roundFreed)
            .put("roundPoints", p.roundPoints)
            .put("roundBestCombo", p.roundBestCombo)
            .put("streakDays", p.streakDays)
            .put("bestStreak", p.bestStreak)
            .put("lastActiveDay", p.lastActiveDay)
            .put("day", p.day)
            .put("dayReviewed", p.dayReviewed)
            .put("dayDeleted", p.dayDeleted)
            .put("dayFreed", p.dayFreed)
            .put("dayBestCombo", p.dayBestCombo)
            .put("dayEdits", p.dayEdits)
            .put("dayGolden", p.dayGolden)
            .put("questsClaimed", JSONArray(p.questsClaimed.toList()))
            .put("questDaysCompleted", p.questDaysCompleted)
            .put("achievements", JSONArray(p.achievements.toList()))
        prefs.edit { putString(PROFILE, o.toString()) }
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() } }.toSet()
    }

    private companion object {
        const val KEPT = "kept"
        const val PENDING = "pending"
        const val PHOTO_MODE = "photo_mode"
        const val SOUND = "sound_on"
        const val HAPTICS = "haptics_on"
        const val PROFILE = "profile_v2"
        // chiavi della 1.x, lette una volta sola per non perdere i punti
        const val SCORE = "score"
        const val FREED = "freed_bytes"
        const val DELETED = "deleted_count"
    }
}
