package com.example.vokasilearning

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import com.example.vokasilearning.screens.AdminAddQuestionScreen
import com.example.vokasilearning.screens.AdminStudentsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/** Run with connectedDebugAndroidTest on a device/emulator when available. */
@RunWith(AndroidJUnit4::class)
class AdminUiInteractionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun studentDirectoryOpensMajorThenClassThenStudent() {
        val registered = listOf(
            Student("nabila", "demo", "Nabila Putri", Classroom("X TE 1", "Teknik Elektronika"))
        )
        composeRule.setContent {
            MaterialTheme {
                AdminStudentsScreen(students = registered, attempts = emptyList())
            }
        }
        composeRule.onNodeWithText("Lihat Kelas").performClick()
        composeRule.onNodeWithText("Pilih Kelas").performClick()
        composeRule.onNodeWithText("Detail").performClick()
        composeRule.onNodeWithText("Status nilai sekolah").assertExists()
        composeRule.onNodeWithText("Belum tersinkron").assertExists()
        composeRule.onNodeWithText("‹ Kembali").performClick()
        composeRule.onNodeWithText("Pilih Siswa").assertExists()
    }

    @Test fun questionEditorWarnsBeforeDiscardingUnfinishedDraft() {
        composeRule.setContent {
            MaterialTheme {
                AdminAddQuestionScreen(
                    onBack = {},
                    onSave = { _, _, _, _, _, _ -> null }
                )
            }
        }
        composeRule.onNodeWithText("Tulis pertanyaan yang jelas")
            .performScrollTo()
            .performTextInput("12 × 4 = ...")
        composeRule.onNodeWithText("‹ Kembali ke Bank Soal")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Batalkan soal?").assertExists()
        composeRule.onNodeWithText("Lanjut Edit").performClick()
        composeRule.onNodeWithText("Batalkan soal?").assertDoesNotExist()
    }
}
