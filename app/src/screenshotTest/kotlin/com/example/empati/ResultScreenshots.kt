package com.example.empati

import com.android.tools.screenshot.PreviewTest
import androidx.compose.runtime.Composable
import com.example.empati.ui.screens.IntegratedResultContent
import com.example.empati.ui.screens.ModuleResultContent

@Composable
private fun ModulePreview(presentation: com.example.empati.data.presentation.UserModulePresentation) {
    PreviewTheme {
        ModuleResultContent(
            presentation = presentation,
            onBack = {},
            onProceed = {},
            onAskAi = {}
        )
    }
}

@Composable
private fun IntegratedPreview(presentation: com.example.empati.data.presentation.UserIntegratedPresentation) {
    PreviewTheme {
        IntegratedResultContent(
            presentation = presentation,
            onBack = {},
            onSave = {},
            onAskAi = {}
        )
    }
}

@PreviewTest
@PhoneTallLightDark
@Composable
fun ResultModuleHypertensionAttention() = ModulePreview(Fixtures.hypertensionAttention)

@PreviewTest
@PhoneTallLightDark
@Composable
fun ResultModuleHypertensionCalm() = ModulePreview(Fixtures.hypertensionPositive)

@PreviewTest
@PhoneTallLightDark
@Composable
fun ResultModuleWellbeingWithSupport() = ModulePreview(Fixtures.wellbeingWithSupport)

@PreviewTest
@PhoneTallLightDark
@Composable
fun ResultModuleIncomplete() = ModulePreview(Fixtures.wellbeingIncomplete)

@PreviewTest
@PhoneTallLightDark
@Composable
fun ResultIntegratedAttention() = IntegratedPreview(Fixtures.integratedAttention)

@PreviewTest
@PhoneTallLightDark
@Composable
fun ResultIntegratedCalm() = IntegratedPreview(Fixtures.integratedPositive)

@PreviewTest
@LargeTextLightDark
@Composable
fun ResultModuleLargeText() = ModulePreview(Fixtures.hypertensionAttention)

@PreviewTest
@LargeTextLightDark
@Composable
fun ResultIntegratedLargeText() = IntegratedPreview(Fixtures.integratedAttention)
