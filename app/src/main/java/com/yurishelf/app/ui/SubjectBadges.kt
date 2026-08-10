package com.yurishelf.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yurishelf.app.domain.AiYuriCategory
import com.yurishelf.app.domain.BangumiCollectionType

@Composable
fun AiCategoryBadge(
    category: AiYuriCategory,
    riskCount: Int,
    modifier: Modifier = Modifier,
) {
    val (background, content) = when (category) {
        AiYuriCategory.STRONG -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        AiYuriCategory.LIGHT -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
        AiYuriCategory.NON -> MaterialTheme.colorScheme.surfaceVariant to
            MaterialTheme.colorScheme.onSurfaceVariant
        AiYuriCategory.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = "AI·${category.label}",
            color = content,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (riskCount > 0) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = "$riskCount 个雷点",
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = riskCount.toString(),
                color = content,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
fun BangumiCollectionBadge(
    type: BangumiCollectionType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.Transparent, RoundedCornerShape(6.dp))
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "B·${type.label}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
