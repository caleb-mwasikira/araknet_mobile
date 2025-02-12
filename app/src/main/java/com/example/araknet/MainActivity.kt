package com.example.araknet

import android.net.Network
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.araknet.data.NetworkMonitor
import com.example.araknet.screens.HomeScreen
import com.example.araknet.screens.LoginScreen
import com.example.araknet.screens.RegisterScreen
import com.example.araknet.screens.Routes
import com.example.araknet.ui.theme.AraknetTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG: String = "Araknet"
        lateinit var networkMonitor: NetworkMonitor
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkMonitor = NetworkMonitor(this)

        enableEdgeToEdge()
        setContent {
            AraknetTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.RegisterScreen.name,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(route = Routes.LoginScreen.name) {
                            LoginScreen(
                                navigateToSignup = {
                                    navController.navigate(route = Routes.RegisterScreen.name)
                                },
                                navigateToHomePage = {
                                    navController.navigate(
                                        route = Routes.HomeScreen.name
                                    )
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

                    }
                }
            }
        }
    }
}
