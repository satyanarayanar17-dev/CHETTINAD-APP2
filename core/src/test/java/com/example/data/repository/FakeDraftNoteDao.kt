package com.example.data.repository

import com.example.data.db.DraftNoteDao
import com.example.data.db.DraftNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeDraftNoteDao : DraftNoteDao {
    private val drafts = mutableListOf<DraftNoteEntity>()

    override fun getDraftsForPatient(patientId: String): Flow<List<DraftNoteEntity>> {
        return flowOf(drafts.filter { it.patientId == patientId })
    }

    override suspend fun saveDraft(draft: DraftNoteEntity) {
        drafts.removeAll { it.patientId == draft.patientId }
        drafts.add(draft)
    }

    override suspend fun deleteDraft(id: String) {
        drafts.removeAll { it.patientId == id }
    }
}
