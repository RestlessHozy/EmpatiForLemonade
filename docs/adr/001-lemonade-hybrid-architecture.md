# ADR 001: Mesin triage deterministik, model bahasa untuk narasi

Status: diterima
Tanggal: 11 November 2026
Konteks: integrasi AMD Lemonade untuk Empati

## Masalah

Empati punya dua jenis output yang bercampur di satu layar. Mesin Certainty Factor menghasilkan level risiko dari 26 pertanyaan dengan formula yang tetap, sementara model bahasa menghasilkan kalimat penjelasan. Keduanya sama-sama terlihat seperti "penilaian" bagi pengguna.

Sebelum ini, batas antara keduanya dijaga dengan aturan sederhana: jangan pernah mengirim angka CF ke model, dan jangan biarkan model menyebut CF, MB, MD, atau persen. Aturan itu mudah diuji, tetapi membuat model kekurangan konteks. Model tidak tahu seberapa kuat indikasinya, jadi nada jawaban sering terlalu ringan untuk hasil yang tinggi, atau terlalu khawatir untuk hasil yang rendah.

Rencana integrasi RAG memperburuk ketegangan ini. Retrieval butuh query yang dibentuk dari hasil skrining, dan query yang baik butuh angka, bukan hanya label.

## Keputusan

Mesin Certainty Factor adalah satu-satunya sumber angka risiko. Model bahasa menerima hasil skrining sebagai konteks terstruktur, termasuk nilai CF, tetapi dilarang menyebut, menghitung ulang, atau menerjemahkan nilai itu menjadi label baru. Level dan kategori yang dibaca pengguna datang dari kode deterministik.

Rinciannya:

1. Konteks dikirim dalam satu skema bernama `empati.screening_context/v1`. Nama enum dipakai apa adanya supaya sama dengan payload riset.
2. Nilai `cf` dipakai model untuk mengatur nada dan memilih penekanan. Nilai itu tidak boleh muncul di teks akhir.
3. Validator keluaran memeriksa hasil generasi, bukan hanya prompt. Larangan yang ditegakkan: akronim CF, MB, MD, kata "persen" atau simbol persen, angka skor mandiri, dan klaim diagnosis.
4. Klaim klinis di luar hasil skrining hanya sah bila ada potongan rujukan dengan id dan judul. Bila retrieval kosong, jawaban berubah menjadi kalimat batasan.
5. Tes zero-leak lama diganti, bukan dihapus. Tes baru memeriksa teks yang sampai ke pengguna.

## Konsekuensi

Yang menguntungkan: hasil triage bisa diaudit karena angkanya tidak pernah melewati model. Pengguna tidak melihat angka yang belum divalidasi pakar. Model mendapat kalibrasi yang cukup untuk memilih nada. Setiap klaim yang bisa salah punya sumber yang bisa dibuka.

Yang harus diterima: dua lapisan validasi, satu di prompt dan satu di pasca-proses. Ini menambah kode yang harus dirawat dan satu titik kegagalan baru bila validator terlalu agresif memotong kalimat. Pengujian perilaku jadi lebih penting daripada sebelumnya, dan itulah alasan satu seam orkestrasi dipilih.

Biaya lain: model bisa menyebut angka dari potongan rujukan, misalnya ambang tekanan darah, dan validator harus membedakan angka rujukan dari angka skor. Aturan yang dipakai: angka yang muncul di dalam teks chunk yang dikutip diizinkan, angka yang tidak berasal dari chunk ditolak.

## Alternatif yang ditolak

**Tetap tanpa angka sama sekali.** Paling aman dan paling murah dirawat, tetapi model kehilangan kalibrasi. Jarak antara CF 0,30 dan CF 0,70 menjadi tidak terlihat, padahal keduanya butuh nada berbeda.

**Menampilkan CF ke pengguna.** Membalik keputusan produk sebelumnya dan menampilkan angka yang belum divalidasi pakar. Berisiko secara klinis dan tidak menambah kepercayaan.

**Menyerahkan penilaian ke model dengan ringkasan gejala.** Lebih sederhana di kode, tetapi menghilangkan sifat deterministik yang menjadi alasan aplikasi ini ada. Dua permintaan dengan jawaban identik bisa menghasilkan level berbeda.

## Rujukan

Riset endpoint dan pola integrasi: `docs/research/lemonade-omnimodal-spec.md`.
