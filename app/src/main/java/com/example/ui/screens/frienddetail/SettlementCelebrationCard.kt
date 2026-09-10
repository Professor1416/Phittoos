package com.example.ui.screens.frienddetail

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.R
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.Slate500
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

/**
 * Centered floating settlement celebration popup with lightly dimmed background.
 */
@Composable
fun SettlementCelebrationDialog(
    event: SettlementCelebrationEvent,
    friendName: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    enableAnimation: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val introText = stringResource(R.string.celebration_intro)
    val headlineText = stringResource(R.string.celebration_headline)
    val supportText = if (!friendName.isNullOrBlank()) {
        stringResource(R.string.celebration_support_with_name, friendName)
    } else {
        stringResource(R.string.celebration_support_generic)
    }

    val announcementText = if (!friendName.isNullOrBlank()) {
        stringResource(R.string.celebration_accessibility_announcement, friendName)
    } else {
        stringResource(R.string.celebration_accessibility_announcement_generic)
    }

    val isSystemAnimationDisabled = remember {
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
    val animationsDisabled = !enableAnimation || isSystemAnimationDisabled

    val popupScale = remember(event.id) { Animatable(if (animationsDisabled) 1f else 0.92f) }
    val popupAlpha = remember(event.id) { Animatable(if (animationsDisabled) 1f else 0f) }
    val checkmarkProgress = remember(event.id) { Animatable(if (animationsDisabled) 1f else 0f) }
    val headlineAlpha = remember(event.id) { Animatable(if (animationsDisabled) 1f else 0f) }
    val headlineScale = remember(event.id) { Animatable(if (animationsDisabled) 1f else 0.92f) }

    var isDismissing by remember(event.id) { mutableStateOf(false) }

    val dismissAction: () -> Unit = remember(event.id, onDismiss, animationsDisabled) {
        {
            if (!isDismissing) {
                isDismissing = true
                if (animationsDisabled) {
                    onDismiss()
                } else {
                    coroutineScope.launch {
                        popupAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 150, easing = LinearEasing)
                        )
                        onDismiss()
                    }
                }
            }
        }
    }

    // Single entrance announcement and staggered animation keyed by event.id
    LaunchedEffect(event.id) {
        try {
            view.announceForAccessibility(announcementText)
        } catch (_: Exception) {
            // No-op if announcement fails
        }

        if (!animationsDisabled) {
            launch {
                popupScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                )
            }
            launch {
                popupAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                )
            }
            launch {
                delay(80)
                checkmarkProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(150)
                launch {
                    headlineAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 200, easing = LinearEasing)
                    )
                }
                launch {
                    headlineScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    // Platform accessibility-adjusted auto-dismiss timeout (nominal 3 seconds)
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
        dismissAction()
    }

    Dialog(
        onDismissRequest = dismissAction,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            dialogWindow?.setDimAmount(0.20f)
            onDispose {}
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Full-screen scrim overlay behind the card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (dialogWindow == null) {
                            Modifier.background(Color.Black.copy(alpha = 0.20f))
                        } else Modifier
                    )
                    .testTag("dialog_scrim_celebration")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismissAction
                    )
            )

            // Centered floating card above the scrim
            Card(
                modifier = modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .testTag("card_settlement_celebration")
                    .graphicsLayer {
                        scaleX = popupScale.value
                        scaleY = popupScale.value
                        alpha = popupAlpha.value
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Close button at top-right with >= 48dp touch target
                    IconButton(
                        onClick = dismissAction,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(48.dp)
                            .testTag("btn_dismiss_celebration")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.celebration_dismiss_cd),
                            tint = Slate500,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Emerald circular badge with a checkmark
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreenLight)
                                .testTag("badge_celebration_checkmark"),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("canvas_celebration_checkmark")
                            ) {
                                val w = size.width
                                val h = size.height
                                val strokePx = 3.5.dp.toPx()

                                val fullPath = Path().apply {
                                    moveTo(w * 0.20f, h * 0.52f)
                                    lineTo(w * 0.42f, h * 0.74f)
                                    lineTo(w * 0.80f, h * 0.28f)
                                }

                                if (checkmarkProgress.value >= 1f) {
                                    drawPath(
                                        path = fullPath,
                                        color = EmeraldGreenDark,
                                        style = Stroke(
                                            width = strokePx,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                } else if (checkmarkProgress.value > 0f) {
                                    val pathMeasure = PathMeasure()
                                    pathMeasure.setPath(fullPath, false)
                                    val length = pathMeasure.length
                                    val dst = Path()
                                    pathMeasure.getSegment(0f, length * checkmarkProgress.value, dst, true)
                                    drawPath(
                                        path = dst,
                                        color = EmeraldGreenDark,
                                        style = Stroke(
                                            width = strokePx,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Smaller introductory line: “Tere mere hisaab…”
                        Text(
                            text = introText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("text_celebration_intro")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Large bold headline: “Phittoos!”
                        Text(
                            text = headlineText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .testTag("text_celebration_headline")
                                .graphicsLayer {
                                    alpha = headlineAlpha.value
                                    scaleX = headlineScale.value
                                    scaleY = headlineScale.value
                                }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. Supporting line: “%1$s ke saath koi hisaab baaki nahi.”
                        Text(
                            text = supportText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate700,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("text_celebration_support")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility alias for SettlementCelebrationCard.
 */
@Composable
fun SettlementCelebrationCard(
    event: SettlementCelebrationEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettlementCelebrationDialog(
        event = event,
        friendName = null,
        onDismiss = onDismiss,
        modifier = modifier
    )
}
