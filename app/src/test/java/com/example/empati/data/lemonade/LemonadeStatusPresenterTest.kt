package com.example.empati.data.lemonade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LemonadeStatusPresenterTest {

    private fun status(vararg ids: String) = LemonadeStatus(
        models = ids.map { ModelInfo(id = it, recipe = "llamacpp", labels = emptyList(), downloaded = true) },
        missingAliases = LemonadeConfig.REQUIRED_ALIASES.filterNot { it in ids }
    )

    @Test
    fun `unchecked server invites a check`() {
        val summary = LemonadeStatusPresenter.summarize(status = null, error = null)

        assertFalse(summary.ready)
        assertEquals("Belum diperiksa", summary.title)
    }

    @Test
    fun `unreachable server explains itself without jargon`() {
        val summary = LemonadeStatusPresenter.summarize(
            status = null,
            error = "Server Lemonade tidak dapat dihubungi"
        )

        assertFalse(summary.ready)
        assertEquals("Server belum tersambung", summary.title)
        assertTrue(summary.detail.contains("Lemonade"))
    }

    @Test
    fun `ready server names the local port and stays short`() {
        val summary = LemonadeStatusPresenter.summarize(
            status = status(*LemonadeConfig.REQUIRED_ALIASES.toTypedArray()),
            error = null
        )

        assertTrue(summary.ready)
        assertEquals("Siap dipakai", summary.title)
        assertTrue(summary.detail.contains("13305"))
    }

    @Test
    fun `partially installed aliases are listed by name`() {
        val summary = LemonadeStatusPresenter.summarize(
            status = status("empati-llm", "empati-embed"),
            error = null
        )

        assertFalse(summary.ready)
        assertEquals("Sebagian siap", summary.title)
        assertTrue(summary.detail.contains("empati-stt"))
        assertTrue(summary.detail.contains("empati-tts"))
    }
}
