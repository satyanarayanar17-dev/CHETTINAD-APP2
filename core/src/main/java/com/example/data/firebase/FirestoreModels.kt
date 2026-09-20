package com.example.data.firebase

import androidx.annotation.Keep

@Keep
data class FirestoreUserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: String = "patient", // patient, doctor, nurse, admin
    val department: String = "General Medicine",
    val staffOrPatientId: String = "",
    val hospitalId: String = "CHETTINAD-HOSPITAL",
    val lastLoginAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "photoUrl" to photoUrl,
        "role" to role,
        "department" to department,
        "staffOrPatientId" to staffOrPatientId,
        "hospitalId" to hospitalId,
        "lastLoginAt" to lastLoginAt
    )
}

@Keep
data class FirestoreClinicalRecord(
    val recordId: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val department: String = "General Medicine",
    val subjective: String = "",
    val objective: String = "",
    val assessment: String = "",
    val plan: String = "",
    val diagnosis: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "recordId" to recordId,
        "patientId" to patientId,
        "patientName" to patientName,
        "doctorId" to doctorId,
        "doctorName" to doctorName,
        "department" to department,
        "subjective" to subjective,
        "objective" to objective,
        "assessment" to assessment,
        "plan" to plan,
        "diagnosis" to diagnosis,
        "timestamp" to timestamp
    )
}

@Keep
data class FirestoreTriageRecord(
    val triageId: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val nurseId: String = "",
    val systolic: Int = 120,
    val diastolic: Int = 80,
    val pulseRate: Int = 75,
    val temperature: Double = 98.6,
    val spo2: Int = 98,
    val painScore: Int = 0,
    val acuity: String = "P3", // P1, P2, P3
    val chiefComplaint: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "triageId" to triageId,
        "patientId" to patientId,
        "patientName" to patientName,
        "nurseId" to nurseId,
        "systolic" to systolic,
        "diastolic" to diastolic,
        "pulseRate" to pulseRate,
        "temperature" to temperature,
        "spo2" to spo2,
        "painScore" to painScore,
        "acuity" to acuity,
        "chiefComplaint" to chiefComplaint,
        "timestamp" to timestamp
    )
}

@Keep
data class FirestorePrescriptionRecord(
    val prescriptionId: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val medications: List<Map<String, Any?>> = emptyList(),
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "prescriptionId" to prescriptionId,
        "patientId" to patientId,
        "doctorId" to doctorId,
        "doctorName" to doctorName,
        "medications" to medications,
        "notes" to notes,
        "timestamp" to timestamp
    )
}
