<div align="center">

<img src="docs/banner.png" alt="DapGalleria – scorri, tieni, elimina" width="100%">

<br>

[![Build](https://github.com/cammo22/DapGalleria/actions/workflows/build.yml/badge.svg)](https://github.com/cammo22/DapGalleria/actions/workflows/build.yml)
[![Ultima release](https://img.shields.io/github/v/release/cammo22/DapGalleria?color=ff4d8d&label=release)](https://github.com/cammo22/DapGalleria/releases/latest)
[![Download](https://img.shields.io/github/downloads/cammo22/DapGalleria/total?color=8b5cf6)](https://github.com/cammo22/DapGalleria/releases)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-2ee59d?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7f52ff?logo=kotlin&logoColor=white)
[![Changelog](https://img.shields.io/badge/📅_Changelog-mantenuto-ffab00)](CHANGELOG.md)
[![Licenza MIT](https://img.shields.io/badge/licenza-MIT-lightgrey)](LICENSE)

**Pulisci la galleria del telefono come se fosse una sala giochi.**<br>
Una foto alla volta: a destra la tieni, a sinistra la mandi nel cestino. Combo, carte d’oro, missioni, giri e livelli.

[**⬇️ Scarica l’APK**](https://github.com/cammo22/DapGalleria/releases/latest) · [Come funziona](#-come-funziona) · [Compilare da sorgente](#-compilare-da-sorgente)

</div>

---

## 🎰 Novità della 2.0: la sala giochi

- **🔁 Il giro**: la barra in alto ti dice quanta galleria hai visto. Quando hai visto *tutte* le foto e *tutti* i video il giro si chiude con la festa e il premio, la galleria si ricarica (anche le foto nuove) e si ricomincia, rimescolato.
- **🔥 Combo**: decidi in fretta e i punti valgono ×2, ×3… fino a ×5.
- **✦ Carte d’oro**: una carta su venti vale ×5 e fa piovere oro.
- **🎯 Missioni del giorno** con il **forziere 🎁**, **giorni di fila** che moltiplicano i punti delle eliminazioni, **24 traguardi** e **9 livelli** fino a 🌌 *Cosmico DaProd*. Tutto nella **Bacheca**.
- **🎨 Editor foto**: ritaglio a formato, ruota, specchia, luce/contrasto/saturazione/calore/sbiadito/vignetta, 11 filtri (c’è anche *Neon DaProd*), prima/dopo.
- **🎬 Editor video**: taglio con la striscia dei fotogrammi, ruota, formato 1:1 / 9:16 / 4:5 / 16:9, look, togli l’audio, **foto da un fotogramma**.
- **✨ Animazioni** curate ovunque, suoni sintetizzati e vibrazioni (si spengono dal menu ⋮).

Tutti i dettagli nel [CHANGELOG](CHANGELOG.md).

## ✨ Cosa fa

- **Swipe stile Tinder** su tutte le foto e i video del telefono, **sempre in ordine casuale**: trascina la carta a destra per **tenere**, a sinistra per **eliminare**. Ci sono anche i pulsanti per chi preferisce toccare.
- **Modalità Foto e modalità Video** con un tocco. Nei video, che partono da soli in loop (muti di default), vedi **durata e tempo trascorso**, puoi **trascinare la barra** per spostarti e usare **−10 s / +10 s**, pausa e audio.
- **✨ Modifica** (quarto pulsante, foto e video): apri l’editor, sistemi e scegli **Sostituisci e tieni** (l’originale finisce nella lista da eliminare) o **Salva copia**. Le modifiche finiscono in *Immagini/DapGalleria* e *Film/DapGalleria*.
- **Foto a schermo intero**: tocca una foto per aprirla e **ingrandirla** con il pizzico (o doppio tocco), poi trascina per spostarti.
- **🏆 Punteggio**: 1 punto a decisione (per la combo, ×5 sulle carte d’oro), 15 a modifica, **1 punto per ogni MB liberato** quando elimini (col bonus dei giorni di fila). Tocca l’anello del livello o i punti per aprire la **Bacheca**.
- **Eliminazione in due tempi, senza rischi.** Uno swipe a sinistra *non cancella nulla*: il contenuto viene solo marchiato. Quando hai finito, apri la lista “Da eliminare” e confermi tutto in un colpo.
- **Revisione veloce della sessione.** Griglia con tutte le miniature marchiate, dimensione totale che libererai, anteprima a schermo intero (video compresi) e filtro Tutti / Foto / Video.
- **Hai swipato per sbaglio?** Tocca la ✕ su una miniatura per **toglierla dalla lista**, oppure usa il pulsante **Annulla** per tornare indietro di uno swipe mentre scorri.
- **Le decisioni restano salvate.** Se chiudi l’app, i contenuti marchiati sono ancora lì e quelli già tenuti non ti vengono riproposti.
- **Privata al 100%**: nessun account, nessuna connessione a internet, niente analytics.

## 🧭 Come funziona

```
   ┌──────────┐   swipe ←    ┌───────────────────┐   conferma    ┌─────────────────┐
   │  Mazzo   │ ───────────▶ │  Da eliminare     │ ────────────▶ │  Eliminati      │
   │ foto/video│              │  (marchiati)      │               │  definitivamente│
   └──────────┘              └───────────────────┘               └─────────────────┘
        │ swipe →                    │ ✕ “togli dalla lista”
        ▼                            ▼
     Tenuti  ◀───────────────────────┘
```

| Gesto / pulsante | Effetto |
| --- | --- |
| Swipe a **destra** o ✓ | **Tieni**: il contenuto non viene più riproposto |
| Swipe a **sinistra** o ✕ | **Marchia** come da eliminare (non è ancora cancellato) |
| ↩︎ Annulla | Torna all’ultimo contenuto e annulla la decisione (anche un ritaglio) |
| ✨ Modifica | Editor foto o video: “Sostituisci” tiene la versione nuova e manda l’originale tra i da eliminare |
| Anello del livello / 🏆 | La Bacheca: livello, missioni, giro, record, traguardi |
| ⋮ in alto | Suoni, vibrazione, ricomincia il giro |
| 🗑 in alto a destra | Apre la lista “Da eliminare” con il numero di elementi |
| Tocco su un video | Pausa / riprendi |
| Tocco su una foto (mazzo o lista) | Schermo intero con zoom a pizzico e doppio tocco |
| Tocco su una miniatura | Anteprima a schermo intero |
| ✕ su una miniatura | Toglie quel contenuto dalla lista (resta nella galleria) |
| **Elimina definitivamente** | Chiede conferma ad Android e cancella i file selezionati |

> **Nota sull’eliminazione.** Da Android 11 in poi è il sistema stesso a mostrare la richiesta di conferma per cancellare i file; su Android 10 la conferma può essere richiesta file per file. I contenuti eliminati **non** passano da un cestino dell’app.

## 📲 Installazione

1. Apri la pagina delle [**Releases**](https://github.com/cammo22/DapGalleria/releases/latest) dal telefono e scarica `DapGalleria-vX.Y.Z.apk`.
2. Aprilo e, se Android lo chiede, consenti l’installazione da questa fonte.
3. Concedi l’accesso a foto e video e inizia a fare swipe.

Requisiti: **Android 8.0 (API 26)** o successivo. Accanto all’APK trovi anche il checksum `.sha256` per verificarne l’integrità.

## 🔐 Permessi e privacy

| Permesso | Perché serve |
| --- | --- |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (Android 13+) · `READ_EXTERNAL_STORAGE` (fino ad Android 12) | Mostrare le tue foto e i tuoi video |
| `WRITE_EXTERNAL_STORAGE` (solo Android 8–9) | Poter eliminare i file; dalle versioni successive lo fa il sistema con la sua conferma |

L’app **non ha il permesso `INTERNET`**: non può inviare nulla fuori dal telefono. Le uniche informazioni salvate sono gli identificativi dei contenuti tenuti / marchiati, in una piccola preferenza locale.

## 🛠 Compilare da sorgente

Serve **JDK 17** e l’**Android SDK** (Android Studio li include entrambi).

```bash
git clone https://github.com/cammo22/DapGalleria.git
cd DapGalleria
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
```

Oppure apri la cartella con Android Studio e premi ▶︎.

### Stack

- **Kotlin** + **Jetpack Compose** (Material 3, tema scuro)
- **Coil** per le miniature (anche dei video), **Media3 / ExoPlayer** per la riproduzione e **Media3 Transformer** per l’editor video
- **MediaStore** per leggere la galleria e `createDeleteRequest` per l’eliminazione sicura
- Architettura semplice: `ViewModel` + `StateFlow`

<details>
<summary><b>Struttura del progetto</b></summary>

```
app/src/main/java/com/dapprod/dapgalleria/
├── MainActivity.kt          permessi, navigazione tra mazzo e lista “Da eliminare”
├── DapGalleriaApp.kt        ImageLoader di Coil (con decoder dei fotogrammi video)
├── data/
│   ├── MediaRepository.kt   legge foto e video da MediaStore
│   ├── MediaDeleter.kt      eliminazione definitiva (Android 8 → 15)
│   ├── ColorMath.kt         matrici colore di filtri e regolazioni (Kotlin puro)
│   ├── PhotoEditing.kt      apre, gira e salva le foto modificate
│   ├── VideoEditing.kt      taglio, effetti ed export dei video (Media3 Transformer)
│   ├── MediaSaver.kt        scrive foto e video nuovi nella galleria
│   └── SessionStore.kt      decisioni, impostazioni e partita su disco
├── game/Game.kt             il gioco: combo, carte d’oro, giri, missioni, traguardi, livelli
└── ui/
    ├── GalleryViewModel.kt  mazzo, swipe, annulla, modifiche, giro, eventi del gioco
    ├── components/          carta, mazzo, player, coriandoli, suoni, HUD dei premi
    └── screens/             swipe, da eliminare, bacheca, fine giro, editor foto e video, permessi
tools/                       script che generano icona e banner
```

</details>

## 🚀 Release automatiche

La release si pubblica da sola, in uno di questi modi:

- **Al merge su `main`**: il file [`VERSION`](VERSION) contiene la versione corrente (es. `1.0.0`). Quando cambia su `main`, GitHub Actions crea il tag `v1.0.0`, compila l’APK firmato e pubblica la release con APK, checksum e note generate dai commit. Per una nuova versione basta aggiornare `VERSION` e fare il merge.
- **Con un tag**: `git tag v1.1.0 && git push origin v1.1.0`.
- **A mano**: *Actions → Release → Run workflow*.

Il numero di versione dell’app (`versionName` / `versionCode`) è ricavato dal tag. Se la release esiste già, il workflow non fa nulla.

### Firma e aggiornamenti

Ogni release è firmata con la stessa chiave, quindi **gli aggiornamenti si installano direttamente sopra la versione già presente**, senza disinstallare e senza perdere punti e decisioni.

Per non richiedere nessuna configurazione, la chiave è inclusa nel repository ([`keystore/dapgalleria-public.jks`](keystore/dapgalleria-public.jks), password `dapgalleria`). È **volutamente pubblica**: serve a dare un’identità stabile all’app, non a proteggerla. Significa che chiunque potrebbe firmare un APK con la stessa identità: scarica DapGalleria solo da queste [Releases](https://github.com/cammo22/DapGalleria/releases) e, se vuoi, verifica il `.sha256`.

> La release `v1.0.0` era firmata con una chiave temporanea: passando da `1.0.0` a `1.1.0` serve **una sola** disinstallazione; da lì in poi si aggiorna sempre sopra.

**Preferisci una chiave privata?** Imposta questi *Secrets* (Settings → Secrets and variables → Actions) e la CI userà quella al posto della chiave pubblica:

```bash
keytool -genkeypair -keystore dapgalleria.jks -alias dapgalleria \
        -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 dapgalleria.jks      # incolla l’output in ANDROID_KEYSTORE_BASE64
```

| Secret | Contenuto |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | il file `.jks` codificato in base64 |
| `ANDROID_KEYSTORE_PASSWORD` | password del keystore |
| `ANDROID_KEY_ALIAS` | alias della chiave (es. `dapgalleria`) |
| `ANDROID_KEY_PASSWORD` | password della chiave |

## 🗺 Idee per il futuro

- [ ] Filtro per album / cartella
- [ ] Widget “ricordami di fare ordine”
- [x] Taglio dei video (2.0)
- [ ] Classifica dei giri e statistiche per mese
- [ ] Gesto verso l’alto per condividere o spostare in un album
- [ ] Tema chiaro e traduzione in inglese

I contributi sono benvenuti: apri una issue o una pull request.

## 📄 Licenza

Distribuito con licenza [MIT](LICENSE).
