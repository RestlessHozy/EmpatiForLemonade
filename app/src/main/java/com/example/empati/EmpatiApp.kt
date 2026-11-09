package com.example.empati

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.empati.ui.theme.EmpatiMotion
import com.example.empati.ui.theme.LocalReducedMotion
import com.example.empati.ui.screens.AuthScreen
import com.example.empati.ui.screens.ConsentScreen
import com.example.empati.ui.screens.IntegratedResultScreen
import com.example.empati.ui.screens.MainScaffold
import com.example.empati.ui.screens.ModuleResultScreen
import com.example.empati.ui.screens.OnboardingScreen
import com.example.empati.ui.screens.QuestionnaireScreen
import com.example.empati.ui.screens.SplashScreen
import kotlinx.coroutines.delay

private fun screenRank(screen: Screen): Int = when (screen) {
    Screen.Splash -> 0
    Screen.Onboarding -> 1
    Screen.Auth -> 2
    Screen.Home, Screen.Chat, Screen.History, Screen.Profile -> 3
    Screen.Consent -> 4
    Screen.DualQuestionnaire -> 5
    Screen.ModuleResult -> 6
    Screen.IntegratedResult -> 7
}

private fun isTabScreen(screen: Screen): Boolean =
    screen == Screen.Home || screen == Screen.Chat || screen == Screen.History || screen == Screen.Profile

@Composable
fun EmpatiApp(vm: EmpatiViewModel = viewModel()) {
    BackHandler(enabled = vm.backStack.isNotEmpty()) { vm.onBack() }

    val targetRoute = if (isTabScreen(vm.route)) Screen.Home else vm.route
    val reducedMotion = LocalReducedMotion.current

    AnimatedContent(
        targetState = targetRoute,
        transitionSpec = {
            val initialRank = screenRank(initialState)
            val targetRank = screenRank(targetState)
            val transition = if (reducedMotion) {
                fadeIn(EmpatiMotion.standard()) togetherWith fadeOut(EmpatiMotion.quick())
            } else if (initialState == Screen.Splash || targetState == Screen.Splash) {
                fadeIn(EmpatiMotion.expressive()) togetherWith fadeOut(EmpatiMotion.standard())
            } else if (targetRank >= initialRank) {
                // Forward: the new page eases in from a short distance; the old one drifts away softly.
                (slideInHorizontally(
                    animationSpec = EmpatiMotion.expressive(),
                    initialOffsetX = { it / 14 }
                ) + fadeIn(EmpatiMotion.standard())) togetherWith
                    (slideOutHorizontally(
                        animationSpec = EmpatiMotion.standard(),
                        targetOffsetX = { -it / 24 }
                    ) + fadeOut(EmpatiMotion.quick()))
            } else {
                (slideInHorizontally(
                    animationSpec = EmpatiMotion.expressive(),
                    initialOffsetX = { -it / 24 }
                ) + fadeIn(EmpatiMotion.standard())) togetherWith
                    (slideOutHorizontally(
                        animationSpec = EmpatiMotion.standard(),
                        targetOffsetX = { it / 14 }
                    ) + fadeOut(EmpatiMotion.quick()))
            }
            transition.using(SizeTransform(clip = false))
        },
        label = "rootNavigationTransition"
    ) { route ->
        when (route) {
            Screen.Splash -> {
                LaunchedEffect(Unit) {
                    delay(1800)
                    vm.finishSplash()
                }
                SplashScreen()
            }

            Screen.Onboarding -> OnboardingScreen(onDone = vm::completeOnboarding)

            Screen.Auth -> AuthScreen(vm)

            Screen.Consent -> ConsentScreen(
                onAgree = vm::startDualScreening,
                onCancel = vm::onBack
            )

            Screen.DualQuestionnaire -> QuestionnaireScreen(vm)

            Screen.ModuleResult -> ModuleResultScreen(vm)

            Screen.IntegratedResult -> IntegratedResultScreen(vm)

            else -> MainScaffold(vm = vm, current = vm.route)
        }
    }
}
