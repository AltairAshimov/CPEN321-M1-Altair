package com.example.cpen321application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cpen321application.ui.navigation.AppDestinations
import com.example.cpen321application.ui.screens.HomeScreen
import com.example.cpen321application.ui.screens.LoginServerScreen
import com.example.cpen321application.ui.screens.PlaceholderScreen
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(AppDestinations.HOME) {
            HomeScreen(navController)
        }

        composable(AppDestinations.LOGIN_SERVER) {
            LoginServerScreen(navController)
        }

        composable(AppDestinations.LIVE_UPDATES) {
            PlaceholderScreen(
                title = "Live Updates",
                navController = navController,
            )
        }

        composable(AppDestinations.TIMER) {
            PlaceholderScreen(
                title = "Timer",
                navController = navController,
            )
        }
    }
}