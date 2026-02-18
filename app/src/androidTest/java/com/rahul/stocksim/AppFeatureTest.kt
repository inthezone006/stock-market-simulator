package com.rahul.stocksim

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFeatureTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testFullAppFlow() {
        // Initial settle to ensure the activity is fully launched
        composeTestRule.waitForIdle()

        // Wait for splash screen to disappear and first meaningful UI to show
        composeTestRule.waitUntil(30000L) {
            composeTestRule.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Portfolio").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // 1. Check if we need to Sign Up (Determine Auth State)
        val isLoginVisible = composeTestRule.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty()

        if (isLoginVisible) {
            // --- SIGN UP FLOW ---
            composeTestRule.onNodeWithText("Don't have an account? Sign up").performClick()
            composeTestRule.waitForIdle()
            
            // Register Screen
            composeTestRule.waitUntilAtLeastOneExists(hasText("Full Name"), 15000L)
            val uniqueEmail = "test_${UUID.randomUUID().toString().take(8)}@test.com"
            composeTestRule.onNodeWithText("Full Name").performTextInput("Automation User")
            composeTestRule.onNodeWithText("Email Address").performTextInput(uniqueEmail)
            
            composeTestRule.onNodeWithText("Continue").performClick()
            composeTestRule.waitForIdle()
            
            // Critical pause to ensure the Navigation transition to PasswordSetup completes
            // and the Lifecycle reaches at least CREATED before we interact with it.
            Thread.sleep(2000)
            
            // Password Setup (Security Setup screen)
            composeTestRule.waitUntilAtLeastOneExists(hasText("Security Setup"), 20000L)
            composeTestRule.waitForIdle()
            
            val testPassword = "Password123!"
            composeTestRule.onNodeWithText("Choose Password").performTextInput(testPassword)
            composeTestRule.onNodeWithText("Confirm Password").performTextInput(testPassword)
            composeTestRule.waitForIdle()
            
            // Click Confirm and wait for the navigation to trigger
            composeTestRule.onNodeWithContentDescription("Confirm").performClick()
            composeTestRule.waitForIdle()
            
            // Critical pause before moving to Balance Selection to avoid lifecycle race conditions
            Thread.sleep(2000)
            
            // Balance Selection (Difficulty)
            composeTestRule.waitUntilAtLeastOneExists(hasText("Choose Your Difficulty"), 20000L)
            composeTestRule.onNodeWithText("Level 3: $25,000 (Standard)").performClick()
            composeTestRule.onNodeWithContentDescription("Confirm Selection").performClick()
            composeTestRule.waitForIdle()
            
            // Final settle for main dashboard entry
            Thread.sleep(2000)
        }

        // --- MAIN APP DASHBOARD (Portfolio) ---
        composeTestRule.waitUntilAtLeastOneExists(hasText("Portfolio"), 20000L)
        composeTestRule.onNodeWithText("Total Account Value").assertIsDisplayed()

        // --- MARKET & SEARCH ---
        composeTestRule.onNodeWithText("Stocks").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilAtLeastOneExists(hasText("Market News"), 20000L)
        
        composeTestRule.onNodeWithText("Search stocks...").performTextInput("AAPL")
        composeTestRule.waitUntilAtLeastOneExists(hasText("Apple Inc."), 15000L)
        composeTestRule.onNodeWithText("AAPL").performClick()
        composeTestRule.waitForIdle()
        
        // Stock Detail Screen
        composeTestRule.waitUntilAtLeastOneExists(hasText("Market Stats"), 20000L)
        composeTestRule.onNodeWithText("BUY").assertIsDisplayed()
        
        // Execute a simulated trade: Buy 1 share
        composeTestRule.onNodeWithText("BUY").performClick()
        composeTestRule.waitForIdle()
        
        // Verify ownership info appears
        composeTestRule.waitUntilAtLeastOneExists(hasText("Your Position"), 15000L)
        
        // Navigate Back to Market
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // --- TRADE HISTORY ---
        composeTestRule.onNodeWithText("Trade").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilAtLeastOneExists(hasText("Buying Power"), 15000L)
        composeTestRule.onNodeWithText("AAPL").assertExists()

        // --- LEADERBOARD ---
        composeTestRule.onNodeWithText("Leaders").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilAtLeastOneExists(hasText("Global Leaderboard"), 15000L)

        // --- TRADING GUIDE ---
        composeTestRule.onNodeWithText("Guide").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilAtLeastOneExists(hasText("Trading Guide"), 15000L)

        // --- SETTINGS & LOGOUT ---
        // Access settings via the Profile icon in top bar
        composeTestRule.onNodeWithContentDescription("Profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilAtLeastOneExists(hasText("Settings"), 15000L)
        
        // Perform Logout
        composeTestRule.onNodeWithText("Logout").performClick()
        composeTestRule.waitForIdle()
        
        // Ensure app returns to the starting state (Sign In screen)
        composeTestRule.waitUntilAtLeastOneExists(hasText("Sign in"), 20000L)
        composeTestRule.waitForIdle()
        
        // Final safety pause to ensure all backstack entries are correctly destroyed 
        // by the system before the test activity is finalized.
        Thread.sleep(3000)
    }
}
