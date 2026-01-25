package com.rahul.stocksim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.rahul.stocksim.ui.theme.StockMarketSimulatorTheme
import com.rahul.stocksim.ui.screens.HomeScreen
import com.rahul.stocksim.ui.screens.LoginScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

//MainActivity = starting point of app
//ComponentActivity = base class for all activities
class MainActivity : ComponentActivity() {
    //onCreate is first function called when app starts
    //if app was previously closed, then savedInstanceState
    //contains a Bundle of data from the last state
    override fun onCreate(savedInstanceState: Bundle?) {
        //call this function
        super.onCreate(savedInstanceState)
        //for modern devices to draw content under system bars too
        enableEdgeToEdge()
        //defines layout of activity with Composables
        setContent {
            //instantiates controller responsible for tracking the
            //back stack of screens the user has visited. because
            //wrapped in remember, the navcontroller survives
            //recompositions. navcontroller can navigate by name
            //or pop from stack or navigate or identify current screen
            val navController = rememberNavController()

            //wraps entire application to ensure that all components
            //inherit the correct colors, typography, and shapes
            StockMarketSimulatorTheme() {
                //fundamental Compose component to provide background
                //it handles color and elevation clipping automatically
                Surface(
                    //allows surface to expand and take up entire screen
                    //and pushes down content to not be overlaid
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    //sets background color
                    color = MaterialTheme.colorScheme.background
                ) {
                    //navhost is container for the current screen
                    //navcontroller holds track of the backstack of screens
                    //startdestination tells app which screen to show first,
                    //as defined in function
                    NavHost(
                        navController = navController,
                        startDestination = "login_screen"
                    ) {
                        //define navigation graph, route is
                        //a unique name for string
                        composable(route = "login_screen") {
                            //when controller is told to navigate to login, execute code inside
                            //block
                            LoginScreen(onLoginSuccess = {
                                //callback: when user clicks login button, the navController
                                //is instructed to switch the view to the screen named "home"
                                navController.navigate("home_screen") {
                                    //remove login_screen from stack, so if back
                                    //is pressed, then the app just exits
                                    popUpTo("login_screen") {
                                        //keeps login_screen unless inclusive is true
                                        inclusive = true
                                    }
                                }
                            })
                        }
                        composable("home_screen") {
                            //this displays home ui
                            //navcontroller is passed into homescreen so other
                            //changes to screens can be made (like back, settings,
                            //profile, etc.)
                            HomeScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}