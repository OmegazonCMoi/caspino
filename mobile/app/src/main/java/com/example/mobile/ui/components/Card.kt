package com.example.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkSurfaceVariant

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    variant: CardVariant = CardVariant.Default,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val (backgroundColor, borderColor) = when (variant) {
        CardVariant.Default -> DarkSurface to DarkBorder
        CardVariant.Elevated -> DarkSurfaceVariant to DarkBorder
        CardVariant.Outlined -> DarkSurface to DarkBorder
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .then(
                if (variant == CardVariant.Outlined) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

enum class CardVariant {
    Default,
    Elevated,
    Outlined
}

