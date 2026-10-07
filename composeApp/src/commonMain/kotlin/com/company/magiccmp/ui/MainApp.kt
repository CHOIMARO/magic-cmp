package com.company.magiccmp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.company.magiccmp.navigation.AppNavHost

@Composable
fun MainApp() {
    val navHostController = rememberNavController()
    Scaffold() { innerPadding ->
        AppNavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navHostController,
        )
    }
}