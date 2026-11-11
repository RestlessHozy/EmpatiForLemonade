# Spesifikasi Lemonade untuk Empati

Riset teknis untuk integrasi backend Empati dengan AMD Lemonade Server. Semua temuan di bawah diambil dari dokumentasi resmi Lemonade dan registry model di repo `lemonade-sdk/lemonade` pada 11 November 2026. Klaim yang tidak bisa saya verifikasi dari sumber primer ditandai **belum terverifikasi**.

Batas keras untuk tahap ini: **tidak ada port 11434 dan tidak ada endpoint gaya Ollama di kode Empati**. Semua panggilan inference memakai Lemonade di `13305`. Lemonade memang menyediakan `/api/tags` untuk kompatibilitas klien Ollama, tetapi Empati tidak memakainya.

## 1. Server dan jaringan

| Setting | Nilai default | Sumber |
| --- | --- | --- |
| Port HTTP | `13305` | Settings reference, `port` |
| Host bind | `"localhost"` | Settings reference, `host` |
| Base URL lokal | `http://localhost:13305` | Contoh di semua halaman API |
| File config | `%USERPROFILE%\.config\lemonade\config.json` (Windows) | Bagian `config.json` |
| Override runtime | `lemond --port 13305 --host 0.0.0.0` | Bagian `lemond CLI` |

Perubahan host yang persisten:

```bash
lemonade config set host=0.0.0.0
```

Host `0.0.0.0` membuka seluruh endpoint dari mesin lain di jaringan, termasuk `/internal/*` (shutdown dan ubah config). Dokumentasi menyarankan `LEMONADE_API_KEY` aktif sebelum membuka host, karena tanpa key semua endpoint inference bisa diakses tanpa autentikasi.

Akses dari Android:

- Emulator memakai alias host loopback `10.0.2.2`, jadi base URL `http://10.0.2.2:13305/v1`.
- Perangkat fisik memakai IP LAN mesin Lemonade, misalnya `http://192.168.1.7:13305/v1`, dan host wajib `0.0.0.0`.
- Konfigurasi saat ini di `OllamaRagService.CANDIDATE_ENDPOINTS` mencoba `127.0.0.1:11434`, `10.0.2.2:11434`, dan IP LAN hardcoded. Ketiganya harus diganti ke port 13305.
- Android memblokir HTTP polos sejak API 28 kecuali diizinkan. Repo belum punya `network_security_config.xml`, jadi tahap implementasi perlu domain-config `cleartextTrafficPermitted` untuk `10.0.2.2` dan IP LAN saat pengujian.

