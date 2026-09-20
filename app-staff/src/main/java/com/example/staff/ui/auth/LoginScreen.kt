package com.example.staff.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.BuildConfig
import com.example.domain.model.Role
import com.example.ui.components.AppLogo

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onDemoLogin: (Role) -> Unit = {},
    onLoginSuccess: (Role) -> Unit,
    onErrorDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    // Navigate when user is successfully logged in
    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            onLoginSuccess(user.role)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            AppLogo(
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chettinad Care Staff",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Clinical Hospital Management Suite",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Staff ID (e.g. dr_smith)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isLoading,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("Password / PIN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !uiState.isLoading,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onLogin(username, pin) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isLoading && username.isNotBlank() && pin.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign In")
                }
            }

            // Flavor-gated demo shortcuts strictly under demo builds
            if (BuildConfig.IS_DEMO) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Demo Environment Quick Sign-In:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onDemoLogin(Role.DOCTOR) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Doctor", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = { onDemoLogin(Role.NURSE) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nurse", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = { onDemoLogin(Role.ADMIN) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Admin", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Chettinad Hospital and Research Institute",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Chettinad Health City • Kelambakkam",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
