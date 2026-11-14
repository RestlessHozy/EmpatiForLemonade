package com.example.empati.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.empati.ui.components.EmpatiLogo
import com.example.empati.ui.components.Reveal
import com.example.empati.ui.theme.DisplayFont
import com.example.empati.ui.theme.EmpatiMotion
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.LocalReducedMotion
import com.example.empati.ui.theme.empati

/**
 * Calm opening moment: two breathing circles resting on a soft halo, the app name, one quiet line.
 * With reduced motion everything renders in its final state immediately.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    var entered by remember { mutableStateOf(reduced) }

    LaunchedEffect(Unit) {
        entered = true
    }

    val arrival by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = EmpatiMotion.expressive(),
        label = "splashArrival"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = EmpatiSpace.xxl)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = arrival
                    val scale = 0.9f + 0.1f * arrival
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                Box(
                    Modifier
                        .size(208.dp)
                        .background(MaterialTheme.empati.aura.copy(alpha = 0.14f), CircleShape)
                )
                Box(
                    Modifier
                        .size(156.dp)
                        .background(MaterialTheme.empati.aura.copy(alpha = 0.22f), CircleShape)
                )
                EmpatiLogo(size = 112.dp, animated = true)
            }

            Spacer(Modifier.height(EmpatiSpace.xl))

            Reveal(index = 1) {
                Text(
                    text = "Empati",
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = DisplayFont,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(EmpatiSpace.sm))

            Reveal(index = 2) {
                Text(
                    text = "Teman pendamping kesehatan tekanan darah dan ketenangan pikiran Anda",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
