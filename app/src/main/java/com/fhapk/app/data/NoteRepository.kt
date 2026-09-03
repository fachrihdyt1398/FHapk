package com.fhapk.app.data

import com.fhapk.app.sync.NoteSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val dao: NoteDao,
    private val scope: CoroutineScope
) {
    private val sync = NoteSync()

    fun observeAll(): Flow<List<Note>> = dao.observeAll()

    suspend fun getById(id: String): Note? = dao.getById(id)

    suspend fun save(note: Note) {
        dao.upsert(note)
        sync.push(note)
    }

    suspend fun delete(note: Note) {
        dao.delete(note)
        sync.delete(note)
    }

    fun startSync() {
        scope.launch {
            sync.signInAnonymously()
            sync.pullAll { note -> mergeRemote(note) }
            sync.listen { note -> mergeRemote(note) }
        }
    }

    private fun mergeRemote(remote: Note) {
        scope.launch {
            val local = dao.getById(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                dao.upsert(remote)
            } else {
                sync.push(local)
            }
        }
    }
}