Sumber fakta emulator: [Android emulator networking](https://developer.android.com/studio/run/emulator-networking).

## 2. Endpoint yang dipakai Empati

Ringkasan endpoint OpenAI-compatible yang relevan (sumber: [OpenAI-Compatible API](https://lemonade-server.ai/docs/api/openai/)):

| Endpoint | Modality | Status dokumentasi |
| --- | --- | --- |
| `POST /v1/chat/completions` | messages ke completion | partially available |
| `POST /v1/embeddings` | teks ke vektor | fully available |
| `POST /v1/audio/transcriptions` | audio ke teks | partially available |
| `POST /v1/audio/speech` | teks ke audio | partially available |
| `GET /v1/models` | daftar model | fully available |
| `WS /v1/realtime` | streaming transkripsi | partial |

### 2.1 Chat dan reasoning

Payload minimum:

```json
{
  "model": "Qwen3-4B-GGUF",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "..."}
  ],
  "stream": false
}
```

Parameter yang tersedia: `messages` (wajib), `model` (wajib), `stream`, `stop`, `logprobs`, `temperature`, `repeat_penalty` (1.0 sampai 2.0), `frequency_penalty` (-2.0 sampai 2.0), `seed`, `top_k`, `top_p`, `tools`, `tool_choice`, `response_format`, `max_tokens`, `max_completion_tokens`, `enable_thinking`, `route_trace`, dan `ctx_size`.

Catatan yang memengaruhi desain prompt:

- `max_tokens` dan `max_completion_tokens` sebaiknya dikirim salah satu. Bila keduanya dikirim, llama.cpp memakai `max_tokens` sedangkan vLLM memakai `max_completion_tokens`.
- `enable_thinking: false` mematikan reasoning pada model yang berpikir. Ini penting untuk latensi chat di perangkat.
- `response_format` mendukung `{"type": "json_object"}`. Bila nanti Empati butuh output terstruktur dari LLM, ini jalurnya.

Respons non-streaming memuat `choices[0].message.content`, `reasoning_content` (model reasoning), `usage`, dan `timings` dari llama.cpp. Streaming memakai SSE dengan `data:` per chunk, diakhiri `data: [DONE]`, dan chunk terpisah berisi `usage` bila `stream_options.include_usage` diset.

Error yang perlu ditangani di klien: `400 invalid_json`, `400 missing_argument`, `400 invalid_argument`, `404 model_not_found`, `409 router_residency_conflict`, `409 slots_pinned_error`, `500 model_load_error`. Pola `409` muncul saat semua model terpin atau eviction bentrok, jadi fallback ke model lain harus tersedia.

Alias untuk failover model (sumber: [Add a Custom Model](https://lemonade-server.ai/docs/guide/configuration/custom-models/#model-aliases-aliasesjson)):

```bash
lemonade alias add empati-reasoning Qwen3-4B-GGUF
lemonade alias add empati-embed nomic-embed-text-v1-GGUF
lemonade alias add empati-stt Whisper-Large-v3-Turbo
lemonade alias add empati-tts kokoro-v1
```

Alias disimpan di `aliases.json` dan dimuat saat startup. Alias bisa dipakai di semua endpoint, muncul sebagai entri mandiri di `/v1/models`, dan target-nya boleh diganti tanpa mengubah kode klien. Nama model konkret (`user.*`, `extra.*`, `builtin.*`) selalu menang atas alias yang bertabrakan, dan pembuatan alias yang menabrak nama model ditolak.

Konsekuensi untuk Empati: hardcode alias `empati-reasoning` di klien, bukan nama model. Ganti model cukup lewat `lemonade alias add` di mesin demo, tanpa rebuild APK. Ini jauh lebih murah daripada daftar `OPENROUTER_CANDIDATE_MODELS` yang sekarang di-hardcode.

### 2.2 Embeddings

```json
{
  "model": "nomic-embed-text-v1-GGUF",
  "input": ["Hello, world!", "How are you?"],
  "encoding_format": "float"
}
```

Parameter: `input` (string atau array string, wajib), `model` (wajib), `encoding_format` (`float` atau `base64`, default `float`), `ctx_size`.

```json
{
  "object": "list",
  "data": [
    {"object": "embedding", "index": 0, "embedding": [0.035327356308698654, "..."]}
  ],
  "model": "nomic-embed-text-v1-GGUF",
  "usage": {"prompt_tokens": 12, "total_tokens": 12}
}
```

Contoh dokumentasi menampilkan 768 nilai float per vektor untuk `nomic-embed-text-v1-GGUF`, jadi normalisasi dan penyimpanan harus mengikuti panjang vektor yang benar-benar dikembalikan, bukan angka yang diasumsikan.

Model embedding yang terdaftar di registry (`src/cpp/resources/server_models.json`): `nomic-embed-text-v1-GGUF` (0.0781 GB), `nomic-embed-text-v2-moe-GGUF`, `Qwen3-Embedding-0.6B-GGUF`, `Qwen3-Embedding-4B-GGUF`, `Qwen3-Embedding-8B-GGUF`.

Error khusus: `400` bila model yang dipilih bukan model embedding, `501` bila model llama.cpp tidak punya label embedding.

### 2.3 Speech to text

Endpoint memakai `multipart/form-data`, bukan JSON. Format file yang didokumentasikan adalah WAV.

```bash
curl http://localhost:13305/v1/audio/transcriptions \
  -F "model=Whisper-Large-v3-Turbo" \
  -F "language=id" \
  -F "response_format=json" \
  -F file=@rekaman.wav
```

```json
{"text": " Saya sering pusing sejak dua minggu lalu.\n"}
```

Parameter: `file` (wajib, WAV), `model` (wajib), `language` (ISO 639-1, default `auto` untuk whisper.cpp dan mengikuti backend untuk model lain), `response_format` (`json`, `verbose_json`, `text`, `srt`, `vtt`), `prompt`, `temperature`.

Catatan per model:

- `Whisper-Large-v3-Turbo` memakai recipe `whispercpp`, checkpoint `ggerganov/whisper.cpp:ggml-large-v3-turbo.bin`, plus cache encoder NPU `amd/whisper-large-turbo-onnx-npu`. Ukuran 1.62 GB, label `transcription` dan `realtime-transcription`.
- `Whisper-Tiny` 0.075 GB dan `Whisper-Base`, `Whisper-Small`, `Whisper-Medium`, `Whisper-Large-v3` juga tersedia.
- Moonshine (`Moonshine-Tiny-Streaming`, `Moonshine-Small-Streaming`, `Moonshine-Medium-Streaming`) memakai recipe `moonshine`. Dokumentasi menyebut Moonshine mengabaikan `temperature`, dan `response_format` `srt` atau `vtt` tidak didukung pada FastFlowLM.
- Bila `language` tidak dikirim, hasil transkripsi Bahasa Indonesia tidak dijamin. Kirim `language=id`.

Untuk input mikrofon berkelanjutan, ada WebSocket `/v1/realtime` di port yang sama (`ws://<host>:13305/v1/realtime?model=Whisper-Tiny`). Audio wajib PCM16 mono 16 kHz, dikirim sebagai base64 dalam potongan sekitar 85 sampai 256 ms. Pesan klien: `session.update`, `input_audio_buffer.append`, `input_audio_buffer.commit`, `input_audio_buffer.clear`. Pesan server mencakup `input_audio_buffer.speech_started`, `conversation.item.input_audio_transcription.delta`, dan `conversation.item.input_audio_transcription.completed`. VAD bawaan bisa diatur lewat `threshold`, `silence_duration_ms`, dan `prefix_padding_ms`, atau dimatikan dengan `turn_detection: null`.

### 2.4 Text to speech

```json
{
  "model": "kokoro-v1",
  "input": "Hasil skrining Anda menunjukkan indikator sedang.",
  "voice": "af_sky",
  "speed": 1.0,
  "response_format": "mp3"
}
```

Respons non-streaming adalah byte audio (`audio/mpeg` untuk mp3). Parameter:

- `input` (wajib), `model` (wajib), `speed` (default 1.0).
- `voice`: untuk `kokoro-v1` menerima nama voice OpenAI (`alloy`, `ash`, dan seterusnya) atau voice Kokoro (`af_sky`, `am_echo`, dan seterusnya), default `shimmer`.
- `response_format`: `kokoro-v1` mendukung `mp3`, `wav`, `opus`, `pcm`; OpenMOSS hanya `wav` dan `pcm`.
- `stream_format: "audio"` atau `stream: true` untuk streaming. Streaming hanya menghasilkan `pcm`, dengan header `audio/l16;rate=24000;endianness=little-endian`.

Implikasi Android: `response_format=mp3` bisa diputar lewat `MediaPlayer` setelah byte disimpan ke cache, sedangkan streaming PCM butuh `AudioTrack` dengan sample rate 24000 dan PCM16 little endian. Untuk tahap 1, jalur non-streaming lebih sederhana dan cukup.

### 2.5 Daftar model

`GET /v1/models` mengembalikan `id`, `recipe`, `labels`, `downloaded`, `size`, `context_length`, `components`, dan `checkpoint_status`. Gunakan `?show_all=true` untuk melihat model katalog yang belum diunduh. `GET /v1/models/{id}` mengembalikan satu model.

Untuk aplikasi, urutan startup yang aman: panggil `/v1/models`, cek apakah alias `empati-reasoning`, `empati-embed`, `empati-stt`, dan `empati-tts` ada. Bila tidak ada, tampilkan status "Lemonade belum siap" alih-alih mengirim request yang memicu unduhan multi-gigabyte.

## 3. MCP gateway

Lemonade mengekspos dirinya sebagai MCP server dengan transport Streamable HTTP, spesifikasi `2025-06-18`, hanya capability `tools`. Endpoint tunggal: `POST http://localhost:13305/mcp` (sumber: [MCP Gateway](https://lemonade-server.ai/docs/api/mcp/)).

Method yang didukung: `initialize`, `notifications/initialized`, `notifications/cancelled`, `tools/list`, `tools/call`, `ping`. `GET /mcp` selalu `405` karena tidak ada event stream dari server.

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {"protocolVersion": "2025-06-18", "capabilities": {}}
}
```

```json
{"jsonrpc": "2.0", "id": 2, "method": "tools/list"}
```

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "lemonade_chat",
    "arguments": {
      "model": "Qwen3-1.7B-GGUF",
      "messages": [{"role": "user", "content": "hi"}],
      "max_tokens": 16
    }
  }
}
```

Tool yang tersedia: `lemonade_list_models`, `lemonade_chat`, `lemonade_transcribe_audio`, `lemonade_generate_image`, `lemonade_omni`, dan `lemonade_docs`.

Batasan yang penting untuk desain:

- Tidak ada tool embeddings dan tidak ada tool TTS. Keduanya harus lewat REST `/v1/embeddings` dan `/v1/audio/speech`.
- `lemonade_chat` mengabaikan `stream`.
- Tidak ada session resumption. Header `Mcp-Session-Id` tidak diterbitkan.
- Kegagalan level tool dikembalikan sebagai hasil JSON-RPC sukses dengan `"isError": true`, bukan sebagai error JSON-RPC. Parser klien harus memeriksa flag itu.
- Kode error JSON-RPC: `-32700` JSON tidak valid, `-32600` bukan objek JSON-RPC, `-32601` method tidak dikenal, `-32602` params salah, `-32603` error internal.

Untuk Empati di Android, MCP bukan jalur utama. Jalur utamanya REST `/v1/*` karena butuh embeddings dan TTS yang tidak tersedia di MCP. MCP berguna bila nanti ada orkestrator desktop atau kalau ingin satu pintu untuk tool lokal.

## 4. Arsitektur neuro-symbolic untuk triage

Bagian ini menyatukan mesin Certainty Factor yang sudah ada dengan RAG dan LLM. Aturan dasarnya: **CF adalah satu-satunya sumber angka risiko, LLM hanya menarasikan**.

### 4.1 Pembagian peran

| Komponen | Peran | Tidak boleh dilakukan |
| --- | --- | --- |
| `CfInferenceEngine`, `CfKnowledgeBase`, `CfResultGenerator` | Menghitung CF, level, kategori dominan, faktor protektif | Diubah formula, ambang, urutan kombinasi, atau nama enum yang dipersist |
| `UserResultPresenter` | Mengubah hasil CF menjadi kalimat pengguna tanpa angka | Membocorkan CF, MB, MD, atau skor |
| Retriever (embeddings) | Mengambil chunk rujukan dengan id stabil | Mengarang chunk saat indeks kosong |
| LLM (Lemonade) | Menarasikan konteks terstruktur dan chunk rujukan | Menghasilkan angka risiko, diagnosis, atau label level baru |

Alasan pembagian ini sejalan dengan literatur: sistem neuro-symbolic yang dapat diaudit memakai LLM untuk penalaran bahasa sementara sistem pakar berbasis aturan mengendalikan input dan memvalidasi output terhadap ontologi ([Nature, 2025](https://www.nature.com/articles/s43856-025-01194-x)). Kerangka lain menambahkan lapisan simbolik yang menegakkan verifikasi aturan di atas jawaban LLM ([Retrieval-LLM-Symbolic, 2026](https://dl.acm.org/doi/full/10.1145/3804601.3804680)). Tinjauan sistematis RAG di layanan kesehatan mencatat bahwa langkah retrieval menurunkan halusinasi sekaligus meningkatkan ketertelusuran sumber ([RAG for LLMs in healthcare](https://pmc.ncbi.nlm.nih.gov/articles/PMC12157099/)).

### 4.2 Konflik yang harus diputuskan sebelum koding

Audit menemukan aturan *zero-leak*: `AsnAndRagArchitectureTest` memastikan prompt dan balasan tidak memuat "CF", "MB", "MD", angka skor, atau persen. Permintaan saat ini adalah mengirim nilai CF sebagai grounded context. Kedua hal ini bertabrakan, dan tidak bisa dipenuhi bersamaan tanpa keputusan eksplisit.

Tiga opsi:

1. **Label saja (paling aman).** Kirim level, kategori, faktor dominan, faktor protektif, dan id gejala. Tidak ada angka sama sekali. Guardrail lama tetap berlaku, tes lama tetap lulus.
2. **Angka untuk mesin, bukan untuk pengguna.** Kirim `cf` numerik dengan instruksi eksplisit bahwa nilai itu hanya untuk kalibrasi nada, tidak boleh disebut, ditampilkan, atau dihitung ulang. Butuh tes guardrail baru yang memeriksa output akhir.
3. **Angka terbuka.** Tampilkan CF kepada pengguna. Ini membalik keputusan desain produk sebelumnya dan mengubah tes secara sengaja.

Spesifikasi ini menyarankan opsi 2 untuk chat dan opsi 1 untuk UI, karena opsi 2 memberi LLM kalibrasi yang lebih baik tanpa mengubah kontrak tampilan.

### 4.3 Skema konteks terstruktur

Nama field mengikuti enum yang sudah dipersist, supaya payload riset dan konteks chat memakai kosakata yang sama.

```json
{
  "schema": "empati.screening_context/v1",
  "captured_at": "2026-11-11T09:12:00+07:00",
  "kb_version": "cf-kb-v2.0",
  "modules": [
    {
      "module": "HYPERTENSION",
      "level": "SEDANG",
      "cf": 0.62,
      "answered": 16,
      "unanswered": 0,
      "dominant_categories": [
        {
          "hypothesis": "LIFESTYLE",
          "cf": 0.71,
          "evidence": [
            {"question_id": "M09", "cf_user": 0.8, "cf_expert": 0.6}
          ]
        },
        {
          "hypothesis": "MEDICAL_HISTORY",
          "cf": 0.34,
          "evidence": [
            {"question_id": "M04", "cf_user": 0.5, "cf_expert": 0.4}
          ]
        }
      ],
      "protective_factors": ["M08"]
    },
    {
      "module": "GBV",
      "level": "RENDAH",
      "cf": 0.11,
      "answered": 10,
      "unanswered": 0,
      "dominant_categories": [],
      "protective_factors": []
    }
  ],
  "integrated": {
    "category": "HYPERTENSION_LIFESTYLE",
    "indication": true,
    "relationship_note": "chronic_stress_pathway"
  }
}
```

Aturan pengisian:

- `module`, `level`, `hypothesis`, dan `category` memakai nama enum apa adanya. Jangan menerjemahkan di payload.
- `evidence` dibatasi tiga item per kategori agar prompt tidak membengkak.
- `unanswered` wajib dikirim. Mesin CF mengecualikan jawaban kosong, dan LLM perlu tahu bahwa sebagian data tidak ada.
- `relationship_note` hanya berisi `chronic_stress_pathway` atau kosong. LLM tidak boleh mengubahnya menjadi klaim kausal.

### 4.4 Alur data

```
QuestionnaireScreen
   |
   v
EmpatiViewModel.answerDual()
   |
   +--> CfResultGenerator.buildModuleResult()  (deterministik, tidak berubah)
   |        |
   |        +--> ModuleResult: cf, level, categories, protective
   |
   +--> CfResultGenerator.integrate()          (deterministik, tidak berubah)
   |
   +--> ScreeningContextBuilder                 (baru, murni serialisasi)
            |
            +--> screening_context JSON  ---> prompt LLM
            |
            +--> UserResultPresenter     ---> tampilan tanpa angka
   |
   v
Chat request ke Lemonade
   |
   +--> POST /v1/embeddings  (query pengguna -> vektor)
   |        |
   |        +--> cosine search di indeks lokal -> top-k chunk + id
   |
   +--> POST /v1/chat/completions
            system: instruksi peran + larangan
            user:   pertanyaan + [KONTEKS SKRINING] + [RUJUKAN]
   |
   v
Guardrail pasca-generate
   |
   +--> buang token terlarang (CF, MB, MD, persen, skor)
   +--> tolak klaim diagnosis
   +--> wajibkan sitasi chunk bila jawaban menyebut fakta klinis
   |
   v
ChatReplyResult --> UI + rag_chat_logs
```

Retrieval saat ini praktis mati: `ChatEngine` memanggil `generateRagResponse` dengan corpus kosong, sehingga `retrievedChunks` selalu kosong dan chip sitasi tidak pernah muncul. Integrasi embeddings adalah kesempatan memperbaiki itu, bukan menambah fitur di atas jalur yang sudah rusak.

### 4.5 Format chunk rujukan

Setiap chunk yang masuk prompt wajib punya id yang bisa dirender ulang di UI.

```json
{
  "id": "who-hypertension-2023#sodium",
  "title": "WHO guideline for the pharmacological treatment of hypertension in adults",
  "publisher": "World Health Organization",
  "year": 2023,
  "section": "Lifestyle modification",
  "text": "Reduce dietary sodium intake to less than 2 g per day ...",
  "url": "https://www.who.int/publications/i/item/9789240049094",
  "score": 0.83
}
```

Aturan: chunk tanpa `id` dan `title` tidak boleh masuk prompt. Bila hasil pencarian kosong atau skor tertinggi di bawah ambang, LLM harus menjawab dengan kalimat batasan dan menawarkan skrining, bukan mengarang rujukan. Ambang awal yang wajar: 0.35 untuk cosine pada embedding ternormalisasi, dan ini harus diukur, bukan ditebak.

### 4.6 Kerangka system prompt

```
Peran:
Anda pendamping percakapan untuk Empati. Anda menarasikan hasil skrining
dan memberi informasi kesehatan umum dalam Bahasa Indonesia yang tenang.

Otoritas:
- Level risiko, kategori, dan faktor dominan sudah dihitung sistem pakar.
  Gunakan hanya nilai yang ada di [KONTEKS SKRINING]. Jangan menghitung,
  memperkirakan, atau mengubahnya.
- Angka cf di konteks bersifat internal. Jangan menyebut CF, MB, MD, bobot,
  skor, atau persentase apa pun.
- Jangan menyatakan diagnosis. Jangan menyebut obat, dosis, atau tindakan medis.

Rujukan:
- Fakta klinis hanya boleh berasal dari [RUJUKAN]. Sebutkan judul sumbernya.
- Jika [RUJUKAN] kosong, jawab bahwa Anda belum punya rujukan yang cukup.

Gaya:
- Maksimal tiga paragraf pendek atau empat poin.
- Kalimat lengkap, tanpa penanda markdown.
- Bila ada indikasi kekerasan, sertakan kontak SAPA 129 (129 atau WhatsApp
  08111-129-129) tanpa mengubah format nomornya.
```

Aturan "tanpa penanda markdown" sudah ada di `enforceOutputGuardrails` dan di Modelfile, jadi prompt baru harus konsisten dengan pembersih yang sudah berjalan.

### 4.7 Klaim nol halusinasi

Nol halusinasi klinis tidak bisa dijamin oleh prompt apa pun. Yang bisa dijamin adalah lingkup klaim:

- Klaim risiko sepenuhnya dihasilkan kode deterministik, bukan model bahasa.
- Klaim klinis di luar hasil skrining hanya boleh muncul bila ada chunk rujukan, dan setiap klaim wajib menyebut sumbernya.
- Bila retrieval kosong, model diarahkan menolak dengan sopan.
- Validator pasca-generate menolak jawaban yang memuat angka risiko, kata diagnosis, atau klaim tanpa sitasi.

Prompt yang menuntut sitasi terbukti mengubah reliabilitas pipeline RAG medis, dan evaluasi pada MedQA menunjukkan perbedaan nyata antar strategi prompting ([Citation-enforced prompting, 2026](https://www.mdpi.com/2076-3417/16/6/3013)). Representasi terstruktur seperti knowledge graph juga dipakai untuk menekan halusinasi pada tanya jawab klinis ([Ontology-grounded knowledge graphs, 2026](https://www.sciencedirect.com/science/article/pii/S1532046426000171)).

## 5. Urutan fallback

Urutan yang diusulkan, semuanya di port 13305:

1. Lemonade lokal lewat alias `empati-reasoning`.
2. Lemonade lewat model konkret bila alias gagal (`404 model_not_found`).
3. Supabase Edge Function `empati-chat`, hanya bila jaringan tersedia dan pengguna mengizinkan cloud.
4. Penyusun jawaban offline deterministik yang sudah ada.

Jalur OpenRouter langsung dan `text.pollinations.ai` dihapus dari klien, karena keduanya membuat dua sumber kebenaran dan memaksa `OPENROUTER_API_KEY` di build. Bila Supabase tetap dipakai sebagai fallback, key-nya sudah ada di edge function.

Klien harus membedakan tiga kegagalan: server tidak terjangkau (timeout koneksi), model belum diunduh (`404 model_not_found` atau `500 model_load_error`), dan model sibuk (`409 slots_pinned_error`). Pesan UI untuk ketiganya berbeda.

## 6. Catatan implementasi Android

- `OllamaRagService` memakai `HttpURLConnection` mentah dengan JSON yang dirakit manual. Untuk `/v1/audio/transcriptions`, body multipart harus dibangun manual dengan boundary, `Content-Disposition`, dan `Content-Type` per bagian. Ini rawan, tetapi menambah OkHttp hanya untuk satu endpoint perlu keputusan terpisah.
- Streaming SSE chat butuh parser baris `data:` dan penanganan `[DONE]`. Saat ini `stream` selalu `false` dan UI memakai `ThinkingBubble`, jadi streaming bisa masuk tahap berikutnya.
- `kotlinx.serialization` belum ada di dependency. Payload baru sebaiknya tetap memakai `JSONObject` agar diff tetap kecil, atau menambah dependency secara sadar.
- Model default harus datang dari `/v1/models` dan alias, bukan konstanta. Konstanta model yang sekarang (`qwen2.5:3b-instruct`, `empati-qwen2.5:3b`) mengacu ke penamaan Ollama dan tidak berlaku di Lemonade.
- Timeout: dokumentasi menyebut `global_timeout` default 600 detik untuk server. Klien sebaiknya tetap memakai timeout lebih pendek (25 sampai 60 detik) agar UI tidak menggantung.
- `LEMONADE_API_KEY` bila diset harus dikirim sebagai `Authorization: Bearer <key>`. Jangan menaruhnya di source; pakai `local.properties` seperti `OPENROUTER_API_KEY` sekarang.

## 7. Yang belum terjawab dokumentasi

- Kecepatan nyata `/v1/embeddings` dan `/v1/audio/transcriptions` pada perangkat AMD target. Ini harus diukur di mesin demo.
- Dukungan Bahasa Indonesia untuk Moonshine. Dokumentasi hanya menyebut `language` sebagai hint dan tidak menjamin akurasi per bahasa.
- Perilaku `response_format=json_object` pada model GGUF kecil yang dipakai. Bila tidak patuh, parsing output terstruktur harus punya fallback.
- Apakah audio `mp3` dari `kokoro-v1` bisa diputar langsung via `MediaPlayer` setelah disimpan di cache internal. Perlu uji di emulator.
- Apakah `LEMONADE_API_KEY` diperlukan pada skenario demo luring di satu laptop. Bila ya, mekanisme distribusi key ke perangkat harus diputuskan.

## 8. Sumber

- [OpenAI-Compatible API](https://lemonade-server.ai/docs/api/openai/)
- [MCP Gateway](https://lemonade-server.ai/docs/api/mcp/)
- [Lemonade Server Configuration](https://lemonade-server.ai/docs/guide/configuration/)
- [Add a Custom Model](https://lemonade-server.ai/docs/guide/configuration/custom-models/)
- [Audio and 3D in the Lemonade App](https://lemonade-server.ai/docs/guide/app/audio-and-3d/)
- Registry model: `lemonade-sdk/lemonade`, `src/cpp/resources/server_models.json` (commit `main`, diakses 11 November 2026)
- [Android emulator networking](https://developer.android.com/studio/run/emulator-networking)
- [NEURON: A Neuro-symbolic System for Grounded Clinical Explainability](https://arxiv.org/html/2605.01189v2)
- [Neuro-symbolic AI for auditable cognitive information systems (Nature, 2025)](https://www.nature.com/articles/s43856-025-01194-x)
- [Retrieval-LLM-Symbolic: A Dynamic Neuro-Symbolic Framework for Medical QA](https://dl.acm.org/doi/full/10.1145/3804601.3804680)
- [Retrieval augmented generation for large language models in healthcare](https://pmc.ncbi.nlm.nih.gov/articles/PMC12157099/)
- [Reducing Hallucinations in Medical AI Through Citation Enforced Prompting](https://www.mdpi.com/2076-3417/16/6/3013)
- [Ontology-grounded knowledge graphs for mitigating hallucinations](https://www.sciencedirect.com/science/article/pii/S1532046426000171)
