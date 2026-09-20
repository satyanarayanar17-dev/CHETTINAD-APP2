package com.example.data.repository

import com.example.data.api.ConsultationData
import com.example.data.api.ConsultationWrite
import com.example.data.api.TriageData
import com.example.data.api.TriageWrite
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class FailurePathTest {
    private val fakeApiService = FakeChettinadApiService()
    private val fakeDraftNoteDao = FakeDraftNoteDao()

    private val sampleTriageWrite = TriageWrite(
        __v = 1,
        data = TriageData(
            temperature = 98.6,
            systolic = 120,
            diastolic = 80,
            pulse = 72,
            spo2 = 98.0,
            weight = 70.0,
            height = 175.0,
            glucose = 95.0,
            complaint = "Fever",
            allergies = null,
            pain = 2,
            notes = null,
            priority = 3
        )
    )

    @Test
    fun triageSubmit_onNetworkFailure_returnsFailureAndDoesNotFakeCompletion() = runBlocking {
        val nurseRepository = NurseRepository(fakeApiService)
        fakeApiService.submitTriageResponse = {
            throw IOException("Network timeout connecting to server")
        }

        val result = nurseRepository.submitTriage("enc-001", sampleTriageWrite)

        assertFalse("Triage submission must fail when network error occurs", result.isSuccess)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun triageSubmit_onHttp500_returnsFailure() = runBlocking {
        val nurseRepository = NurseRepository(fakeApiService)
        val errorResponse = Response.error<Unit>(
            500,
            "{\"error\": \"Internal Server Error\"}".toResponseBody("application/json".toMediaTypeOrNull())
        )
        fakeApiService.submitTriageResponse = { errorResponse }

        val result = nurseRepository.submitTriage("enc-001", sampleTriageWrite)

        assertFalse("Triage submission must return failure on 500 error", result.isSuccess)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun patientRecords_onHttp404_doesNotReturnFakePatient() = runBlocking {
        val patientRepository = PatientRepository(fakeApiService)
        val errorResponse = Response.error<com.example.data.api.PatientRecord>(
            404,
            "{\"error\": \"Patient not found\"}".toResponseBody("application/json".toMediaTypeOrNull())
        )
        fakeApiService.getPatientRecordResponse = {
            throw HttpException(errorResponse)
        }

        val result = patientRepository.getMyRecords("PT-NONEXISTENT")

        assertFalse("Patient repository must not return success on 404", result.isSuccess)
        assertTrue(result.isFailure)
        assertNull("No fake or synthetic dossier should be returned", result.getOrNull())
    }

    @Test
    fun patientRecords_onHttp500_doesNotReturnFakePatient() = runBlocking {
        val patientRepository = PatientRepository(fakeApiService)
        val errorResponse = Response.error<com.example.data.api.PatientRecord>(
            500,
            "{\"error\": \"Database connection error\"}".toResponseBody("application/json".toMediaTypeOrNull())
        )
        fakeApiService.getPatientRecordResponse = {
            throw HttpException(errorResponse)
        }

        val result = patientRepository.getMyRecords("PT-1001")

        assertFalse("Patient repository must not return success on 500", result.isSuccess)
        assertTrue(result.isFailure)
        assertNull("No fallback or mock patient should be returned on backend error", result.getOrNull())
    }

    @Test
    fun consultationCompletion_onBackendFailure_returnsFailureAndDoesNotMarkCompleted() = runBlocking {
        val doctorRepository = DoctorRepository(fakeApiService, fakeDraftNoteDao)
        val errorResponse = Response.error<com.example.data.api.NoteVersion>(
            500,
            "{\"error\": \"Failed to commit consultation\"}".toResponseBody("application/json".toMediaTypeOrNull())
        )
        val write = ConsultationWrite(
            __v = 2,
            data = ConsultationData(complaint = "Chest pain", diagnosis_ids = listOf("diag-1"))
        )
        fakeApiService.completeConsultationResponse = {
            throw HttpException(errorResponse)
        }

        val result = doctorRepository.completeConsultation("enc-002", write)

        assertFalse("Consultation completion must fail when backend returns an error", result.isSuccess)
        assertTrue(result.isFailure)
        assertNull("No completed NoteVersion should be returned on failure", result.getOrNull())
    }
}
