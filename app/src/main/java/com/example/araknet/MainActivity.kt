package com.example.araknet

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.araknet.data.ApiService
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
