## Problem Statement

Pengguna Empati sudah bisa menyelesaikan skrining hipertensi dan KBG, tetapi percakapan setelahnya tidak benar-benar terhubung ke hasil skrining itu. Jawaban asisten berasal dari model yang dipanggil lewat jalur yang tidak bisa diandalkan: klien mencoba `127.0.0.1:11434`, lalu IP LAN hardcoded, lalu layanan cloud, lalu teks offline. Di laptop demo tanpa jaringan, sebagian besar jawaban jatuh ke teks offline yang sama untuk semua pertanyaan.

Pencarian rujukan juga tidak berjalan. Korpus RAG dikirim kosong ke lapisan retrieval, sehingga daftar chunk yang ditemukan selalu kosong dan chip sitasi tidak pernah muncul. Pengguna melihat jawaban kesehatan tanpa sumber, padahal aplikasi ini menyebut dirinya pendamping klinis.

Pengguna juga hanya bisa mengetik. Orang yang sedang menceritakan kekerasan atau gejala fisik sering lebih mudah berbicara daripada menulis, terutama di ponsel. Balasan asisten selalu berbentuk teks.

Bagi penyelenggara demo, tidak ada satu perintah pun yang bisa menyiapkan model dan membuktikan performanya di perangkat AMD. Semua model hardcoded di kode, port berbeda-beda, dan tidak ada bukti terukur bahwa inference lokal berjalan.

## Solution

Empati memakai satu server Lemonade di port 13305 sebagai mesin untuk semua modality: transkripsi suara, embedding, chat, dan sintesis suara. Tidak ada jalur kedua ke Ollama.

Setelah skrining selesai, hasil Certainty Factor diubah menjadi konteks terstruktur yang dikirim ke model. Model menarasikan konteks itu, sementara angka risiko tetap dihitung kode deterministik. Setiap fakta klinis di luar hasil skrining harus berasal dari potongan rujukan yang ditemukan lewat embedding, dan potongan itu ditampilkan ke pengguna.

Pengguna bisa merekam keluhan dengan suara, melihat teks hasil transkripsi masuk ke kolom chat, lalu mendengarkan balasan asisten. Kontrolnya tetap mengikuti sistem visual Soft Daylight yang sudah ada, dengan dua tombol kecil di sekitar composer dan bubble balasan.

Developer mendapat dua skrip PowerShell: satu untuk menarik model dan memasang alias, satu untuk menjalankan benchmark dan menyimpulkan angka latensi.

## User Stories

