package com.example.araknet

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.araknet.data.ApiService
import com.example.araknet.data.AraknetVpnService
import com.example.araknet.data.NetworkMonitor
import com.example.araknet.data.getRetrofitBuilder
import com.example.araknet.screens.ForgotPasswordScreen
import com.example.araknet.screens.HomeScreen
import com.example.araknet.screens.LoginScreen
import com.example.araknet.screens.RegisterScreen
import com.example.araknet.screens.ResetPasswordScreen
import com.example.araknet.screens.Routes
import com.example.araknet.ui.theme.AraknetTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG: String = "Araknet"
        lateinit var networkMonitor: NetworkMonitor
        var retrofitService: ApiService? = null
    }

    private val REQUEST_CODE_VPN = 1001

    // Handling the result from the VPN preparation request
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_VPN) {
            if (resultCode == RESULT_OK) {
                // User has allowed VPN configuration, start the VPN service
                startVpnService()
            } else {
                Toast.makeText(this, "VPN service permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVpnService() {
        val vpnServiceIntent = Intent(this, AraknetVpnService::class.java)
        startService(vpnServiceIntent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkMonitor = NetworkMonitor(this)
        retrofitService = lazy {
            val retrofitBuilder = getRetrofitBuilder(applicationContext)
            retrofitBuilder.create(ApiService::class.java)
        }.value

        enableEdgeToEdge()
        setContent {
            AraknetTheme {
                val navController = rememberNavController()

                val context = LocalContext.current
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    context.startActivity(intent) // Wait for result before starting VPN
                } else {
                    context.startService(Intent(context, AraknetVpnService::class.java))
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.LoginScreen.name,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(route = Routes.LoginScreen.name) {
                            LoginScreen(
                                navigateToSignup = {
                                    navController.navigate(route = Routes.RegisterScreen.name)
                                },
                                navigateToHomePage = {
                                    navController.navigate(route = Routes.HomeScreen.name)
                                },
                                navigateToForgotPasswordScreen = {
                                    navController.navigate(route = Routes.ForgotPasswordScreen.name)
                                }
                            )
                        }

                        composable(route = Routes.RegisterScreen.name) {
                            RegisterScreen(
                                navigateToLogin = {
                                    navController.navigate(route = Routes.LoginScreen.name)
                                }
                            )
                        }

                        composable(route = Routes.HomeScreen.name) {
                            HomeScreen()
                        }

                        composable(route = Routes.ForgotPasswordScreen.name) {
                            ForgotPasswordScreen(
                                navigateToLoginScreen = {
                                    navController.navigate(Routes.LoginScreen.name)
                                },
                                navigateToResetPasswordScreen = { email ->
                                    navController.navigate("${Routes.ResetPasswordScreen.name}/$email")
                                }
                            )
                        }

                        composable(
                            route = "${Routes.ResetPasswordScreen.name}/{email}",
                            arguments = listOf(navArgument("email") {
                                type = NavType.StringType
                            })  // Specify argument type
                        ) { backStackEntry ->
                            val email = backStackEntry.arguments?.getString("email")
                                ?: ""  // Retrieve argument

                            ResetPasswordScreen(
                                email = email,
                                navigateToLoginScreen = {
                                    navController.navigate(Routes.LoginScreen.name)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
