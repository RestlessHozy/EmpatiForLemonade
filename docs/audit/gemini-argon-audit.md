# Laporan Deep Audit Arsitektur & Kesiapan Submisi: Empati (AMD Lemonade Developer Challenge)

**Tanggal Audit:** 12 November 2026  
**Target Submisi:** AMD Lemonade Developer Challenge (`http://127.0.0.1:13305/v1` / `http://10.0.2.2:13305/v1`)  
**Commit Basis Audit:** `a34f582` (`docs: paket submisi AMD, evaluasi benchmark, dan naskah demo`)  
**Status Kode Saat Ini:** Belum dimodifikasi (menunggu konfirmasi eksekusi perbaikan)

---

## Ringkasan Eksekutif

Seluruh tiket implementasi awal (`[T01]` hingga `[T11]`) telah membangun fondasi klien jaringan `LemonadeUnifiedClient`, jembatan deterministik `HybridTriageBridge`, indeks vektor `EmbeddingIndex`, perekam/pemutar suara (`AndroidVoiceRecorder`, `SpeechPlaybackController`), skrip penyiapan PowerShell, serta 176 unit test yang seluruhnya lulus (`0 failures`).

Namun, audit mendalam lintas lapisan (Data, Domain, ViewModel, UI Compose, dan Skrip Runtime) menemukan **14 temuan kritis dan menengah (blind spots)** yang akan terlihat jelas saat juri menjalankan demo langsung atau meninjau kode:
1. **Krisis Konsistensi Branding ("SRYA Emora" / "AI-EMPATI" vs "Empati"):** Nama lama **`SRYA Emora`** masih tampil di ikon peluncur aplikasi (`strings.xml`), Splash Screen, Onboarding, Login, Home, Header & indikator mengetik Chat, layar Hasil Skrining, Profil, pesan sambutan pertama, hingga jawaban ketika pengguna bertanya *"Siapa kamu?"* (`OllamaRagService.SRYA_EMORA_SELF_INTRO`).
2. **Terputusnya Konteks Triage pada Retrieval & Lintas Sesi:**
   - Fungsi `HybridTriageBridge.toRetrievalQuery(context)` sudah dibuat dan diuji, tetapi **tidak pernah dipanggil** oleh `EmpatiChatUseCase` maupun `EmpatiViewModel` saat mencari rujukan di `LocalCorpusIndex`. Akibatnya, pertanyaan umum seperti *"Apa langkah untuk hasil skrining saya?"* kehilangan kata kunci klinis dari hasil Certainty Factor.
   - `GroundedChatGateway` hanya meneruskan `context.triage` (yang hanya hidup di memori sesaat setelah kuesioner selesai) dan **mengabaikan `context.snapshot`** (`ScreeningContextSnapshot` yang tersimpan di SQLite/SharedPreferences). Saat aplikasi dibuka ulang atau pengguna login kembali, LLM menerima pesan `"Belum ada hasil skrining pada sesi ini."` meskipun riwayat skrining pengguna ada.
3. **Risiko Mode Thinking `Qwen3-4B-GGUF` di Lemonade:** Spesifikasi riset (`docs/research/lemonade-omnimodal-spec.md` §2.1) mewajibkan `enable_thinking: false` untuk `Qwen3-4B-GGUF` agar kuota `max_tokens: 512` tidak habis oleh blok `<think>...</think>` dan tag tersebut tidak bocor ke gelembung chat maupun sintesis suara `kokoro-v1`. Payload `LemonadeUnifiedClient` saat ini belum mengirim parameter tersebut dan belum menyaring blok `<think>`.
4. **Celah Runtime Android (Audio, Streaming, Status Server, & Cleartext LAN):** Indikator status di header Chat menampilkan `"Lokal siap"` hanya dengan memeriksa `vm.lemonadeStatus != null` walaupun alias model wajib belum terpasang (`isReady == false`), perekaman mikrofon tidak menghentikan pemutaran TTS aktif, kegagalan `MediaPlayer` ditelan sebagai selesai normal (`Idle`), dan `network_security_config.xml` memblokir HTTP cleartext ke IP LAN (`192.168.x.x`) saat demo perangkat fisik tanpa `adb reverse`.

---

## Kriteria 1: Integritas Arsitektur Dual-System (Certainty Factor & RAG)

