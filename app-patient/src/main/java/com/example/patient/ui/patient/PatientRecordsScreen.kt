package com.example.patient.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PatientRecordsScreen(
    patientId: String,
    uiState: PatientRecordsUiState,
    onLoadRecords: (String) -> Unit
) {
    LaunchedEffect(patientId) {
        onLoadRecords(patientId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.dossier == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.error != null && uiState.dossier == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = uiState.error, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onLoadRecords(patientId) }) { Text("Retry") }
            }
        } else {
            val dossier = uiState.dossier
            val allPastNotes = dossier?.pastNotes ?: emptyList()

            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Chettinad Care",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "My Medical Records",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        if (dossier != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "MRN: ${dossier.patient.mrn} • ${dossier.patient.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(24.dp)
                ) {
                    // Hospital Notes Section
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Hospital Records & History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (allPastNotes.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "No clinical consultation records found on the hospital server.",
                                    modifier = Modifier.padding(24.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(allPastNotes) { note ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            note.diagnosis,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            note.createdAt.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Treatment Plan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(note.plan, style = MaterialTheme.typography.bodyMedium)
                                    if (note.followUpInstructions.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "Follow-up: ${note.followUpInstructions}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
