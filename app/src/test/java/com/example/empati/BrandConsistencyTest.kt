package com.example.empati

import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.io.File

class BrandConsistencyTest {

    private val retiredNames = listOf("SRYA Emora", "SRYA-Emora", "AI-EMPATI")

    @Test
    fun `user facing sources only use the Empati name`() {
        val roots = listOf(File("src/main"), File("../supabase/functions"))
        Assume.assumeTrue(roots.any { it.isDirectory })

        val offenders = roots.filter { it.isDirectory }
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml", "ts") }.toList() }
            .filter { file -> retiredNames.any { name -> file.readText().contains(name) } }
            .map { it.name }

        assertTrue("old brand still present in: $offenders", offenders.isEmpty())
    }
}