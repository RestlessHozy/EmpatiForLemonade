package com.example.empati.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.empati.EmpatiViewModel
import com.example.empati.Screen
import com.example.empati.ui.components.EmpatiSurfaces
import com.example.empati.ui.components.SoftDivider
import com.example.empati.ui.components.focusHalo
import com.example.empati.ui.components.pressScale
import com.example.empati.ui.theme.EmpatiMotion
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.LocalReducedMotion

private data class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val screen: Screen,
    val isAction: Boolean = false
)

private val bottomNavTabs = listOf(
    BottomNavTab("Beranda", Icons.Rounded.Home, Icons.Outlined.Home, Screen.Home),
    BottomNavTab("Skrining", Icons.AutoMirrored.Rounded.FactCheck, Icons.AutoMirrored.Outlined.FactCheck, Screen.Consent, isAction = true),
    BottomNavTab("Konsultasi", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline, Screen.Chat),
    BottomNavTab("Riwayat", Icons.Rounded.History, Icons.Outlined.History, Screen.History),
    BottomNavTab("Profil", Icons.Rounded.Person, Icons.Outlined.Person, Screen.Profile)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScaffold(vm: EmpatiViewModel, current: Screen, modifier: Modifier = Modifier) {
    val isImeVisible = WindowInsets.isImeVisible
    // Saat berada di tab Chat dan keyboard terbuka, sembunyikan BottomNavBar agar chat composer duduk tepat di atas keyboard
    val showBottomNav = !(current == Screen.Chat && isImeVisible)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    fadeIn(EmpatiMotion.standard()) togetherWith fadeOut(EmpatiMotion.quick())
                },
                label = "tabContentTransition"
            ) { tab ->
                when (tab) {
                    Screen.Chat -> ChatScreen(vm)
                    Screen.History -> HistoryScreen(vm)
                    Screen.Profile -> ProfileScreen(vm)
                    else -> HomeScreen(vm)
                }
            }
        }
        if (showBottomNav) {
            BottomNavBar(
                selected = current,
                onSelectTab = vm::selectTab,
                onScreening = { vm.navigateTo(Screen.Consent) }
            )
        }
    }
}

/**
 * Calm bottom bar: a card-toned surface with a hairline top edge. The selected tab gets a tonal pill
 * behind a filled icon. "Skrining" is an action (it opens the consent flow) and is never shown as selected.
 */
@Composable
fun BottomNavBar(
    selected: Screen,
    onSelectTab: (Screen) -> Unit,
    onScreening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EmpatiSurfaces.card())
    ) {
        SoftDivider()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = EmpatiSpace.readableWidth)
                    .fillMaxWidth()
                    .padding(horizontal = EmpatiSpace.xs, vertical = EmpatiSpace.xs),
                horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xxs)
            ) {
                bottomNavTabs.forEach { tab ->
                    val isSelected = !tab.isAction && selected == tab.screen
                    BottomNavItem(
                        tab = tab,
                        selected = isSelected,
                        onClick = { if (tab.isAction) onScreening() else onSelectTab(tab.screen) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: BottomNavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduced = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val itemShape = RoundedCornerShape(20.dp)
    val colors = MaterialTheme.colorScheme

    val pill by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = EmpatiMotion.standard(),
        label = "navPill"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        animationSpec = EmpatiMotion.quick(),
        label = "navIconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.onSurface else colors.onSurfaceVariant,
        animationSpec = EmpatiMotion.quick(),
        label = "navLabelColor"
    )

    val clickModifier = if (tab.isAction) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier.selectable(
            selected = selected,
            interactionSource = interactionSource,
            indication = ripple(),
            role = Role.Tab,
            onClick = onClick
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 60.dp)
            .pressScale(interactionSource, 0.94f)
            .focusHalo(interactionSource, itemShape)
            .clip(itemShape)
            .then(clickModifier)
            .padding(vertical = EmpatiSpace.xxs)
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = pill
                        scaleX = 0.6f + 0.4f * pill
                    }
                    .background(colors.primaryContainer, CircleShape)
            )
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    if (reduced) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(EmpatiMotion.quick()) togetherWith fadeOut(EmpatiMotion.quick())
                    }
                },
                label = "navIcon"
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(EmpatiSpace.xxs))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
