package com.example.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary

@Composable
fun CroupierPunchlineBanner(
    punchline: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, shape)
            .border(1.dp, DarkBorder, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\uD83C\uDFB0",
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = punchline,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            color = DarkTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
