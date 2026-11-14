# Evaluasi PageIndex sebagai lapisan RAG Empati

Tanggal: 12 November 2026
Status: rekomendasi, belum diimplementasikan
Pertanyaan: apakah [VectifyAI/PageIndex](https://github.com/VectifyAI/PageIndex) bisa dipakai sebagai bagian RAG tanpa mengorbankan kelayakan di AMD Lemonade Developer Challenge?

## Fakta yang sudah diperiksa

Diambil langsung dari repositori PageIndex pada 12 November 2026.

| Hal | Temuan |
|---|---|
| Lisensi | MIT, jadi kompatibel dengan lisensi MIT proyek ini |
| Bahasa dan distribusi | Python (`pip install pageindex`). Tidak ada pustaka Kotlin atau Android |
| Cara kerja | Membangun indeks pohon (daftar isi hierarkis dengan ringkasan per node) dari dokumen panjang, lalu LLM menelusuri pohon itu untuk memilih bagian yang relevan. Tanpa vektor dan tanpa chunking |
| Ketergantungan | `litellm`, `openai`, `openai-agents`, `mcp`, `PyPDF2`, `pypdfium2` |
| Kebutuhan model | Mode lokal SDK tetap memanggil LLM lewat API key penyedia. README menyarankan model terbaik yang terjangkau untuk tahap penelusuran (`chat=`), dan model dasar cukup untuk tahap indeks (`index=`) |
| Jalur cloud | PageIndex Cloud dan Page Search API tersedia, keduanya mengirim dokumen dan pertanyaan keluar perangkat |

## Yang belum bisa dipastikan

Aturan resmi kompetisi tidak berhasil saya temukan. Pencarian hanya mengembalikan ringkasan umum dari situs pihak ketiga, bukan teks aturan. Saya tidak mau menyimpulkan "diterima" atau "ditolak" dari sumber seperti itu. Sebelum memutuskan, baca halaman resmi tantangan dan cek tiga hal: apakah ada syarat inferensi harus lewat Lemonade, apakah ada larangan ketergantungan cloud, dan apakah ada syarat lisensi terbuka.

Yang bisa dipegang dari dalam repo: seluruh jalur inferensi aplikasi sekarang lewat `:13305` (chat, embedding, STT, TTS). Selama tambahan PageIndex tidak mengubah itu, risiko terhadap syarat "berbasis Lemonade" kecil.

## Kecocokan teknis dengan Empati

1. **Tidak bisa berjalan di Android.** Aplikasi harus melakukan retrieval di perangkat. PageIndex hanya Python, jadi yang bisa dipakai paling jauh adalah tahap pembuatan indeks di laptop.
2. **Korpus saat ini terlalu kecil untuk manfaatnya.** `EmpatiCorpus` berisi potongan pendek yang sudah kurasi (sekitar belasan chunk). PageIndex dirancang untuk dokumen panjang seperti laporan keuangan atau pedoman ratusan halaman. Pada korpus sekecil ini, pohon tidak menambah apa pun dibanding cosine similarity yang sudah ada.
3. **Biaya latensi di perangkat.** Penelusuran pohon berarti minimal satu panggilan LLM tambahan sebelum jawaban dimulai. Dengan `Qwen3-4B-GGUF` di mesin demo, itu menambah detik yang terasa di chat suara.
4. **Kualitas penalaran model kecil belum terbukti.** Benchmark yang dipublikasikan PageIndex memakai model besar. Belum ada bukti penelusuran pohon stabil pada 4B atau 1.7B parameter.
5. **Bertabrakan dengan ADR-001 bila tidak hati-hati.** Retrieval sekarang deterministik dan terukur (ambang cosine 0,35). Retrieval berbasis penalaran LLM tidak deterministik, sehingga dua pertanyaan yang sama bisa menarik bagian berbeda. Itu melemahkan argumen auditabilitas yang menjadi dasar arsitektur.

## Opsi

**A. Tetap dengan embedding `empati-embed` (default saat ini).** Tanpa risiko baru. Retrieval terukur dan jalur penuh ada di Lemonade.

**B. PageIndex hanya sebagai alat build-time.** Di laptop, jalankan PageIndex terhadap PDF panjang (misalnya pedoman WHO atau Kemenkes) dengan LiteLLM diarahkan ke `http://127.0.0.1:13305/v1` dan model `empati-llm`, lalu simpan pohon hasilnya sebagai JSON di aset aplikasi. Aplikasi memakai pohon itu sebagai daftar isi untuk mempersempit dokumen, lalu embedding memilih chunk di dalam bagian terpilih. Semua inferensi tetap lewat Lemonade. Catatan: pengarahan LiteLLM ke endpoint OpenAI-compatible Lemonade adalah praktik yang umum, tetapi belum saya uji untuk PageIndex, dan tahap penelusuran agentik mungkin butuh function calling yang belum diverifikasi pada `Qwen3-4B-GGUF`.

**C. PageIndex Cloud atau Page Search API.** Tidak disarankan. Dokumen dan pertanyaan pengguna keluar dari perangkat, bertentangan dengan posisi lokal-pertama Empati dan berisiko terhadap penilaian kompetisi.

## Rekomendasi

Pertahankan opsi A untuk submisi. Pindah ke opsi B hanya bila korpus berkembang menjadi dokumen panjang, dan lakukan sebagai eksperimen terukur:

1. Siapkan 20 sampai 30 pertanyaan uji dengan chunk jawaban yang diketahui.
2. Bandingkan hit-rate top-4 embedding saja dengan pohon ditambah embedding, di mesin yang sama.
3. Catat tambahan latensi per pertanyaan di `docs/BENCHMARK_EVALUATION.md`.
4. Adopsi hanya bila hit-rate naik jelas dan latensi tambahan masih nyaman untuk chat suara.

Bila diadopsi, tambahkan atribusi MIT PageIndex di README dan jangan masukkan paket Python ke APK.
