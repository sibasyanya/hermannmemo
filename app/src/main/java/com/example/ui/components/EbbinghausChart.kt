package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.exp

@Composable
fun EbbinghausChart(
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Кривая забывания Эббингауза",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Динамика сохранения информации в памяти с интервалами и без них",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val w = size.width
                val h = size.height
                val paddingLeft = 32f
                val paddingBottom = 24f
                val chartW = w - paddingLeft - 8f
                val chartH = h - paddingBottom - 8f

                // Draw Grid lines
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(paddingLeft, 8f),
                    end = Offset(paddingLeft, h - paddingBottom),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(paddingLeft, h - paddingBottom),
                    end = Offset(w - 8f, h - paddingBottom),
                    strokeWidth = 1.5f
                )

                // 1. Natural forgetting curve without reviews (Red/Orange dashed or solid)
                val decayPath = Path()
                decayPath.moveTo(paddingLeft, 8f) // 100%
                val steps = 60
                for (i in 1..steps) {
                    val progress = i / steps.toFloat()
                    val x = paddingLeft + progress * chartW
                    // Ebbinghaus formula approximation: R = exp(-t/S)
                    val r = 0.2f + 0.8f * exp(-progress * 4.5f)
                    val y = (h - paddingBottom) - (r * chartH)
                    decayPath.lineTo(x, y)
                }
                drawPath(
                    path = decayPath,
                    color = Color(0xFFEF4444),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // 2. Repetition 1 (jumps back at 20% width)
                val rep1X = paddingLeft + chartW * 0.20f
                val rep1Path = Path()
                rep1Path.moveTo(rep1X, 8f)
                for (i in 1..40) {
                    val progress = i / 40f
                    val x = rep1X + progress * (chartW * 0.80f)
                    val r = 0.45f + 0.55f * exp(-progress * 2.8f)
                    val y = (h - paddingBottom) - (r * chartH)
                    rep1Path.lineTo(x, y)
                }
                drawPath(
                    path = rep1Path,
                    color = Color(0xFF3B82F6),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // 3. Repetition 2 (jumps back at 45% width)
                val rep2X = paddingLeft + chartW * 0.45f
                val rep2Path = Path()
                rep2Path.moveTo(rep2X, 8f)
                for (i in 1..30) {
                    val progress = i / 30f
                    val x = rep2X + progress * (chartW * 0.55f)
                    val r = 0.70f + 0.30f * exp(-progress * 1.5f)
                    val y = (h - paddingBottom) - (r * chartH)
                    rep2Path.lineTo(x, y)
                }
                drawPath(
                    path = rep2Path,
                    color = Color(0xFF10B981),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Draw intervention markers
                drawCircle(Color(0xFF3B82F6), radius = 5f, center = Offset(rep1X, 8f))
                drawCircle(Color(0xFF10B981), radius = 5f, center = Offset(rep2X, 8f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(color = Color(0xFFEF4444), label = "Без повторений")
                LegendItem(color = Color(0xFF3B82F6), label = "1-е повторение")
                LegendItem(color = Color(0xFF10B981), label = "В долговременную память")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
