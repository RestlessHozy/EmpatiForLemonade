package com.example.empati.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import com.example.empati.EmpatiViewModel
import com.example.empati.data.UserProfile
import com.example.empati.data.auth.AsnNipValidator
import com.example.empati.data.auth.NipValidationResult
import com.example.empati.data.auth.UserGenderOption
import com.example.empati.data.auth.UserOccupationCategory
import com.example.empati.ui.components.ChoiceChip
import com.example.empati.ui.components.EmpatiLogo
import com.example.empati.ui.components.EmpatiTextField
import com.example.empati.ui.components.PrimaryButton
import com.example.empati.ui.components.Reveal
import com.example.empati.ui.components.ScreenFrame
import com.example.empati.ui.components.SectionLabel
import com.example.empati.ui.components.SegmentedSwitch
import com.example.empati.ui.components.SoftDivider
import com.example.empati.ui.components.SoftNotice
import com.example.empati.ui.components.TonalCard
import com.example.empati.ui.components.VisibilityToggle
import com.example.empati.ui.components.errorTone
import com.example.empati.ui.theme.DisplayFont
import com.example.empati.ui.theme.EmpatiMetricStyle
import com.example.empati.ui.theme.EmpatiMotion
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.empati
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

/**
 * UI-only state holder for the sign-in / sign-up form. All business rules (credential checks,
 * persistence, session entry) stay in [EmpatiViewModel]; this class only models what the user is typing.
 */
@Stable
class AuthFormState(
    initialMode: AuthMode = AuthMode.LOGIN,
    initialAccountId: String = "",
    initialPin: String = "",
    initialError: String? = null
) {
    var mode by mutableStateOf(initialMode)
    var accountId by mutableStateOf(initialAccountId)
    var pin by mutableStateOf(initialPin)
    var pinVisible by mutableStateOf(false)
    var fullName by mutableStateOf("")
    var gender by mutableStateOf(UserGenderOption.FEMALE.label)
    var category by mutableStateOf(UserOccupationCategory.GENERAL.label)
    var domicile by mutableStateOf("")
    var occupation by mutableStateOf("")
    var errorMessage by mutableStateOf(initialError)

    val trimmedId: String get() = accountId.trim()

    val identity: NipValidationResult? by derivedStateOf {
        trimmedId.takeIf { it.isNotEmpty() }?.let { AsnNipValidator.validateGeneralIdentity(it) }
    }

    /** Last verified identity, kept so the confirmation can fade out instead of vanishing. */
    var lastValidIdentity by mutableStateOf<NipValidationResult.Valid?>(null)
        private set

    init {
        (identity as? NipValidationResult.Valid)?.let { lastValidIdentity = it }
    }

    fun updateAccountId(raw: String) {
        accountId = raw.take(50)
        errorMessage = null
        (identity as? NipValidationResult.Valid)?.let { lastValidIdentity = it }
    }

    fun switchTo(newMode: AuthMode) {
        if (newMode == mode) return
        mode = newMode
        errorMessage = null
    }
}

