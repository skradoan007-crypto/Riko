package com.rikodev.sync.nexmesh.feature.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

@Composable
fun ExitConfirmationDialog(
    visible: Boolean,
    hasActiveBackgroundWork: Boolean,
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    val p = LocalNexMeshPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x5A0A0F1E))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.90f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut(targetScale = 0.94f) + fadeOut()
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    cornerRadius = 28.dp,
                    elevated = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .liquidGlassSurface(
                                    shape = CircleShape,
                                    topColor = p.accentPillTop,
                                    bottomColor = p.accentPillBottom,
                                    rimTop = p.glassRimTop,
                                    rimBottom = p.glassRimBottom,
                                    shadowColor = p.glassShadow,
                                    elevation = 4.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PowerSettingsNew,
                                contentDescription = null,
                                tint = p.inkAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Exit NexMesh?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.inkPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (hasActiveBackgroundWork) {
                                "Are you sure you want to close the app? Active background storage sharing will continue safely."
                            } else {
                                "Are you sure you want to exit NexMesh?"
                            },
                            fontSize = 13.sp,
                            color = p.inkSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidGlassPillButton(
                                text = "Stay",
                                onClick = onDismiss,
                                isPrimary = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            LiquidGlassPillButton(
                                text = "Exit",
                                onClick = onConfirmExit,
                                isPrimary = false,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
