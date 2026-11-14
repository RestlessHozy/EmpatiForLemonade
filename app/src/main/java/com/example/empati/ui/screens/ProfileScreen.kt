package com.example.empati.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import com.example.empati.BuildConfig
import com.example.empati.EmpatiViewModel
import com.example.empati.MainActivity
import com.example.empati.data.UserProfile
import com.example.empati.data.lemonade.LemonadeStatus
import com.example.empati.data.lemonade.LemonadeStatusPresenter
import com.example.empati.data.initials
import com.example.empati.ui.components.EmpatiAvatar
import com.example.empati.ui.components.EmpatiSurfaces
import com.example.empati.ui.components.EmpatiTextField
import com.example.empati.ui.components.GhostButton
import com.example.empati.ui.components.Reveal
import com.example.empati.ui.components.ScreenFrame
import com.example.empati.ui.components.ScreenInsets
import com.example.empati.ui.components.SectionChip
import com.example.empati.ui.components.SectionLabel
import com.example.empati.ui.components.SoftDivider
import com.example.empati.ui.components.SoftIconTile
import com.example.empati.ui.components.TonalCard
import com.example.empati.ui.components.focusHalo
import com.example.empati.ui.components.pressScale
import com.example.empati.ui.theme.EmpatiMetricStyle
import com.example.empati.ui.theme.EmpatiSpace

/** Editable (or locked) profile fields. [NIP] is the immutable account identifier. */
enum class ProfileField(val label: String, val icon: ImageVector) {
    NAME("Nama lengkap", Icons.Rounded.Person),
    NIP("ID akun (email / no. HP / ID)", Icons.Rounded.Badge),
    INSTANSI("Domisili / tempat aktivitas", Icons.Rounded.Business),
    JABATAN("Pekerjaan / peran sehari-hari", Icons.Rounded.WorkOutline)
}

