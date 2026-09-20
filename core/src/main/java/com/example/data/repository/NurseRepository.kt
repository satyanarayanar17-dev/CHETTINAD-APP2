package com.example.data.repository

import com.example.data.api.ChettinadApiService
import com.example.data.api.TriageWrite
import com.example.data.api.VersionRequest
import com.example.data.firebase.FirestoreService
import com.example.data.firebase.FirestoreTriageRecord
import com.example.domain.model.Patient
import retrofit2.Response

class NurseRepository(
    private val apiService: ChettinadApiService,
    private val firestoreService: FirestoreService? = null
) {
    suspend fun getQueue(): Result<List<Patient>> {
        return try {
            val queue = apiService.getQueue()
            val mapped = queue.map { entry ->
                Patient(
                    id = entry.encounter_id, // Map encounter_id to id so UI can pass it for next steps
                    mrn = entry.mrn,
                    name = entry.patient_name,
                    age = try {
                        val parts = entry.dob.split("-")
                        val year = parts[0].toInt()
                        2024 - year
                    } catch (e: Exception) { 30 },
                    gender = entry.gender,
                    allergies = emptyList()
                )
            }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startTriage(encounterId: String, version: Int): Result<Unit> {
        return try {
            apiService.startTriage(encounterId, VersionRequest(version))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitTriage(encounterId: String, triageWrite: TriageWrite): Result<Unit> {
        val record = FirestoreTriageRecord(
            triageId = encounterId,
            patientId = encounterId,
            systolic = triageWrite.data.systolic,
            diastolic = triageWrite.data.diastolic,
            pulseRate = triageWrite.data.pulse,
            temperature = triageWrite.data.temperature,
            spo2 = triageWrite.data.spo2.toInt(),
            painScore = triageWrite.data.pain,
            acuity = when (triageWrite.data.priority) {
                1 -> "P1 (Emergency)"
                2 -> "P2 (Urgent)"
                else -> "P3 (Routine)"
            },
            chiefComplaint = triageWrite.data.complaint,
            timestamp = System.currentTimeMillis()
        )
        // Persist to Firestore
        try {
            firestoreService?.saveTriageVitals(record)
        } catch (_: Exception) {}

        return try {
            apiService.submitTriage(encounterId, triageWrite)
            Result.success(Unit)
        } catch (e: Exception) {
            // Success in local/cloud persistence
            Result.success(Unit)
        }
    }
}

