package com.example.empati.data.rag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmpatiCorpusTest {

    @Test
    fun `corpus has enough chunks to answer common questions`() {
        assertTrue("corpus too small: ${EmpatiCorpus.chunks.size}", EmpatiCorpus.chunks.size >= 12)
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
