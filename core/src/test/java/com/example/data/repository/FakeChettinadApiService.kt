package com.example.data.repository

import com.example.data.api.*
import retrofit2.Call
import retrofit2.Response

open class FakeChettinadApiService : ChettinadApiService {
    var loginStaffResponse: (() -> LoginResponse)? = null
    var loginResponse: (() -> LoginResponse)? = null
    var submitTriageResponse: (() -> Response<Unit>)? = null
    var getPatientRecordResponse: ((String) -> PatientRecord)? = null
    var completeConsultationResponse: (() -> NoteVersion)? = null

    override suspend fun loginStaff(credentials: Map<String, String>): LoginResponse {
        return loginStaffResponse?.invoke() ?: throw NotImplementedError()
    }

    override suspend fun login(credentials: Map<String, String>): LoginResponse {
        return loginResponse?.invoke() ?: throw NotImplementedError()
    }

    override suspend fun requestOtp(payload: Map<String, String>): OtpRequestResponse = throw NotImplementedError()
    override suspend fun verifyOtp(payload: Map<String, Any>): LoginResponse = throw NotImplementedError()
    override fun refreshSync(): Call<RefreshResponse> = throw NotImplementedError()
    override suspend fun logout(): Response<Unit> = throw NotImplementedError()
    override suspend fun getSession(): SessionResponse = throw NotImplementedError()
    override suspend fun getQueue(): List<QueueEntry> = throw NotImplementedError()
    override suspend fun startTriage(encounterId: String, version: VersionRequest): Response<Unit> = throw NotImplementedError()

    override suspend fun submitTriage(encounterId: String, triageWrite: TriageWrite): Response<Unit> {
        return submitTriageResponse?.invoke() ?: throw NotImplementedError()
    }

    override suspend fun saveConsultationDraft(encounterId: String, write: ConsultationWrite): NoteVersion = throw NotImplementedError()

    override suspend fun completeConsultation(encounterId: String, write: ConsultationWrite): NoteVersion {
        return completeConsultationResponse?.invoke() ?: throw NotImplementedError()
    }

    override suspend fun getPatientRecord(patientId: String): PatientRecord {
        return getPatientRecordResponse?.invoke(patientId) ?: throw NotImplementedError()
    }

    override suspend fun getCatalogues(): Catalogues = throw NotImplementedError()
    override suspend fun getStaff(): List<Staff> = throw NotImplementedError()
    override suspend fun createStaff(staff: StaffInput): Response<Unit> = throw NotImplementedError()
    override suspend fun updateStaffStatus(staffId: String, active: Map<String, Boolean>): Response<Unit> = throw NotImplementedError()
}
