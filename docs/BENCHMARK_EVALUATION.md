# Evaluasi performa Empati di Lemonade

Dokumen ini menjelaskan cara mengukur performa pipeline Empati, hasil pengukuran terukur yang sudah dijalankan pada laptop AMD pengembang (`AMD Ryzen 7 7735HS`), dan analisis batas arsitektur pada perangkat AMD dengan memori terpadu (`Ryzen AI Max+ 395 / Strix Halo`).

## Apa yang diukur

`scripts/bench-empati.ps1` mengukur waktu total satu permintaan untuk empat endpoint:

| Nama di laporan | Endpoint | Isi permintaan |
|---|---|---|
| chat | `POST /v1/chat/completions` | Prompt pendek, `max_tokens=64`, non-streaming |
| embeddings | `POST /v1/embeddings` | Satu kalimat bahasa Indonesia |
| transcription | `POST /v1/audio/transcriptions` | WAV hening 1 detik, 16 kHz mono, `language=id` |
| speech | `POST /v1/audio/speech` | Satu kalimat hasil skrining, mp3 |

Skrip melakukan satu pemanasan chat, lalu `-Repeats` percobaan per endpoint dan melaporkan median. Hasil ditulis ke `docs/BENCHMARK_RESULTS.md`.

Yang belum diukur oleh skrip:

- Time to first token untuk chat streaming. Skrip memakai permintaan non-streaming, jadi angkanya waktu total, bukan TTFT.
- Waktu muat model pertama setelah server dimulai.
- Latensi di bawah permintaan bersamaan.
- Pemakaian VRAM, RAM, daya, dan suhu. Alat ini perlu diambil dari sisi server, misalnya `rocm-smi`, Task Manager, atau `lemonade status` bila versi yang dipakai menyediakannya.

Untuk mengukur TTFT, kirim `stream=true`, catat waktu saat byte pertama dengan `data:` berisi `delta.content` tiba, lalu kurangi waktu permintaan dikirim. Tambahkan pengukuran itu ke skrip sebelum mengklaim angka TTFT.

## Metodologi

1. Jalankan `scripts/setup-empati-lemonade.ps1` di mesin target dan pastikan kelima alias terdaftar.
2. Biarkan server menyelesaikan muat model pertama sebelum pengukuran. Pemanasan otomatis di skrip hanya mengirim satu permintaan chat.
3. Jalankan `pwsh ./scripts/bench-empati.ps1 -BaseUrl http://localhost:13305 -Repeats 5 -ReportPath docs/BENCHMARK_RESULTS.md`.
4. Catat perangkat, versi Lemonade, versi driver, dan model yang aktif di berkas hasil. Tanpa itu, angka tidak bisa dibandingkan.
5. Ulangi pada kondisi dingin dan panas bila ingin melihat dampak muat model.

Median dipakai karena satu permintaan pertama bisa memuat model dan tidak mewakili kondisi stabil. Untuk klaim latensi pengguna, laporkan median dan persentil ke-95, bukan rata-rata saja.

## Tabel hasil terukur (`scripts/bench-empati.ps1 -Repeats 5`)

Diukur langsung pada **2026-11-14 23:03:04 +08:00** di laptop pengembang (`AMD Ryzen 7 7735HS`, 8C/16T, `AMD Radeon(TM) Graphics` driver `32.0.21045.16010`, `15.25 GB` RAM terpadu, Lemonade Server `v2026.46.2`, `http://127.0.0.1:13305`, semua model dimuat bersama):

| Metrik | Endpoint / Alias | Perangkat | Kondisi | Hasil Median (Berhasil) |
|---|---|---|---|---|
| Waktu total chat 64 token | `chat` (`empati-llm` / `Qwen3-4B-GGUF`) | Ryzen 7 7735HS, Radeon Graphics, 15.25 GB | Hangat, seluruh 4 model residen bersama | **33704.2 ms** (`5/5`) |
| Waktu total embedding | `embeddings` (`empati-embed` / `nomic-embed-text-v1-GGUF`) | Ryzen 7 7735HS, Radeon Graphics, 15.25 GB | Hangat | **44.1 ms** (`5/5`) |
| Waktu transkripsi 1 detik audio | `transcription` (`empati-stt` / `Whisper-Large-v3-Turbo`) | Ryzen 7 7735HS, Radeon Graphics, 15.25 GB | Hangat | **10575.1 ms** (`5/5`) |
| Waktu sintesis satu kalimat | `speech` (`empati-tts` / `kokoro-v1`) | Ryzen 7 7735HS, Radeon Graphics, 15.25 GB | Hangat, CPU ONNX | **1436.3 ms** (`5/5`) |
| TTFT chat streaming | `chat` (`stream=true`) | Ryzen 7 7735HS | — | Belum diukur skrip |

Catatan pengamatan lapangan: ketika keempat model (`Qwen3-4B-GGUF` Q4_0, `nomic-embed-text-v1-GGUF`, `Whisper-Large-v3-Turbo`, dan `kokoro-v1`) ditahan bersamaan di mesin 15.25 GB RAM, sisa RAM fisik menyentuh ~0.41 GB sehingga Windows melakukan paging agresif dan memperlambat decode chat/STT (dibanding target < 8 detik pada konfigurasi 32 GB+). Embedding (`44.1 ms`) dan Kokoro TTS (`1.44 s`) tetap sangat responsif.

