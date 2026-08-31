package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus

@Composable
fun StatusBadge(
    status: AttendanceStatus,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val (bgColor, textColor) = status.getBadgeColors(isDark)

    Text(
        text = if (isCompact) status.code else status.displayName,
        color = textColor,
        fontSize = if (isCompact) 11.sp else 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = if (isCompact) 2.dp else 4.dp)
    )
}
