package com.fishlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RecentValueChips(
    values: List<String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(values) { value ->
            AssistChip(
                onClick = { onValueSelected(value) },
                label = { 
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelSmall
                    ) 
                }
            )
        }
    }
}