### Bagian yang Sudah Sesuai
- **Isolasi Mesin Pakar 100% Deterministik:** Perhitungan pada `CfInputParser.kt`, `CfKnowledgeBase.kt`, `CfInferenceEngine.kt`, dan `CfResultGenerator.kt` berjalan murni di memori perangkat menggunakan rumus Shortliffe & Buchanan (`CF(H,e) = CF(E) * (MB - MD)` dan kombinasi berurutan). Tidak ada satu pun pemanggilan LLM di jalur perhitungan skor maupun penetapan `CfLevel` / `IntegratedCategory`.
- **Kontrak Jembatan Terstruktur (`empati.screening_context/v1`):** `HybridTriageBridge.kt` membentuk struktur `TriageContext` yang merangkum modul (`HYPERTENSION`, `GBV`), nilai `cf`, kategori dominan (maksimal 3 kategori, 3 bukti per kategori), faktor pelindung, serta catatan jalur stres kronis (`chronic_stress_pathway`), lalu menerjemahkannya ke blok prompt berbahasa Indonesia melalui `HybridTriageBridge.toPromptBlock`.
- **Pemilihan Kutipan Ketat (`CitationPicker`):** `EmpatiChatUseCase.kt` menyaring `chunks` yang diambil sehingga hanya rujukan yang benar-benar disebut (judul/penerbit/singkatan WHO, Kemenkes, Komnas Perempuan, SAPA 129) di dalam jawaban yang ditampilkan sebagai chip kutipan di UI.

### Blind Spot & Bug yang Ditemukan
1. **`[KRITIS - C1.1]` `HybridTriageBridge.toRetrievalQuery` Tidak Pernah Dipakai Saat Retrieval Vektor:**
   - **Lokasi:** `app/src/main/java/com/example/empati/data/triage/HybridTriageBridge.kt` (baris 153–165), `app/src/main/java/com/example/empati/data/chat/EmpatiChatUseCase.kt` (baris 47 & 63), dan `app/src/main/java/com/example/empati/EmpatiViewModel.kt` (baris 144–149).
   - **Masalah:** ADR-001 (`docs/adr/001-lemonade-hybrid-architecture.md`) menetapkan: *"Retrieval butuh query yang dibentuk dari hasil skrining."* Fungsi `HybridTriageBridge.toRetrievalQuery(context)` telah ditulis untuk mengubah hasil CF menjadi kata kunci klinis bebas angka (misalnya `"hipertensi tekanan darah tinggi tingkat sedang faktor gaya hidup garam aktivitas fisik"`). Namun `EmpatiChatUseCase.reply` dan `replyStreaming` hanya memanggil `retriever.retrieve(sanitized, retrievalLimit)` menggunakan kalimat mentah pengguna. Ketika pengguna menekan chip *"Analisis hasil skrining saya"* atau bertanya *"Apa yang harus saya lakukan dengan hasil saya?"*, vektor query tidak memuat topik klinis hasil skrining pengguna sehingga pencarian kosinus di `EmbeddingIndex` (ambang `0.35`) gagal menarik rujukan yang relevan.
2. **`[KRITIS - C1.2]` Konteks Skrining Hilang Saat Sesi Dibuka Kembali (`GroundedChatGateway` Mengabaikan `snapshot`):**
   - **Lokasi:** `app/src/main/java/com/example/empati/EmpatiViewModel.kt` (baris 155–164, 497–508) dan `app/src/main/java/com/example/empati/data/chat/GroundedChatGateway.kt` (baris 34–38, 88–92).
   - **Masalah:** `currentTriageContext()` di `EmpatiViewModel` hanya membaca variabel memori `htModuleResult`, `gbvModuleResult`, dan `integrated` yang bernilai `null` ketika pengguna baru login atau membuka ulang aplikasi. Walaupun `EmpatiViewModel` memuat `richSnapshot` (`ScreeningContextSnapshot`) dari `ScreeningStore.loadQualitativeSnapshot(...)` dan memasukkannya ke `ChatRequestContext`, `GroundedChatGateway.generate` dan `generateStreaming` hanya meneruskan `context.triage` ke `EmpatiPromptBuilder.buildUserPrompt` dan sama sekali mengabaikan `context.snapshot`. Akibatnya, blok `[Hasil skrining pengguna]` berisi `"Belum ada hasil skrining pada sesi ini."` padahal pengguna memiliki riwayat skrining aktif. Selain itu, `EmpatiViewModel` tidak memulihkan `htModuleResult`/`gbvModuleResult` atau membangun `TriageContext` dari `ResearchRecord` terakhir milik pengguna saat `enterSession()`.
3. **`[MENENGAH - C1.3]` `AnswerGuardrail` Belum Menyaring Kebocoran Angka Desimal CF Mentah:**
   - **Lokasi:** `app/src/main/java/com/example/empati/data/chat/AnswerGuardrail.kt` (baris 9–17) dan `app/src/main/java/com/example/empati/data/triage/HybridTriageBridge.kt` (baris 196).
   - **Masalah:** `HybridTriageBridge.toPromptBlock` menyisipkan baris `"Kekuatan indikasi internal: 0.62 dari 1.00 (hanya untuk kalibrasi nada bicara, jangan disebut ke pengguna)."` ke dalam prompt LLM. Jika model tetap membocorkan kalimat seperti `"Kekuatan indikasi Anda 0.62 dari 1.00"` tanpa kata `CF`/`skor`/`%`, regex `NUMERIC_SCORE` di `AnswerGuardrail` (`\b(indikator|nilai|skor)\s+\d+(?:[.,]\d+)?\b`) tidak menangkap pola `0.xx` atau `dari 1.00`.
