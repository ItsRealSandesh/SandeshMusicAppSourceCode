package com.sandeshmusic.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandeshmusic.app.ui.theme.CoralPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeBoosterBottomSheet(
    boostPercent: Int,
    bassBoostPercent: Int,
    onSetVolumeBoost: (Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier.testTag("volume_booster_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CoralPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Volume booster icon",
                            tint = CoralPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Volume Booster",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Amplify audio beyond 100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("volume_booster_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dynamic Boost Gauge Card
            val isHighBoost = boostPercent > 150
            val gaugeColor by animateColorAsState(
                targetValue = when {
                    boostPercent >= 200 -> Color(0xFFFF5252)
                    boostPercent > 150 -> Color(0xFFFF9800)
                    boostPercent > 100 -> CoralPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(300),
                label = "gauge_color"
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = gaugeColor.copy(alpha = if (boostPercent > 100) 0.6f else 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$boostPercent%",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1).sp
                            ),
                            color = gaugeColor
                        )
                        if (boostPercent > 100) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Boost active",
                                tint = gaugeColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    val levelLabel = when {
                        boostPercent >= 200 -> "MAXIMUM BOOST ⚡"
                        boostPercent >= 175 -> "HIGH GAIN (+7.5 dB)"
                        boostPercent >= 150 -> "PUNCHY BOOST (+5.0 dB)"
                        boostPercent > 100 -> "MILD BOOST (+2.5 dB)"
                        else -> "STANDARD (NATURAL 100%)"
                    }

                    Text(
                        text = levelLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Animated Visualizer Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(28.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val barCount = 12
                        val progressFraction = (boostPercent - 100) / 150f
                        for (i in 0 until barCount) {
                            val barActive = (i.toFloat() / barCount.toFloat()) <= progressFraction || (boostPercent == 100 && i < 2)
                            val targetHeight = if (barActive) {
                                ((i + 1) * 2.2f + 4f).coerceIn(4f, 26f).dp
                            } else {
                                4.dp
                            }
                            val animatedHeight by animateFloatAsState(
                                targetValue = targetHeight.value,
                                animationSpec = tween(250, easing = FastOutSlowInEasing),
                                label = "bar_anim_$i"
                            )

                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(animatedHeight.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (barActive) gaugeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Booster Slider Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gain Boost Level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$boostPercent%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CoralPrimary
                )
            }

            Slider(
                value = boostPercent.toFloat(),
                onValueChange = { onSetVolumeBoost(it.toInt()) },
                valueRange = 100f..250f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = gaugeColor,
                    activeTrackColor = gaugeColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("volume_booster_slider")
            )

            // Preset Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val presets = listOf(100, 125, 150, 175, 200)
                presets.forEach { preset ->
                    val isSelected = boostPercent == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetVolumeBoost(preset) },
                        label = {
                            Text(
                                text = if (preset == 100) "100%" else if (preset == 200) "200% ⚡" else "$preset%",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("preset_chip_$preset")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bass Boost / Low-end Punch Control
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Bass punch",
                                tint = CoralPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bass Punch & Subwoofer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (bassBoostPercent > 0) "+$bassBoostPercent%" else "OFF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bassBoostPercent > 0) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Slider(
                        value = bassBoostPercent.toFloat(),
                        onValueChange = { onSetBassBoost(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = CoralPrimary,
                            activeTrackColor = CoralPrimary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bass_boost_slider")
                    )
                }
            }

            // High Volume Warning
            AnimatedVisibility(visible = isHighBoost) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Hearing warning",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "High volume booster is active. Maintain safe volume levels to protect your hearing with headphones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset Button
            if (boostPercent > 100 || bassBoostPercent > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSetVolumeBoost(100)
                            onSetBassBoost(0)
                        }
                        .testTag("reset_booster_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset audio effects",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset to Normal Audio (100%)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