1. Sebagai pengguna baru, saya ingin aplikasi memberi tahu apakah server Lemonade siap dipakai, supaya saya tidak menunggu jawaban yang tidak akan datang.
2. Sebagai pengguna, saya ingin melihat model dan alias mana yang aktif, supaya saya tahu apakah saya memakai model lokal atau jalur cadangan.
3. Sebagai pengguna, saya ingin menekan tombol rekam dan menceritakan keluhan dengan suara, supaya saya tidak perlu mengetik panjang.
4. Sebagai pengguna, saya ingin melihat status rekaman yang jelas (meminta izin, merekam, memproses, selesai), supaya saya tahu kapan harus bicara dan kapan berhenti.
5. Sebagai pengguna, saya ingin hasil transkripsi muncul di kolom chat sebelum dikirim, supaya saya bisa memperbaiki kata yang salah dengar.
6. Sebagai pengguna, saya ingin membatalkan rekaman di tengah jalan, supaya saya tidak terjebak ketika salah mulai.
7. Sebagai pengguna yang menolak izin mikrofon, saya ingin penjelasan singkat dan jalan kembali ke input teks, supaya aplikasi tetap bisa dipakai.
8. Sebagai pengguna, saya ingin balasan asisten bisa diputar sebagai suara, supaya saya bisa mendengarkan sambil melakukan hal lain.
9. Sebagai pengguna, saya ingin tombol putar berubah menjadi berhenti saat audio berjalan, supaya saya mengendalikan pemutaran.
10. Sebagai pengguna, saya ingin audio balasan tersimpan sementara, supaya memutar ulang tidak memicu permintaan baru ke server.
11. Sebagai pengguna, saya ingin jawaban asisten menyebut sumber yang dipakai, supaya saya bisa memeriksa klaimnya.
12. Sebagai pengguna, saya ingin bisa membuka detail sumber dari chip sitasi, supaya saya tahu penerbit dan tahunnya.
13. Sebagai pengguna, saya ingin jawaban asisten menjelaskan hasil skrining saya dalam bahasa manusia, supaya saya paham artinya.
14. Sebagai pengguna, saya ingin asisten menyebut bahwa sebagian pertanyaan belum dijawab bila skrining tidak lengkap, supaya saya tidak menerima kesimpulan yang terlalu percaya diri.
15. Sebagai pengguna, saya ingin asisten menolak menjawab ketika tidak ada rujukan yang relevan, supaya saya tidak menerima karangan.
16. Sebagai pengguna, saya ingin asisten tidak pernah menyebut bobot pakar, nilai CF, atau persentase, supaya saya tidak salah memahami angka yang belum divalidasi.
17. Sebagai pengguna, saya ingin asisten tidak memberi diagnosis atau dosis obat, supaya saya tidak mengambil keputusan medis dari aplikasi.
18. Sebagai pengguna yang menyinggung kekerasan, saya ingin nomor SAPA 129 muncul dengan format persis, supaya saya bisa menghubungi layanan yang benar.
19. Sebagai pengguna, saya ingin jawaban muncul bertahap saat dibuat, supaya saya tahu aplikasi masih bekerja.
20. Sebagai pengguna, saya ingin jawaban tetap muncul utuh ketika streaming gagal, supaya percakapan tidak berhenti di tengah kalimat.
21. Sebagai pengguna tanpa jaringan, saya ingin aplikasi memberi jawaban terbatas yang jujur, supaya saya tetap mendapat arahan awal.
22. Sebagai pengguna, saya ingin riwayat chat tetap menyimpan sumber dan modality input, supaya catatan riset konsisten.
23. Sebagai peneliti, saya ingin setiap catatan konsultasi menyimpan versi basis aturan dan model yang dipakai, supaya data bisa direproduksi.
24. Sebagai pengguna dengan pembaca layar, saya ingin tombol rekam dan putar punya label aksesibel, supaya saya bisa memakainya.
25. Sebagai pengguna yang mengaktifkan kurangi animasi, saya ingin indikator rekam dan putar tidak berdenyut, supaya tampilan tidak mengganggu.
26. Sebagai developer, saya ingin mengganti model hanya dengan mengubah alias di server, supaya saya tidak perlu membangun ulang APK.
27. Sebagai developer, saya ingin satu perintah untuk menarik model dan memasang alias, supaya mesin demo bisa disiapkan dari nol.
28. Sebagai developer, saya ingin satu perintah untuk mengukur latensi chat, embedding, transkripsi, dan TTS, supaya klaim performa punya angka.
29. Sebagai developer, saya ingin tes kontrak HTTP yang gagal ketika port atau nama field berubah, supaya integrasi tidak rusak diam-diam.
30. Sebagai juri demo, saya ingin melihat aplikasi bekerja sepenuhnya luring di perangkat AMD, supaya nilai privasi dan on-device inference terlihat tanpa penjelasan panjang.

## Implementation Decisions

**Satu klien jaringan.** Lapisan jaringan baru bernama `LemonadeUnifiedClient` menggantikan pemakaian langsung `HttpURLConnection` untuk chat. Permukaannya empat fungsi: `transcribeAudio`, `generateEmbeddings`, `streamChatCompletion`, dan `synthesizeSpeech`, ditambah `listModels` untuk pemeriksaan kesiapan. Semua memakai base URL `http://<HOST>:13305/v1`. Port 11434 dan endpoint `/api/chat` dihapus, bukan sekadar dijadikan fallback.

**Alias sebagai kontrak.** Nama model tidak boleh muncul di kode aplikasi. Kontraknya empat alias: `empati-llm`, `empati-embed`, `empati-stt`, `empati-tts`. Model di balik alias dipilih lewat `lemonade alias add` di mesin demo. Ini memisahkan identitas model dari kode dan memungkinkan failover tanpa rilis baru.

**Konteks triage sebagai skema tunggal.** Output Certainty Factor diserialisasi ke satu kontrak `empati.screening_context/v1` yang dipakai bersama oleh chat, catatan riset, dan tampilan debug. Nama enum (`HYPERTENSION`, `GBV`, `CfLevel`, `Hypothesis`, `IntegratedCategory`) dipakai apa adanya. Skema ini berasal dari riset di `docs/research/lemonade-omnimodal-spec.md`, bukan dari prototipe kode.

