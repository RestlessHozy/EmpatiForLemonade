package com.example.empati.data.retrieval

import com.example.empati.data.triage.ReferenceChunk
import kotlin.math.sqrt

data class IndexedChunk(
    val chunk: ReferenceChunk,
    val vector: FloatArray,
    val textHash: String
)

data class ScoredChunk(
    val chunk: ReferenceChunk,
    val score: Double
)

class EmbeddingIndex(
    private val minScore: Double = 0.35,
    private val topK: Int = 4
) {

    fun search(query: FloatArray, entries: List<IndexedChunk>): List<ScoredChunk> {
        if (query.isEmpty()) return emptyList()
        val queryNorm = norm(query)
        if (queryNorm == 0.0) return emptyList()

        return entries
            .mapNotNull { entry ->
                if (entry.vector.size != query.size) return@mapNotNull null
                val entryNorm = norm(entry.vector)
                if (entryNorm == 0.0) return@mapNotNull null
                ScoredChunk(entry.chunk, dot(query, entry.vector) / (queryNorm * entryNorm))
            }
            .filter { it.score.isFinite() && it.score >= minScore }
            .sortedByDescending { it.score }
            .take(topK)
    }

    private fun dot(a: FloatArray, b: FloatArray): Double {
        var sum = 0.0
        for (i in a.indices) sum += a[i].toDouble() * b[i].toDouble()
        return sum
    }

    private fun norm(vector: FloatArray): Double {
        var sum = 0.0
        for (value in vector) sum += value.toDouble() * value.toDouble()
        return sqrt(sum)
    }
}

fun interface Embedder {
    fun embed(texts: List<String>): List<FloatArray>
}

/**
 * Menjaga indeks tetap sinkron dengan korpus: chunk baru di-embed, chunk yang
 * teksnya berubah di-embed ulang, dan chunk yang hilang dibuang. Hash dipakai
 * supaya indeks ulang tidak memanggil server untuk teks yang sama.
 */
class CorpusIndexer(private val embedder: Embedder) {

    fun index(corpus: List<ReferenceChunk>, existing: List<IndexedChunk>): List<IndexedChunk> {
        val existingById = existing.associateBy { it.chunk.id }
        val toEmbed = mutableListOf<ReferenceChunk>()
        val kept = mutableListOf<IndexedChunk>()

        corpus.forEach { chunk ->
            val hash = hashOf(chunk.text)
            val previous = existingById[chunk.id]
            when {
                previous != null && previous.textHash == hash -> kept += previous.copy(chunk = chunk)
                else -> toEmbed += chunk
            }
        }

        // Chunk yang gagal di-embed (server mati, model belum dimuat) tidak masuk indeks sama sekali.
        // Menyimpan vektor kosong hanya membuang tempat dan memaksa penulisan ulang di setiap pesan.
        val fresh = if (toEmbed.isEmpty()) {
            emptyList()
        } else {
            val vectors = embedder.embed(toEmbed.map { "${it.title}. ${it.text}" })
            toEmbed.mapIndexedNotNull { index, chunk ->
                val vector = vectors.getOrNull(index)?.takeIf { it.isNotEmpty() } ?: return@mapIndexedNotNull null
                IndexedChunk(
                    chunk = chunk,
                    vector = vector,
                    textHash = hashOf(chunk.text)
                )
            }
        }

        return (kept + fresh).sortedBy { it.chunk.id }
    }

    companion object {
        fun hashOf(text: String): String {
            var hash = 0x811C9DC5.toInt()
            for (character in text) {
                hash = hash xor character.code
                hash *= 0x01000193
            }
            return hash.toUInt().toString(16).padStart(8, '0')
        }
    }
}

/**
 * Memilih chunk yang benar-benar disebut dalam jawaban. Nama penerbit dipakai
 * sebagai sinyal, karena prompt mewajibkan model menyebut penerbit, bukan id.
 */
object CitationPicker {

    fun usedIn(answer: String, candidates: List<ReferenceChunk>): List<ReferenceChunk> {
        val normalized = answer.lowercase()
        return candidates.filter { chunk ->
            aliasesOf(chunk.publisher).any { alias -> mentions(normalized, alias) }
        }
    }

    private fun aliasesOf(publisher: String): List<String> {
        val full = publisher.lowercase()
        if (full.isBlank()) return emptyList()
        return buildList {
            add(full)
            when {
                full.startsWith("world health organization") -> {
                    add("who")
                    add("organisasi kesehatan dunia")
                }
                full.startsWith("kementerian kesehatan") -> {
                    add("kemenkes")
                    add("kementerian kesehatan")
                }
                full.startsWith("kementerian pemberdayaan perempuan") -> {
                    add("kemenpppa")
                    add("sapa 129")
                }
                full.startsWith("perhimpunan dokter spesialis kardiovaskular") -> {
                    add("perki")
                    add("perhimpunan dokter spesialis kardiovaskular")
                }
                full.startsWith("komisi nasional anti kekerasan terhadap perempuan") -> {
                    add("komnas perempuan")
                }
            }
        }
    }

    private fun mentions(answer: String, alias: String): Boolean =
        Regex("(?<![a-z0-9])${Regex.escape(alias)}(?![a-z0-9])").containsMatchIn(answer)
}
