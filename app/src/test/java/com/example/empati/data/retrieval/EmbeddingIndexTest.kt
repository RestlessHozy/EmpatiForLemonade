package com.example.empati.data.retrieval

import com.example.empati.data.triage.ReferenceChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingIndexTest {

    private fun chunk(id: String, text: String = "isi $id") = ReferenceChunk(
        id = id,
        title = "Judul $id",
        publisher = "Penerbit $id",
        year = 2024,
        section = "Bagian",
        text = text,
        url = "https://example.org/$id"
    )

    private fun entry(id: String, vector: FloatArray, text: String = "isi $id") =
        IndexedChunk(chunk = chunk(id), vector = vector, textHash = CorpusIndexer.hashOf(text))

    private class RecordingEmbedder(
        private val block: (List<String>) -> List<FloatArray>
    ) : Embedder {
        val seen = mutableListOf<String>()
        override fun embed(texts: List<String>): List<FloatArray> {
            seen += texts.map { it.substringAfter(". ") }
            return block(texts)
        }
    }

    @Test
    fun `search ranks chunks by cosine similarity`() {
        val index = EmbeddingIndex(minScore = 0.0, topK = 3)
        val entries = listOf(
            entry("dekat", floatArrayOf(1f, 0f)),
            entry("miring", floatArrayOf(0.7f, 0.7f)),
            entry("jauh", floatArrayOf(0f, 1f))
        )

        val result = index.search(floatArrayOf(1f, 0f), entries)

        assertEquals(listOf("dekat", "miring", "jauh"), result.map { it.chunk.id })
        assertEquals(1.0, result[0].score, 0.0001)
    }

    @Test
    fun `search drops chunks below the threshold and respects top k`() {
        val index = EmbeddingIndex(minScore = 0.6, topK = 1)
        val entries = listOf(
            entry("a", floatArrayOf(1f, 0f)),
            entry("b", floatArrayOf(0.8f, 0.6f)),
            entry("c", floatArrayOf(0f, 1f))
        )

        val result = index.search(floatArrayOf(1f, 0f), entries)

        assertEquals(1, result.size)
        assertEquals("a", result.single().chunk.id)
    }

    @Test
    fun `search ignores vectors with a different dimension or zero norm`() {
        val index = EmbeddingIndex(minScore = 0.0, topK = 10)
        val entries = listOf(
            entry("salah-dimensi", floatArrayOf(1f, 0f, 0f)),
            entry("nol", floatArrayOf(0f, 0f)),
            entry("benar", floatArrayOf(1f, 0f))
        )

        val result = index.search(floatArrayOf(1f, 0f), entries)

        assertEquals(listOf("benar"), result.map { it.chunk.id })
    }

    @Test
    fun `empty query returns no citations instead of guessing`() {
        val index = EmbeddingIndex(minScore = 0.0, topK = 4)

        val result = index.search(floatArrayOf(), listOf(entry("a", floatArrayOf(1f, 0f))))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `indexer only embeds new or changed text and drops removed chunks`() {
        val embedder = RecordingEmbedder { texts -> texts.map { floatArrayOf(it.length.toFloat(), 1f) } }
        val indexer = CorpusIndexer(embedder)
        val corpus = listOf(
            chunk("tetap", "teks lama"),
            chunk("berubah", "teks sebelum"),
            chunk("baru", "teks baru")
        )
        val existing = listOf(
            IndexedChunk(chunk("tetap", "teks lama"), floatArrayOf(5f, 1f), CorpusIndexer.hashOf("teks lama")),
            IndexedChunk(chunk("berubah", "teks sebelum"), floatArrayOf(1f, 1f), CorpusIndexer.hashOf("teks lama")),
            IndexedChunk(chunk("hilang", "teks hilang"), floatArrayOf(2f, 1f), CorpusIndexer.hashOf("teks hilang"))
        )

        val result = indexer.index(corpus, existing)

        assertEquals(listOf("teks baru", "teks sebelum"), embedder.seen.sorted())
        assertEquals(setOf("tetap", "berubah", "baru"), result.map { it.chunk.id }.toSet())
        assertEquals(3, result.size)
    }

    @Test
    fun `citations only include chunks the answer actually used`() {
        val who = ReferenceChunk(
            id = "who-hypertension-2023#definition",
            title = "Hypertension",
            publisher = "World Health Organization",
            year = 2023,
            section = "Key facts",
            text = "Hipertensi ditandai tekanan darah 140 per 90 atau lebih.",
            url = "https://www.who.int/news-room/fact-sheets/detail/hypertension"
        )
        val kemkes = ReferenceChunk(
            id = "kemkes-htn-2023#screening",
            title = "Cek tekanan darah",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Pencegahan",
            text = "Periksa tekanan darah secara rutin.",
            url = "https://www.kemkes.go.id/"
        )
        val answer = "Panduan World Health Organization menyebut hipertensi ditandai tekanan darah 140 per 90 atau lebih."

        val citations = CitationPicker.usedIn(answer, listOf(who, kemkes))

        assertEquals(listOf("who-hypertension-2023#definition"), citations.map { it.id })
    }

    @Test
    fun `citations stay empty when the answer names no source`() {
        val chunks = listOf(chunk("a", "Garam berlebih menaikkan tekanan darah."))

        val citations = CitationPicker.usedIn("Kurangi garam secara bertahap.", chunks)

        assertTrue(citations.isEmpty())
    }
}