```json
{
  "schema": "empati.screening_context/v1",
  "kb_version": "cf-kb-v2.0",
  "modules": [
    {
      "module": "HYPERTENSION",
      "level": "SEDANG",
      "cf": 0.62,
      "answered": 16,
      "unanswered": 0,
      "dominant_categories": [
        {"hypothesis": "LIFESTYLE", "cf": 0.71, "evidence": [{"question_id": "M09", "cf_user": 0.8, "cf_expert": 0.6}]}
      ],
      "protective_factors": ["M08"]
    }
  ],
  "integrated": {"category": "HYPERTENSION_LIFESTYLE", "indication": true, "relationship_note": "chronic_stress_pathway"}
}
```

**Kebijakan angka.** Nilai `cf` boleh masuk prompt sebagai kalibrasi internal. Nilai itu tidak boleh muncul di teks yang dibaca pengguna, dan validator keluaran menolak kemunculan "CF", "MB", "MD", "persen", atau angka 0 sampai 100 yang berdiri sendiri. Tes zero-leak yang ada diperbarui untuk memeriksa keluaran akhir, bukan sekadar isi prompt. Aturan ini menggantikan larangan lama yang memblokir angka di prompt.

**Otoritas risiko.** Level, kategori dominan, dan faktor protektif hanya berasal dari mesin Certainty Factor. Model bahasa tidak menghitung ulang, membulatkan, atau memberi label baru. Prompt menyatakan larangan ini secara eksplisit dan validator memeriksa kepatuhannya.

**Korpus dan retrieval.** Korpus rujukan disimpan sebagai chunk dengan id stabil. Setiap chunk punya `id`, `title`, `publisher`, `year`, `section`, `text`, `url`. Embedding dihitung lewat Lemonade, disimpan sebagai blob float32 di SQLite lokal bersama panjang vektor dan hash teks. Pencarian memakai cosine similarity di Kotlin, tanpa pustaka native baru. Nilai awal: `topK` 4 dan ambang 0.35, keduanya konstanta yang bisa diubah dan akan diukur ulang. Chunk tanpa id atau judul ditolak sebelum masuk prompt.

**Kebijakan retrieval kosong.** Bila tidak ada chunk di atas ambang, jawaban harus berupa kalimat batasan yang menyebut bahwa rujukan belum cukup, ditambah tawaran skrining. Model tidak boleh mengisi kekosongan dengan pengetahuan umum.

**Streaming dengan cadangan.** Chat memakai SSE dari `/v1/chat/completions`. Parser mengumpulkan `delta.content`, mengabaikan `reasoning_content`, dan berhenti pada `[DONE]`. Bila koneksi putus setelah teks pertama diterima, UI mempertahankan teks parsial dan menandainya belum lengkap. Bila putus sebelum teks pertama, permintaan diulang sekali tanpa streaming.

**Voice input.** Rekaman memakai PCM16 mono 16 kHz, dibungkus menjadi WAV sebelum dikirim sebagai `multipart/form-data` ke `/v1/audio/transcriptions` dengan `model` alias `empati-stt` dan `language=id`. Batas rekaman 60 detik. Kontrol memakai satu state machine tanpa dependensi Android di dalamnya:

```text
Idle -> RequestingPermission -> Recording -> Transcribing -> Ready -> Idle
  ^            |                    |              |
  |            v                    v              v
  +--------- PermissionDenied    Cancelled       Error
```

**Voice output.** `/v1/audio/speech` dipanggil dengan alias `empati-tts`, `response_format` mp3, dan voice tetap (bukan pilihan pengguna di tahap ini). Hasil disimpan di cache internal dengan nama hash teks ditambah voice, lalu diputar lewat pemutar audio platform. Streaming PCM tidak dipakai karena menambah kerumitan pemutaran.

**Urutan fallback.** Lemonade lokal, lalu Supabase Edge Function bila jaringan tersedia dan pengguna mengizinkan cloud, lalu penyusun offline deterministik. Jalur OpenRouter langsung dan `text.pollinations.ai` dihapus dari klien. Status jalur yang sedang dipakai ditampilkan di layar Profil.

**Konfigurasi host.** Host dan port dibaca dari satu sumber konfigurasi build, dengan nilai bawaan `10.0.2.2:13305` untuk debug emulator dan dapat diubah tanpa mengubah kode sumber. HTTP polos hanya diizinkan untuk host pengembangan lewat konfigurasi keamanan jaringan, tidak untuk domain publik.

**Skrip otomasi.** Dua skrip PowerShell 7. `setup-lemonade.ps1` menarik model yang dibutuhkan, memasang empat alias, dan memverifikasi `/v1/models`. `bench-empati.ps1` menjalankan pengukuran chat, embedding, transkripsi, dan TTS, lalu menulis ringkasan. Keduanya punya `-DryRun` yang hanya mencetak perintah, dan keduanya menolak berjalan bila `lemonade` tidak ada di PATH.

