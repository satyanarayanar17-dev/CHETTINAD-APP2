package com.example.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService {

    private val tag = "FirestoreService"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val db = FirebaseFirestore.getInstance()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = settings
            } catch (e: Exception) {
                Log.d(tag, "Firestore settings already applied or cannot change: ${e.message}")
            }
            db
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore instance error: ${e.message}")
            null
        }
    }

    suspend fun saveUserProfile(profile: FirestoreUserProfile): Result<Unit> {
        return try {
            val db = firestore ?: return Result.failure(IllegalStateException("Firestore not available"))
            db.collection("users")
                .document(profile.uid)
                .set(profile.toMap())
                .await()
            Log.d(tag, "User profile saved to Firestore: ${profile.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save user profile", e)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<FirestoreUserProfile?> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing user profile", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    val profile = FirestoreUserProfile(
                        uid = snapshot.id,
                        email = data?.get("email") as? String ?: "",
                        displayName = data?.get("displayName") as? String ?: "",
                        photoUrl = data?.get("photoUrl") as? String ?: "",
                        role = data?.get("role") as? String ?: "patient",
                        department = data?.get("department") as? String ?: "General Medicine",
                        staffOrPatientId = data?.get("staffOrPatientId") as? String ?: "",
                        hospitalId = data?.get("hospitalId") as? String ?: "CHETTINAD-HOSPITAL",
                        lastLoginAt = (data?.get("lastLoginAt") as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveClinicalRecord(record: FirestoreClinicalRecord): Result<Unit> {
        return try {
            val db = firestore ?: return Result.failure(IllegalStateException("Firestore not available"))
            val docRef = if (record.recordId.isNotBlank()) {
                db.collection("clinical_records").document(record.recordId)
            } else {
                db.collection("clinical_records").document()
            }
            val finalRecord = if (record.recordId.isBlank()) record.copy(recordId = docRef.id) else record
            docRef.set(finalRecord.toMap()).await()
            Log.d(tag, "Clinical record saved to Firestore: ${finalRecord.recordId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save clinical record", e)
            Result.failure(e)
        }
    }

    fun observePatientRecords(patientId: String): Flow<List<FirestoreClinicalRecord>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection("clinical_records")
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing patient records", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    FirestoreClinicalRecord(
                        recordId = doc.id,
                        patientId = data["patientId"] as? String ?: "",
                        patientName = data["patientName"] as? String ?: "",
                        doctorId = data["doctorId"] as? String ?: "",
                        doctorName = data["doctorName"] as? String ?: "",
                        department = data["department"] as? String ?: "General Medicine",
                        subjective = data["subjective"] as? String ?: "",
                        objective = data["objective"] as? String ?: "",
                        assessment = data["assessment"] as? String ?: "",
                        plan = data["plan"] as? String ?: "",
                        diagnosis = data["diagnosis"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(records)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveTriageVitals(triage: FirestoreTriageRecord): Result<Unit> {
        return try {
            val db = firestore ?: return Result.failure(IllegalStateException("Firestore not available"))
            val docRef = if (triage.triageId.isNotBlank()) {
                db.collection("triage_queue").document(triage.triageId)
            } else {
                db.collection("triage_queue").document()
            }
            val finalTriage = if (triage.triageId.isBlank()) triage.copy(triageId = docRef.id) else triage
            docRef.set(finalTriage.toMap()).await()
            Log.d(tag, "Triage record saved to Firestore: ${finalTriage.triageId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save triage record", e)
            Result.failure(e)
        }
    }

    fun observeTriageQueue(): Flow<List<FirestoreTriageRecord>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection("triage_queue")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing triage queue", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    FirestoreTriageRecord(
                        triageId = doc.id,
                        patientId = data["patientId"] as? String ?: "",
                        patientName = data["patientName"] as? String ?: "",
                        nurseId = data["nurseId"] as? String ?: "",
                        systolic = (data["systolic"] as? Number)?.toInt() ?: 120,
                        diastolic = (data["diastolic"] as? Number)?.toInt() ?: 80,
                        pulseRate = (data["pulseRate"] as? Number)?.toInt() ?: 75,
                        temperature = (data["temperature"] as? Number)?.toDouble() ?: 98.6,
                        spo2 = (data["spo2"] as? Number)?.toInt() ?: 98,
                        painScore = (data["painScore"] as? Number)?.toInt() ?: 0,
                        acuity = data["acuity"] as? String ?: "P3",
                        chiefComplaint = data["chiefComplaint"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun savePrescription(prescription: FirestorePrescriptionRecord): Result<Unit> {
        return try {
            val db = firestore ?: return Result.failure(IllegalStateException("Firestore not available"))
            val docRef = if (prescription.prescriptionId.isNotBlank()) {
                db.collection("prescriptions").document(prescription.prescriptionId)
            } else {
                db.collection("prescriptions").document()
            }
            val finalPrescription = if (prescription.prescriptionId.isBlank()) prescription.copy(prescriptionId = docRef.id) else prescription
            docRef.set(finalPrescription.toMap()).await()
            Log.d(tag, "Prescription saved to Firestore: ${finalPrescription.prescriptionId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save prescription", e)
            Result.failure(e)
        }
    }
}
