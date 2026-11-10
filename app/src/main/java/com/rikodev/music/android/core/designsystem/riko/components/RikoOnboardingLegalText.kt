package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — ONBOARDING LEGAL TEXT (SM-UI-SPEC-001 v2.0, Section 2.23)
//
// Centered 2-line fine-print: "By continuing, you agree to our Terms of
// Service and Privacy Policy" — Terms/Privacy as tappable inline links in
// accent-violet + underline (spec 2.23 recommendation).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoOnboardingLegalText(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
    prefix: String = "By continuing, you agree to our ",
    termsLabel: String = "Terms of Service",
    middle: String = " and ",
    privacyLabel: String = "Privacy Policy",
    style: TextStyle = RikoTypography.Legal
) {
    val annotated = buildAnnotatedString {
        append(prefix)
        pushStringAnnotation(tag = "terms", annotation = "terms")
        withStyle(
            SpanStyle(
                color = AccentViolet,
                textDecoration = TextDecoration.Underline
            )
        ) { append(termsLabel) }
        pop()
        append(middle)
        pushStringAnnotation(tag = "privacy", annotation = "privacy")
        withStyle(
            SpanStyle(
                color = AccentViolet,
                textDecoration = TextDecoration.Underline
            )
        ) { append(privacyLabel) }
        pop()
    }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style.copy(color = RikoTextSecondary, textAlign = TextAlign.Center),
        onClick = { offset ->
            annotated.getStringAnnotations("terms", offset, offset)
                .firstOrNull()?.let { onTermsClick() }
            annotated.getStringAnnotations("privacy", offset, offset)
                .firstOrNull()?.let { onPrivacyClick() }
        }
    )
}