4. **`[RENDAH - C1.4]` Koeksistensi Tumpukan RAG Lama (`OllamaRagService` / `ChatEngine`) dan Baru (`EmpatiChatUseCase`):**
   - **Lokasi:** `app/src/main/java/com/example/empati/data/rag/OllamaRagService.kt`, `app/src/main/java/com/example/empati/data/ChatEngine.kt`, dan `app/src/main/java/com/example/empati/data/chat/EmpatiChatUseCase.kt`.
   - **Masalah:** `EmpatiChatUseCase` masih bergantung pada utilitas statis di `OllamaRagService` (`sanitizePrivacyInput`, `SRYA_EMORA_SELF_INTRO`, `OUT_OF_SCOPE_REFUSAL`) dan `RagKnowledgeRepository`. Teks konstanta di dalam `OllamaRagService` masih membawa identitas dan aturan generasi lama.

---

## Kriteria 2: Kepatuhan Spesifikasi AMD Lemonade (`:13305`)

### Bagian yang Sudah Sesuai
- **Port & Endpoint Tunggal `:13305`:** `LemonadeConfig.kt` dan `LemonadeUnifiedClient.kt` menggunakan port `13305` dan lima endpoint OpenAI-compatible (`/v1/models`, `/v1/chat/completions`, `/v1/embeddings`, `/v1/audio/transcriptions`, `/v1/audio/speech`).
- **Kontrak Alias Model:** Keempat alias wajib (`empati-llm`, `empati-embed`, `empati-stt`, `empati-tts`) didefinisikan di `LemonadeConfig.REQUIRED_ALIASES` dan diverifikasi terhadap keluaran `/v1/models`.
- **Bebas Jejak Ollama:** Tidak ada referensi ke port `11434` atau `/api/chat` di seluruh kode sumber Kotlin maupun skrip PowerShell (dijaga oleh unit test di `LemonadeUnifiedClientTest` dan `verify-dryrun.ps1`).
- **Skrip Penyiapan & Benchmark (`scripts/`):** `setup-empati-lemonade.ps1` dan `bench-empati.ps1` telah mendukung `-DryRun`, mengatur `host=0.0.0.0` dan `max_loaded_models=2`, mengunduh kelima model (`Qwen3-4B-GGUF`, `Qwen3-1.7B-GGUF`, `nomic-embed-text-v1-GGUF`, `Whisper-Large-v3-Turbo`, `kokoro-v1`), mendaftarkan alias, serta menulis laporan JSON ke `docs/benchmarks/`.

### Blind Spot & Bug yang Ditemukan
1. **`[KRITIS - C2.1]` Absennya `"enable_thinking": false` dan Pembersihan Tag `<think>` untuk `Qwen3-4B-GGUF`:**
   - **Lokasi:** `app/src/main/java/com/example/empati/data/lemonade/LemonadeUnifiedClient.kt` (baris 255–270), `app/src/main/java/com/example/empati/data/chat/AnswerGuardrail.kt`, dan `scripts/bench-empati.ps1` (baris 133–143).
   - **Masalah:** Dokumen spesifikasi (`docs/research/lemonade-omnimodal-spec.md` §2.1) menegaskan bahwa `Qwen3-4B-GGUF` adalah model reasoning yang memerlukan `"enable_thinking": false` pada payload `/v1/chat/completions`. Tanpa opsi tersebut (dan tanpa pembersihan blok `<think>...</think>` di sisi klien/guardrail), model dapat menghabiskan `max_tokens: 512` untuk penalaran internal atau menampilkan tag `<think>` mentah di layar chat dan membacakannya lewat TTS `kokoro-v1`.
2. **`[MENENGAH - C2.2]` Indikator Header Chat Menampilkan `"Lokal siap"` Meski Alias Model Belum Lengkap:**
   - **Lokasi:** `app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt` (baris 221).
   - **Masalah:** `ChatScreen` menghitung `llmReachable = vm.lemonadeStatus != null`. Ketika server Lemonade menyala tetapi model `empati-llm` / `empati-embed` belum diunduh atau aliasnya belum dibuat (`vm.lemonadeStatus?.isReady == false`), badge di header Chat tetap berwarna hijau dan bertuliskan `"Lokal siap"`. Seharusnya menggunakan `vm.lemonadeStatus?.isReady == true`.
