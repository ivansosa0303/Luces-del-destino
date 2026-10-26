package com.example

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.AppBaseScaffold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppBaseScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appBaseScaffold_rendersTopBarAndBottomNavigationTabs() {
        var selectedTab = AppNavTab.EXPLORER
        var cloudSyncClicked = false

        composeTestRule.setContent {
            AppBaseScaffold(
                currentTab = selectedTab,
                onTabSelected = { selectedTab = it },
                selectedProjectName = null,
                onNavigateBack = null,
                currentUser = null,
                onOpenCloudSync = { cloudSyncClicked = true },
                compilerBadgeCount = 3
            ) {
                Text("Contenido de prueba")
            }
        }

        // Verify Scaffold and TopAppBar
        composeTestRule.onNodeWithTag("app_base_scaffold").assertIsDisplayed()
        composeTestRule.onNodeWithTag("main_top_app_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luces del Destino").assertIsDisplayed()
        composeTestRule.onNodeWithText("OFFLINE").assertIsDisplayed()

        // Verify NavigationBar and all tabs
        composeTestRule.onNodeWithTag("main_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_stories").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_projects").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_explorer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_compiler").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_converter").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_history").assertIsDisplayed()

        // Verify content slot renders
        composeTestRule.onNodeWithText("Contenido de prueba").assertIsDisplayed()

        // Test Cloud sync action
        composeTestRule.onNodeWithTag("btn_cloud_sync_topbar").performClick()
        assertTrue("Cloud sync click handler should be invoked", cloudSyncClicked)

        // Test Tab navigation click
        composeTestRule.onNodeWithTag("tab_converter").performClick()
        assertEquals(AppNavTab.CONVERTER, selectedTab)
    }

    @Test
    fun appBaseScaffold_showsBackButton_whenOnNavigateBackProvided() {
        var backClicked = false

        composeTestRule.setContent {
            AppBaseScaffold(
                currentTab = AppNavTab.EXPLORER,
                onTabSelected = {},
                selectedProjectName = "Proyecto Alpha",
                onNavigateBack = { backClicked = true },
                currentUser = null,
                onOpenCloudSync = {}
            ) {
                Text("Sub-pantalla")
            }
        }

        // Project name displayed
        composeTestRule.onNodeWithText("Proyecto Alpha").assertIsDisplayed()

        // Back button must be displayed and clickable
        composeTestRule.onNodeWithTag("top_app_bar_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_app_bar_back_button").performClick()
        assertTrue("Back click handler should be invoked", backClicked)
    }
}
