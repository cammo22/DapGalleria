package com.dapprod.dapgalleria.data

/**
 * Regole su cosa far vedere nel mazzo. Kotlin puro (niente Android): si prova con JUnit.
 */
object MediaRules {

    /**
     * Android mette tra i "video" anche i file `.ts` (i sorgenti TypeScript, o i pezzettini degli streaming):
     * non hanno un'anteprima e non si riproducono, quindi nel mazzo sarebbero solo carte nere.
     */
    fun isHiddenVideo(name: String, mimeType: String?): Boolean =
        name.endsWith(".ts", ignoreCase = true) ||
            mimeType.equals("video/vnd.dlna.mpeg-tts", ignoreCase = true) ||
            mimeType.equals("video/mp2t", ignoreCase = true)

    /**
     * Copie identiche dello stesso file (stessa foto salvata da WhatsApp, Telegram, screenshot...): a ogni gruppo
     * si fa vedere una carta sola, la prima nell'ordine dato. Ritorna carta mostrata → le altre copie.
     * [signatures] sono coppie (chiave, firma); la firma `null` vuol dire "non so confrontarlo": mai considerato copia.
     */
    fun twins(signatures: List<Pair<String, String?>>): Map<String, List<String>> {
        val groups = LinkedHashMap<String, MutableList<String>>()
        for ((key, signature) in signatures) {
            if (signature != null) groups.getOrPut(signature) { ArrayList() } += key
        }
        val result = LinkedHashMap<String, List<String>>()
        for (keys in groups.values) {
            if (keys.size > 1) result[keys.first()] = keys.drop(1)
        }
        return result
    }

    /** Due file con tipo, peso, dimensioni (e durata, per i video) uguali sono la stessa cosa. Senza dati sicuri: null. */
    fun signature(type: MediaType, sizeBytes: Long, width: Int, height: Int, durationMs: Long): String? =
        if (sizeBytes > 0 && width > 0 && height > 0) "$type:$sizeBytes:${width}x$height:$durationMs" else null
}
