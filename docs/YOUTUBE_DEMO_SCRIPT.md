# Naskah video demo Empati

Durasi target 3 menit. Format 1080p, 30 atau 60 fps, rekaman layar perangkat Android ditambah satu potongan terminal untuk benchmark. Narasi bahasa Indonesia, subtitle Indonesia dan Inggris.

## Sebelum merekam

- Jalankan `scripts/setup-empati-lemonade.ps1` sampai selesai dan pastikan kelima alias muncul di `/v1/models`.
- Buka aplikasi sekali dan kirim satu pesan, supaya model chat sudah termuat. Muat model pertama bisa memakan puluhan detik dan merusak tempo video.
- Siapkan akun demo dengan satu hasil skrining. Jangan tampilkan nama, NIP, atau riwayat nyata.
- Untuk membuktikan klaim luring, aktifkan mode pesawat dan sambungkan perangkat fisik lewat `adb reverse tcp:13305 tcp:13305`. Emulator memakai `10.0.2.2` dan tetap bisa mencapai host tanpa internet.
- Isi angka benchmark hanya dari `docs/BENCHMARK_RESULTS.md` yang dijalankan di mesin yang direkam. Bila belum ada, jangan sebut angka.
- Rekam tanpa keyboard. Seluruh interaksi memakai tombol di layar dan suara.

## 0:00 sampai 0:20, pembuka

Layar: Beranda Empati, lalu potongan diagram arsitektur selama dua detik.

Narasi:

> Aplikasi ini menilai risiko hipertensi dan kesejahteraan psikososial, lalu mengajak pengguna berbicara tentang hasilnya. Seluruh model berjalan di satu server Lemonade pada perangkat yang sama, di port 13305.

## 0:20 sampai 0:50, pembagian kerja

Layar: diagram arsitektur, sorot kotak mesin Certainty Factor, lalu kotak Lemonade.

Narasi:

> Ada dua lapisan yang tidak boleh tercampur. Angka risiko dihitung mesin Certainty Factor yang deterministik. Model bahasa hanya menarasikan hasil itu dan fakta dari potongan rujukan yang ditemukan lewat embedding. Kalau rujukan tidak cukup, jawaban harus mengatakan itu.

> Satu server menangani empat modality: chat, embedding, transkripsi, dan suara. Aplikasi hanya tahu empat alias, jadi model bisa diganti tanpa membangun ulang APK.

## 0:50 sampai 1:50, demo suara tanpa keyboard

Layar: tab Konsultasi. Jangan ketuk kolom teks.

Narasi dan aksi:

> Saya mulai dari keluhan suara. (ketuk tombol mikrofon)

> Aplikasi meminta izin mikrofon, lalu merekam dalam 16 kHz mono. Batasnya 60 detik dan ada tombol batal selama merekam.

> (bicara) Saya sering pusing menjelang sore, tidur saya kurang, dan saya jarang olahraga. Tekanan darah terakhir 150 per 95.

> (ketuk Selesai, tunggu transkripsi)

> Hasil transkripsi masuk ke kolom chat, bukan langsung terkirim. Saya bisa memperbaiki kata yang salah dengar sebelum mengirim.

> (ketuk Kirim, biarkan jawaban mengalir)

> Jawaban muncul bertahap. Angka risiko tidak dihitung ulang di sini. Yang ditambahkan model adalah penjelasan dan langkah kecil hari ini.

> (setelah selesai) Chip di bawah jawaban menunjukkan rujukan yang dipakai. Saya buka salah satunya. (ketuk chip)

> Isinya penerbit, tahun, bagian, dan teks potongan. Pengguna bisa memeriksa klaim tanpa keluar aplikasi.

> (tutup sheet, ketuk tombol putar)

> Balasan bisa didengarkan. Audio disimpan di cache, jadi memutar ulang pesan yang sama tidak memanggil server lagi. (ketuk berhenti)

## 1:50 sampai 2:20, bukti deterministik dan status

Layar: kembali ke hasil skrining, lalu Profil. Sorot panel status Lemonade.

Narasi:

> Hasil skrining tetap tersimpan sebagai angka dari mesin pakar. Saya bisa meminta ringkasannya tanpa model.

> (ketuk pintasan Lihat hasil skrining saya)

> Balasan ini disusun dari data skrining, bukan dari model, jadi angkanya konsisten.

> Di Profil ada status server dan alias yang aktif. Kalau server mati, model sedang dimuat, atau model sibuk, aplikasi memberi pesan yang berbeda. Status jalur cadangan juga muncul di layar chat.

## 2:20 sampai 2:45, bukti performa dan dampak komunitas

Layar: potongan terminal yang menjalankan `scripts/bench-empati.ps1`. Tampilkan tabel hasil, lalu potongan `README` bagian instalasi.

Narasi:

> Pengukuran memakai skrip yang sama yang bisa dijalankan juri. (sebut angka hanya bila sudah diukur, misalnya) Di mesin ini, chat 64 token selesai dalam X detik dan transkripsi rekaman 10 detik dalam Y detik.

> Seluruh kode berlisensi MIT. Instalasi untuk klinik, kampus, atau layanan konseling cukup menjalankan satu skrip penyiapan, lalu memasang APK. Jalur utamanya berjalan luring. Dua jalur keluar data, yaitu fallback cloud dan sinkronisasi riset, didokumentasikan di README supaya operator bisa mematikannya.

## 2:45 sampai 3:00, penutup

Layar: kembali ke percakapan, kursor di tombol mikrofon.

Narasi:

> Skrining yang bisa diaudit dan jawaban yang punya sumber, semuanya berjalan di perangkat. Repositori, skrip benchmark, dan catatan arsitektur ada di deskripsi video.

## Catatan produksi

- Jangan menyebut angka latensi sebelum menjalankan benchmark di mesin yang direkam.
- Jangan menampilkan data peserta atau riwayat nyata. Pakai akun demo.
- Potong jeda saat model memuat. Jangan mempercepat rekaman transkripsi atau jawaban sehingga durasinya menipu.
- Tampilkan mode pesawat bila klaim luring ingin dibuktikan. Bila cloud fallback aktif, jangan menyebut demo sepenuhnya luring.
- Simpan rekaman mentah dan berkas hasil benchmark agar klaim bisa diperiksa.
