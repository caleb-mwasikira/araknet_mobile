package com.example.araknet.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.araknet.R
import com.example.araknet.data.AuthError
import com.example.araknet.data.AuthState
import com.example.araknet.data.AuthViewModel
import com.example.araknet.ui.theme.AraknetTheme
import kotlinx.coroutines.runBlocking

@Composable
fun LoginScreen(
    navigateToSignup: () -> Unit,
    navigateToHomePage: () -> Unit,
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
            LoginHeader()
            LoginForm(
                navigateToSignup = navigateToSignup,
                navigateToHomePage = navigateToHomePage,
            )
        }
    }
}

@Composable
fun LoginHeader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(
                text = "Get Started Now",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Enter your credentials to access your account",
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
fun LoginForm(
    modifier: Modifier = Modifier,
    navigateToSignup: () -> Unit,
    navigateToHomePage: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth(),
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
            labelText = "Email Address",
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
            displayForgotPassword = true,
        )

        ElevatedButton(
            onClick = {
                runBlocking {
                    authViewModel.loginUser()

                    if (authViewModel.authState == AuthState.Success) {
                        Toast.makeText(
                            context,
                            "Login success",
                            Toast.LENGTH_LONG
                        ).show()

                        navigateToHomePage()
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = MaterialTheme.colorScheme.primary,
            )
        ) {
            Text(
                text = "Login",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Don't have an account?",
                style = MaterialTheme.typography.bodyMedium,
            )

            Surface(
                onClick = { navigateToSignup() },
                modifier = Modifier.padding(0.dp),
            ) {
                Text(
                    text = " Register",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun FormInputField(
    labelText: String,
    value: String,
    onValueChanged: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = Modifier
            .fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge,
        label = {
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelLarge
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = keyboardType
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun FormPasswordField(
    labelText: String,
    value: String,
    onValueChanged: (String) -> Unit,
    displayForgotPassword: Boolean = false,
) {
    var passwordVisible: Boolean by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Bottom,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier
                .fillMaxWidth(),
            label = {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelLarge
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(id = if (passwordVisible) R.drawable.visibility_off else R.drawable.visibility),
                        contentDescription = "display password",
                        tint = MaterialTheme.colorScheme.surfaceTint,
                    )
                }
            },
            shape = MaterialTheme.shapes.medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // display error messages here
            Text(text = "")

            if (displayForgotPassword) {
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

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    AraknetTheme {
        LoginScreen(
            navigateToSignup = {},
            navigateToHomePage = {}
        )
    }
}