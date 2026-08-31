package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.AttendanceStatus
import com.example.ui.components.AttendanceEditDialog
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.MonthlyKpiOverview
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import com.example.util.ExportReportUtil
import com.example.util.PdfExportUtil
import com.example.util.SalaryCalculator

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAttendance: () -> Unit,
    onNavigateToSalary: () -> Unit,
    onNavigateToOvertime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val recentAttendance by viewModel.recentAttendance.collectAsState()
    val shifts by viewModel.shifts.collectAsState()

    var showEditDialogForDate by remember { mutableStateOf<String?>(null) }
    var selectedRecordForDialog by remember { mutableStateOf<AttendanceRecordEntity?>(null) }

    val todayDate = DateUtils.getTodayDateString()
    val isPunchedIn = todayAttendance?.inTime != null && todayAttendance?.outTime == null
    val isPunchedOut = todayAttendance?.outTime != null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_dashboard"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Month Navigation Header
        item {
            MonthSelectorHeader(
                selectedMonth = selectedMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onSelectCurrentMonth = { viewModel.setSelectedMonth(DateUtils.getCurrentMonthPrefix()) }
            )
        }

        // 2. Punch Clock Action Card (1-Tap Punch In / Out)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_punch_clock"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isPunchedIn) Color(0xFF16A34A) else if (isPunchedOut) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today: ${DateUtils.formatFullDate(todayDate)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (todayAttendance != null) {
                            StatusBadge(status = todayAttendance!!.status)
                        }
                    }

                    // Punch In / Out Times
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Punch In", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = todayAttendance?.inTime?.let { DateUtils.formatTime12H(it) } ?: "--:--",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (todayAttendance?.inTime != null) StatusPresentDark else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Punch Out", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = todayAttendance?.outTime?.let { DateUtils.formatTime12H(it) } ?: "--:--",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (todayAttendance?.outTime != null) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Worked", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = if (todayAttendance != null && todayAttendance!!.totalWorkMinutes > 0)
                                    DateUtils.formatMinutesToHoursMins(todayAttendance!!.totalWorkMinutes)
                                else if (isPunchedIn) "Active Duty"
                                else "0h 0m",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Punch Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.punchInToday() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_punch_in"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPunchedIn) Color(0xFF15803D) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPunchedIn) "Punched In ✓" else "Punch In", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.punchOutToday() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_punch_out"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPunchedOut) Color(0xFF1E3A8A) else IndustrialAmber
                            )
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPunchedOut) "Punched Out ✓" else "Punch Out", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Monthly KPI Overview
        item {
            MonthlyKpiOverview(
                summary = monthlySummary,
                currencySymbol = userProfile.currencySymbol
            )
        }

        // 4. Quick Actions Row
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.EditCalendar,
                            label = "Mark Date",
                            onClick = {
                                selectedRecordForDialog = todayAttendance
                                showEditDialogForDate = todayDate
                            }
                        )
                        QuickActionItem(
                            icon = Icons.Default.Calculate,
                            label = "OT Calc",
                            onClick = onNavigateToOvertime
                        )
                        QuickActionItem(
                            icon = Icons.Default.ReceiptLong,
                            label = "Payslip",
                            onClick = onNavigateToSalary
                        )
                        QuickActionItem(
                            icon = Icons.Default.FileDownload,
                            label = "Export PDF",
                            onClick = {
                                PdfExportUtil.generateAndShareSalarySlipPdf(context, userProfile, monthlySummary)
                            }
                        )
                    }
                }
            }
        }

        // 5. Recent Activity Logs
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Attendance Logs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onNavigateToAttendance) {
                        Text("View All")
                    }
                }
            }
        }

        if (recentAttendance.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No attendance records logged yet", fontWeight = FontWeight.SemiBold)
                        Text("Tap 'Punch In' or 'Mark Date' above to record today's shift.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(recentAttendance) { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            selectedRecordForDialog = record
                            showEditDialogForDate = record.date
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                text = if (record.inTime != null && record.outTime != null)
                                    "In: ${DateUtils.formatTime12H(record.inTime)} • Out: ${DateUtils.formatTime12H(record.outTime)} • ${DateUtils.formatMinutesToHoursMins(record.totalWorkMinutes)}"
                                else if (record.inTime != null) "In: ${DateUtils.formatTime12H(record.inTime)} • In Progress"
                                else "No punch recorded",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (record.overtimeMinutes > 0) {
                                Text(
                                    text = "+${DateUtils.formatMinutesToHoursMins(record.overtimeMinutes)} OT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndustrialAmber,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFEDD5))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            StatusBadge(status = record.status)
                        }
                    }
                }
            }
        }
    }

    // Dialog for Date Editing
    if (showEditDialogForDate != null) {
        AttendanceEditDialog(
            date = showEditDialogForDate!!,
            initialRecord = selectedRecordForDialog,
            shifts = shifts,
            defaultShiftHours = userProfile.defaultShiftHours,
            onDismiss = {
                showEditDialogForDate = null
                selectedRecordForDialog = null
            },
            onSave = { date, status, shiftId, inTime, outTime, breakMin, manualOt, otMult, isLate, lateMin, isEarly, earlyMin, notes ->
                viewModel.saveAttendanceRecord(
                    date, status, shiftId, inTime, outTime, breakMin, manualOt, otMult, isLate, lateMin, isEarly, earlyMin, notes
                )
            },
            onDelete = { date -> viewModel.deleteAttendance(date) }
        )
    }
}

@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
