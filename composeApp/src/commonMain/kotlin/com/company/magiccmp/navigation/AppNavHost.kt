package com.company.magiccmp.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.company.feature.home.navigation.HomeGraph
import com.company.feature.home.navigation.homeGraph

@Composable
fun AppNavHost(
    modifier: Modifier,
    navController: NavHostController
) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = HomeGraph,
        ) {
            homeGraph(
                navController = navController,
            )
        }
}