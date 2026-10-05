package com.dapprod.dapgalleria.data

import android.content.Context
import androidx.core.content.edit

/** Salva su disco le decisioni dell'utente, cosi' non si perdono se l'app viene chiusa. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("dapgalleria_session", Context.MODE_PRIVATE)

    /** Contenuti che hai deciso di tenere (non vengono più riproposti). */
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

    var randomOrder: Boolean
        get() = prefs.getBoolean(RANDOM, false)
        set(value) = prefs.edit { putBoolean(RANDOM, value) }

    var showReviewed: Boolean
        get() = prefs.getBoolean(SHOW_REVIEWED, false)
        set(value) = prefs.edit { putBoolean(SHOW_REVIEWED, value) }

    private companion object {
        const val KEPT = "kept"
        const val PENDING = "pending"
        const val PHOTO_MODE = "photo_mode"
        const val RANDOM = "random"
        const val SHOW_REVIEWED = "show_reviewed"
    }
}
