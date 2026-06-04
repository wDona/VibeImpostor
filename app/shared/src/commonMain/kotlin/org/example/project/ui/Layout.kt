package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Spacing.lg,
    verticalSpacing: Dp = Spacing.lg,
    scroll: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val outer = Modifier.fillMaxSize()
    val outerScroll = if (scroll) outer.verticalScroll(rememberScrollState()) else outer
    Column(
        modifier = outerScroll,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content
        )
    }
}
