package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import com.example.util.SalaryCalculator

@Composable
fun OvertimeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val monthAttendance by viewModel.monthAttendance.collectAsState()

    // Interactive Simulator State
    var simOtHours by remember { mutableFloatStateOf(2.0f) }
    var simMultiplier by remember { mutableDoubleStateOf(1.5) }

    val simEarnings = remember(simOtHours, simMultiplier, monthlySummary.hourlyRate) {
        simOtHours * monthlySummary.hourlyRate * simMultiplier
    }

    val otRecords = remember(monthAttendance) {
        monthAttendance.filter { it.overtimeMinutes > 0 || it.manualOtHoursOverride != null }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_overtime"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector Header
        item {
            MonthSelectorHeader(
                selectedMonth = selectedMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onSelectCurrentMonth = { viewModel.setSelectedMonth(DateUtils.getCurrentMonthPrefix()) }
            )
        }

        // 1. Month OT Total Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_ot_summary"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL OVERTIME EARNED",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Slate200
                            )
                            Text(
                                text = SalaryCalculator.formatCurrency(monthlySummary.totalOvertimePay, userProfile.currencySymbol),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp
                                ),
                                color = IndustrialAmberLight
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(IndustrialAmber.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = IndustrialAmberLight,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Slate700)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total OT Hours", fontSize = 12.sp, color = Slate200)
                            Text(
                                "%.1f hrs".format(monthlySummary.totalOvertimeHours),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Column {
                            Text("Base Hourly Rate", fontSize = 12.sp, color = Slate200)
                            Text(
                                SalaryCalculator.formatCurrency(monthlySummary.hourlyRate, userProfile.currencySymbol) + "/hr",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Column {
                            Text("Double OT (2.0x)", fontSize = 12.sp, color = Slate200)
                            Text(
                                "%.1f hrs".format(monthlySummary.doubleOtHours),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive Overtime Wage Calculator / Simulator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Live OT Wage Simulator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Formula: (Base Rate ${SalaryCalculator.formatCurrency(monthlySummary.hourlyRate, userProfile.currencySymbol)}/hr) × (Hours) × (Multiplier)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Hours Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Overtime Duration:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "%.1f Hours".format(simOtHours),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = simOtHours,
                            onValueChange = { simOtHours = it },
                            valueRange = 0.5f..12.0f,
                            steps = 22
                        )
                    }

                    // Multiplier Chips
                    Text("Rate Multiplier:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1.0 to "1.0x", 1.25 to "1.25x", 1.5 to "1.5x (Standard)", 2.0 to "2.0x (Holiday)").forEach { (rate, label) ->
                            FilterChip(
                                selected = simMultiplier == rate,
                                onClick = { simMultiplier = rate },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Calculated Result Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simulated OT Payout:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = SalaryCalculator.formatCurrency(simEarnings, userProfile.currencySymbol),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 3. Overtime Engine Configuration Specs Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Overtime Rules",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    RuleRow("Shift Standard", "${userProfile.defaultShiftHours} Hours/Day")
                    RuleRow("Calculation Base", userProfile.otBaseMode.displayName)
                    RuleRow("Working Days Mode", userProfile.workingDaysMode.displayName)
                    RuleRow("Rounding Rule", userProfile.otRounding.displayName)
                    RuleRow("Sunday/Holiday Multiplier", "${userProfile.sundayOtMultiplier}x (Double Rate)")
                    RuleRow("Min OT Threshold", "${userProfile.minOtThresholdMinutes} Minutes")
                }
            }
        }

        // 4. Monthly Overtime Daily Logs Header
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Daily Overtime Logs (${otRecords.size} Days)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (otRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Slate500, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No overtime recorded for ${DateUtils.formatMonthDisplay(selectedMonth)}", color = Slate500)
                    }
                }
            }
        } else {
            items(otRecords) { record ->
                val otHours = if (record.manualOtHoursOverride != null) record.manualOtHoursOverride else record.overtimeMinutes / 60.0
                val otPay = otHours * monthlySummary.hourlyRate * record.otMultiplier

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = DateUtils.formatFullDate(record.date),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Rate: ${record.otMultiplier}x • In: ${record.inTime ?: "--"} • Out: ${record.outTime ?: "--"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%.2f hrs OT".format(otHours),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IndustrialAmber
                                )
                            )
                            Text(
                                text = "+${SalaryCalculator.formatCurrency(otPay, userProfile.currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPresentDark
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RuleRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
