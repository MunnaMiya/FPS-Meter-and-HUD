package com.growtotype.fpsmeterandhud.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.growtotype.fpsmeterandhud.ui.screens.HardwareTab
import com.growtotype.fpsmeterandhud.ui.screens.MonitorTab
import com.growtotype.fpsmeterandhud.ui.screens.OverlaySettingsTab
import com.growtotype.fpsmeterandhud.ui.screens.SessionDetailScreen
import com.growtotype.fpsmeterandhud.ui.screens.SessionsTab
import com.growtotype.fpsmeterandhud.ui.theme.AccentGreen
import com.growtotype.fpsmeterandhud.viewmodel.MainViewModel

enum class NavTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    MONITOR("Monitor", Icons.Default.Speed, "tab_monitor"),
    SESSIONS("Sessions", Icons.Default.History, "tab_sessions"),
    OVERLAY("Overlay", Icons.Default.Tune, "tab_overlay"),
    HARDWARE("Hardware", Icons.Default.Hardware, "tab_hardware")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedSession by viewModel.selectedSession.collectAsState()
    val settings by viewModel.overlaySettings.collectAsState()

    if (selectedSession != null) {
        SessionDetailScreen(
            session = selectedSession!!,
            onBack = { viewModel.selectSession(null) },
            onDelete = { session ->
                viewModel.deleteSession(session)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FPS Meter & HUD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.updateSettings(settings.copy(isDarkTheme = !settings.isDarkTheme))
                        },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (settings.isDarkTheme) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = if (settings.isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF052E1E),
                            selectedTextColor = AccentGreen,
                            indicatorColor = AccentGreen,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (selectedTabIndex) {
            0 -> MonitorTab(viewModel = viewModel, modifier = screenModifier)
            1 -> SessionsTab(viewModel = viewModel, modifier = screenModifier)
            2 -> OverlaySettingsTab(viewModel = viewModel, modifier = screenModifier)
            3 -> HardwareTab(viewModel = viewModel, modifier = screenModifier)
        }
    }
}
