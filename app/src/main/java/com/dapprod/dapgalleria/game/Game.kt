package com.dapprod.dapgalleria.game

import kotlin.math.roundToLong

/*
 * La sala giochi di DapGalleria: punti, combo, carte d'oro, giri, missioni del giorno,
 * giorni di fila, traguardi e livelli. Kotlin puro (niente Android): si prova con i test JVM.
 */

const val MB = 1024L * 1024L
const val GB = 1024L * MB

/** Tutto quello che il giocatore ha fatto, salvato su disco. */
data class Profile(
    val score: Long = 0,
    val freedBytes: Long = 0,
    val deletedCount: Int = 0,
    val reviewedCount: Int = 0,
    val bestCombo: Int = 0,
    val goldenFound: Int = 0,
    val edits: Int = 0,
    val roundsCompleted: Int = 0,
    // il giro in corso: finisce quando tutte le foto e tutti i video sono stati visti
    val round: Int = 1,
    val roundReviewed: Int = 0,
    val roundDeleted: Int = 0,
    val roundFreed: Long = 0,
    val roundPoints: Long = 0,
    val roundBestCombo: Int = 0,
    // giorni di fila
    val streakDays: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDay: Long = -1,
    // le missioni del giorno [day]
    val day: Long = -1,
    val dayReviewed: Int = 0,
    val dayDeleted: Int = 0,
    val dayFreed: Long = 0,
    val dayBestCombo: Int = 0,
    val dayEdits: Int = 0,
    val dayGolden: Int = 0,
    val questsClaimed: Set<String> = emptySet(),
    val questDaysCompleted: Int = 0,
    val achievements: Set<String> = emptySet(),
) {
    val level: Level get() = Level.forScore(score)
    val nextLevel: Level? get() = Level.entries.getOrNull(level.ordinal + 1)

    /** 0..1 verso il livello successivo (1 se sei già al massimo). */
    val levelProgress: Float
        get() {
            val next = nextLevel ?: return 1f
            val span = (next.minScore - level.minScore).toFloat()
            return ((score - level.minScore) / span).coerceIn(0f, 1f)
        }

    /** Bonus sui punti delle eliminazioni per i giorni di fila: +10% al giorno, fino a ×2. */
    val streakMultiplier: Float get() = 1f + 0.1f * (streakDays - 1).coerceIn(0, 10)

    fun questProgress(q: Quest): Long = when (q.kind) {
        QuestKind.REVIEW -> dayReviewed.toLong()
        QuestKind.DELETE -> dayDeleted.toLong()
        QuestKind.FREE_MB -> dayFreed / MB
        QuestKind.COMBO -> dayBestCombo.toLong()
        QuestKind.EDIT -> dayEdits.toLong()
        QuestKind.GOLDEN -> dayGolden.toLong()
    }
}

enum class Level(val title: String, val emoji: String, val minScore: Long) {
    ROOKIE("Novellino", "🌱", 0),
    TRIMMER("Sfoltitore", "✂️", 150),
    SWEEPER("Spazzino", "🧹", 600),
    TIDIER("Riordinatore", "🗂️", 1_500),
    MASTER("Maestro dell'ordine", "🎯", 4_000),
    MINIMAL("Minimalista", "💎", 10_000),
    GURU("Guru del vuoto", "🌀", 25_000),
    LEGEND("Leggenda del vuoto", "👑", 60_000),
    COSMIC("Cosmico DaProd", "🌌", 150_000);

    companion object {
        fun forScore(score: Long): Level = entries.last { score >= it.minScore }
    }
}

enum class QuestKind { REVIEW, DELETE, FREE_MB, COMBO, EDIT, GOLDEN }

data class Quest(val id: String, val kind: QuestKind, val target: Long, val reward: Long) {
    val title: String
        get() = when (kind) {
            QuestKind.REVIEW -> "Valuta $target contenuti"
            QuestKind.DELETE -> "Elimina $target contenuti"
            QuestKind.FREE_MB -> "Libera $target MB"
            QuestKind.COMBO -> "Fai una combo ×$target"
            QuestKind.EDIT -> if (target == 1L) "Modifica una foto o un video" else "Modifica $target foto o video"
            QuestKind.GOLDEN -> if (target == 1L) "Trova una carta d'oro" else "Trova $target carte d'oro"
        }
    val emoji: String
        get() = when (kind) {
            QuestKind.REVIEW -> "👀"
            QuestKind.DELETE -> "🗑️"
            QuestKind.FREE_MB -> "💾"
            QuestKind.COMBO -> "🔥"
            QuestKind.EDIT -> "🎨"
            QuestKind.GOLDEN -> "✨"
        }
}

object Quests {
    const val CHEST_ID = "chest"
    const val CHEST_REWARD = 250L