@Composable
fun AuthScreen(vm: EmpatiViewModel, modifier: Modifier = Modifier) {
    val form = remember { AuthFormState() }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthScreenContent(
        form = form,
        submitting = submitting,
        onSubmit = {
            scope.launch {
                submitting = true
                // Let the loading state paint before the (CPU-heavy) credential hashing runs.
                withFrameNanos { }
                withFrameNanos { }
                form.errorMessage = if (form.mode == AuthMode.LOGIN) {
                    vm.loginAsn(form.trimmedId, form.pin)
                } else if (form.fullName.isBlank() || form.domicile.isBlank() || form.occupation.isBlank()) {
                    "Lengkapi nama, domisili atau tempat aktivitas, dan pekerjaan atau peran Anda."
                } else {
                    vm.registerAsn(
                        UserProfile(
                            name = form.fullName.trim(),
                            nip = form.trimmedId,
                            instansi = form.domicile.trim(),
                            jabatan = form.occupation.trim(),
                            jenisAsn = form.category,
                            golongan = form.gender
                        ),
                        form.pin
                    )
                }
                submitting = false
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AuthScreenContent(
    form: AuthFormState,
    submitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isLogin = form.mode == AuthMode.LOGIN
    val identity = form.identity

    ScreenFrame(modifier = modifier) {
        Spacer(Modifier.height(EmpatiSpace.xl))

        Reveal(index = 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmpatiLogo(size = 44.dp, animated = true)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "SRYA Emora",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = DisplayFont,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(Modifier.height(EmpatiSpace.xxl))

        Reveal(index = 1) {
            AnimatedContent(
                targetState = form.mode,
                transitionSpec = {
                    fadeIn(EmpatiMotion.standard()) togetherWith fadeOut(EmpatiMotion.quick())
                },
                label = "authHeadline"
            ) { mode ->
                Column {
                    Text(
                        text = if (mode == AuthMode.LOGIN) "Selamat datang kembali" else "Mari mulai bersama",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(EmpatiSpace.sm))
                    Text(
                        text = if (mode == AuthMode.LOGIN)
                            "Masuk untuk melanjutkan skrining dan obrolan Anda dengan aman."
                        else
                            "Ceritakan sedikit tentang diri Anda agar pendampingan terasa lebih dekat dan personal.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(EmpatiSpace.xl))

        Reveal(index = 2) {
            SegmentedSwitch(
                options = listOf("Masuk", "Daftar"),
                selectedIndex = if (isLogin) 0 else 1,
                onSelect = { form.switchTo(if (it == 0) AuthMode.LOGIN else AuthMode.REGISTER) }
            )
        }

        Spacer(Modifier.height(EmpatiSpace.md))

        Reveal(index = 3) {
            TonalCard(
                elevated = true,
                contentPadding = PaddingValues(EmpatiSpace.lg),
                modifier = Modifier.fillMaxWidth()
            ) {
                EmpatiTextField(
                    value = form.accountId,
                    onValueChange = form::updateAccountId,
                    label = "ID akun",
                    placeholder = "Email, nomor HP, username, NIK, atau NIP",
                    leadingIcon = Icons.Rounded.AlternateEmail,
                    valueStyle = EmpatiMetricStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    errorText = (identity as? NipValidationResult.Invalid)
                        ?.takeIf { form.trimmedId.length >= 4 }?.reason,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    contentType = if (isLogin) ContentType.Username else ContentType.NewUsername
                )

                AnimatedVisibility(
                    visible = identity is NipValidationResult.Valid,
                    enter = fadeIn(EmpatiMotion.quick()) + expandVertically(EmpatiMotion.standard()),
                    exit = fadeOut(EmpatiMotion.quick()) + shrinkVertically(EmpatiMotion.quick())
                ) {
                    val valid = form.lastValidIdentity
                    Column {
                        Spacer(Modifier.height(EmpatiSpace.xs))
                        if (valid != null) {
                            val meta = valid.metadata
                            SoftNotice(
                                text = if (meta.identityKind == "NIP 18 Digit") {
                                    "Terdeteksi ${meta.identityKind} · lahir ${meta.birthDateFormatted} · ${meta.genderLabel}"
                                } else {
                                    "Format terverifikasi · ${meta.identityKind}"
                                },
                                icon = Icons.Rounded.CheckCircle,
                                tone = MaterialTheme.empati.calm,
                                liveRegion = true
                            )
                        }
                    }
                }

                Spacer(Modifier.height(EmpatiSpace.sm))

                EmpatiTextField(
                    value = form.pin,
                    onValueChange = {
                        form.pin = it
                        form.errorMessage = null
                    },
                    label = "PIN atau kata sandi",
                    supportingText = "Minimal 6 karakter. Tempel dari pengelola kata sandi diperbolehkan.",
                    leadingIcon = Icons.Rounded.Lock,
                    visualTransformation = if (form.pinVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isLogin) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        if (isLogin) onSubmit()
                    }),
                    contentType = if (isLogin) ContentType.Password else ContentType.NewPassword,
                    trailing = {
                        VisibilityToggle(
                            visible = form.pinVisible,
                            onToggle = { form.pinVisible = !form.pinVisible },
                            subject = "PIN"
                        )
                    }
                )

                // Progressive disclosure: registration details unfold only when needed.
                AnimatedVisibility(
                    visible = !isLogin,
                    enter = fadeIn(EmpatiMotion.standard()) + expandVertically(EmpatiMotion.expressive()),
                    exit = fadeOut(EmpatiMotion.quick()) + shrinkVertically(EmpatiMotion.standard())
                ) {
                    Column {
                        Spacer(Modifier.height(EmpatiSpace.lg))
                        SoftDivider()
                        Spacer(Modifier.height(EmpatiSpace.lg))
                        Text(
                            text = "Sedikit tentang Anda",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(EmpatiSpace.xxs))
                        Text(
                            text = "Tersimpan aman di akun Anda dan bisa diperbarui kapan saja.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(EmpatiSpace.md))
                        EmpatiTextField(
                            value = form.fullName,
                            onValueChange = {
                                form.fullName = it
                                form.errorMessage = null
                            },
                            label = "Nama lengkap atau panggilan",
                            leadingIcon = Icons.Rounded.Person,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            contentType = ContentType.PersonFullName
                        )

                        Spacer(Modifier.height(EmpatiSpace.lg))
                        SectionLabel("Jenis kelamin")
                        Spacer(Modifier.height(EmpatiSpace.xs))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs),
                            modifier = Modifier.selectableGroup()
                        ) {
                            UserGenderOption.entries.forEach { option ->
                                ChoiceChip(
                                    label = option.label,
                                    selected = form.gender == option.label,
                                    onClick = { form.gender = option.label }
                                )
                            }
                        }

                        Spacer(Modifier.height(EmpatiSpace.lg))
                        SectionLabel("Latar belakang atau aktivitas utama")
                        Spacer(Modifier.height(EmpatiSpace.xs))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs),
                            verticalArrangement = Arrangement.spacedBy(EmpatiSpace.xs / 2),
                            modifier = Modifier.selectableGroup()
                        ) {
                            UserOccupationCategory.entries.forEach { option ->
                                ChoiceChip(
                                    label = option.label,
                                    selected = form.category == option.label,
                                    onClick = { form.category = option.label }
                                )
                            }
                        }

                        Spacer(Modifier.height(EmpatiSpace.lg))
                        EmpatiTextField(
                            value = form.domicile,
                            onValueChange = {
                                form.domicile = it
                                form.errorMessage = null
                            },
                            label = "Kota domisili atau tempat beraktivitas",
                            leadingIcon = Icons.Rounded.Business,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Spacer(Modifier.height(EmpatiSpace.sm))
                        EmpatiTextField(
                            value = form.occupation,
                            onValueChange = {
                                form.occupation = it
                                form.errorMessage = null
                            },
                            label = "Pekerjaan atau peran sehari-hari",
                            leadingIcon = Icons.Rounded.WorkOutline,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = form.errorMessage != null,
            enter = fadeIn(EmpatiMotion.quick()) + expandVertically(EmpatiMotion.standard()),
            exit = fadeOut(EmpatiMotion.quick()) + shrinkVertically(EmpatiMotion.quick())
        ) {
            Column {
                Spacer(Modifier.height(EmpatiSpace.md))
                SoftNotice(
                    text = form.errorMessage.orEmpty(),
                    icon = Icons.Rounded.ErrorOutline,
                    tone = errorTone(),
                    title = "Belum bisa melanjutkan",
                    liveRegion = true
                )
            }
        }

        Spacer(Modifier.height(EmpatiSpace.md))

        Reveal(index = 4) {
            SoftNotice(
                text = "Kata sandi Anda dienkripsi dan jawaban skrining dipisahkan dari identitas asli menggunakan kode samaran satu arah.",
                icon = Icons.Rounded.VerifiedUser,
                tone = MaterialTheme.empati.presence
            )
        }

        Spacer(Modifier.height(EmpatiSpace.xl))

        Reveal(index = 5) {
            PrimaryButton(
                text = when {
                    submitting -> "Memeriksa…"
                    isLogin -> "Masuk"
                    else -> "Daftar dan masuk"
                },
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                loading = submitting,
                enabled = !submitting
            )
        }

        Spacer(Modifier.height(EmpatiSpace.xxl))
    }
}
