package com.example.araknet.screens.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.araknet.R

@Composable
fun PasswordField(
    labelText: String,
    value: String,
    onValueChanged: (String) -> Unit,
    navigateToForgotPasswordScreen: (() -> Unit)? = null,
) {
    var passwordVisible: Boolean by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
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
                Icon(
                    painter = painterResource(R.drawable.lock_24dp),
                    contentDescription = null,
                )
            },
            trailingIcon = {
                val trailingIconId: Int = if (passwordVisible) {
                    R.drawable.visibility_off
                } else {
                    R.drawable.visibility
                }

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(id = trailingIconId),
                        contentDescription = "display password",
                        tint = MaterialTheme.colorScheme.surfaceTint,
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Password
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = textFieldColors(),
        )

        navigateToForgotPasswordScreen?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = navigateToForgotPasswordScreen,
                ) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPasswordField() {
    MaterialTheme {
        PasswordField(
            labelText = "Enter Password",
            value = "NoMercy",
            onValueChanged = {},
        )
    }
}