3. **`[MENENGAH - C2.3]` `CorpusIndexer` Mengotori Penyimpanan Saat Server Embedding Gagal/Offline:**
   - **Lokasi:** `app/src/main/java/com/example/empati/data/retrieval/EmbeddingIndex.kt` (baris 67–88) dan `app/src/main/java/com/example/empati/data/retrieval/LocalCorpusIndex.kt` (baris 23–40).
   - **Masalah:** Saat server Lemonade belum aktif, `EmpatiViewModel` mengembalikan `emptyList()` dari `embedder.embed(texts)`. Namun `CorpusIndexer.index` tetap membangun `IndexedChunk` dengan `FloatArray(0)`, lalu `LocalCorpusIndex.ensureIndexed` menyimpannya ke `SharedPreferences` (`merged != existing`). Saat dibaca kembali oleh `EmbeddingStoreCodec.decode`, entri ber-vektor kosong tersebut dibuang (`if (vector.isEmpty()) return@mapNotNull null`), sehingga setiap pesan chat memicu penulisan ulang `SharedPreferences` secara sia-sia. Jika `embedder.embed` mengembalikan daftar kosong atau ukuran yang tidak sesuai, `CorpusIndexer` seharusnya mempertahankan entri lama yang valid dan tidak mengindeks chunk gagal dengan `FloatArray(0)`.
4. **`[RENDAH - C2.4]` Penulisan Field 16-bit Menggunakan `Int32` pada Header WAV di `bench-empati.ps1`:**
   - **Lokasi:** `scripts/bench-empati.ps1` (baris 69–92).
   - **Masalah:** Fungsi `New-BenchmarkWavBytes` memakai `Write-Int` (4 byte `Int32`) untuk menulis field WAV yang seharusnya 2 byte (`Int16`), yaitu `AudioFormat` (offset 20), `NumChannels` (offset 22), `BlockAlign` (offset 32), dan `BitsPerSample` (offset 34). Ini kebetulan tidak merusak hasil karena byte urutan berikutnya menimpa 2 byte atas yang bernilai nol, namun sebaiknya menggunakan helper `Write-Short` (`[BitConverter]::GetBytes([int16]$value)`) agar identik dengan `WavCodec.kt`.
5. **`[RENDAH - C2.5]` Peringatan Kompilasi `Float == Double` di `MiniJson.kt`:**
   - **Lokasi:** `app/src/main/java/com/example/empati/data/lemonade/MiniJson.kt` (baris 165).
   - **Masalah:** `value == floor(value.toDouble())` membandingkan `Float` dengan `Double`, yang di Kotlin selalu bernilai `false` dan memicu peringatan kompilator. Seharusnya `value == floor(value)`.

---

## Kriteria 3: Kesiapan Runtime Android (Audio & Local Network)

### Bagian yang Sudah Sesuai
- **Perekaman Audio 16 kHz Mono PCM16 & Enkapsulasi WAV:** `AndroidVoiceRecorder.kt` merekam dari `MediaRecorder.AudioSource.MIC` pada 16.000 Hz mono PCM 16-bit di daemon thread terpisah dengan batas keras 60 detik (`960_000` sampel), dan `WavCodec.kt` membungkusnya menjadi WAV 44-byte yang valid.
- **Siklus Hidup Perekam & Pemutar Audio:** `VoiceInputController` dan `SpeechPlaybackController` dibersihkan melalui `DisposableEffect(Unit)` di `ChatScreen.kt` (memanggil `voiceController.cancel()` dan `speechController.stop()` saat pengguna keluar dari layar Chat).
- **Caching TTS Berbasis Hash Konten:** `SpeechCacheKey.kt` menormalisasi teks dan menghitung SHA-256 (`tts-<voice>-<24hex>.mp3`) sehingga pemutaran ulang balasan yang sama langsung membaca `context.cacheDir/tts/` tanpa membebani server `kokoro-v1`.

### Blind Spot & Bug yang Ditemukan
1. **`[MENENGAH - C3.1]` Tabrakan Mikrofon dan Pemutaran TTS (`VoiceInput` Tidak Menghentikan `SpeechPlayback`):**
   - **Lokasi:** `app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt` (baris 240–248).
   - **Masalah:** Saat pengguna menekan tombol mikrofon (`onVoiceClick`) ketika asisten sedang membacakan jawaban (`SpeechPlaybackState.Playing`), pemutaran TTS tidak dihentikan. Akibatnya, mikrofon merekam suara TTS asisten sendiri. Sebaliknya, menekan tombol `"Dengarkan"` saat mikrofon sedang merekam juga perlu dicegah atau menghentikan rekaman terlebih dahulu.
