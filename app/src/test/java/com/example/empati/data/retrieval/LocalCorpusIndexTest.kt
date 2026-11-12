package com.example.empati.data.retrieval

import com.example.empati.data.triage.ReferenceChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCorpusIndexTest {

    private class MemoryStorage(var value: String? = null) : IndexStorage {
        override fun read(): String? = value
        override fun write(value: String) {
            this.value = value
        }
    }

    private fun chunk(id: String, text: String = "teks $id") = ReferenceChunk(
        id = id,
        title = "Judul $id",
        publisher = "Penerbit $id",
        year = 2024,
        section = "Bagian",
        text = text,
        url = "https://example.org/$id"
    )

    private class CountingEmbedder : Embedder {
        var calls = 0
        override fun embed(texts: List<String>): List<FloatArray> {
            calls++
            return texts.map { text ->
                val seed = text.length.toFloat()
                floatArrayOf(seed, 1f, 0f)
            }
        }
    }

    @Test
    fun `codec round trips chunks vectors and hashes`() {
        val entries = listOf(
            IndexedChunk(chunk("a"), floatArrayOf(0.5f, -0.25f, 1f), "abcd1234"),
            IndexedChunk(chunk("b"), floatArrayOf(0f, 0f, 1f), "deadbeef")
        )

        val decoded = EmbeddingStoreCodec.decode(EmbeddingStoreCodec.encode(entries))

        assertEquals(2, decoded.size)
        assertEquals("a", decoded[0].chunk.id)
        assertEquals("Penerbit a", decoded[0].chunk.publisher)
        assertEquals(0.5f, decoded[0].vector[0], 0.0001f)
        assertEquals(-0.25f, decoded[0].vector[1], 0.0001f)
        assertEquals("deadbeef", decoded[1].textHash)
    }

    @Test
    fun `codec tolerates empty and corrupt payloads`() {
        assertTrue(EmbeddingStoreCodec.decode(null).isEmpty())
        assertTrue(EmbeddingStoreCodec.decode("").isEmpty())
        assertTrue(EmbeddingStoreCodec.decode("bukan json").isEmpty())
        assertTrue(EmbeddingStoreCodec.decode("""{"entries":[{"id":"x"}]}""").isEmpty())
    }

    @Test
    fun `index persists vectors and does not call the embedder twice`() {
        val storage = MemoryStorage()
        val embedder = CountingEmbedder()
        val corpus = listOf(chunk("a"), chunk("b"))

        val first = LocalCorpusIndex(embedder, storage).ensureIndexed(corpus)
        val second = LocalCorpusIndex(embedder, storage).ensureIndexed(corpus)

        assertEquals(2, first.size)
        assertEquals(2, second.size)
        assertEquals(1, embedder.calls)
        assertTrue(storage.value!!.contains("\"a\""))
    }

    @Test
    fun `search works after reloading the stored index`() {
        val storage = MemoryStorage()
        val corpus = listOf(chunk("a", "garam dan tekanan darah"))
        LocalCorpusIndex(CountingEmbedder(), storage).ensureIndexed(corpus)

        val results = LocalCorpusIndex(CountingEmbedder(), storage).search("garam", corpus)

        assertEquals(1, results.size)
        assertEquals("a", results.single().chunk.id)
    }

    @Test
    fun `search returns nothing when the index has no entries`() {
        val results = LocalCorpusIndex(CountingEmbedder(), MemoryStorage()).search("garam", emptyList())

        assertTrue(results.isEmpty())
    }
}
