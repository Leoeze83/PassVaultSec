package com.passvaultsec.app.core.security

import com.passvaultsec.app.domain.model.Note
import com.passvaultsec.app.domain.model.NoteLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    @Test
    fun createAndRestoreBackup_withValidPassword_restoresAllNotes() {
        // Arrange
        val notes = listOf(
            Note(
                id = "note-1",
                title = "Nota de Seguridad",
                content = "Contenido confidencial",
                location = NoteLocation(
                    latitude = -34.6037,
                    longitude = -58.3816,
                    address = "Obelisco, Buenos Aires",
                    placeName = "Obelisco"
                )
            ),
            Note(
                id = "note-2",
                title = "Segunda Nota",
                content = "Texto de prueba"
            )
        )
        val masterPassword = "SuperSecurePassword123!"

        // Act
        val backupBytes = BackupManager.createEncryptedBackup(notes, masterPassword)
        val restoreResult = BackupManager.restoreEncryptedBackup(backupBytes, masterPassword)

        // Assert
        assertTrue(restoreResult.isSuccess)
        val restoredNotes = restoreResult.getOrThrow()
        assertEquals(2, restoredNotes.size)
        assertEquals("Nota de Seguridad", restoredNotes[0].title)
        assertEquals("Contenido confidencial", restoredNotes[0].content)
        assertNotNull(restoredNotes[0].location)
        assertEquals("Obelisco", restoredNotes[0].location?.placeName)
        assertEquals("Segunda Nota", restoredNotes[1].title)
    }

    @Test
    fun restoreBackup_withIncorrectPassword_fails() {
        // Arrange
        val notes = listOf(Note(id = "note-1", title = "Privado"))
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword999"

        val backupBytes = BackupManager.createEncryptedBackup(notes, correctPassword)

        // Act
        val restoreResult = BackupManager.restoreEncryptedBackup(backupBytes, wrongPassword)

        // Assert
        assertTrue(restoreResult.isFailure)
    }

    @Test
    fun restoreBackup_withCorruptedBytes_fails() {
        // Arrange
        val corruptedBytes = byteArrayOf(0x00, 0x01, 0x02)

        // Act
        val restoreResult = BackupManager.restoreEncryptedBackup(corruptedBytes, "AnyPassword123")

        // Assert
        assertTrue(restoreResult.isFailure)
    }
}