2. **`[MENENGAH - C3.2]` Kegagalan `MediaPlayer` Ditelan Sebagai Selesai Normal (`Idle` alih-alih `Error`):**
   - **Lokasi:** `app/src/main/java/com/example/empati/data/speech/SpeechAudio.kt` (baris 57–68) dan `app/src/main/java/com/example/empati/data/speech/SpeechPlaybackController.kt` (baris 59–66).
   - **Masalah:** Di `MediaPlayerSpeechAudioPlayer.play`, baik `setOnErrorListener` maupun blok `catch` memanggil `onCompleted()`, yang mengubah state `SpeechPlaybackController` menjadi `SpeechPlaybackState.Idle` tanpa pesan kesalahan. Jika file MP3 korup atau decoder gagal, pengguna hanya melihat tombol kembali ke `"Dengarkan"` tanpa tahu mengapa suara tidak keluar.
3. **`[MENENGAH - C3.3]` Kontensi Pembaruan State UI pada Streaming Delta (`EmpatiViewModel.sendChat`):**
   - **Lokasi:** `app/src/main/java/com/example/empati/EmpatiViewModel.kt` (baris 514–545).
   - **Masalah:** Setiap potongan token SSE (`onDelta`) di thread `Dispatchers.IO` memanggil `AnswerGuardrail.enforce(partial.toString())` lalu meluncurkan coroutine baru `viewModelScope.launch(Dispatchers.Main.immediate)` yang menyalin seluruh `chatMessages.toMutableList()`. Setelah `withContext(Dispatchers.IO)` selesai, coroutine utama langsung memodifikasi `chatMessages` (baris 537–545). Jika masih ada job delta yang antre atau jika pengguna menekan `"Hentikan"` (`stopChatGeneration`) tepat saat `withContext` selesai, urutan penulisan `chatMessages` dapat saling menimpa.
4. **`[MENENGAH - C3.4]` Konfigurasi Cleartext Network untuk Demo Perangkat Fisik via Wi-Fi LAN:**
   - **Lokasi:** `app/src/main/res/xml/network_security_config.xml`.
   - **Masalah:** `<base-config cleartextTrafficPermitted="false">` hanya membuka pengecualian untuk `10.0.2.2`, `127.0.0.1`, dan `localhost`. Ini berfungsi untuk emulator (`10.0.2.2`) dan perangkat fisik via USB (`adb reverse tcp:13305 tcp:13305` → `127.0.0.1`). Namun `setup-empati-lemonade.ps1` dan `ProfileScreen.kt` mendorong pengguna memasukkan IP LAN laptop (mis. `http://192.168.1.7:13305`) untuk demo nirkabel. Pada Android 9+, koneksi HTTP ke `192.168.x.x` akan langsung ditolak oleh OS dengan `Cleartext HTTP traffic to 192.168.x.x not permitted`! Solusi bersih: tambahkan `<debug-overrides><trust-anchors>...</trust-anchors></debug-overrides>` atau izinkan cleartext pada build debug / konfigurasi jaringan lokal agar pengujian LAN ke `:13305` berjalan mulus.

---

## Kriteria 4: Kualitas UI & Bahasa (`/humanizer` & Rebranding 100% ke "Empati")

### Bagian yang Sudah Sesuai
- **Arsitektur Visual *Soft Daylight* & Minimalisme:** Sistem tema di `Color.kt`, `Type.kt`, `Theme.kt`, dan `Components.kt` sangat rapi: menggunakan palet *Sage / Clay / Mist / Stone* dengan rasio kontras ≥ 4,5:1, tipografi ekspresif *Fraunces* + fungsional *Inter* + *JetBrains Mono* (khusus metrik/kode), tanpa border tebal atau bayangan kasar, serta mendukung `LocalReducedMotion`.
- **Prompt Baru (`EmpatiPromptBuilder.SYSTEM_PROMPT`):** Sudah menggunakan nama `"Empati"`, melarang klise bot (*"Tentu saja!"*, *"Sebagai asisten AI"*), mewajibkan 2–4 kalimat lugas tanpa Markdown agar nyaman dibacakan oleh TTS `kokoro-v1`, dan menjaga format kontak darurat SAPA 129 (`129` atau WhatsApp `08111-129-129`).

