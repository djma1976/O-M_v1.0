package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TechBluePrimaryLight

@Composable
fun ChecklistCard(
    checklistRaw: String,
    onToggleItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = checklistRaw.split(";").map { it.trim() }.filter { it.isNotEmpty() }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2D)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateCardBorder)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val completedCount = items.count { it.endsWith("::true") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SAFETY & WORK VERIFICATION CHECKLIST",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TechBluePrimaryLight,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$completedCount / ${items.size} Done",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (completedCount == items.size) OperationalEmerald else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
            }

            items.forEachIndexed { index, itemStr ->
                val parts = itemStr.split("::")
                if (parts.size == 2) {
                    val title = parts[0]
                    val isChecked = parts[1].toBoolean()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleItem(index) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleItem(index) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = OperationalEmerald,
                                uncheckedColor = Color(0xFF64748B),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isChecked) Color(0xFF94A3B8) else Color(0xFFF1F5F9),
                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
