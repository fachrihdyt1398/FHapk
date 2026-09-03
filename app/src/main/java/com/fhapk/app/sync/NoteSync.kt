package com.fhapk.app.sync

import com.fhapk.app.data.Note
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class NoteSync {

    private val firebaseReady = try {
        FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()
    } catch (_: Exception) {
        false
    }

    private val db: FirebaseFirestore? = if (firebaseReady) FirebaseFirestore.getInstance() else null
    private val auth: FirebaseAuth? = if (firebaseReady) FirebaseAuth.getInstance() else null
    private var uid: String? = null

    private val notesRef
        get() = uid?.let { id ->
            db?.collection("users")?.document(id)?.collection("notes")
        }

    suspend fun signInAnonymously() {
        val a = auth ?: return
        uid = try {
            a.signInAnonymously().await().user?.uid
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pullAll(onNote: (Note) -> Unit) {
        val ref = notesRef ?: return
        val snapshot = try {
            ref.get().await()
        } catch (_: Exception) {
            return
        }
        snapshot.documents.forEach { doc ->
            toNote(doc)?.let(onNote)
        }
    }

    fun listen(onNote: (Note) -> Unit): ListenerRegistration? {
        val ref = notesRef ?: return null
        return ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            snapshot.documents.forEach { doc ->
                toNote(doc)?.let(onNote)
            }
        }
    }

    suspend fun push(note: Note) {
        val ref = notesRef?.document(note.id) ?: return
        try {
            ref.set(
                mapOf(
                    "title" to note.title,
                    "content" to note.content,
                    "updatedAt" to note.updatedAt
                )
            ).await()
        } catch (_: Exception) {
            // offline: tersimpan lokal, tersinkron otomatis saat online kembali
        }
    }

    suspend fun delete(note: Note) {
        val ref = notesRef?.document(note.id) ?: return
        try {
            ref.delete().await()
        } catch (_: Exception) {
        }
    }

    private fun toNote(doc: DocumentSnapshot): Note? {
        val data = doc.data ?: return null
        return Note(
            id = doc.id,
            title = data["title"] as? String ?: "",
            content = data["content"] as? String ?: "",
            updatedAt = data["updatedAt"] as? Long ?: 0L
        )
    }
}