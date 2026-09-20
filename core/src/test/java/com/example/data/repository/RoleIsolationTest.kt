package com.example.data.repository

import com.example.data.api.LoginResponse
import com.example.data.api.PersistentCookieJar
import com.example.domain.model.Role
import com.example.util.TokenManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoleIsolationTest {
    private val fakeApiService = FakeChettinadApiService()
    private val tokenManager = TokenManager()
    private val cookieJar = PersistentCookieJar()
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        authRepository = AuthRepository(fakeApiService, tokenManager, cookieJar)
    }

    @Test
    fun patientLogin_rejectsDoctorCredentials() = runBlocking {
        fakeApiService.loginResponse = {
            LoginResponse(
                access_token = "valid-token-dr",
                role = "doctor",
                account_type = "staff",
                userId = "dr_smith",
                name = "Dr. Smith",
                must_change_password = false,
                token_type = "bearer"
            )
        }

        val result = authRepository.loginPatient(mapOf("username" to "dr_smith", "password" to "doctor123"))

        assertFalse("Patient login must fail when staff credentials (doctor) are provided", result.isSuccess)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun patientLogin_rejectsNurseCredentials() = runBlocking {
        fakeApiService.loginResponse = {
            LoginResponse(
                access_token = "valid-token-nurse",
                role = "nurse",
                account_type = "staff",
                userId = "nurse_joy",
                name = "Nurse Joy",
                must_change_password = false,
                token_type = "bearer"
            )
        }

        val result = authRepository.loginPatient(mapOf("username" to "nurse_joy", "password" to "nurse123"))

        assertFalse("Patient login must fail when staff credentials (nurse) are provided", result.isSuccess)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun patientLogin_rejectsAdminCredentials() = runBlocking {
        fakeApiService.loginResponse = {
            LoginResponse(
                access_token = "valid-token-admin",
                role = "admin",
                account_type = "staff",
                userId = "admin_01",
                name = "Admin",
                must_change_password = false,
                token_type = "bearer"
            )
        }

        val result = authRepository.loginPatient(mapOf("username" to "admin_01", "password" to "admin123"))

        assertFalse("Patient login must fail when staff credentials (admin) are provided", result.isSuccess)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun staffLogin_rejectsPatientCredentials() = runBlocking {
        fakeApiService.loginStaffResponse = {
            LoginResponse(
                access_token = "valid-token-patient",
                role = "patient",
                account_type = "patient",
                userId = "PT1001",
                name = "Patient One",
                must_change_password = false,
                token_type = "bearer"
            )
        }

        val result = authRepository.loginStaff(mapOf("username" to "PT1001", "password" to "patient123"))

        assertFalse("Staff login must fail when patient credentials are provided", result.isSuccess)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun patientLogin_succeedsForPatientRole() = runBlocking {
        fakeApiService.loginResponse = {
            LoginResponse(
                access_token = "valid-token-patient",
                role = "patient",
                account_type = "patient",
                userId = "PT1001",
                name = "Patient One",
                must_change_password = false,
                token_type = "bearer"
            )
        }

        val result = authRepository.loginPatient(mapOf("username" to "PT1001", "password" to "patient123"))

        assertTrue("Patient login should succeed for patient credentials", result.isSuccess)
        val user = result.getOrNull()
        assertEquals(Role.PATIENT, user?.role)
    }

    @Test
    fun staffLogin_succeedsForDoctorRole() = runBlocking {
        fakeApiService.loginStaffResponse = {
            LoginResponse(
                access_token = "valid-token-dr",
                role = "doctor",
                account_type = "staff",
                userId = "dr_smith",
                name = "Dr. Smith",
                must_change_password = false,
                token_type = "bearer"
            )
        }

        val result = authRepository.loginStaff(mapOf("username" to "dr_smith", "password" to "doctor123"))

        assertTrue("Staff login should succeed for doctor credentials", result.isSuccess)
        val user = result.getOrNull()
        assertEquals(Role.DOCTOR, user?.role)
    }
}