    /** Le tre missioni del giorno: sempre le stesse per tutto il giorno, diverse il giorno dopo. */
    fun forDay(day: Long): List<Quest> {
        var seed = day * 6364136223846793005L + 1442695040888963407L
        fun next(n: Int): Int {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            return ((seed ushr 33) % n).toInt()
        }
        val review = listOf(30L, 50L, 80L)[next(3)]
        val second = when (next(2)) {
            0 -> Quest("d$day-del", QuestKind.DELETE, listOf(10L, 20L, 30L)[next(3)], 120)
            else -> Quest("d$day-mb", QuestKind.FREE_MB, listOf(100L, 250L, 500L)[next(3)], 150)
        }
        val third = when (next(3)) {
            0 -> Quest("d$day-combo", QuestKind.COMBO, listOf(10L, 15L, 25L)[next(3)], 120)
            1 -> Quest("d$day-edit", QuestKind.EDIT, 1L, 100)
            else -> Quest("d$day-gold", QuestKind.GOLDEN, 1L, 100)
        }
        return listOf(Quest("d$day-rev", QuestKind.REVIEW, review, 80), second, third)
    }
}

enum class Achievement(val emoji: String, val title: String, val description: String, val reward: Long, val check: (Profile) -> Boolean) {
    FIRST_SWIPE("👆", "Primo passo", "Valuta il primo contenuto", 10, { it.reviewedCount >= 1 }),
    REVIEW_100("💯", "Cento occhiate", "Valuta 100 contenuti", 50, { it.reviewedCount >= 100 }),
    REVIEW_1000("👀", "Mille occhiate", "Valuta 1.000 contenuti", 300, { it.reviewedCount >= 1_000 }),
    REVIEW_10000("🦅", "Occhio di falco", "Valuta 10.000 contenuti", 2_000, { it.reviewedCount >= 10_000 }),
    DELETE_1("🗑️", "Via il primo", "Elimina il primo contenuto", 20, { it.deletedCount >= 1 }),
    DELETE_100("🧨", "Cento in meno", "Elimina 100 contenuti", 200, { it.deletedCount >= 100 }),
    DELETE_1000("💣", "Demolitore", "Elimina 1.000 contenuti", 1_500, { it.deletedCount >= 1_000 }),
    FREE_100MB("📦", "Primi 100 MB", "Libera 100 MB", 50, { it.freedBytes >= 100 * MB }),
    FREE_1GB("💾", "Un giga d'aria", "Libera 1 GB", 300, { it.freedBytes >= GB }),
    FREE_10GB("🚀", "Dieci giga", "Libera 10 GB", 2_000, { it.freedBytes >= 10 * GB }),
    COMBO_10("🔥", "Combo ×10", "10 decisioni una dietro l'altra", 50, { it.bestCombo >= 10 }),
    COMBO_25("⚡", "In fiamme", "Combo ×25", 150, { it.bestCombo >= 25 }),
    COMBO_50("🌋", "Inarrestabile", "Combo ×50", 500, { it.bestCombo >= 50 }),
    STREAK_3("📅", "Tre giorni di fila", "Fai pulizia 3 giorni di fila", 100, { it.bestStreak >= 3 }),
    STREAK_7("🗓️", "Una settimana", "7 giorni di fila", 400, { it.bestStreak >= 7 }),
    STREAK_30("🏅", "Un mese pulito", "30 giorni di fila", 3_000, { it.bestStreak >= 30 }),
    GOLD_1("✨", "Carta d'oro", "Trova una carta d'oro", 50, { it.goldenFound >= 1 }),
    GOLD_10("🌟", "Cercatore d'oro", "Trova 10 carte d'oro", 400, { it.goldenFound >= 10 }),
    EDIT_1("🎨", "Ritoccatore", "Modifica una foto o un video", 30, { it.edits >= 1 }),
    EDIT_25("🎬", "Regista", "25 modifiche salvate", 400, { it.edits >= 25 }),
    ROUND_1("🔁", "Giro completo", "Vedi tutta la galleria", 500, { it.roundsCompleted >= 1 }),
    ROUND_5("🏆", "Cinque giri", "Completa 5 giri", 2_500, { it.roundsCompleted >= 5 }),
    CHEST_1("🎁", "Forziere", "Completa tutte le missioni di un giorno", 100, { it.questDaysCompleted >= 1 }),
    CHEST_7("💰", "Sette forzieri", "Missioni complete in 7 giorni", 1_000, { it.questDaysCompleted >= 7 }),
}

/** Quello che è successo con un'azione: lo usa la UI per festeggiare. */
data class Outcome(
    /** Punti dell'azione (senza missioni e traguardi). */
    val points: Long = 0,
    val combo: Int = 0,
    val multiplier: Int = 1,
    val golden: Boolean = false,
    val quests: List<Quest> = emptyList(),
    val chest: Boolean = false,
    val achievements: List<Achievement> = emptyList(),
    val levelUp: Level? = null,
    /** Punti da missioni, forziere e traguardi. */
    val bonusPoints: Long = 0,
)

