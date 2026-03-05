package com.example.mobile.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary

@Composable
fun BetInput(
    bet: Int,
    onBetChange: (Int) -> Unit,
    maxBet: Int,
    modifier: Modifier = Modifier,
    minBet: Int = 1,
    step: Int = 10,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppButton(
            text = "-$step",
            onClick = {
                val newBet = (bet - step).coerceAtLeast(minBet)
                onBetChange(newBet)
            },
            variant = ButtonVariant.Outline,
            size = ButtonSize.Medium,
            enabled = enabled && bet > minBet,
            modifier = Modifier.weight(1f)
        )

        TextField(
            value = if (bet == 0) "" else bet.toString(),
            onValueChange = { raw ->
                val filtered = raw.filter { char -> char.isDigit() }
                if (filtered.isEmpty()) {
                    onBetChange(0)
                } else {
                    val parsed = filtered.toIntOrNull() ?: 0
                    onBetChange(parsed.coerceIn(0, maxBet))
                }
            },
            modifier = Modifier
                .width(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = DarkTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            placeholder = {
                Text(
                    text = "Mise",
                    color = DarkTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                disabledContainerColor = DarkSurface.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = AccentBlue
            )
        )

        AppButton(
            text = "+$step",
            onClick = {
                val newBet = (bet + step).coerceAtMost(maxBet)
                onBetChange(newBet)
            },
            variant = ButtonVariant.Outline,
            size = ButtonSize.Medium,
            enabled = enabled && bet < maxBet,
            modifier = Modifier.weight(1f)
        )
    }
}
