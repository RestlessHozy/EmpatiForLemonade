package com.example.empati

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.empati.data.RiskLevel
import com.example.empati.ui.components.BackIconButton
import com.example.empati.ui.components.BreathingDots
import com.example.empati.ui.components.ChoiceChip
import com.example.empati.ui.components.EmpatiAvatar
import com.example.empati.ui.components.EmpatiLogo
import com.example.empati.ui.components.EmpatiTextField
import com.example.empati.ui.components.GhostButton
import com.example.empati.ui.components.PrimaryButton
import com.example.empati.ui.components.RiskBadge
import com.example.empati.ui.components.SectionChip
import com.example.empati.ui.components.SectionLabel
import com.example.empati.ui.components.SegmentedSwitch
import com.example.empati.ui.components.SoftDivider
import com.example.empati.ui.components.SoftIconTile
import com.example.empati.ui.components.SoftNotice
import com.example.empati.ui.components.StateOrb
import com.example.empati.ui.components.TonalCard
import com.example.empati.ui.components.ToneBadge
import com.example.empati.ui.components.errorTone
import com.example.empati.ui.theme.EmpatiMetricStyle
import com.example.empati.ui.theme.EmpatiTypography
import com.example.empati.ui.theme.empati

@Composable
private fun Sheet(content: @Composable () -> Unit) {
    PreviewTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) { content() }
    }
}

@PreviewTest
@ComponentSheetLightDark
@Composable
fun ComponentsButtons() = Sheet {
    SectionLabel("Tombol utama")
    PrimaryButton(text = "Mulai skrining", onClick = {})
    PrimaryButton(text = "Memeriksa…", onClick = {}, loading = true, enabled = false)
    PrimaryButton(text = "Belum bisa dipilih", onClick = {}, enabled = false)
    PrimaryButton(text = "Mengerti", onClick = {}, trailingIcon = null)
    SectionLabel("Tombol sekunder")
    GhostButton(text = "Coba dengan akun demo", onClick = {})
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BackIconButton(onClick = {})
        SectionChip(text = "Modul hipertensi")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@PreviewTest
@ComponentSheetLightDark
@Composable
fun ComponentsSelection() = Sheet {
    SectionLabel("Segmented switch")
    SegmentedSwitch(options = listOf("Masuk", "Daftar"), selectedIndex = 0, onSelect = {})
    SegmentedSwitch(options = listOf("Masuk", "Daftar"), selectedIndex = 1, onSelect = {})
    SectionLabel("Choice chips")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceChip(label = "Perempuan", selected = true, onClick = {})
        ChoiceChip(label = "Laki-laki", selected = false, onClick = {})
        ChoiceChip(label = "Masyarakat umum", selected = false, onClick = {})
        ChoiceChip(label = "Karyawan / pekerja", selected = true, onClick = {})
    }
}

@PreviewTest
@ComponentSheetLightDark
@Composable
fun ComponentsTextFields() = Sheet {
    EmpatiTextField(
        value = "",
        onValueChange = {},
        label = "ID akun",
        placeholder = "Email, nomor HP, username, NIK, atau NIP",
        leadingIcon = Icons.Rounded.AlternateEmail
    )
    EmpatiTextField(
        value = "ratna.andari@empati.id",
        onValueChange = {},
        label = "ID akun",
        leadingIcon = Icons.Rounded.AlternateEmail,
        valueStyle = EmpatiMetricStyle.copy(color = MaterialTheme.colorScheme.onSurface)
    )
    EmpatiTextField(
        value = "ab",
        onValueChange = {},
        label = "ID akun",
        leadingIcon = Icons.Rounded.AlternateEmail,
        errorText = "ID akun minimal 4 karakter."
    )
    EmpatiTextField(
        value = "123456",
        onValueChange = {},
        label = "PIN atau kata sandi",
        supportingText = "Minimal 6 karakter.",
        leadingIcon = Icons.Rounded.Lock,
        keyboardOptions = KeyboardOptions.Default
    )
}

@PreviewTest
@ComponentSheetLightDark
@Composable
fun ComponentsStatusAndTones() = Sheet {
    SectionLabel("Lencana risiko")
    FlowRow2 {
        RiskBadge(RiskLevel.LOW)
        RiskBadge(RiskLevel.MEDIUM)
        RiskBadge(RiskLevel.HIGH)
        RiskBadge(RiskLevel.VERY_HIGH)
    }
    SectionLabel("Lencana nada")
    FlowRow2 {
        ToneBadge("Tenang", MaterialTheme.empati.calm)
        ToneBadge("Perlu perhatian", MaterialTheme.empati.attentive)
        ToneBadge("Segera periksa", MaterialTheme.empati.concerned)
        ToneBadge("Belum lengkap", MaterialTheme.empati.neutral)
    }
    SectionLabel("Pemberitahuan lembut")
    SoftNotice(
        text = "Jawaban Anda aman dan dirahasiakan di perangkat ini.",
        icon = Icons.Rounded.Lock,
        tone = MaterialTheme.empati.calm
    )
    SoftNotice(
        text = "Layanan SAPA 129 bersifat rahasia dan bebas stigma.",
        icon = Icons.Rounded.SupportAgent,
        tone = MaterialTheme.empati.presence,
        title = "Butuh teman bicara?"
    )
    SoftNotice(
        text = "ID akun minimal 4 karakter.",
        icon = Icons.Rounded.VerifiedUser,
        tone = errorTone()
    )
}

@PreviewTest
@ComponentSheetLightDark
@Composable
fun ComponentsStateOrbs() = Sheet {
    SectionLabel("Orb keadaan afektif")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StateOrb(tone = MaterialTheme.empati.calm, icon = Icons.Rounded.Spa, size = 80.dp)
        StateOrb(tone = MaterialTheme.empati.attentive, icon = Icons.Rounded.MonitorHeart, size = 80.dp)
        StateOrb(tone = MaterialTheme.empati.concerned, icon = Icons.Rounded.Favorite, size = 80.dp)
        StateOrb(tone = MaterialTheme.empati.urgent, icon = Icons.Rounded.Shield, size = 80.dp)
    }
    SectionLabel("Identitas")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        EmpatiLogo(size = 72.dp)
        EmpatiAvatar(initials = "RA", size = 56.dp)
        BreathingDots()
    }
    SectionLabel("Skala tipografi")
    Text("Mari mulai bersama", style = EmpatiTypography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
    Text("Hasil yang terasa tenang", style = EmpatiTypography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    Text("Judul bagian", style = EmpatiTypography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    Text(
        "Teks isi dibuat lapang agar nyaman dibaca, dengan jarak baris sekitar 1,6 kali ukuran huruf.",
        style = EmpatiTypography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text("Label kecil", style = EmpatiTypography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("ratna.andari@empati.id · DOC-HT-02", style = EmpatiMetricStyle, color = MaterialTheme.colorScheme.onSurface)
}

@PreviewTest
@ComponentSheetLightDark
@Composable
fun ComponentsSurfaces() = Sheet {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Text("Kartu biasa", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "Kedalaman datang dari lapisan nada, bukan garis tebal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    TonalCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
        Text("Kartu utama (melayang)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "Bayangan lembut hanya untuk elemen yang melayang.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    TonalCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SoftIconTile(icon = Icons.Rounded.MonitorHeart)
            Text("Kartu yang bisa diketuk", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(16.dp))
        SoftDivider()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow2(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}
