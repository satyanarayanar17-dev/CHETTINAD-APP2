package com.example.patient.ui.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysElements() {
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onDemoLogin = {},
                onLoginSuccess = {},
                onErrorDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Chettinad Care").assertExists()
        composeTestRule.onNodeWithText("Patient Health Portal").assertExists()
        composeTestRule.onNodeWithText("Patient ID (e.g. PT1001)").assertExists()
        composeTestRule.onNodeWithText("PIN / Password").assertExists()
        composeTestRule.onNodeWithText("Sign In").assertExists()
    }
}
