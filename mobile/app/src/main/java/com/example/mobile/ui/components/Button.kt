package com.example.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.AccentBlueHover
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary

enum class ButtonVariant {
    Primary,
    Secondary,
    Outline,
    Ghost,
    Destructive
}

enum class ButtonSize {
    Small,
    Medium,
    Large
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val (backgroundColor, textColor, borderColor) = when (variant) {
        ButtonVariant.Primary -> {
            val bg = if (enabled && !loading) {
                if (isPressed) DarkSurfaceVariant else DarkSurface
            } else {
                DarkSurface.copy(alpha = 0.5f)
            }
            Triple(bg, DarkTextPrimary, Color.Transparent)
        }
        ButtonVariant.Secondary -> {
            val bg = if (enabled && !loading) {
                if (isPressed) DarkSurfaceVariant else DarkSurface
            } else {
                DarkSurface.copy(alpha = 0.5f)
            }
            Triple(bg, DarkTextPrimary, Color.Transparent)
        }
        ButtonVariant.Outline -> {
            val bg = Color.Transparent
            val border = if (enabled && !loading) DarkBorder else DarkBorder.copy(alpha = 0.5f)
            val text = if (enabled && !loading) DarkTextPrimary else DarkTextSecondary
            Triple(bg, text, border)
        }
        ButtonVariant.Ghost -> {
            val bg = if (enabled && !loading && isPressed) {
                DarkSurfaceVariant
            } else {
                Color.Transparent
            }
            Triple(bg, DarkTextPrimary, Color.Transparent)
        }
        ButtonVariant.Destructive -> {
            val bg = if (enabled && !loading) {
                if (isPressed) Color(0xFFD32F2F) else Color(0xFFFF3B30)
            } else {
                Color(0xFFFF3B30).copy(alpha = 0.5f)
            }
            Triple(bg, Color.White, Color.Transparent)
        }
    }
    
    val animatedBackground = backgroundColor
    val animatedTextColor = textColor
    val animatedBorderColor = borderColor
    
    val (height, horizontalPadding, verticalPadding, fontSize) = when (size) {
        ButtonSize.Small -> Quadruple(32.dp, 12.dp, 8.dp, 13.sp)
        ButtonSize.Medium -> Quadruple(40.dp, 18.dp, 10.dp, 15.sp)
        ButtonSize.Large -> Quadruple(48.dp, 20.dp, 14.dp, 17.sp)
    }
    
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (variant == ButtonVariant.Outline) {
                    Modifier.border(1.dp, animatedBorderColor, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            )
            .background(animatedBackground)
            .clickable(
                enabled = enabled && !loading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = animatedTextColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = animatedTextColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