**ADR.** Keputusan pembagian peran antara kalkulasi deterministik dan model bahasa dicatat di `docs/adr/001-lemonade-hybrid-architecture.md`, termasuk alasan mengganti kebijakan zero-leak.

**Branding.** Nama aplikasi, `applicationId`, dan sisa string lama dikerjakan di jalur terpisah dan tidak menjadi bagian spesifikasi ini, kecuali string yang menyebut nama asisten di dalam kontrol suara baru.

## Testing Decisions

Tes yang baik memeriksa perilaku yang bisa diamati dari luar: teks yang dikembalikan, payload yang dikirim ke jaringan, state yang terlihat pengguna, dan file yang dihasilkan. Tes tidak memeriksa nama fungsi privat, jumlah pemanggilan internal, atau urutan inisialisasi.

**Seam 1, `EmpatiChatUseCase`.** Titik perilaku tertinggi. Port `LemonadeGateway`, `KnowledgeRetriever`, dan `TriageContextSource` diganti fake in-memory. Yang diuji: urutan fallback, penolakan saat retrieval kosong, kepatuhan guardrail pada teks akhir, kebijakan angka, pewarisan `unanswered`, dan bentuk payload konteks yang diterima gateway (sehingga serialisasi skema ikut teruji). Prior art: `AsnAndRagArchitectureTest`.

**Seam 2, kontrak wire `LemonadeUnifiedClient`.** Klien asli diuji terhadap server HTTP JDK di localhost. Yang diuji: method dan path, header `Authorization` saat key diset, nama bagian multipart dan `Content-Type` untuk transkripsi, kunci JSON untuk chat, embeddings, dan speech, parsing SSE termasuk `[DONE]`, pemetaan `404 model_not_found` dan `409 slots_pinned_error` ke hasil yang bisa ditangani UI, serta timeout. Tes ini juga menegaskan tidak ada port 11434 di kode.

**Seam 3, `VoiceInputController`.** State machine diuji dengan fake recorder dan fake transcribe serta clock virtual. Yang diuji: transisi normal, pembatalan, timeout 60 detik, izin ditolak, dan kegagalan transkripsi. Dua screenshot test menambahkan kontrol rekam dan putar ke suite Compose yang sudah ada.

**Seam 4, skrip.** Mode `-DryRun` diperiksa oleh skrip verifikasi yang mencocokkan daftar perintah, nama model, keempat alias, port 13305, dan ketiadaan 11434. Bila Pester tersedia dipakai, bila tidak, pemeriksaan dilakukan dengan fungsi PowerShell biasa.

**Yang dipertahankan.** `CfEngineTest` tidak diubah. Tes itu adalah penjaga regresi formula dan urutan kombinasi Certainty Factor. Penambahan tes baru tidak boleh mengunci internal CF.

**Yang tidak diuji otomatis.** Kualitas jawaban model, akurasi transkripsi per kata, hardware mikrofon, dan edge function Supabase. Untuk kualitas jawaban, dipakai daftar periksa manual di runbook demo.

## Out of Scope

- Perubahan formula, ambang, urutan kombinasi, atau nama enum Certainty Factor.
- Perubahan skema Supabase dan isi edge function.
- Rebranding nama aplikasi dan `applicationId`.
- Pemilihan voice oleh pengguna, kloning suara, dan desain suara.
- Input gambar atau dokumen sebagai modality baru.
- Fine tuning model dan evaluasi klinis formal.
- Dukungan iOS atau desktop.
- Klaim angka latensi sebelum benchmark dijalankan di perangkat target.

## Further Notes

Riset pendukung, termasuk payload endpoint dan alasan pembagian peran neuro-symbolic, ada di `docs/research/lemonade-omnimodal-spec.md`.

Nilai jual untuk penilaian: seluruh pipeline berjalan di perangkat dengan satu server lokal, modality masuk dan keluar tidak bergantung cloud, hasil triage bisa diaudit karena deterministik, dan setiap klaim klinis punya sumber yang bisa dibuka. Skrip benchmark memberi bukti terukur alih-alih klaim.

Risiko utama: waktu muat model pertama pada mesin demo. Runbook harus memuat langkah pemanasan model sebelum presentasi, dan UI harus membedakan "model sedang dimuat" dari "server tidak ada".
