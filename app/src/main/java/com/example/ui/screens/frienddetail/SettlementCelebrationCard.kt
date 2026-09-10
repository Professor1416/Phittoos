package com.example.ui.screens.frienddetail

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.EmeraldGreenSurface
import com.example.ui.theme.Slate700
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

data class SettlementCelebrationEvent(
    val id: Long = nextId.incrementAndGet()
) {
    companion object {
        private val nextId = AtomicLong(0)
    }
}

@Composable
fun SettlementCelebrationCard(
    event: SettlementCelebrationEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val headlineText = stringResource(R.string.celebration_headline)
    val subtitleText = stringResource(R.string.celebration_subtitle)
    val announcementText = "$headlineText $subtitleText"

    val animationsDisabled = remember {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }

    val scale = remember(event.id) { Animatable(if (animationsDisabled) 1f else 0.92f) }
    val alpha = remember(event.id) { Animatable(if (animationsDisabled) 1f else 0f) }

    // One-time announcement and entrance animation keyed by event.id
    LaunchedEffect(event.id) {
        try {
            view.announceForAccessibility(announcementText)
        } catch (_: Exception) {
            // No-op if accessibility announcement fails
        }

        if (!animationsDisabled) {
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                )
            }
        }
    }

    // Platform accessibility-adjusted timeout (nominal 3 seconds) keyed by event.id
    LaunchedEffect(event.id) {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val timeoutMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && accessibilityManager != null) {
            accessibilityManager.getRecommendedTimeoutMillis(
                3000,
                AccessibilityManager.FLAG_CONTENT_CONTROLS or AccessibilityManager.FLAG_CONTENT_TEXT
            )
        } else {
            if (accessibilityManager?.isTouchExplorationEnabled == true) 10000 else 3000
        }
        delay(timeoutMillis.toLong())
        onDismiss()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_settlement_celebration")
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = EmeraldGreenSurface
        ),
        border = BorderStroke(1.dp, EmeraldGreenLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldGreenDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreenDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("button_dismiss_celebration")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.celebration_dismiss_cd),
                    tint = EmeraldGreenDark
                )
            }
        }
    }
}
