package com.example.araknet.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.araknet.data.AuthError
import com.example.araknet.data.AuthState
import com.example.araknet.data.AuthViewModel
import com.example.araknet.ui.theme.AraknetTheme
import kotlinx.coroutines.runBlocking

@Composable
fun RegisterScreen(
    navigateToLogin: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState, enabled = true),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            RegisterHeader()
            RegisterForm(
                navigateToLogin = navigateToLogin
            )
        }
    }
}

@Composable
fun RegisterHeader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(
                text = "Create Account", style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Welcome to Araknet",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surfaceTint,
            )
        }

        // ----- or -----
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "or",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surfaceTint,
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RegisterForm(
    modifier: Modifier = Modifier,
    navigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val errors = buildList<AuthError> {
            addAll(authViewModel.fieldErrors)
            addAll(authViewModel.serverErrors)
        }

        if (errors.isNotEmpty()) {
            val errMessage = errors.first().errMessage
            ErrMessage(errMessage = errMessage)
        }

        FormInputField(
            labelText = "Username",
            value = authViewModel.username,
            onValueChanged = { newValue ->
                authViewModel.username = newValue
            },
        )

        FormInputField(
            labelText = "Email",
            value = authViewModel.email,
            onValueChanged = { newValue ->
                authViewModel.email = newValue
            },
            keyboardType = KeyboardType.Email,
        )

        FormPasswordField(
            labelText = "Password",
            value = authViewModel.password,
            onValueChanged = { newValue ->
                authViewModel.password = newValue
            },
        )

        Column {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = authViewModel.agreedToTerms,
                    onCheckedChange = { value ->
                        authViewModel.agreedToTerms = value
                    },
                )

                Text(
                    text = "I agree to the Terms and Conditions",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            ElevatedButton(
                onClick = {
                    runBlocking {
                        authViewModel.registerUser()

                        if (authViewModel.authState == AuthState.Success) {
                            Toast.makeText(
                                context, "Account created successfully", Toast.LENGTH_LONG
                            ).show()

                            navigateToLogin()
                            return@runBlocking
                        }

                        //
                        authViewModel.connectionErrors.forEach { error ->
                            Toast.makeText(
                                context,
                                error.errMessage,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = MaterialTheme.colorScheme.primary,
                )
            ) {
                Text(
                    text = "Create Account",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
            )

            Surface(
                onClick = { navigateToLogin() },
                modifier = Modifier.padding(0.dp),
            ) {
                Text(
                    text = " Sign in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun ErrMessage(errMessage: String) {
    Surface(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = errMessage,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun PreviewErrMessage() {
    AraknetTheme {
        ErrMessage(errMessage = "Test error message")
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewRegisterScreen() {
    AraknetTheme {
        RegisterScreen(navigateToLogin = {})
    }
}