package com.example.empati.data.retrieval

import com.example.empati.data.lemonade.MiniJson
import com.example.empati.data.triage.ReferenceChunk

interface IndexStorage {
    fun read(): String?
    fun write(value: String)
}

/**
 * Menyimpan vektor sebagai hex little-endian. Hex dipilih karena `java.util.Base64`
 * baru tersedia sejak API 26 sementara aplikasi ini mendukung API 24.
 */
object EmbeddingStoreCodec {

    private const val VERSION = 1

    fun encode(entries: List<IndexedChunk>): String = MiniJson.write(
        mapOf(
            "version" to VERSION,
            "entries" to entries.map { entry ->
                mapOf(
                    "id" to entry.chunk.id,
                    "title" to entry.chunk.title,
                    "publisher" to entry.chunk.publisher,
                    "year" to entry.chunk.year,
                    "section" to entry.chunk.section,
                    "text" to entry.chunk.text,
                    "url" to entry.chunk.url,
                    "vector" to vectorToHex(entry.vector),
                    "hash" to entry.textHash
                )
            }
        )
    )

    fun decode(raw: String?): List<IndexedChunk> {
        if (raw.isNullOrBlank()) return emptyList()
        val root = runCatching { MiniJson.parse(raw) as? Map<*, *> }.getOrNull() ?: return emptyList()
        val entries = root["entries"] as? List<*> ?: return emptyList()
        return entries.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val id = map["id"] as? String ?: return@mapNotNull null
            val title = map["title"] as? String ?: return@mapNotNull null
            val publisher = map["publisher"] as? String ?: return@mapNotNull null
            val section = map["section"] as? String ?: return@mapNotNull null
            val text = map["text"] as? String ?: return@mapNotNull null
            val url = map["url"] as? String ?: return@mapNotNull null
            val hash = map["hash"] as? String ?: return@mapNotNull null
            val vector = (map["vector"] as? String)?.let { hexToVector(it) } ?: return@mapNotNull null
            if (vector.isEmpty()) return@mapNotNull null
            IndexedChunk(
                chunk = ReferenceChunk(
                    id = id,
                    title = title,
                    publisher = publisher,
                    year = (map["year"] as? Number)?.toInt() ?: 0,
                    section = section,
                    text = text,
                    url = url
                ),
                vector = vector,
                textHash = hash
            )
        }
    }

    private fun vectorToHex(vector: FloatArray): String {
        val builder = StringBuilder(vector.size * 8)
        for (value in vector) {
            val bits = value.toRawBits()
            builder.append(HEX[(bits ushr 28) and 0xF])
            builder.append(HEX[(bits ushr 24) and 0xF])
            builder.append(HEX[(bits ushr 20) and 0xF])
            builder.append(HEX[(bits ushr 16) and 0xF])
            builder.append(HEX[(bits ushr 12) and 0xF])
            builder.append(HEX[(bits ushr 8) and 0xF])
            builder.append(HEX[(bits ushr 4) and 0xF])
            builder.append(HEX[bits and 0xF])
        }
        return builder.toString()
    }

    private fun hexToVector(hex: String): FloatArray {
        if (hex.length % 8 != 0) return FloatArray(0)
        val result = FloatArray(hex.length / 8)
        for (i in result.indices) {
            var bits = 0
            for (j in 0 until 8) {
                val digit = Character.digit(hex[i * 8 + j], 16)
                if (digit < 0) return FloatArray(0)
                bits = (bits shl 4) or digit
            }
            result[i] = Float.fromBits(bits)
        }
        return result
    }

    private val HEX = "0123456789abcdef".toCharArray()
}

/**
 * Indeks korpus yang bertahan antar sesi. Penyimpanan diserahkan ke pemanggil
 * lewat [IndexStorage], sehingga kode ini tetap bisa diuji tanpa Android.
 */
class LocalCorpusIndex(
    private val embedder: Embedder,
    private val storage: IndexStorage,
    private val indexer: CorpusIndexer = CorpusIndexer(embedder),
    private val embeddingIndex: EmbeddingIndex = EmbeddingIndex()
) {

    fun load(): List<IndexedChunk> = EmbeddingStoreCodec.decode(storage.read())

    fun ensureIndexed(corpus: List<ReferenceChunk>): List<IndexedChunk> {
        val existing = load()
        val merged = indexer.index(corpus, existing)
        if (merged != existing) {
            storage.write(EmbeddingStoreCodec.encode(merged))
        }
        return merged
    }

    fun search(query: String, corpus: List<ReferenceChunk>): List<ScoredChunk> {
        if (corpus.isEmpty() || query.isBlank()) return emptyList()
        val entries = ensureIndexed(corpus)
        if (entries.isEmpty()) return emptyList()
        val vector = embedder.embed(listOf(query)).firstOrNull() ?: return emptyList()
        return embeddingIndex.search(vector, entries)
    }
}
