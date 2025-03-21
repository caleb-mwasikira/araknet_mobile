package com.example.araknet.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.araknet.ui.theme.AraknetTheme
import com.example.araknet.utils.titlecase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navigateToLoginScreen: () -> Unit,
    navigateToResetPasswordScreen: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
) {
    val isConnected by MainActivity.networkMonitor.isConnected.collectAsState()
    val context = LocalContext.current
    val authStatus by authViewModel.authStatus.collectAsState()
    var authError by remember { mutableStateOf<Error?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.authErrors.collectLatest { error ->
            authError = error

            launch {
                delay(5000)  // Hide after n seconds
                authError = null
            }
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

    LaunchedEffect(Unit) {
        authViewModel.ioErrors.collect { error ->
            Toast.makeText(context, error.message.titlecase(), Toast.LENGTH_LONG)
                .show()
        }
    }

    // Runs when authStatus changes
    LaunchedEffect(authStatus) {
        if (authStatus == Status.Success) {
            val message = "Password reset email sent successfully"
            Log.d(MainActivity.TAG, message)

            Toast.makeText(context, message, Toast.LENGTH_LONG)
                .show()

            // Delay for a few seconds for user to read Toast message
            delay(1000)
            navigateToResetPasswordScreen(authViewModel.email)
        }
    }

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
            .padding(24.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = navigateToLoginScreen,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_left_24dp),
                            contentDescription = "Back to Login Page",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Text(
                    text = "Enter your email to reset your accounts password",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            ForgotPasswordForm(
                email = authViewModel.email,
                onEmailChange = { newEmail ->
                    authViewModel.email = newEmail
                },
                onSendForgotPasswordForm = {
                    authViewModel.forgotPassword()
                }
            )
        }
    }
}

@Composable
fun ForgotPasswordForm(
    email: String,
    onEmailChange: (String) -> Unit,
    onSendForgotPasswordForm: () -> Unit,
    btnContainerColor: Color = MaterialTheme.colorScheme.primary,
    btnContentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        InputField(
            labelText = "Email",
            value = email,
            onValueChanged = { newValue ->
                onEmailChange(newValue)
            },
            leadingIconId = R.drawable.mail_24dp,
            keyboardType = KeyboardType.Email,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            ElevatedButton(
                onClick = onSendForgotPasswordForm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = btnContainerColor,
                    contentColor = btnContentColor,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "Send Password Reset",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewForgotPasswordScreen() {
    AraknetTheme {
        ForgotPasswordScreen(
            navigateToLoginScreen = {},
            navigateToResetPasswordScreen = {},
        )
    }
}