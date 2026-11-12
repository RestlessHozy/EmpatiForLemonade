package com.example.empati.data.lemonade

import com.example.empati.data.chat.ChatRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCapabilityPresenterTest {

    @Test
    fun `missing server is named separately from a loading model`() {
        val server = ChatCapabilityPresenter.forFailure(LemonadeFailure.Unreachable(null))
        val loading = ChatCapabilityPresenter.forFailure(LemonadeFailure.ModelNotReady("empati-llm", null))

        assertEquals(ChatCapability.SERVER_MISSING, server?.capability)
        assertEquals(ChatCapability.MODEL_LOADING, loading?.capability)
        assertEquals("Server Lemonade tidak tersedia", server?.title)
        assertEquals("Model sedang dimuat", loading?.title)
    }

    @Test
    fun `busy model has its own message`() {
        val busy = ChatCapabilityPresenter.forFailure(LemonadeFailure.Busy(null))

        assertEquals(ChatCapability.MODEL_BUSY, busy?.capability)
        assertEquals("Model sedang sibuk", busy?.title)
        assertTrue(busy!!.detail.isNotBlank())
    }

    @Test
    fun `each notice tells the user what to do next`() {
        val notices = listOf(
            LemonadeFailure.Unreachable(null),
            LemonadeFailure.ModelNotReady("empati-llm", null),
            LemonadeFailure.Busy(null)
        ).mapNotNull { ChatCapabilityPresenter.forFailure(it) }

        assertEquals(3, notices.size)
        notices.forEach { notice ->
            assertTrue(notice.title.isNotBlank())
            assertTrue(notice.detail.isNotBlank())
        }
    }

    @Test
    fun `the notice names which fallback route answered`() {
        val cloud = ChatCapabilityPresenter.forFailure(LemonadeFailure.Unreachable(null), ChatRoute.CLOUD)
        val offline = ChatCapabilityPresenter.forFailure(LemonadeFailure.Unreachable(null), ChatRoute.OFFLINE)

        assertTrue(cloud!!.detail.contains("cloud Supabase"))
        assertTrue(offline!!.detail.contains("offline"))
    }

    @Test
    fun `a successful local answer carries no notice`() {
        assertNull(ChatCapabilityPresenter.forFailure(null))
    }
}
