package com.example.vokasilearning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vokasilearning.screens.LoginScreen
import com.example.vokasilearning.ui.VokasiBottomNav
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumentation smoke checks; requires an actual Android device/emulator. */
@RunWith(AndroidJUnit4::class)
class ResponsiveLoginNavigationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun fullNameInputPreservesSpacesAsTyped() {
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(onLogin = { _, _ -> false })
            }
        }
        val nameField = composeRule.onAllNodes(hasSetTextAction())[0]
        nameField.performTextInput("abdi rizal saputra")
        nameField.assertTextEquals("abdi rizal saputra")
    }

    @Test fun fiveAdminTabsAreAccessibleInNarrowViewport() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    VokasiBottomNav(
                        items = listOf(
                            Triple(Page.ADMIN_HOME, "Ringkasan", ""),
                            Triple(Page.ADMIN_STUDENTS, "Siswa", ""),
                            Triple(Page.ADMIN_MONITOR, "Nilai", ""),
                            Triple(Page.ADMIN_QUESTIONS, "Soal", ""),
                            Triple(Page.ADMIN_SCHOOL, "Sekolah", "")
                        ),
                        currentPage = Page.ADMIN_HOME,
                        onNavigate = {}
                    )
                }
            }
        }
        listOf("Ringkasan", "Siswa", "Nilai", "Soal", "Sekolah").forEach { label ->
            composeRule.onNodeWithText(label).assertExists()
        }
    }
}
