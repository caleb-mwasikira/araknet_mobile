package com.example.araknet.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.araknet.R

@Composable
fun SoftKeyboard(
    value: String,
    onValueChange: (newValue: String) -> Unit,
) {
    Column {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(9) { index ->
                val id = "${index + 1}"

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(48.dp)
                        .background(color = Color.Transparent, shape = CircleShape)
                        .clickable {
                            val newValue = value + id
                            onValueChange(newValue)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(color = Color.Transparent, shape = CircleShape)
                    .clickable {
                        if (value.isEmpty() || value.contains(".", ignoreCase = true)) {
                            return@clickable
                        }

                        val newValue = "$value."
                        onValueChange(newValue)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ".",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(color = Color.Transparent, shape = CircleShape)
                    .clickable {
                        val newValue = value + "0"
                        onValueChange(newValue)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            IconButton(
                onClick = {
                    if (value.isEmpty()) {
                        return@IconButton
                    }

                    // Delete last char
                    val newValue = value.take(value.length - 1)
                    onValueChange(newValue)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(color = Color.Transparent, shape = CircleShape)
            ){
                Icon(
                    painter = painterResource(R.drawable.chevron_left_24dp),
                    contentDescription = "Backspace"
                )
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun PreviewSoftKeyboard() {
    MaterialTheme {
        SoftKeyboard(
            value = "",
            onValueChange = {}
        )
    }
}