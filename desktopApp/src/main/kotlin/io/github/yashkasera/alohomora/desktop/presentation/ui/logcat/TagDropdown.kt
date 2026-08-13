package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenu
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField

@Composable
fun TagDropdown(
    selectedTag: String?,
    availableTags: List<String>,
    onSelectTag: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selectedTag ?: "All tags"

    Box(modifier = Modifier.width(240.dp)) {
        AlohomoraTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            placeholder = "Tag",
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.Transparent)
                .clickable { expanded = true },
        )
        AlohomoraDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AlohomoraDropdownMenuItem(
                text = { Text("All tags") },
                onClick = {
                    expanded = false
                    onSelectTag(null)
                },
            )
            availableTags.forEach { tag ->
                AlohomoraDropdownMenuItem(
                    text = { Text(tag) },
                    onClick = {
                        expanded = false
                        onSelectTag(tag)
                    },
                )
            }
        }
    }
}
