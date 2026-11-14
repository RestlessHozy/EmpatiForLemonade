package com.example.empati.data.rag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmpatiCorpusTest {

    @Test
    fun `corpus has enough chunks to answer common questions`() {
        assertTrue("corpus too small: ${EmpatiCorpus.chunks.size}", EmpatiCorpus.chunks.size >= 28)
    }

    @Test
    fun `lexical search retrieves relevant clinical and gbv chunks without leaking gbv into hypertension queries`() {
        val saltChunks = EmpatiCorpus.searchLexical("Berapa batas konsumsi garam dan makanan asin seperti mie instan?", limit = 4)
        assertTrue(saltChunks.isNotEmpty())
        assertTrue(saltChunks.any { it.id.contains("salt") || it.id.contains("sodium") || it.id.contains("gizi") })
        assertTrue(saltChunks.none { it.id.startsWith("sapa129") || it.id.startsWith("who-vaw") })

        val measureChunks = EmpatiCorpus.searchLexical("Bagaimana cara ukur tensi yang benar pakai tensimeter di rumah?", limit = 4)
        assertTrue(measureChunks.any { it.id == "perki-htn-2024#home-measurement" })

        val gbvChunks = EmpatiCorpus.searchLexical("Saya mengalami ancaman kekerasan KDRT dan butuh bantuan", limit = 4)
        assertTrue(gbvChunks.isNotEmpty())
        assertTrue(gbvChunks.any { it.id.startsWith("sapa129") || it.id.startsWith("who-vaw") || it.id.startsWith("komnas-perempuan") })
    }

    @Test
    fun `every chunk has a unique id and a resolvable source`() {
        val ids = EmpatiCorpus.chunks.map { it.id }

        assertEquals("ids must be unique", ids.size, ids.toSet().size)
        EmpatiCorpus.chunks.forEach { chunk ->
            assertTrue("blank id", chunk.id.isNotBlank())
            assertTrue("blank title for ${chunk.id}", chunk.title.isNotBlank())
            assertTrue("blank publisher for ${chunk.id}", chunk.publisher.isNotBlank())
            assertTrue("blank section for ${chunk.id}", chunk.section.isNotBlank())
            assertTrue("blank text for ${chunk.id}", chunk.text.isNotBlank())
            assertTrue("url must be https for ${chunk.id}", chunk.url.startsWith("https://"))
            assertTrue("year looks wrong for ${chunk.id}", chunk.year in 1990..2100)
        }
    }

    @Test
    fun `lookup by id returns the same chunk`() {
        val sample = EmpatiCorpus.chunks.first()
        assertEquals(sample, EmpatiCorpus.byId(sample.id))
        assertEquals(null, EmpatiCorpus.byId("tidak-ada"))
    }

    @Test
    fun `corpus avoids internal scoring vocabulary`() {
        EmpatiCorpus.chunks.forEach { chunk ->
            val text = chunk.text.lowercase()
            listOf("certainty factor", "mb ", "md ", "skor cf").forEach { banned ->
                assertTrue("${chunk.id} contains $banned", !text.contains(banned))
            }
        }
    }

    @Test
    fun `corpus covers hypertension lifestyle stress and violence support`() {
        val corpus = EmpatiCorpus.chunks.joinToString(" ") { it.text.lowercase() }

        assertTrue(corpus.contains("tekanan darah"))
        assertTrue(corpus.contains("garam") || corpus.contains("natrium"))
        assertTrue(corpus.contains("stres"))
        assertTrue(corpus.contains("kekerasan"))
        assertTrue(corpus.contains("129"))
    }
}
