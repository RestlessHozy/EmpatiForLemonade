package com.example.empati.ui.screens

import com.example.empati.data.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStreamingBubbleTest {

    @Test
    fun `before first visible token only thinking bubble is shown without empty assistant bubble`() {
        val messages = listOf(
            ChatMessage(isUser = false, text = "Halo, ada yang bisa dibantu?"),
            ChatMessage(isUser = true, text = "Apa itu konseling?"),
            ChatMessage(isUser = false, text = "")
        )

        val visible = visibleChatMessages(messages, isGenerating = true)

        assertEquals(2, visible.size)
        assertEquals(listOf(0, 1), visible.map { it.index })
        assertTrue(shouldShowThinkingBubble(messages, isGenerating = true))
    }

    @Test
    fun `whitespace-only delta stays hidden behind thinking bubble`() {
        val messages = listOf(
            ChatMessage(isUser = true, text = "Hai"),
            ChatMessage(isUser = false, text = "  \n ")
        )

        val visible = visibleChatMessages(messages, isGenerating = true)

        assertEquals(1, visible.size)
        assertTrue(visible.single().value.isUser)
        assertTrue(shouldShowThinkingBubble(messages, isGenerating = true))
    }

    @Test
    fun `once streaming tokens appear thinking bubble disappears so only one bot bubble exists`() {
        val messages = listOf(
            ChatMessage(isUser = true, text = "Saya cemas."),
            ChatMessage(isUser = false, text = "Saya mengerti")
        )

        val visible = visibleChatMessages(messages, isGenerating = true)

        assertEquals(2, visible.size)
        assertEquals(1, visible.last().index)
        assertEquals("Saya mengerti", visible.last().value.text)
        assertFalse(shouldShowThinkingBubble(messages, isGenerating = true))
    }

    @Test
    fun `draft state ending with user message preserves thinking bubble`() {
        val messages = listOf(
            ChatMessage(isUser = false, text = "Selamat datang"),
            ChatMessage(isUser = true, text = "Pertanyaan saya")
        )

        val visible = visibleChatMessages(messages, isGenerating = true)

        assertEquals(2, visible.size)
        assertTrue(shouldShowThinkingBubble(messages, isGenerating = true))
    }

    @Test
    fun `after generation stops thinking bubble never shows and settled reply renders`() {
        val messages = listOf(
            ChatMessage(isUser = true, text = "Terima kasih"),
            ChatMessage(isUser = false, text = "Sama-sama.")
        )

        val visible = visibleChatMessages(messages, isGenerating = false)

        assertEquals(2, visible.size)
        assertFalse(shouldShowThinkingBubble(messages, isGenerating = false))
    }
}

