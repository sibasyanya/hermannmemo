package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningContainer
import com.example.ui.theme.AmberWarningContainerDark

@Composable
fun BacklogBanner(
    overdueCount: Int,
    isBacklogActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (overdueCount <= 0) return

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isBacklogActive) {
        if (isDark) AmberWarningContainerDark else AmberWarningContainer
    } else {
        if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
    }
    val borderColor = if (isBacklogActive) AmberWarning else Color(0xFF60A5FA)
    val iconColor = if (isBacklogActive) AmberWarning else Color(0xFF3B82F6)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("backlog_banner")
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (isBacklogActive) Icons.Default.Warning else Icons.Default.Shield,
                contentDescription = "Статус долгов",
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBacklogActive) {
                        "Защита от лавины долгов активна"
                    } else {
                        "Накопилось повторений: $overdueCount"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBacklogActive) {
                        "Очередь заморожена ($overdueCount карточек). Материал выдается порциями по 30–35 шт. в день. Изучение новых карточек заблокировано до закрытия долгов."
                    } else {
                        "Повторите накопившиеся карточки, чтобы закрепить знания по кривой забывания Эббингауза."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
