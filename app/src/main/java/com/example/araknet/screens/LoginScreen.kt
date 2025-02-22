package com.example.araknet.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.araknet.MainActivity
import com.example.araknet.R
import com.example.araknet.data.AuthError
import com.example.araknet.data.AuthState
import com.example.araknet.data.AuthViewModel
import com.example.araknet.data.NetworkMonitor
import com.example.araknet.ui.theme.AraknetTheme
import com.example.araknet.utils.titlecase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navigateToSignup: () -> Unit,
    navigateToHomePage: () -> Unit,
    networkMonitor: NetworkMonitor? = null,
    authViewModel: AuthViewModel = viewModel(),
) {
    val isConnected: Boolean = networkMonitor?.isConnected?.collectAsState()?.value ?: false
    val context = LocalContext.current
    var authError by remember { mutableStateOf<AuthError?>(null) }
    val authState by authViewModel.authState.collectAsState()

    // Runs whenever isConnected changes
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            Toast.makeText(context, "No Internet Connection", Toast.LENGTH_LONG)
                .show()
        }
    }

    // Runs whenever authState changes
    LaunchedEffect(authState) {
        if (authState == AuthState.Success) {
            val message = "Login successful. Redirecting to Home Page"
            Log.d(MainActivity.TAG, message)

            Toast.makeText(context, message, Toast.LENGTH_LONG)
                .show()

            // Delay for a few seconds for user to read Toast message
            delay(1000)
            navigateToHomePage()
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.authErrors.collect { error ->
            if (error is AuthError.ValidationError) {
                authError = error
                return@collect
            }

            Toast.makeText(context, error.errMessage.titlecase(), Toast.LENGTH_LONG)
                .show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical=8.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Column {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                )

                Text(
                    text = "Secure your connection and enjoy the internet without limits",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            val coroutineScope = rememberCoroutineScope()

            LoginForm(
                navigateToSignup = navigateToSignup,
                onLoginBtnClicked = {
                    if (!isConnected) {
                        Toast.makeText(context, "No Internet Connection", Toast.LENGTH_LONG)
                            .show()
                        return@LoginForm
                    }

                    coroutineScope.launch {
                        authViewModel.loginUser()
                    }
                },
                isConnected = isConnected,
                authViewModel = authViewModel,
            )

            // Display error messages
            authError?.let { error ->
                ErrorMessage(
                    message = error.errMessage,
                    onDismissRequest = {
                        authError = null
                    }
                )

                coroutineScope.launch {
                    delay(2000)
                    authError = null
                }
            }
        }
    }
}

@Composable
fun RecentLogin(
    email: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val defaultCardColors = CardDefaults.cardColors()
    val containerColor: Color = if (isSelected) MaterialTheme.colorScheme.primary else defaultCardColors.containerColor
    val contentColor: Color = if(isSelected) MaterialTheme.colorScheme.onPrimary else defaultCardColors.contentColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors().copy(
            containerColor = containerColor,
            contentColor = contentColor,
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.person_24dp),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.width(24.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Email Address",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )

                Text(
                    email,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if(isSelected) {
                Icon(
                    painter = painterResource(R.drawable.check_24dp),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
fun LoginForm(
    modifier: Modifier = Modifier,
    navigateToSignup: () -> Unit,
    onLoginBtnClicked: () -> Unit,
    isConnected: Boolean = false,
    authViewModel: AuthViewModel,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InputField(
            labelText = "Email Address",
            value = authViewModel.email,
            onValueChanged = { newValue ->
                authViewModel.email = newValue
            },
            leadingIconId = R.drawable.mail_24dp,
            keyboardType = KeyboardType.Email,
        )

        PasswordField(
            labelText = "Password",
            value = authViewModel.password,
            onValueChanged = { newValue ->
                authViewModel.password = newValue
            },
            displayForgotPassword = true,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
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

            ElevatedButton(
                onClick = onLoginBtnClicked,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = containerColor,
                    contentColor = contentColor,
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Login",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyLarge,
                )

                TextButton(onClick = { navigateToSignup() }) {
                    Text(
                        text = "Register",
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
fun PreviewLoginScreen() {
    AraknetTheme {
        LoginScreen(navigateToSignup = {}, navigateToHomePage = {})
    }
}