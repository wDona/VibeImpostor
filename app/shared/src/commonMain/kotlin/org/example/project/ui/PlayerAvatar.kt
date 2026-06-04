package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlayerAvatar(
    name: String,
    colorIndex: Int,
    size: Dp = 36.dp,
    dimmed: Boolean = false,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val base = playerColor(colorIndex)
    val bg = if (dimmed) base.copy(alpha = 0.3f) else base
    val ringMod = if (highlight) modifier.border(2.dp, base, CircleShape) else modifier
    Box(
        modifier = ringMod
            .size(size)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
            color = Color.White
        )
    }
}
