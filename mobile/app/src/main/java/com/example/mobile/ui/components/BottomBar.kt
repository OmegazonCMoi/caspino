package com.example.mobile.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextSecondary

data class BottomBarItem(
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null,
    val onClick: () -> Unit
)

@Composable
fun AppBottomBar(
    items: List<BottomBarItem>,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    var translateY by remember { mutableStateOf(100f) }
    
    val animatedTranslateY by animateFloatAsState(
        targetValue = translateY,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "bottom_bar_translate"
    )
    
    LaunchedEffect(Unit) {
        delay(300)
        translateY = 0f
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .offset(y = animatedTranslateY.dp)
    ) {
        // Fond avec blur
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect
                            .createBlurEffect(30f, 30f, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkSurface.copy(alpha = 0.85f),
                            DarkSurface.copy(alpha = 0.9f)
                        )
                    )
                )
        )
        
        // Icônes au-dessus (sans blur)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { index, item ->
                BottomBarItemComposable(
                    item = item,
                    isSelected = index == selectedIndex,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItemComposable(
    item: BottomBarItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val iconColor = if (isSelected) AccentBlue else DarkTextSecondary
    val backgroundColor = if (isPressed) {
        DarkTextSecondary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }
    
    val iconToShow = if (isSelected && item.selectedIcon != null) {
        item.selectedIcon
    } else {
        item.icon
    }
    
    // Si c'est une icône filled, on utilise blanc, sinon on utilise la couleur normale
    val finalIconColor = if (isSelected && item.selectedIcon != null) {
        Color.White
    } else {
        iconColor
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = item.onClick
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = iconToShow,
                contentDescription = item.icon.toString(),
                modifier = Modifier.size(24.dp),
                tint = finalIconColor
            )
        }
    }
}

