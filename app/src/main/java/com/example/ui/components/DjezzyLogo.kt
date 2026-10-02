package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DjezzyRed

@Composable
fun DjezzyEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Iconic Djezzy rounded triangle pointing to the right
            val path = Path().apply {
                moveTo(w * 0.12f, h * 0.10f)
                // Top curve to apex
                cubicTo(
                    w * 0.20f, h * 0.08f,
                    w * 0.85f, h * 0.44f,
                    w * 0.94f, h * 0.50f
                )
                // Apex rounded corner
                cubicTo(
                    w * 0.98f, h * 0.53f,
                    w * 0.98f, h * 0.57f,
                    w * 0.94f, h * 0.60f
                )
                // Bottom curve back to bottom left
                cubicTo(
                    w * 0.85f, h * 0.66f,
                    w * 0.20f, h * 0.92f,
                    w * 0.12f, h * 0.90f
                )
                // Bottom left corner
                cubicTo(
                    w * 0.06f, h * 0.88f,
                    w * 0.06f, h * 0.82f,
                    w * 0.06f, h * 0.70f
                )
                // Left vertical edge up
                lineTo(w * 0.06f, h * 0.30f)
                // Top left corner
                cubicTo(
                    w * 0.06f, h * 0.18f,
                    w * 0.06f, h * 0.12f,
                    w * 0.12f, h * 0.10f
                )
                close()
            }

            drawPath(
                path = path,
                color = DjezzyRed,
                style = Fill
            )
        }

        // DJEZZY + جازي inside emblem
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(start = size * 0.12f, end = size * 0.25f)
        ) {
            Text(
                text = "DJEZZY",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.20f).sp,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "جازي",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.16f).sp,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}

@Composable
fun DjezzyBrandHeader(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 56.dp,
    showSubtitle: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DjezzyEmblem(size = emblemSize)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "DJEZZY OPS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            if (showSubtitle) {
                Text(
                    text = "Field Maintenance & Telecom Network",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
