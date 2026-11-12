package com.example.empati.data.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatFallbackLadderTest {

    @Test
    fun `local model wins when it answers`() {
        val outcome = ChatFallbackLadder(
            lemonade = { "jawaban lokal" },
            cloud = { "jawaban cloud" }
        ).answer()

        assertEquals(ChatRoute.LEMONADE, outcome.route)
        assertEquals("jawaban lokal", outcome.text)
    }

    @Test
    fun `cloud is used when the local model fails`() {
        val outcome = ChatFallbackLadder(
            lemonade = { throw IllegalStateException("server mati") },
            cloud = { "jawaban cloud" }
        ).answer()

        assertEquals(ChatRoute.CLOUD, outcome.route)
        assertEquals("jawaban cloud", outcome.text)
    }

    @Test
    fun `offline text is the last resort`() {
        val outcome = ChatFallbackLadder(
            lemonade = { throw IllegalStateException("mati") },
            cloud = { throw IllegalStateException("jaringan mati") }
        ).answer()

        assertEquals(ChatRoute.OFFLINE, outcome.route)
        assertTrue(outcome.text.contains("belum bisa"))
        assertTrue(ChatFallbackLadder.DEFAULT_OFFLINE_TEXT == outcome.text)
    }

    @Test
    fun `blank answers count as failure`() {
        val outcome = ChatFallbackLadder(
            lemonade = { "   " },
            cloud = { "" }
        ).answer()

        assertEquals(ChatRoute.OFFLINE, outcome.route)
    }

    @Test
    fun `cloud stays optional`() {
        val outcome = ChatFallbackLadder(
            lemonade = { throw IllegalStateException("mati") },
            cloud = null
        ).answer()

        assertEquals(ChatRoute.OFFLINE, outcome.route)
    }
}