/** Riepilogo di un giro appena finito. */
data class RoundSummary(
    val round: Int,
    val reviewed: Int,
    val deleted: Int,
    val freedBytes: Long,
    val points: Long,
    val bestCombo: Int,
    val bonus: Long,
)

class GameEngine(initial: Profile) {
    var profile: Profile = initial
        private set

    var combo: Int = 0
        private set
    private var lastSwipeAt: Long = Long.MIN_VALUE

    /** Moltiplicatore della combo in corso. */
    val multiplier: Int get() = multiplierFor(combo)

    /** Millisecondi che restano per tenere viva la combo (0 = spenta). */
    fun comboTimeLeft(nowMs: Long): Long =
        if (combo == 0 || lastSwipeAt == Long.MIN_VALUE) 0 else (COMBO_WINDOW_MS - (nowMs - lastSwipeAt)).coerceAtLeast(0)

    /** Una decisione (tieni o elimina) sul mazzo. */
    fun swipe(golden: Boolean, nowMs: Long, today: Long): Outcome {
        combo = if (lastSwipeAt != Long.MIN_VALUE && nowMs - lastSwipeAt <= COMBO_WINDOW_MS) combo + 1 else 1
        lastSwipeAt = nowMs
        val mult = multiplierFor(combo)
        val points = SWIPE_POINTS * mult * (if (golden) GOLDEN_MULTIPLIER else 1)
        val p = rollDay(profile, today)
        val after = p.copy(
            score = p.score + points,
            reviewedCount = p.reviewedCount + 1,
            bestCombo = maxOf(p.bestCombo, combo),
            goldenFound = p.goldenFound + if (golden) 1 else 0,
            roundReviewed = p.roundReviewed + 1,
            roundPoints = p.roundPoints + points,
            roundBestCombo = maxOf(p.roundBestCombo, combo),
            dayReviewed = p.dayReviewed + 1,
            dayBestCombo = maxOf(p.dayBestCombo, combo),
            dayGolden = p.dayGolden + if (golden) 1 else 0,
        )
        return settle(after, today, Outcome(points = points, combo = combo, multiplier = mult, golden = golden))
    }

    /** Annulla l'ultimo swipe: toglie i punti presi e spegne la combo (niente trucchi swipe/annulla). */
    fun undoSwipe(points: Long, golden: Boolean) {
        val p = profile
        combo = 0
        lastSwipeAt = Long.MIN_VALUE
        profile = p.copy(
            score = (p.score - points).coerceAtLeast(0),
            reviewedCount = (p.reviewedCount - 1).coerceAtLeast(0),
            goldenFound = (p.goldenFound - if (golden) 1 else 0).coerceAtLeast(0),
            roundReviewed = (p.roundReviewed - 1).coerceAtLeast(0),
            roundPoints = (p.roundPoints - points).coerceAtLeast(0),
            dayReviewed = (p.dayReviewed - 1).coerceAtLeast(0),
            dayGolden = (p.dayGolden - if (golden) 1 else 0).coerceAtLeast(0),
        )
    }

    /** Il sistema ha eliminato davvero [count] file per [bytes] byte; [basePoints] = 1 punto per MB (min 1 a file). */
    fun deleted(count: Int, bytes: Long, basePoints: Long, today: Long): Outcome {
        if (count <= 0) return Outcome()
        val p = rollDay(profile, today)
        val points = (basePoints * p.streakMultiplier).roundToLong()
        val after = p.copy(
            score = p.score + points,
            freedBytes = p.freedBytes + bytes,
            deletedCount = p.deletedCount + count,
            roundDeleted = p.roundDeleted + count,
            roundFreed = p.roundFreed + bytes,
            roundPoints = p.roundPoints + points,
            dayDeleted = p.dayDeleted + count,
            dayFreed = p.dayFreed + bytes,
        )
        return settle(after, today, Outcome(points = points))
    }

    /** Una foto o un video modificati e salvati. */
    fun edited(today: Long): Outcome {
        val p = rollDay(profile, today)
        val after = p.copy(score = p.score + EDIT_POINTS, edits = p.edits + 1, dayEdits = p.dayEdits + 1, roundPoints = p.roundPoints + EDIT_POINTS)
        return settle(after, today, Outcome(points = EDIT_POINTS))
    }