### Blind Spot & Bug yang Ditemukan
1. **`[KRITIS - C4.1]` Rebranding ke "Empati" Belum Tuntas (Nama Lama `"SRYA Emora"` & `"AI-EMPATI"` Masih Tersebar di 14 File):**
   - **Daftar Lokasi yang Masih Menampilkan `"SRYA Emora"` atau `"AI-EMPATI"`:**
     1. `app/src/main/res/values/strings.xml` (baris 2): `<string name="app_name">SRYA Emora</string>` — nama aplikasi di launcher Android masih **SRYA Emora**!
     2. `app/src/main/java/com/example/empati/ui/screens/SplashScreen.kt` (baris 91): Judul utama layar pembuka masih `"SRYA Emora"`.
     3. `app/src/main/java/com/example/empati/ui/screens/OnboardingScreen.kt` (baris 117): Slide ke-3 berjudul `"Ngobrol bersama SRYA Emora"`.
     4. `app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt` (baris 189): Header halaman masuk/daftar masih `"SRYA Emora"`.
     5. `app/src/main/java/com/example/empati/ui/screens/HomeScreen.kt` (baris 419, 527): Kartu fitur tertulis `"Tanya SRYA Emora"` dan `"ngobrol dengan SRYA Emora"`.
     6. `app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt` (baris 394, 767): Judul top bar Chat tertulis `"SRYA Emora"` dan indikator mengetik tertulis `"SRYA Emora sedang mengetik…"`.
     7. `app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt` (baris 364, 706): Tombol CTA tertulis `"Tanyakan ke SRYA Emora"` dan `"Diskusikan dengan SRYA Emora"`.
     8. `app/src/main/java/com/example/empati/ui/screens/ProfileScreen.kt` (baris 232–233): Kartu informasi tertulis `"Tentang SRYA Emora"` dan `"Versi 4.1.4 · Inklusif semua kalangan"` (tidak sinkron dengan `versionName = "4.1.5"` di `app/build.gradle.kts`).
     9. `app/src/main/java/com/example/empati/EmpatiViewModel.kt` (baris 241): Pesan pembuka chat pertama kali berbunyi `"Halo, saya SRYA Emora..."`.
     10. `app/src/main/java/com/example/empati/data/chat/EmpatiChatUseCase.kt` (baris 110) & `app/src/main/java/com/example/empati/data/rag/OllamaRagService.kt` (baris 29–46, 139): Ketika pengguna menyapa `"Halo"`, asisten menjawab `"Saya Empati..."` (baris 120), tetapi ketika pengguna bertanya `"Siapa kamu?"`, asisten menjawab `"Halo, salam kenal! Saya SRYA Emora..."` (`OllamaRagService.SRYA_EMORA_SELF_INTRO`).
     11. `app/src/main/java/com/example/empati/data/rag/RagKnowledgeRepository.kt` (baris 168): Hanya mengenali `"apa itu srya emora"` dan `"siapa srya emora"`, belum mengenali `"apa itu empati"` atau `"siapa empati"`.
     12. `app/src/screenshotTest/kotlin/com/example/empati/PreviewSupport.kt` (baris 153, 216): Fixture screenshot test masih memakai `"AI-EMPATI"`.
     13. `supabase/functions/empati-chat/index.ts` (baris 87, 126): Header `X-Title` dan `User-Agent` masih menyebut `"SRYA Emora"`.
     14. `app/src/test/java/com/example/empati/auth/AsnAndRagArchitectureTest.kt` (baris 195, 362–380): Unit test lama masih menguji string `"SRYA Emora"`.

---

## Kriteria 5: Kelengkapan Test (`./gradlew testDebugUnitTest`)

### Hasil Eksekusi Unit Test Saat Ini
- **Perintah:** `./gradlew testDebugUnitTest --rerun-tasks` (dengan JDK 21) & `pwsh ./scripts/tests/verify-dryrun.ps1`
- **Hasil Unit Test JVM:** **21 test suites, 176 tests, 0 failures, 0 errors (`BUILD SUCCESSFUL`)**.
  - `CfEngineTest`: 30/30 lulus
  - `LemonadeUnifiedClientTest`: 14/14 lulus
  - `HybridTriageBridgeTest`: 13/13 lulus
  - `VoiceInputControllerTest`: 13/13 lulus
  - `AsnAndRagArchitectureTest`: 13/13 lulus
  - `EmpatiChatUseCaseTest`: 11/11 lulus
  - `AnswerGuardrailTest`: 8/8 lulus
  - `GroundedChatGatewayTest`: 8/8 lulus
  - `EmbeddingIndexTest`: 8/8 lulus
  - `SpeechPlaybackControllerTest`: 8/8 lulus
  - `EmpatiPromptBuilderTest`: 7/7 lulus
  - `UserPresentationTest`: 7/7 lulus
  - `SpeechCacheKeyTest`: 6/6 lulus
  - `ChatFallbackLadderTest`: 5/5 lulus
  - `ChatCapabilityPresenterTest`: 5/5 lulus
  - `EmpatiCorpusTest`: 5/5 lulus
  - `LocalCorpusIndexTest`: 5/5 lulus
  - `LemonadeStatusPresenterTest`: 4/4 lulus
  - `StreamingChatTest`: 4/4 lulus
  - `WavCodecTest`: 1/1 lulus
  - `ExampleUnitTest`: 1/1 lulus
