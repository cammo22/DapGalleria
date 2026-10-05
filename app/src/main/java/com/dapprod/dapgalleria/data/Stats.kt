package com.dapprod.dapgalleria.data

/** Risultati accumulati nel tempo: ogni eliminazione definitiva fa guadagnare punti in base allo spazio liberato. */
data class Stats(
    val score: Long = 0L,
    val freedBytes: Long = 0L,
    val deletedCount: Int = 0,
) {
    val level: Level get() = Level.forScore(score)
    val nextLevel: Level? get() = Level.entries.getOrNull(level.ordinal + 1)

    /** 0..1 verso il livello successivo (1 se sei già al massimo). */
    val progressToNext: Float
        get() {
            val next = nextLevel ?: return 1f
            val span = (next.minScore - level.minScore).toFloat()
            return ((score - level.minScore) / span).coerceIn(0f, 1f)
        }

    companion object {
        private const val BYTES_PER_POINT = 1024L * 1024L

        /** 1 punto ogni MB liberato, almeno 1 punto per ogni elemento eliminato. */
        fun pointsFor(sizeBytes: Long): Long = maxOf(1L, sizeBytes / BYTES_PER_POINT)
    }
}

enum class Level(val title: String, val minScore: Long) {
    ROOKIE("Novellino", 0),
    TRIMMER("Sfoltitore", 100),
    SWEEPER("Spazzino", 500),
    TIDIER("Maestro dell'ordine", 2_000),
    LEGEND("Leggenda del vuoto", 10_000);

    companion object {
        fun forScore(score: Long): Level = entries.last { score >= it.minScore }
    }
}
