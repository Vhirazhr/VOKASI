package com.example.vokasilearning.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StudentLoginNameTest {
    @Test
    fun fullNameWithSpacesMapsToExistingInternalAlias() {
        val identity = StudentLoginName.parse("  Abdi   Rizal SAPUTRA  ")
        assertNotNull(identity)
        assertEquals(listOf("abdi.rizal.saputra"), identity!!.aliases)
        assertEquals("abdi rizal saputra", identity.normalizedFullName)
    }

    @Test
    fun punctuationIsNormalizedLikeServerProvisioning() {
        val identity = StudentLoginName.parse("Areeta Fa'adillah Ardiana Putri")
        assertEquals(listOf("areeta.fa.adillah.ardiana"), identity!!.aliases)
    }

    @Test
    fun classHintEnablesSafeCollisionDisambiguation() {
        val identity = StudentLoginName.parse("Sample Same Name | XI TMI 2")
        assertEquals("XI TMI 2", identity!!.classHint)
        assertEquals("sample.same.name", identity.aliases[0])
        assertEquals("sample.same.name.2", identity.aliases[1])
    }

    @Test
    fun originalUsernameStillWorks() {
        val identity = StudentLoginName.parse("abdi.rizal.saputra")
        assertEquals(listOf("abdi.rizal.saputra"), identity!!.aliases)
        assertNull(identity.normalizedFullName)
    }

    @Test
    fun incompleteClassHintIsRejected() {
        assertNull(StudentLoginName.parse("Sample Name | "))
    }
}
