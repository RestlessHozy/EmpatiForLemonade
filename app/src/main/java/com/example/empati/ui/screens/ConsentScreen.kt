package com.example.empati.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.empati.ui.components.GhostButton
import com.example.empati.ui.components.PrimaryButton
import com.example.empati.ui.components.Reveal
import com.example.empati.ui.components.ScreenFrame
import com.example.empati.ui.components.ScreenHeader
import com.example.empati.ui.components.ScreenTopBar
import com.example.empati.ui.components.SoftDivider
import com.example.empati.ui.components.SoftIconTile
import com.example.empati.ui.components.TonalCard
import com.example.empati.ui.theme.EmpatiMotion
import com.example.empati.ui.theme.EmpatiSpace

private data class ConsentItem(
    val icon: ImageVector,
    val title: String,
    val body: String,
    /** Warm (clay) tile for the human-support item; everything else uses the calm brand tile. */
    val warm: Boolean = false
)

private val consentItems = listOf(
    ConsentItem(
        icon = Icons.Rounded.HealthAndSafety,
        title = "Untuk apa skrining ini?",
        body = "Skrining ini membantu Anda mengenali tanda risiko tekanan darah tinggi dari sisi kebiasaan harian, beban pikiran, maupun kenyamanan relasi. Hasilnya berupa panduan awal untuk menjaga kesehatan, bukan diagnosis dokter."
    ),
    ConsentItem(
        icon = Icons.AutoMirrored.Rounded.Assignment,
        title = "Apa saja yang ditanyakan?",
        body = "Anda akan menjawab beberapa pertanyaan ringan seputar riwayat tensi, pola makan dan tidur, beban stres, serta rasa aman dalam hubungan sehari-hari di rumah, tempat kerja, atau lingkungan sekitar."
    ),
    ConsentItem(
        icon = Icons.Rounded.Lock,
        title = "Perlindungan privasi & enkripsi",
        body = "Riwayat skrining disimpan di perangkat Anda, sedangkan pencadangan riset diamankan menggunakan kode samaran satu arah tanpa menyertakan nama, email, nomor HP, atau identitas asli Anda."
    ),
    ConsentItem(
        icon = Icons.Rounded.VerifiedUser,
        title = "Sepenuhnya dalam kendali Anda",
        body = "Pengisian ini bersifat sukarela. Anda bebas berhenti kapan saja, atau menghapus seluruh akun dan riwayat data Anda secara permanen lewat menu Profil."
    ),
    ConsentItem(
        icon = Icons.Rounded.SupportAgent,
        title = "Dukungan darurat & ruang aman",
        body = "Jika Anda sedang merasa tidak aman atau membutuhkan teman bicara segera, Anda dapat menghubungi layanan rahasia SAPA 129 (Telepon 129 / WhatsApp 08111-129-129) atau fasilitas kesehatan terdekat.",
        warm = true
    )
)

@Composable
fun ConsentScreen(onAgree: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    ConsentScreenContent(onAgree = onAgree, onCancel = onCancel, modifier = modifier)
}

/**
 * Stateless consent layout: header, a scrollable list of consent points in the middle and actions pinned
 * to the bottom. Long points show their first lines and expand on demand; [initiallyExpanded] opens
 * all of them (useful for previews and tests).
 */
@Composable
fun ConsentScreenContent(
    onAgree: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    ScreenFrame(modifier = modifier, scrollable = false) {
        ScreenTopBar(onBack = onCancel)

        Spacer(Modifier.height(EmpatiSpace.md))

        Reveal(index = 0) {
            ScreenHeader(
                title = "Sebelum kita mulai",
                subtitle = "Kenali bagaimana kami menjaga kenyamanan dan kerahasiaan cerita Anda."
            )
        }

        Spacer(Modifier.height(EmpatiSpace.lg))

        Reveal(
            index = 1,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            TonalCard(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = EmpatiSpace.lg, vertical = EmpatiSpace.xs)
                ) {
                    consentItems.forEachIndexed { index, item ->
                        ConsentSection(item = item, initiallyExpanded = initiallyExpanded)
                        if (index < consentItems.lastIndex) {
                            SoftDivider(modifier = Modifier.padding(start = ConsentTextInset))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(EmpatiSpace.md))
        Reveal(index = 2) {
            Column {
                PrimaryButton(text = "Saya memahami dan setuju", onClick = onAgree)
                Spacer(Modifier.height(EmpatiSpace.xxs))
                GhostButton(text = "Batalkan", onClick = onCancel)
            }
        }
        Spacer(Modifier.height(EmpatiSpace.xs))
    }
}

/** Icon tile (44) + gap (16): dividers start where the text starts. */
private val ConsentTextInset = 60.dp

@Composable
private fun ConsentSection(item: ConsentItem, initiallyExpanded: Boolean) {
    var expanded by rememberSaveable(item.title) { mutableStateOf(initiallyExpanded) }
    var overflows by remember { mutableStateOf(false) }
    val showToggle = overflows || expanded

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EmpatiSpace.lg)
    ) {
        if (item.warm) {
            SoftIconTile(
                icon = item.icon,
                container = MaterialTheme.colorScheme.secondaryContainer,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        } else {
            SoftIconTile(icon = item.icon)
        }
        Spacer(Modifier.width(EmpatiSpace.md))
        Column(
            modifier = Modifier
                .weight(1f)
                .animateContentSize(animationSpec = EmpatiMotion.standard())
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(EmpatiSpace.xxs))
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else CollapsedLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layout ->
                    if (!expanded) overflows = layout.hasVisualOverflow
                }
            )
            if (showToggle) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .heightIn(min = EmpatiSpace.touchTarget)
                        .clickable(role = Role.Button) { expanded = !expanded }
                ) {
                    Text(
                        text = if (expanded) "Tampilkan lebih sedikit" else "Baca selengkapnya",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private const val CollapsedLines = 3
