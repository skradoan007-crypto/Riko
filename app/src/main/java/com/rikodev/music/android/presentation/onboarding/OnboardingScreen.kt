package com.rikodev.music.android.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.riko.components.RikoPrimaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton
import com.rikodev.music.android.core.designsystem.riko.components.rikoScreenFoundation
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onContinueWithGoogle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .rikoScreenFoundation(depth = 0.85f)
            .imePadding()
    ) {
        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RikoThemeV2.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Music",
                style = RikoThemeV2.Type.npTitle.copy(fontSize = 40.sp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Brings Us",
                style = RikoThemeV2.Type.npTitle.copy(fontSize = 40.sp),
                color = RikoThemeV2.Colors.accent,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Together",
                style = RikoThemeV2.Type.npTitle.copy(fontSize = 40.sp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(RikoThemeV2.Spacing.lg))

            Text(
                text = "Listen \u2022 Feel \u2022 Your Music",
                style = RikoThemeV2.Type.cardSubtitle,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(RikoThemeV2.Spacing.xl))

            RikoPrimaryButton(
                // SM spec 3.1: the button says "Get Started" (the old
                // "Create Account" promised an account that doesn't exist).
                text = "Get Started",
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(RikoThemeV2.Spacing.md))

            RikoSecondaryButton(
                text = "Skip",
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(RikoThemeV2.Spacing.xl))
            Spacer(
                Modifier.padding(
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
            )
        }
    }
}
