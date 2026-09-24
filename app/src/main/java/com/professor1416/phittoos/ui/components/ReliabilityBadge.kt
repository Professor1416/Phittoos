package com.professor1416.phittoos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professor1416.phittoos.domain.ReliabilityInfo
import com.professor1416.phittoos.domain.ReliabilityLevel
import com.professor1416.phittoos.ui.theme.PhittoosColors

/**
 * Compact, accessible reliability level pill.
 * Color is always accompanied by clear textual label (New, Green, Yellow, Red).
 */
@Composable
fun ReliabilityPill(
    level: ReliabilityLevel,
    modifier: Modifier = Modifier
) {
    val financialColors = PhittoosColors.financial
    val (bgColor, textColor, label) = when (level) {
        ReliabilityLevel.NEW -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "New"
        )
        ReliabilityLevel.GREEN -> Triple(
            financialColors.lentContainer,
            financialColors.onLentContainer,
            "Green"
        )
        ReliabilityLevel.YELLOW -> Triple(
            financialColors.partialContainer,
            financialColors.onPartialContainer,
            "Yellow"
        )
        ReliabilityLevel.RED -> Triple(
            financialColors.overdueContainer,
            financialColors.onOverdueContainer,
            "Red"
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier.testTag("reliability_pill_${level.name.lowercase()}")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/**
 * Detailed private reliability section displayed on Friend Detail screen.
 * Shows level, summary explanation, and explicit privacy reassurance.
 */
@Composable
fun FriendDetailReliabilitySection(
    reliability: ReliabilityInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("friend_detail_reliability_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Reliability",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                ReliabilityPill(level = reliability.level)
            }

            // Private indicator badge
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Only you can see this",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = reliability.summaryText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}