- **Hasil Verifikasi Skrip DryRun (`verify-dryrun.ps1`):** **LULUS** (`Pemeriksaan DryRun lulus: host, lima alias, model, bench, port 13305, tanpa jejak Ollama.`).
- **Catatan Lingkungan:** Variabel sistem `JAVA_HOME` pada mesin host saat ini menunjuk ke path tidak valid (`C:\java\jdk8se\bin`), sedangkan JDK yang valid tersedia di `C:\Users\ADVAN\.jdks\corretto-21.0.5`. Penambahan `org.gradle.java.home` pada `gradle.properties` (atau perbaikan `JAVA_HOME` lokal) akan memastikan `./gradlew` langsung berjalan tanpa perlu override environment di terminal baru.

### Celah Cakupan Test (Test Gaps) yang Perlu Ditambahkan
1. **Test Pengayaan Query Retrieval dari Triage (`EmpatiChatUseCaseTest`):** Memastikan ketika `TriageContext` atau `ScreeningContextSnapshot` tersedia, query yang dikirim ke `KnowledgeRetriever` diperkaya dengan kosakata klinis hasil skrining sehingga pertanyaan seperti *"Analisis hasil skrining saya"* tetap menarik chunk rujukan yang tepat.
2. **Test Fallback `ScreeningContextSnapshot` di `GroundedChatGatewayTest` & `EmpatiPromptBuilderTest`:** Memastikan saat `TriageContext` bernilai `null` tetapi `ScreeningContextSnapshot` ada (skenario buka ulang aplikasi), prompt ke LLM tetap menyertakan ringkasan hasil skrining pengguna.
3. **Test `enable_thinking: false` & Sanitasi `<think>` (`LemonadeUnifiedClientTest` & `AnswerGuardrailTest`):** Memastikan request `/v1/chat/completions` mengirim `"enable_thinking":false` dan `AnswerGuardrail` menghapus blok `<think>...</think>` serta angka desimal `0.xx dari 1.00`.
4. **Test Rebranding Penuh (`EmpatiChatUseCaseTest` & `AsnAndRagArchitectureTest`):** Memastikan pertanyaan identitas (`"siapa kamu"`, `"apa itu empati"`) selalu memperkenalkan **Empati** secara konsisten tanpa residu `"SRYA Emora"`.

---

## Rencana Eksekusi Perbaikan (Menunggu Konfirmasi Anda)

Jika Anda menyetujui temuan audit di atas, saya siap mengeksekusi perbaikan terarah berikut sebelum persiapan deploy:

1. **Paket 1 — Konsistensi Rebranding 100% ke "Empati" & Humanizer Copy (`[C4.1]`):**
   - Mengubah seluruh teks UI di `strings.xml`, `SplashScreen.kt`, `OnboardingScreen.kt`, `AuthScreen.kt`, `HomeScreen.kt`, `ChatScreen.kt`, `ResultScreen.kt`, `ProfileScreen.kt` (termasuk sinkronisasi nomor versi ke `4.1.5`), `EmpatiViewModel.kt`, `OllamaRagService.kt`, `RagKnowledgeRepository.kt`, `PreviewSupport.kt`, dan `supabase/functions/empati-chat/index.ts` dari `"SRYA Emora"` / `"AI-EMPATI"` menjadi **"Empati"** dengan bahasa yang hangat dan alami.
   - Memperbarui asersi terkait di `AsnAndRagArchitectureTest.kt` dan menambahkan pemeriksaan otomatis agar tidak ada lagi string `"SRYA Emora"` di layar UI maupun respons chatbot.
2. **Paket 2 — Penyambungan Utuh Dual-System Triage ke RAG & Lintas Sesi (`[C1.1]`, `[C1.2]`, `[C1.3]`):**
   - Menghubungkan `HybridTriageBridge.toRetrievalQuery(triage)` (dan fallback kata kunci dari `ScreeningContextSnapshot`) ke dalam `EmpatiChatUseCase` saat memanggil `retriever.retrieve(...)`.
   - Memperbarui `EmpatiPromptBuilder.buildUserPrompt` dan `GroundedChatGateway` agar menyertakan konteks dari `ScreeningContextSnapshot` apabila `TriageContext` in-memory tidak tersedia, serta memulihkan `TriageContext` dari `ResearchRecord` terakhir milik pengguna aktif di `EmpatiViewModel.enterSession()`.
   - Memperkuat `AnswerGuardrail.enforce` untuk menyaring blok `<think>...</think>` dan kebocoran angka desimal internal (`0.xx` / `dari 1.00`).
