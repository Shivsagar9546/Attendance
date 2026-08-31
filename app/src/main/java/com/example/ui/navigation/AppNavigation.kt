package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Attendance : Screen("attendance", "Register", Icons.Default.CalendarMonth)
    object Overtime : Screen("overtime", "Overtime", Icons.Default.Timer)
    object Salary : Screen("salary", "Salary", Icons.Default.ReceiptLong)
    object ShiftsHolidays : Screen("shifts_holidays", "Shifts", Icons.Default.EventAvailable)
    object ProfileSettings : Screen("settings", "Profile", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Attendance,
    Screen.Overtime,
    Screen.Salary,
    Screen.ShiftsHolidays,
    Screen.ProfileSettings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            Screen.Dashboard.route -> "Workforce Attendance & OT"
                            Screen.Attendance.route -> "Monthly Attendance Register"
                            Screen.Overtime.route -> "Overtime (OT) Engine"
                            Screen.Salary.route -> "Salary & Payslip Register"
                            Screen.ShiftsHolidays.route -> "Shifts & Annual Holidays"
                            Screen.ProfileSettings.route -> "Profile & Salary Settings"
                            else -> "Attendance & OT"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    Text(
                        text = userProfile.name.ifBlank { "Worker" },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToSalary = { navController.navigate(Screen.Salary.route) },
                    onNavigateToOvertime = { navController.navigate(Screen.Overtime.route) }
                )
            }
            composable(Screen.Attendance.route) {
                AttendanceRegisterScreen(viewModel = viewModel)
            }
            composable(Screen.Overtime.route) {
                OvertimeScreen(viewModel = viewModel)
            }
            composable(Screen.Salary.route) {
                SalaryScreen(viewModel = viewModel)
            }
            composable(Screen.ShiftsHolidays.route) {
                ShiftsHolidaysScreen(viewModel = viewModel)
            }
            composable(Screen.ProfileSettings.route) {
                ProfileSettingsScreen(viewModel = viewModel)
            }
        }
    }
}
