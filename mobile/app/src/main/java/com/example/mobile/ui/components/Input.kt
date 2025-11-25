package com.example.mobile.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val borderColor = when {
        isError -> Color(0xFFFF3B30)
        !enabled -> DarkBorder.copy(alpha = 0.5f)
        else -> DarkBorder
    }
    
    val focusedBorderColor = if (isError) {
        Color(0xFFFF3B30)
    } else {
        AccentBlue
    }
    
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DarkTextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                ),
            enabled = enabled,
            singleLine = singleLine,
            placeholder = {
                placeholder?.let {
                    Text(
                        text = it,
                        color = DarkTextSecondary,
                        fontSize = 16.sp
                    )
                }
            },
            textStyle = TextStyle(
                color = DarkTextPrimary,
                fontSize = 16.sp
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurface,
                disabledContainerColor = DarkSurface.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = DarkTextPrimary,
                unfocusedTextColor = DarkTextPrimary,
                disabledTextColor = DarkTextSecondary,
                cursorColor = AccentBlue
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 12.sp,
                color = Color(0xFFFF3B30),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