3. **Paket 3 — Kepatuhan Runtime Lemonade & Ketahanan Indeks/Audio/Jaringan (`[C2.1]`–`[C2.5]`, `[C3.1]`–`[C3.4]`):**
   - Menambahkan `"enable_thinking": false` pada payload `/v1/chat/completions` di `LemonadeUnifiedClient.kt` dan `scripts/bench-empati.ps1`, serta memperbaiki `Write-Short` pada header WAV di `bench-empati.ps1` dan perbandingan `Float` di `MiniJson.kt`.
   - Memperbaiki `llmReachable` di `ChatScreen.kt` menjadi `vm.lemonadeStatus?.isReady == true`, menghentikan TTS saat mikrofon mulai merekam, meneruskan error `MediaPlayer` ke `SpeechPlaybackState.Error`, merapikan sinkronisasi delta streaming di `EmpatiViewModel.sendChat`, mencegah `CorpusIndexer` menyimpan vektor kosong saat gagal embed, mengizinkan cleartext LAN di mode debug untuk pengujian perangkat fisik, serta mengatur `org.gradle.java.home` yang valid.
   - Menjalankan ulang seluruh unit test (`./gradlew testDebugUnitTest`) dan `pwsh ./scripts/tests/verify-dryrun.ps1` hingga 100% hijau.

---

## Status Perbaikan (12 November 2026)

Seluruh paket dikerjakan. `./gradlew testDebugUnitTest`: 22 suite, 192 test, 0 gagal. `verify-dryrun.ps1` lulus.

| Temuan | Status | Perubahan |
|---|---|---|
| C1.1 retrieval tanpa konteks triage | Selesai | `EmpatiChatUseCase.retrievalQueryFor` menambahkan kosakata klinis dari `HybridTriageBridge.toRetrievalQuery` untuk pertanyaan tentang hasil sendiri |
| C1.2 konteks hilang lintas sesi | Selesai | `GroundedChatGateway` meneruskan `snapshot`; `EmpatiPromptBuilder` dan `HybridTriageBridge.toPromptBlock(snapshot)` memakainya bila konteks mesin tidak ada |
| C1.3 kebocoran desimal CF | Selesai | `AnswerGuardrail` menyaring `0.62 dari 1.00` dan angka setelah label keyakinan |
| C1.4 tumpukan RAG lama | Sebagian | Teks dan identitas diganti ke Empati. Kelas lama (`OllamaRagService`, `ChatEngine`) masih ada karena `SupabaseCloudSyncService`, `EmpatiDatabase`, dan tes lama bergantung padanya. Penghapusan butuh tiket terpisah |
| C2.1 `enable_thinking` | Selesai | Dikirim di payload chat dan stream, plus di `bench-empati.ps1`. `AnswerGuardrail` membuang blok `<think>` |
| C2.2 badge "Lokal siap" | Selesai | Memakai `lemonadeStatus?.isReady == true` |
| C2.3 indeks vektor kosong | Selesai | `CorpusIndexer` tidak mengindeks chunk yang gagal di-embed |
| C2.4 header WAV skrip | Selesai | `Write-Short` untuk field 16-bit |
| C2.5 `Float == Double` | Selesai | `MiniJson` |
| C3.1 mikrofon vs TTS | Selesai | Merekam menghentikan TTS; memutar TTS membatalkan rekaman |
| C3.2 error `MediaPlayer` | Selesai | Callback `onError` menghasilkan `SpeechPlaybackState.Error` |
| C3.3 race delta streaming | Selesai | Delta yang antre dibuang setelah stream ditutup |
| C3.4 cleartext LAN | Selesai (hanya debug) | `app/src/debug/res/xml/network_security_config.xml`. Build release tetap hanya `10.0.2.2`, `127.0.0.1`, `localhost` |
| C4.1 rebranding | Selesai | Semua teks UI, prompt, User-Agent, fixture screenshot, dan edge function. Versi di Profil kini dari `BuildConfig.VERSION_NAME` |

Sisa yang sengaja tidak disentuh:

- Nama berkas drawable `srya_emora_logo_*` dan gambar logonya. Gambar perlu diganti desainer.
- Dokumen akar lama (`FRONTEND_DESIGN.md`, `EXPERT_SYSTEM.md`, `PROJECT_CONTEXT_SUMMARY.md`, `DATABASE_AUTH_AND_RAG_ARCHITECTURE.md`) masih memakai nama dan arsitektur lama.
- Komentar tabel di migrasi Supabase yang sudah diterapkan.
- `JAVA_HOME` mesin ini menunjuk ke path yang tidak ada. Memasukkan path JDK ke `gradle.properties` akan merusak build di mesin lain.
- Edge function `empati-chat` sudah diubah di repo tetapi belum di-deploy ke Supabase.
