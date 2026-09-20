package com.example.data.repository

import com.example.data.api.ChettinadApiService
import com.example.data.api.TriageWrite
import com.example.data.api.VersionRequest
import com.example.domain.model.Patient
import retrofit2.HttpException

open class NurseRepository(
    private val apiService: ChettinadApiService
) {
    open suspend fun getQueue(): Result<List<Patient>> {
        return try {
            val queue = apiService.getQueue()
            val mapped = queue.map { entry ->
                Patient(
                    id = entry.patient_id,
                    mrn = entry.mrn,
                    name = entry.patient_name,
                    age = try {
                        val parts = entry.dob.split("-")
                        val year = parts[0].toInt()
                        2024 - year
                    } catch (e: Exception) { 30 },
                    gender = entry.gender,
                    allergies = emptyList(),
                    encounterId = entry.encounter_id
                )
            }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun startTriage(encounterId: String, version: Int): Result<Unit> {
        return try {
            val response = apiService.startTriage(encounterId, VersionRequest(version))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun submitTriage(encounterId: String, triageWrite: TriageWrite): Result<Unit> {
        return try {
            val response = apiService.submitTriage(encounterId, triageWrite)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

