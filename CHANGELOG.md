# Changelog DapGalleria 🃏

Tutte le versioni notevoli dell'app. Le date sono in formato AAAA-MM-GG.
Ogni versione pubblicata ha la sua [release GitHub](https://github.com/cammo22/DapGalleria/releases) con l'APK firmato:
si installa sopra la versione di prima, senza perdere punti e decisioni.

## [2.0.0] — 2026-10-08 · La sala giochi della galleria 🎰

Il grande aggiornamento: «l'app mi piace ma è ancora troppo semplice». Ora fare ordine è un gioco che non vuoi
smettere, con animazioni curate dappertutto, e prima di tenere una foto o un video li puoi sistemare per bene.

### 🎮 Il gioco
- **Il giro**: la barra in alto dice quanta galleria hai già visto. Quando hai visto **tutte le foto e tutti i video**
  il giro si chiude con la sua festa (coppa, numeri del giro, premio), la galleria **si ricarica** (anche le foto
  nuove) e si **ricomincia** da capo, tutto rimescolato. Il premio vale solo se nel giro hai valutato qualcosa.
- **Combo**: decidi in fretta (entro 4 secondi) e la combo sale. Dalla 5ª carta i punti valgono ×2, poi ×3, ×4,
  fino a **×5**. La fiammella in alto mostra la combo e la barretta del tempo che resta.
- **Carte d'oro** ✦: circa una carta su venti è d'oro, con il bordo che brilla. Vale **×5** e fa piovere oro.
  Resta la stessa per tutto il giro: annullare non la fa cambiare.
- **Missioni del giorno** 🎯: tre missioni nuove ogni giorno (valuta, elimina, libera MB, combo, modifica, carta
  d'oro). Fatte tutte e tre si apre il **forziere** 🎁 (+250).
- **Giorni di fila** 🔥: ogni giorno in cui fai pulizia la serie sale, e le eliminazioni rendono il 10% in più per
  ogni giorno (fino a ×2). Se salti un giorno la serie si spegne.
- **24 traguardi** da sbloccare (Occhio di falco, Un giga d'aria, Inarrestabile, Un mese pulito…) e **9 livelli**,
  da 🌱 Novellino a 🌌 Cosmico DaProd, con la festa quando sali.
- **Punti**: ogni decisione vale 1 punto (per la combo, ×5 se d'oro), ogni modifica 15, ogni MB liberato 1 (per il
  bonus dei giorni di fila). Annullare uno swipe toglie i punti presi: niente trucchi.
- **La Bacheca** (tocca l'anello del livello o i punti): livello, missioni, giro, memoria del telefono, i tuoi
  record, traguardi e tutti i livelli.
- **La festa dello spazio liberato**: dopo l'eliminazione i MB contano in su, i punti pure, la barra della memoria
  si accende di verde e ti dice quante foto nuove ci stanno.

### ✨ Le animazioni
- Mazzo a **tre carte** con la profondità: quella dietro sale mentre trascini quella davanti.
- La carta **gira dal punto dove la prendi** (presa in basso gira al contrario, come una carta vera), si alza un
  poco, e il **timbro** TIENI / ELIMINA batte quando passi la soglia (con un "tic" di vibrazione).
- **Annulla** fa rientrare la carta dal lato da cui era uscita, con la molla.
- I pulsanti **crescono e si colorano** mentre la carta si piega verso di loro, e rimbalzano quando li premi.
- **Punti che volano**, coriandoli ai lati a ogni scelta, pioggia di coriandoli per livelli, forzieri e giri.
- **Sfondo al neon** vivo (magenta, viola, ciano) che si muove piano dietro a tutto, la pillola Foto/Video che
  scivola, il cestino che trema quando si riempie, i punti che scorrono come un contatore da flipper.
- **Suoni** sintetizzati al primo avvio (niente file nell'APK) e **vibrazioni**: si spengono dal menu ⋮.

### 🎨 L'editor delle foto (pulsante ✨ Modifica)
- **Ritaglio** libero o a formato: Originale, 1:1, 4:5, 3:4, 9:16, 4:3, 16:9 (col formato fisso l'angolo opposto resta fermo).
- **Ruota** di 90° e **specchia**.
- **Regola**: luce, contrasto, saturazione, calore, sbiadito, vignetta. Un tocco sul numero lo azzera.
- **11 filtri** con l'anteprima sulla tua foto: Vivace, Caldo, Freddo, B/N, Noir, Vintage, Pellicola, Cinema,
  **Neon DaProd**, Dramma, e l'intensità.
- **Prima/dopo**: tieni premuto il pulsante in alto per vedere l'originale.
- **Sostituisci e tieni** (l'originale va tra i da eliminare) o **Salva copia** (le tieni tutte e due).

### 🎬 L'editor dei video
- **Taglia** con la striscia dei fotogrammi e due maniglie: l'anteprima gira in tondo solo nel pezzo scelto.
- **Ruota**, **formato** (1:1, 9:16, 4:5, 16:9 con la maschera di quello che resta), **look** (Vivace, Caldo,
  Freddo, B/N, Dramma) con luce e contrasto, **togli l'audio**.
- **📸 Foto da qui**: salva il fotogramma che stai guardando come foto nuova.
- Il video si crea sul telefono (Media3 Transformer, MP4 H.264) con la percentuale e il tasto Annulla.
- Le modifiche si salvano in *Immagini/DapGalleria* e *Film/DapGalleria*, con la data dell'originale.

### Per chi ha curiosità
- Il gioco è Kotlin puro in `game/Game.kt` (`GameEngine`, `Profile`, `Quests`, `Achievement`, `Level`), con le
  prove JUnit in `app/src/test`; la CI le fa girare prima di compilare. I colori dei filtri sono matrici in
  `data/ColorMath.kt`, le stesse per l'anteprima e per il salvataggio.
- La partita si salva in JSON nelle preferenze (`profile_v2`): punti, MB liberati ed eliminati della 1.x si recuperano.
- Nuove dipendenze: `media3-transformer` e `media3-effect` (stessa versione di ExoPlayer). Ancora niente permesso INTERNET.
- **Nota onesta**: l'anteprima dei look del video usa `RenderEffect`, che c'è da Android 12; sui telefoni più
  vecchi il look si vede solo nel video salvato.

## [1.1.1] — 2026-10-05

- Ritaglio libero, schermata del ritaglio leggibile, foto a schermo intero con zoom.

## [1.1.0] — 2026-10-05

- Ordine casuale, controlli dei video, punteggio e ritaglio. Firma stabile per aggiornare sopra.

## [1.0.0] — 2026-10-04

- Prima versione: swipe a destra per tenere, a sinistra per eliminare, lista da confermare.
