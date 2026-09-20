package com.example.patient.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
    onDemoLogin: () -> Unit = {},
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
                text = "Chettinad Care",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Patient Health Portal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Patient ID (e.g. PT1001)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isLoading,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN / Password") },
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

            // Demo quick-login button strictly flavor-gated
            if (BuildConfig.IS_DEMO) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onDemoLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Demo Patient Sign-In (PT1001)")
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
