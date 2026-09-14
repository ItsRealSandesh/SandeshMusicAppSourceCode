package com.sandeshmusic.app.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandeshmusic.app.ui.theme.CoralLight
import com.sandeshmusic.app.ui.theme.CoralPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_wave")

    // Glowing pulse scale
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    // Animated sound bars
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 28f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(410, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )
    val bar5Height by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar5"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        // Show splash animation smoothly then transition to app
        delay(1800)
        onSplashFinished()
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B0F19),
            Color(0xFF111827),
            Color(0xFF0F172A)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .testTag("app_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative ambient glow
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(glowScale)
                .clip(CircleShape)
                .background(CoralPrimary.copy(alpha = 0.12f))
                .blur(40.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // App Logo Container
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(if (startAnimation) 1f else 0.8f)
                ) {}

                // Inner gradient icon button
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .size(88.dp)
                        .scale(if (startAnimation) 1f else 0.7f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CoralPrimary, CoralLight, Color(0xFFFF8A65))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Sandesh Music Logo",
                            tint = Color.White,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Title
            Text(
                text = "Sandesh Music",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "High Fidelity • Cloud & Local Player",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Sound Equalizer Wave Animation
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(40.dp)
            ) {
                val bars = listOf(bar1Height, bar2Height, bar3Height, bar4Height, bar5Height)
                bars.forEach { heightVal ->
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(heightVal.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(CoralLight, CoralPrimary)
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Version 2.0 Pill Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "v2.0",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = CoralPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
