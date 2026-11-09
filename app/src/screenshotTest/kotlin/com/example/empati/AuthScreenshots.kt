package com.example.empati

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.tools.screenshot.PreviewTest
import com.example.empati.ui.screens.AuthFormState
import com.example.empati.ui.screens.AuthMode
import com.example.empati.ui.screens.AuthScreenContent

@Composable
private fun AuthPreview(
    mode: AuthMode = AuthMode.LOGIN,
    accountId: String = "ratna.andari@empati.id",
    pin: String = "123456",
    error: String? = null,
    submitting: Boolean = false,
    configure: AuthFormState.() -> Unit = {}
) {
    PreviewTheme {
        val form = remember {
            AuthFormState(
                initialMode = mode,
                initialAccountId = accountId,
                initialPin = pin,
                initialError = error
            ).apply(configure)
        }
        AuthScreenContent(form = form, submitting = submitting, onSubmit = {})
    }
}

@PreviewTest
@PhoneLightDark
@Composable
fun AuthLogin() = AuthPreview()

@PreviewTest
@PhoneLightDark
@Composable
fun AuthLoginEmpty() = AuthPreview(accountId = "", pin = "")

@PreviewTest
@PhoneLightDark
@Composable
fun AuthLoginInvalidIdWithError() = AuthPreview(
    accountId = "ab",
    pin = "123",
    error = "ID akun atau PIN tidak sesuai. Periksa kembali lalu coba lagi."
)

@PreviewTest
@PhoneLightDark
@Composable
fun AuthLoginSubmitting() = AuthPreview(submitting = true)

@PreviewTest
@PhoneTallLightDark
@Composable
fun AuthRegisterFilled() = AuthPreview(
    mode = AuthMode.REGISTER,
    accountId = "0812-3456-7890",
    pin = "rahasia123"
) {
    fullName = "Ratna Andari"
    category = "Karyawan / Pekerja"
    domicile = "Kota Bandung"
    occupation = "Pekerja kantoran"
}

@PreviewTest
@PhoneTallLightDark
@Composable
fun AuthRegisterEmpty() = AuthPreview(mode = AuthMode.REGISTER, accountId = "", pin = "")

@PreviewTest
@LargeTextLightDark
@Composable
fun AuthRegisterLargeText() = AuthPreview(
    mode = AuthMode.REGISTER,
    accountId = "ratna.andari@empati.id",
    pin = "rahasia123"
)
