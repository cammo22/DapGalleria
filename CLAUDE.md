# DapGalleria — leggi questo per primo

App Android (Kotlin + Jetpack Compose) per ripulire la galleria con lo swipe, fatta come una sala giochi DaProd.

- **Si scrive in italiano parlato**: commenti, CHANGELOG, README, testi dell'interfaccia. Nomi tecnici in inglese dove serve.
- **Una versione = `VERSION` + voce in `CHANGELOG.md`.** Unita su `main`, `.github/workflows/release.yml` crea il tag,
  compila l'APK firmato (chiave pubblica in `keystore/`) e pubblica la release. `versionCode` = MAJOR*10000+MINOR*100+PATCH.
- **Niente permesso INTERNET**: la CI fallisce se compare.
- **Le prove**: `./gradlew testDebugUnitTest` (gioco e filtri, JUnit). Il gioco (`game/Game.kt`) e `data/ColorMath.kt`
  sono Kotlin puro apposta: si provano anche senza Android.
- **Il gioco**: `GameEngine` tiene `Profile` (salvato in JSON da `SessionStore`), combo (finestra 4 s, ×1..×5),
  carte d'oro (`isGolden(key, round)`, stabili nel giro), missioni del giorno (`Quests.forDay`, deterministiche),
  giorni di fila, traguardi (`Achievement`), livelli. Ogni azione ritorna un `Outcome` che la UI festeggia
  (`GameEvent` → `GameHud` in `ui/components/RewardLayer.kt`). Annullare toglie i punti (`undoSwipe`).
- **Il giro**: finisce quando foto e video sono tutti "visti" (tenuti o da eliminare) e nel giro si è valutato
  qualcosa; allora `kept` si svuota, la galleria si ricarica e si ricomincia (`GalleryViewModel.checkRoundComplete`).
- **Editor**: foto in `ui/screens/PhotoEditorScreen.kt` + `data/PhotoEditing.kt` (matrice colore uguale in anteprima e
  salvataggio); video in `ui/screens/VideoEditorScreen.kt` + `data/VideoEditing.kt` (Media3 Transformer; Media3
  ruota in senso antiorario). Il salvataggio passa da `data/MediaSaver.kt`. "Sostituisci" = l'originale va tra i da eliminare.
- **Suoni** sintetizzati in WAV nella cache al primo avvio (`ui/components/Feedback.kt`), niente file nell'APK.