    /** Tutta la galleria vista: si chiude il giro col suo premio e ne parte uno nuovo. */
    fun completeRound(today: Long): Pair<RoundSummary, Outcome> {
        val p = rollDay(profile, today)
        val bonus = ROUND_BONUS + p.roundReviewed.toLong()
        val summary = RoundSummary(p.round, p.roundReviewed, p.roundDeleted, p.roundFreed, p.roundPoints + bonus, p.roundBestCombo, bonus)
        val after = p.copy(
            score = p.score + bonus,
            roundsCompleted = p.roundsCompleted + 1,
            round = p.round + 1,
            roundReviewed = 0,
            roundDeleted = 0,
            roundFreed = 0,
            roundPoints = 0,
            roundBestCombo = 0,
        )
        combo = 0
        lastSwipeAt = Long.MIN_VALUE
        return summary to settle(after, today, Outcome(points = bonus))
    }

    /** Ricomincia il giro senza premio (dal menu). */
    fun restartRound() {
        val p = profile
        profile = p.copy(round = p.round + 1, roundReviewed = 0, roundDeleted = 0, roundFreed = 0, roundPoints = 0, roundBestCombo = 0)
    }

    /** Aggiorna giorno e serie anche solo aprendo l'app (senza contare l'attività). */
    fun refreshDay(today: Long) {
        if (profile.day != today) profile = resetDay(profile, today)
    }

    /** Missioni completate, forziere, traguardi e livello: aggiunge i premi e li riporta nell'esito. */
    private fun settle(after: Profile, today: Long, base: Outcome): Outcome {
        val levelBefore = profile.level
        var p = touchStreak(after, today)
        var bonus = 0L

        val quests = Quests.forDay(today)
        val done = quests.filter { it.id !in p.questsClaimed && p.questProgress(it) >= it.target }
        if (done.isNotEmpty()) {
            bonus += done.sumOf { it.reward }
            p = p.copy(questsClaimed = p.questsClaimed + done.map { it.id })
        }
        var chest = false
        if (Quests.CHEST_ID !in p.questsClaimed && quests.all { it.id in p.questsClaimed }) {
            chest = true
            bonus += Quests.CHEST_REWARD
            p = p.copy(questsClaimed = p.questsClaimed + Quests.CHEST_ID, questDaysCompleted = p.questDaysCompleted + 1)
        }
        p = p.copy(score = p.score + bonus)

        // i traguardi possono sbloccarsene altri (es. i punti): si gira finché non cambia niente
        val unlocked = ArrayList<Achievement>()
        while (true) {
            val snapshot = p
            val fresh = Achievement.entries.filter { it.name !in snapshot.achievements && it.check(snapshot) }
            if (fresh.isEmpty()) break
            unlocked += fresh
            val reward = fresh.sumOf { it.reward }
            bonus += reward
            p = p.copy(achievements = p.achievements + fresh.map { it.name }, score = p.score + reward)
        }

        profile = p
        val levelAfter = p.level
        return base.copy(
            quests = done,
            chest = chest,
            achievements = unlocked,
            levelUp = if (levelAfter.ordinal > levelBefore.ordinal) levelAfter else null,
            bonusPoints = bonus,
        )
    }

    companion object {
        const val COMBO_WINDOW_MS = 4_000L
        const val SWIPE_POINTS = 1L
        const val GOLDEN_MULTIPLIER = 5
        const val GOLDEN_ODDS = 20
        const val EDIT_POINTS = 15L
        const val ROUND_BONUS = 300L

        fun multiplierFor(combo: Int): Int = when {
            combo >= 35 -> 5
            combo >= 20 -> 4
            combo >= 10 -> 3
            combo >= 5 -> 2
            else -> 1
        }

        /** Circa 1 carta su 20 è d'oro; resta la stessa per tutto il giro (annullare non la fa cambiare). */
        fun isGolden(key: String, round: Int): Boolean =
            Math.floorMod(key.hashCode() * 31 + round * 7919, GOLDEN_ODDS) == 0

        /** 1 punto per ogni MB liberato, almeno 1 per ogni file. */
        fun pointsForDelete(sizeBytes: Long): Long = maxOf(1L, sizeBytes / MB)

        private fun resetDay(p: Profile, today: Long): Profile = p.copy(
            day = today,
            dayReviewed = 0,
            dayDeleted = 0,
            dayFreed = 0,
            dayBestCombo = 0,
            dayEdits = 0,
            dayGolden = 0,
            questsClaimed = emptySet(),
            // la serie si spezza se ieri non hai fatto niente
            streakDays = if (p.lastActiveDay >= today - 1) p.streakDays else 0,
        )

        private fun rollDay(p: Profile, today: Long): Profile = if (p.day != today) resetDay(p, today) else p

        private fun touchStreak(p: Profile, today: Long): Profile {
            if (p.lastActiveDay == today) return p
            val streak = if (p.lastActiveDay == today - 1) p.streakDays + 1 else 1
            return p.copy(lastActiveDay = today, streakDays = streak, bestStreak = maxOf(p.bestStreak, streak))
        }
    }
}
