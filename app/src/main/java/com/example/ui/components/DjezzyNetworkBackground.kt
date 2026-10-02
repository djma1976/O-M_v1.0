package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R

/**
 * Global Djezzy network globe background used across all screens.
 * Renders the high-tech network globe image with a translucent gradient scrim,
 * allowing input, button, and card areas to remain clear, readable, and touch-accessible.
 */
@Composable
fun DjezzyNetworkBackground(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        // High-tech telecom network globe background image
        Image(
            painter = painterResource(id = R.drawable.network),
            contentDescription = "Djezzy Telecom Network Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Translucent gradient scrim matching recovery and login pages
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55060512),
                            Color(0x770B081B),
                            Color(0x9912091E)
                        )
                    )
                )
        )

        // Screen content (inputs, buttons, cards, headers)
        content()
    }
}