Jangan menyalin angka dari mesin lain ke tabel ini tanpa mencatat spesifikasi lengkap. Model, kuantisasi, kapasitas RAM bebas, panjang prompt, dan driver mengubah hasil.

## Analisis arsitektur pada Ryzen AI Max+ 395

Angka berikut adalah spesifikasi publik kelas perangkat, bukan hasil ukur repositori ini. Verifikasi ulang pada unit yang dipakai.

- 16 inti Zen 5, 32 thread.
- GPU Radeon 8060S dengan 40 compute unit RDNA 3.5.
- NPU XDNA 2 dengan 50 TOPS INT8.
- Memori LPDDR5X-8000 terpadu, lebar bus 256 bit, sampai 128 GB.
- Bandwidth puncak hitungan dari lebar bus: 8000 MT/s x 256 bit / 8 = 256 GB/s. Angka nyata lebih rendah.

Memori terpadu menghapus salinan bobot antara RAM dan VRAM. CPU, GPU, dan NPU membaca pool yang sama, jadi model chat, Whisper, Kokoro, dan indeks embedding bisa tinggal di memori tanpa migrasi. Ini yang membuat satu mesin kecil bisa menjalankan seluruh pipeline.

Konsekuensinya, semua komponen bersaing pada bandwidth yang sama. Decode token dan decode audio sama-sama membaca bobot dari memori. Saat chat dan transkripsi berjalan bersamaan, throughput per proses turun meskipun total pemakaian memori masih longgar.

### Perkiraan batas decode

Decode menghasilkan satu token per pembacaan bobot, ditambah KV cache. Batas atas kasar:

```text
tokens/detik <= bandwidth / (ukuran bobot + KV yang dibaca per token)
```

Untuk model 4B pada kuantisasi Q4 dengan berkas sekitar 2,5 GB dan KV cache 2048 konteks, batas teoretis berada di sekitar 90 sampai 100 token per detik pada 256 GB/s. Implementasi nyata biasanya mencapai 30 sampai 60 persen dari batas itu. Angka ini perkiraan, bukan janji. Hasil sebenarnya bergantung pada kernel, backend GPU atau NPU, dan panjang prompt.

Prefill prompt lebih berat di compute daripada decode. Prompt Empati berisi konteks skrining dan beberapa potongan rujukan, jadi TTFT akan lebih ditentukan oleh kecepatan prefill dan panjang konteks daripada bandwidth memori saja.

### Anggaran memori

| Komponen | Perkiraan | Catatan |
|---|---|---|
| LLM chat 4B Q4 | sekitar 2,5 GB | Berkas model, tanpa KV cache |
| KV cache 2048 konteks | ratusan MB | Tergantung jumlah layer dan presisi |
| Whisper Large v3 Turbo | 1,62 GB | Ukuran yang tercatat di catatan riset Lemonade |
| Embedding nomic | 0,078 GB | Ukuran registry |
| Kokoro | di bawah 1 GB | Tergantung paket voice |
| Aplikasi Android dan sistem | 2 sampai 4 GB | Tergantung perangkat |

Total pipeline masih di bawah 8 GB. Mesin 32 GB cukup untuk demo dan pemakaian satu pengguna. 64 GB atau 128 GB memberi ruang untuk model chat lebih besar, konteks lebih panjang, atau beberapa permintaan bersamaan, dengan catatan bandwidth tetap menjadi batas utama.

### Skalabilitas

Untuk satu pengguna, antrean permintaan terjadi karena aplikasi mengirim satu permintaan chat pada satu waktu dan streaming dihentikan saat pengguna menutup layar. Lonjakan muncul saat transkripsi dan chat berjalan berdekatan, misalnya pengguna merekam, mengirim, lalu memutar balasan.

Untuk beberapa pengguna di klinik atau kampus, ukur dulu:

- Throughput chat saat dua sampai empat permintaan bersamaan.
- Waktu transkripsi saat chat sedang decode.
- Pemakaian memori puncak saat semua model termuat.
- Apakah Lemonade menolak permintaan dengan `409 slots_pinned_error` dan seberapa sering.

Bila batas itu tercapai, urutan penanganannya: batasi jumlah pengguna per server, pisahkan server STT dan TTS dari server chat, lalu tambah replika. Jangan menaikkan konteks atau ukuran model sebelum bottleneck bandwidth terukur.

## Target internal

Angka berikut adalah target rekayasa, bukan hasil pengukuran. Pakai untuk menilai apakah hasil benchmark layak demo.

| Skenario | Target |
|---|---|
| Chat 64 token, kondisi hangat | di bawah 8 detik |
| Transkripsi rekaman 10 detik | di bawah 4 detik |
| Sintesis satu kalimat | di bawah 3 detik |
| Embedding satu kueri | di bawah 300 ms |

Bila hasil jauh di atas target, periksa dulu apakah model sedang dimuat, apakah prompt memakai konteks berlebih, dan apakah backend berjalan di GPU atau CPU.

## Reproduksi

```powershell
pwsh ./scripts/setup-empati-lemonade.ps1
pwsh ./scripts/bench-empati.ps1 -BaseUrl http://localhost:13305 -Repeats 5
pwsh ./scripts/tests/verify-dryrun.ps1
```

Hasil benchmark tidak disimpan di repositori. Berkas `docs/BENCHMARK_RESULTS.md` masuk daftar abaikan supaya angka dari mesin pengembang tidak tercampur dengan angka mesin demo.