// ═══════════════════════════════════════════════════════════════════════════
//  Stateful entry point (ViewModel contract unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ProfileScreen(vm: EmpatiViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { vm.refreshLemonadeStatus() }
    ProfileScreenContent(
        profile = vm.profile,
        onUpdateProfile = vm::updateProfile,
        onLogout = vm::logoutAsn,
        onClearAllData = vm::clearAllData,
        modifier = modifier,
        lemonadeStatus = vm.lemonadeStatus,
        lemonadeError = vm.lemonadeError,
        onRefreshLemonade = vm::refreshLemonadeStatus
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Stateless content (dialog state lives here so the page renders without a ViewModel)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * @param initialEditingField opens the edit dialog for this field on first composition (useful for screenshots).
 * @param initialShowDeleteConfirm opens the "delete all data" confirmation on first composition.
 */
@Composable
fun ProfileScreenContent(
    profile: UserProfile,
    onUpdateProfile: (UserProfile) -> Unit,
    onLogout: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier,
    initialEditingField: ProfileField? = null,
    initialShowDeleteConfirm: Boolean = false,
    lemonadeStatus: LemonadeStatus? = null,
    lemonadeError: String? = null,
    onRefreshLemonade: () -> Unit = {}
) {
    var editingField by remember { mutableStateOf(initialEditingField) }
    var editValue by remember {
        mutableStateOf(initialEditingField?.valueIn(profile).orEmpty())
    }
    var showDeleteConfirm by remember { mutableStateOf(initialShowDeleteConfirm) }

    fun startEdit(field: ProfileField) {
        editingField = field
        editValue = field.valueIn(profile)
    }

    ScreenFrame(modifier = modifier, insets = ScreenInsets.StatusBar) {
        Spacer(Modifier.height(EmpatiSpace.md))

        Reveal(modifier = Modifier.fillMaxWidth(), index = 0) {
            IdentityHeader(profile = profile)
        }

        Spacer(Modifier.height(EmpatiSpace.xxl))

        Reveal(modifier = Modifier.fillMaxWidth(), index = 1) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionLabel(
                    text = "Data profil",
                    modifier = Modifier.padding(start = EmpatiSpace.xs)
                )
                Spacer(Modifier.height(EmpatiSpace.sm))
                TonalCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = EmpatiSpace.xs)
                ) {
                    ProfileRow(
                        icon = ProfileField.NAME.icon,
                        label = ProfileField.NAME.label,
                        value = profile.name,
                        onEdit = { startEdit(ProfileField.NAME) }
                    )
                    RowDivider()
                    ProfileRow(
                        icon = ProfileField.NIP.icon,
                        label = ProfileField.NIP.label,
                        value = profile.nip,
                        // NIP adalah identitas login dan salt kredensial — tidak dapat diubah setelah registrasi.
                        onEdit = null,
                        isIdentifier = true
                    )
                    RowDivider()
                    ProfileRow(
                        icon = ProfileField.INSTANSI.icon,
                        label = ProfileField.INSTANSI.label,
                        value = profile.instansi,
                        onEdit = { startEdit(ProfileField.INSTANSI) }
                    )
                    RowDivider()
                    ProfileRow(
                        icon = ProfileField.JABATAN.icon,
                        label = ProfileField.JABATAN.label,
                        value = profile.jabatan,
                        onEdit = { startEdit(ProfileField.JABATAN) }
                    )
                }
            }
        }

        Spacer(Modifier.height(EmpatiSpace.xl))

        Reveal(modifier = Modifier.fillMaxWidth(), index = 2) {
            LemonadeStatusSection(
                status = lemonadeStatus,
                error = lemonadeError,
                onRefresh = onRefreshLemonade
            )
        }

        Spacer(Modifier.height(EmpatiSpace.xl))

        Reveal(modifier = Modifier.fillMaxWidth(), index = 3) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionLabel(
                    text = "Pengaturan dan keamanan",
                    modifier = Modifier.padding(start = EmpatiSpace.xs)
                )
                Spacer(Modifier.height(EmpatiSpace.sm))
                TonalCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = EmpatiSpace.xs)
                ) {
                    ProfileRow(
                        icon = Icons.Rounded.Lock,
                        label = "Perlindungan layar",
                        value = if (MainActivity.ENABLE_SCREENSHOT_PROTECTION)
                            "Tangkapan layar diblokir aktif"
                        else
                            "Tangkapan & perekaman layar diizinkan",
                        onEdit = null,
                        showTrailing = false,
                        warmTile = true
                    )
                    RowDivider()
                    ProfileRow(
                        icon = Icons.Rounded.Notifications,
                        label = "Pengingat pemeriksaan",
                        value = "Nonaktif",
                        onEdit = null,
                        showTrailing = false,
                        warmTile = true
                    )
                    RowDivider()
                    ProfileRow(
                        icon = Icons.Rounded.Info,
                        label = "Tentang Empati",
                        value = "Versi ${BuildConfig.VERSION_NAME} · Inklusif semua kalangan",
                        onEdit = null,
                        showTrailing = false,
                        warmTile = true
                    )
                }
            }
        }

        Spacer(Modifier.height(EmpatiSpace.xl))

        Reveal(modifier = Modifier.fillMaxWidth(), index = 3) {
            Column(modifier = Modifier.fillMaxWidth()) {
                GhostButton(
                    text = "Keluar sesi / ganti akun",
                    onClick = onLogout
                )
                Spacer(Modifier.height(EmpatiSpace.xs))
                DeleteDataRow(onClick = { showDeleteConfirm = true })
            }
        }

        Spacer(Modifier.height(EmpatiSpace.huge))
    }

    editingField?.let { field ->
        EditFieldDialog(
            field = field,
            value = editValue,
            onValueChange = { editValue = it },
            onSave = {
                onUpdateProfile(applyEdit(profile, field, editValue))
                editingField = null
            },
            onDismiss = { editingField = null }
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            onConfirm = {
                onClearAllData()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

private fun ProfileField.valueIn(profile: UserProfile): String = when (this) {
    ProfileField.NAME -> profile.name
    ProfileField.NIP -> profile.nip
    ProfileField.INSTANSI -> profile.instansi
    ProfileField.JABATAN -> profile.jabatan
}

private fun applyEdit(profile: UserProfile, field: ProfileField, value: String): UserProfile = when (field) {
    ProfileField.NAME -> profile.copy(name = value)
    ProfileField.NIP -> profile // NIP immutable
    ProfileField.INSTANSI -> profile.copy(instansi = value)
    ProfileField.JABATAN -> profile.copy(jabatan = value)
}

private fun String.lowerFirst(): String = replaceFirstChar { it.lowercase() }

// ── Identity ────────────────────────────────────────────────────────────────

@Composable
private fun IdentityHeader(profile: UserProfile) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profil dan privasi",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(EmpatiSpace.lg))
        EmpatiAvatar(initials = profile.initials(), size = 96.dp)
        Spacer(Modifier.height(EmpatiSpace.md))
        Text(
            text = profile.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(EmpatiSpace.xxs))
        Text(
            text = "${profile.jabatan} · ${profile.instansi}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(EmpatiSpace.sm))
        SectionChip(text = "${profile.golongan} · ${profile.jenisAsn}")
    }
}

// ── Rows ────────────────────────────────────────────────────────────────────

private val RowHorizontalPadding = 20.dp
private val RowTileSize = 44.dp
private val RowTileGap = 16.dp

@Composable
private fun RowDivider() {
    SoftDivider(
        modifier = Modifier.padding(
            start = RowHorizontalPadding + RowTileSize + RowTileGap,
            end = RowHorizontalPadding
        )
    )
}

/**
 * Icon tile · label · value · optional trailing state.
 * [onEdit] != null → tappable "edit" row; [onEdit] == null with [showTrailing] → locked row; otherwise informational.
 */
@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isIdentifier: Boolean = false,
    showTrailing: Boolean = true,
    warmTile: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onEdit != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    role = Role.Button,
                    onClickLabel = "Ubah ${label.lowerFirst()}",
                    onClick = onEdit
                ) else Modifier
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = RowHorizontalPadding, vertical = EmpatiSpace.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoftIconTile(
            icon = icon,
            size = RowTileSize,
            container = if (warmTile) colors.secondaryContainer else colors.primaryContainer,
            tint = if (warmTile) colors.onSecondaryContainer else colors.onPrimaryContainer
        )
        Spacer(Modifier.width(RowTileGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(EmpatiSpace.xxs))
            Text(
                text = value,
                style = if (isIdentifier) EmpatiMetricStyle.copy(fontSize = 14.sp, lineHeight = 20.sp)
                else MaterialTheme.typography.bodyLarge,
                color = colors.onSurface
            )
        }
        if (showTrailing) {
            Spacer(Modifier.width(EmpatiSpace.sm))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(EmpatiSurfaces.inset(), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (onEdit != null) Icons.Rounded.Edit else Icons.Rounded.Lock,
                    contentDescription = if (onEdit != null) "Ubah ${label.lowerFirst()}"
                    else "$label terkunci",
                    tint = if (onEdit != null) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Destructive action ──────────────────────────────────────────────────────

@Composable
private fun DeleteDataRow(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressScale(interactionSource, 0.98f)
            .focusHalo(interactionSource, CircleShape, colors.error)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = EmpatiSpace.xl, vertical = EmpatiSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = colors.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(EmpatiSpace.sm))
            Text(
                text = "Hapus seluruh data akun & riwayat saya",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onErrorContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Dialogs ─────────────────────────────────────────────────────────────────

@Composable
private fun EditFieldDialog(
    field: ProfileField,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = EmpatiSurfaces.card(),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = "Ubah ${field.label.lowerFirst()}",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            EmpatiTextField(
                value = value,
                onValueChange = onValueChange,
                label = field.label,
                leadingIcon = field.icon
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                modifier = Modifier.heightIn(min = EmpatiSpace.touchTarget)
            ) {
                Text(text = "Simpan", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.heightIn(min = EmpatiSpace.touchTarget)
            ) {
                Text(text = "Batal", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = EmpatiSurfaces.card(),
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
        icon = {
            SoftIconTile(
                icon = Icons.Rounded.DeleteOutline,
                size = 52.dp,
                container = colors.errorContainer,
                tint = colors.onErrorContainer
            )
        },
        title = {
            Text(
                text = "Hapus semua data?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Seluruh riwayat skrining, percakapan, serta akun profil Anda akan dihapus permanen dari perangkat ini maupun dari penyimpanan akun.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error,
                    contentColor = colors.onError
                ),
                modifier = Modifier.heightIn(min = EmpatiSpace.touchTarget)
            ) {
                Text(text = "Hapus permanen", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceVariant),
                modifier = Modifier.heightIn(min = EmpatiSpace.touchTarget)
            ) {
                Text(text = "Batal", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
private fun LemonadeStatusSection(
    status: LemonadeStatus?,
    error: String?,
    onRefresh: () -> Unit
) {
    val summary = LemonadeStatusPresenter.summarize(status, error)

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(
            text = "Status layanan",
            modifier = Modifier.padding(start = EmpatiSpace.xs)
        )
        Spacer(Modifier.height(EmpatiSpace.sm))
        TonalCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(EmpatiSpace.md)
        ) {
            Text(text = summary.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(EmpatiSpace.xs))
            Text(text = summary.detail, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(EmpatiSpace.sm))
            GhostButton(
                text = if (summary.ready) "Periksa ulang" else "Periksa kesiapan server",
                onClick = onRefresh
            )
        }
    }
}
