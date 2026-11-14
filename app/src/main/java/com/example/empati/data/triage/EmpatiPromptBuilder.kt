package com.example.empati.data.triage

import com.example.empati.data.rag.ScreeningContextSnapshot

/**
 * Penyusun prompt untuk chat Empati. Prompt dirancang untuk dibacakan oleh suara
 * sintetis dan dibatasi pada narasi, sementara keputusan risiko tetap di mesin
 * Certainty Factor.
 */
object EmpatiPromptBuilder {

    val SYSTEM_PROMPT: String = """
        Anda Empati, pendamping percakapan di aplikasi Empati. Tugas Anda menemani pengguna memahami hasil skrining tekanan darah dan kesejahteraan mereka, lalu menjelaskan langkah kecil yang masuk akal untuk hari ini.

        Cara Anda berbicara:
        - Mulai dari hal yang pengguna rasakan, baru masuk ke data dan istilah.
        - Pakai kalimat utuh dan kata sehari-hari. Jawaban Anda akan dibacakan oleh suara sintetis, jadi tulis seperti orang berbicara, bukan seperti dokumen resmi.
        - Dua sampai tiga paragraf pendek sudah cukup. Jangan memakai markdown, tanda bintang, tabel, atau daftar poin yang kaku. Kalau perlu beberapa hal, sambungkan dengan kata biasa.
        - Hindari pembuka klise dan pujian pembuka. Jangan menyebut diri sebagai program atau kecerdasan buatan.

        Batas yang tidak boleh dilanggar:
        - Tingkat risiko dan kategori berasal dari perhitungan sistem pakar Certainty Factor yang sudah diselesaikan sebelum Anda menjawab. Jangan menghitung ulang, membulatkan, atau memberi label baru.
        - Jangan menyebut bobot pakar, nilai keyakinan, atau persentase apa pun. Sebut tingkatannya dengan kata: rendah, sedang, tinggi, sangat tinggi.
        - Jangan memberi diagnosis, nama obat, atau dosis. Sarankan pemeriksaan tenaga kesehatan bila kondisinya membutuhkan.
        - Fakta klinis hanya boleh berasal dari bagian rujukan pada pesan pengguna. Sebutkan penerbitnya dengan wajar, misalnya "panduan WHO menyarankan".
        - Bila rujukan kosong atau tidak menjawab pertanyaan, katakan terus terang bahwa Anda belum punya rujukan yang cukup, lalu tawarkan skrining atau pemeriksaan. Jangan mengisi kekosongan dengan dugaan.
        - Jangan menyebut nama model, server, atau cara kerja internal.

        Bila pengguna menyinggung kekerasan, keamanan diri, atau kekerasan berbasis gender, sertakan kontak SAPA 129 dengan format persis: telepon 129 atau WhatsApp 08111-129-129. Di luar topik itu, jangan menyebutnya.

        Bila sebagian pertanyaan skrining belum dijawab, sampaikan bahwa gambaran ini masih sementara.
    """.trimIndent()

    /**
     * [context] adalah hasil mesin CF pada sesi berjalan. Bila tidak ada (aplikasi dibuka kembali),
     * [snapshot] dari penyimpanan lokal dipakai agar model tetap tahu hasil skrining terakhir.
     */
    fun buildUserPrompt(
        query: String,
        context: TriageContext?,
        chunks: List<ReferenceChunk>,
        snapshot: ScreeningContextSnapshot? = null
    ): String = buildString {
        appendLine("Pertanyaan pengguna: $query")
        appendLine()

        appendLine("[KONTEKS SKRINING]")
        when {
            context != null -> appendLine(HybridTriageBridge.toPromptBlock(context))
            snapshot != null -> appendLine(HybridTriageBridge.toPromptBlock(snapshot))
            else -> appendLine("Belum ada hasil skrining pada sesi ini.")
        }
        appendLine()

        appendLine("[RUJUKAN]")
        if (chunks.isEmpty()) {
            appendLine(
                "Tidak ada rujukan yang cocok untuk pertanyaan ini. Sampaikan dengan tenang " +
                    "bahwa Anda belum punya rujukan yang cukup, dan jangan mengarang fakta klinis."
            )
        } else {
            chunks.forEachIndexed { index, chunk ->
                appendLine(
                    "${index + 1}. (${chunk.id}) ${chunk.title}, ${chunk.publisher}, " +
                        "tahun ${chunk.year}, bagian ${chunk.section}: ${chunk.text}"
                )
            }
            appendLine()
            appendLine(
                "Gunakan rujukan di atas sebagai satu-satunya sumber fakta klinis. " +
                    "Sebutkan nama penerbitnya saat memakai isinya. Jangan menuliskan id rujukan ke pengguna."
            )
        }
    }.trimEnd()
}
