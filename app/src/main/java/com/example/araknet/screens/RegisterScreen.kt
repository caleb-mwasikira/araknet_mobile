package com.example.araknet.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.araknet.MainActivity
import com.example.araknet.R
import com.example.araknet.data.AuthViewModel
import com.example.araknet.data.Error
import com.example.araknet.data.Status
import com.example.araknet.screens.widgets.ErrorMessage
import com.example.araknet.screens.widgets.InputField
import com.example.araknet.screens.widgets.PasswordField
import com.example.araknet.ui.theme.AraknetTheme
import com.example.araknet.utils.titlecase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    navigateToLogin: () -> Unit
) {
    val isConnected by MainActivity.networkMonitor.isConnected.collectAsState()
    val context = LocalContext.current

    // Run once when screen is first composed
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            Toast.makeText(context, "No Internet Connection", Toast.LENGTH_LONG)
                .show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal=12.dp),
            ) {
                Text(
                    text = "Get started with secure browsing",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Create your account and enjoy a fast, private, and secure internet experience.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            RegisterForm(
                navigateToLogin = navigateToLogin,
                isConnected = isConnected,
            )
        }
    }
}

@Composable
fun RegisterForm(
    modifier: Modifier = Modifier,
    navigateToLogin: () -> Unit,
    isConnected: Boolean = false,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var authError by remember { mutableStateOf<Error?>(null) }
    val authStatus by authViewModel.authStatus.collectAsState()

    // Runs when authStatus changes
    LaunchedEffect(authStatus) {
        if (authStatus == Status.Success) {
            val message = "Registration successful. Redirecting to Login Page"
            Log.d(MainActivity.TAG, message)

            Toast.makeText(context, message, Toast.LENGTH_LONG)
                .show()

            // Delay for a few seconds for user to read Toast message
            delay(1000)
            navigateToLogin()
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.authErrors.collect { error ->
            authError = error

            launch {
                delay(5000)  // Hide after n seconds
                authError = null
            }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.ioErrors.collect { error ->
            Toast.makeText(context, error.message.titlecase(), Toast.LENGTH_LONG)
                .show()
        }
    }

    authError?.let {
        ErrorMessage(
            it.message.titlecase(),
            onDismissRequest = {
                authError = null
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        InputField(
            labelText = "Username",
            value = authViewModel.username,
            onValueChanged = { newValue ->
                authViewModel.username = newValue
            },
            leadingIconId = R.drawable.person_24dp,
        )

        InputField(
            labelText = "Email",
            value = authViewModel.email,
            onValueChanged = { newValue ->
                authViewModel.email = newValue
            },
            keyboardType = KeyboardType.Email,
            leadingIconId = R.drawable.mail_24dp,
        )

        PasswordField(
            labelText = "Password",
            value = authViewModel.password,
            onValueChanged = { newValue ->
                authViewModel.password = newValue
            },
        )

        // Terms And Conditions section
        Row(
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
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            val containerColor = if (isConnected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
            val contentColor = if (isConnected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val coroutineScope = rememberCoroutineScope()

            ElevatedButton(
                onClick = {
                    if (!isConnected) {
                        Toast.makeText(context, "No Internet Connection", Toast.LENGTH_LONG)
                            .show()
                        return@ElevatedButton
                    }

                    coroutineScope.launch {
                        authViewModel.registerUser()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = containerColor,
                    contentColor = contentColor,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "Create Account",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyLarge,
                )

                TextButton(onClick = { navigateToLogin() }) {
                    Text(
                        text = "Sign in",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRegisterScreen() {
    AraknetTheme {
        RegisterScreen(navigateToLogin = {})
    }
}