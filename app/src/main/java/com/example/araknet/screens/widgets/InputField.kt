package com.example.araknet.screens.widgets

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.araknet.R


@Composable
fun textFieldColors() = TextFieldDefaults.colors().copy(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
)


@Composable
fun InputField(
    labelText: String,
    value: String,
    onValueChanged: (String) -> Unit,
    @DrawableRes leadingIconId: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodyLarge,
        )

        TextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp)),
            textStyle = MaterialTheme.typography.bodyLarge,
            leadingIcon = {
                // Display leading icon if leadingIconId is not null
                leadingIconId?.let {
                    Icon(
                        painter = painterResource(leadingIconId),
                        contentDescription = null,
                    )
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = keyboardType
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = textFieldColors()
        )

    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInputField() {
    MaterialTheme {
        InputField(
            labelText = "Enter Username",
            leadingIconId = R.drawable.person_24dp,
            value = "Death By Romy",
            onValueChanged = {},
        )
    }
}