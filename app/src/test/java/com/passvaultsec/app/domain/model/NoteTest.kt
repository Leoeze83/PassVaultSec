package com.passvaultsec.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTest {

    @Test
    fun `test owner can always edit and read note`() {
        val note = Note(
            id = "note-1",
            ownerId = "owner-uid-123",
            ownerEmail = "owner@gmail.com",
            title = "Nota Confidencial"
        )

        // Verificación por UID
        assertTrue(note.canEdit("cualquier_correo@gmail.com", "owner-uid-123"))
        assertTrue(note.canRead("cualquier_correo@gmail.com", "owner-uid-123"))

        // Verificación por Correo
        assertTrue(note.canEdit("owner@gmail.com", "otro-uid"))
        assertTrue(note.canRead("owner@gmail.com", "otro-uid"))
    }

    @Test
    fun `test collaborator with EDITOR role can edit and read`() {
        val note = Note(
            id = "note-2",
            ownerId = "owner-uid",
            ownerEmail = "owner@gmail.com",
            collaborators = mapOf(
                "colaborador@gmail.com" to Collaborator(
                    email = "colaborador@gmail.com",
                    role = CollaboratorRole.EDITOR
                )
            )
        )

        assertTrue(note.canEdit("colaborador@gmail.com", "collab-uid"))
        assertTrue(note.canRead("colaborador@gmail.com", "collab-uid"))
    }

    @Test
    fun `test collaborator with VIEWER role can read but cannot edit`() {
        val note = Note(
            id = "note-3",
            ownerId = "owner-uid",
            ownerEmail = "owner@gmail.com",
            collaborators = mapOf(
                "lector@gmail.com" to Collaborator(
                    email = "lector@gmail.com",
                    role = CollaboratorRole.VIEWER
                )
            )
        )

        assertFalse(note.canEdit("lector@gmail.com", "lector-uid"))
        assertTrue(note.canRead("lector@gmail.com", "lector-uid"))
    }

    @Test
    fun `test stranger cannot read and cannot edit`() {
        val note = Note(
            id = "note-4",
            ownerId = "owner-uid",
            ownerEmail = "owner@gmail.com"
        )

        assertFalse(note.canEdit("desconocido@gmail.com", "stranger-uid"))
        assertFalse(note.canRead("desconocido@gmail.com", "stranger-uid"))
    }

    @Test
    fun `test collaborator email matching is case insensitive`() {
        val note = Note(
            id = "note-5",
            ownerId = "owner-uid",
            ownerEmail = "Owner@Gmail.com",
            collaborators = mapOf(
                "colaborador@gmail.com" to Collaborator(
                    email = "colaborador@gmail.com",
                    role = CollaboratorRole.EDITOR
                )
            )
        )

        assertTrue(note.canEdit("CoLaBoRaDoR@gmail.com", "random-uid"))
        assertTrue(note.canRead("COLABORADOR@GMAIL.COM", "random-uid"))
        assertTrue(note.collaboratorEmails.contains("colaborador@gmail.com"))
    }
}
