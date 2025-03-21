package com.example.araknet.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.araknet.screens.widgets.SoftKeyboard

@Composable
fun EnterOtpNumber(
    requiredOtpLength: Int,
    otpNumber: String,
    onNewOtpValue: (String) -> Unit,
    resendOtp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp),
        ) {
            Text(
                "Enter OTP code",
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                "An OTP code has been sent to your email. Enter OTP code to continue",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(requiredOtpLength) { index ->
                        val isColored = index < otpNumber.length
                        val borderColor =
                            if (isColored) Color.Transparent else MaterialTheme.colorScheme.scrim
                        val backgroundColor =
                            if (isColored) MaterialTheme.colorScheme.tertiary else Color.Transparent

                        val otpChar: String = otpNumber.toCharArray().getOrNull(index)?.toString() ?: ""

                        Box(
                            modifier = Modifier
                                .border(2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                                .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
                                .size(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                otpChar,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }

                TextButton(
                    onClick = resendOtp,
                ) {
                    Text(
                        "Get new code or resend",
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }

            SoftKeyboard(
                value = otpNumber,
                onValueChange = { newValue ->
                    if(newValue.isNotEmpty() && newValue.last() == '.') {
                        return@SoftKeyboard
                    }

                    onNewOtpValue(newValue)
                },
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun PreviewEnterOtpNumber() {
    MaterialTheme {
        EnterOtpNumber(
            requiredOtpLength = 6,
            otpNumber = "123",
            onNewOtpValue = {},
            resendOtp = {},
        )
    }
}