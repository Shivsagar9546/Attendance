package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.model.AttendanceStatus
import com.example.ui.components.AttendanceEditDialog
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import java.util.*

enum class ViewMode { CALENDAR, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceRegisterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthAttendance by viewModel.monthAttendance.collectAsState()
    val monthHolidays by viewModel.monthHolidays.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var viewMode by remember { mutableStateOf(ViewMode.CALENDAR) }
    var selectedFilterStatus by remember { mutableStateOf<AttendanceStatus?>(null) }
    var showBatchMenu by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    var selectedDateForEdit by remember { mutableStateOf<String?>(null) }
    var selectedRecordForEdit by remember { mutableStateOf<AttendanceRecordEntity?>(null) }

    val daysInMonth = remember(selectedMonth) { DateUtils.getDaysInMonth(selectedMonth) }
    val attendanceMap = remember(monthAttendance) { monthAttendance.associateBy { it.date } }
    val holidayMap = remember(monthHolidays) { monthHolidays.associateBy { it.date } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_attendance_register")
    ) {
        // Month Selector Header
        MonthSelectorHeader(
            selectedMonth = selectedMonth,
            onPreviousMonth = { viewModel.previousMonth() },
            onNextMonth = { viewModel.nextMonth() },
            onSelectCurrentMonth = { viewModel.setSelectedMonth(DateUtils.getCurrentMonthPrefix()) }
        )

        // View Mode Toggle & Batch Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // View Mode Switcher
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = viewMode == ViewMode.CALENDAR,
                    onClick = { viewMode = ViewMode.CALENDAR },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Default.CalendarViewMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calendar")
                }
                SegmentedButton(
                    selected = viewMode == ViewMode.LIST,
                    onClick = { viewMode = ViewMode.LIST },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.Default.ViewList, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("List View")
                }
            }

            // Batch Actions Dropdown
            Box {
                OutlinedButton(
                    onClick = { showBatchMenu = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Batch Actions", modifier = Modifier.size(16.dp))
                    Text("Tools", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = showBatchMenu,
                    onDismissRequest = { showBatchMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Mark Sundays as Weekly Off") },
                        leadingIcon = { Icon(Icons.Default.Weekend, contentDescription = null) },
                        onClick = {
                            viewModel.batchMarkMonthSundaysAsWeeklyOff(selectedMonth)
                            showBatchMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Fill All Weekdays as Present (8h)") },
                        leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                        onClick = {
                            viewModel.batchFillMonthAsPresent(selectedMonth)
                            showBatchMenu = false
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Clear Month Records", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showBatchMenu = false
                            showConfirmClearDialog = true
                        }
                    )
                }
            }
        }

        // Filter chips row
        ScrollableTabRow(
            selectedTabIndex = if (selectedFilterStatus == null) 0 else AttendanceStatus.values().indexOf(selectedFilterStatus) + 1,
            edgePadding = 16.dp,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedFilterStatus == null,
                onClick = { selectedFilterStatus = null },
                text = { Text("All (${daysInMonth.size})", fontSize = 12.sp) }
            )
            listOf(
                AttendanceStatus.PRESENT,
                AttendanceStatus.ABSENT,
                AttendanceStatus.HALF_DAY,
                AttendanceStatus.PAID_LEAVE,
                AttendanceStatus.WEEKLY_OFF,
                AttendanceStatus.FESTIVAL_HOLIDAY,
                AttendanceStatus.NIGHT_SHIFT
            ).forEach { status ->
                val count = daysInMonth.count { date ->
                    val rec = attendanceMap[date]
                    val hol = holidayMap[date]
                    val isSun = DateUtils.isSunday(date)
                    val st = rec?.status ?: if (hol != null) AttendanceStatus.FESTIVAL_HOLIDAY else if (isSun) AttendanceStatus.WEEKLY_OFF else AttendanceStatus.ABSENT
                    st == status
                }
                Tab(
                    selected = selectedFilterStatus == status,
                    onClick = { selectedFilterStatus = status },
                    text = { Text("${status.displayName} ($count)", fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main View
        if (viewMode == ViewMode.CALENDAR) {
            CalendarGridView(
                daysInMonth = daysInMonth,
                attendanceMap = attendanceMap,
                holidayMap = holidayMap,
                selectedFilterStatus = selectedFilterStatus,
                onDateClick = { date ->
                    selectedDateForEdit = date
                    selectedRecordForEdit = attendanceMap[date]
                }
            )
        } else {
            AttendanceListView(
                daysInMonth = daysInMonth,
                attendanceMap = attendanceMap,
                holidayMap = holidayMap,
                selectedFilterStatus = selectedFilterStatus,
                currencySymbol = userProfile.currencySymbol,
                onDateClick = { date ->
                    selectedDateForEdit = date
                    selectedRecordForEdit = attendanceMap[date]
                }
            )
        }
    }

    // Confirm Clear Dialog
    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("Clear Month Attendance?") },
            text = { Text("This will delete all marked attendance and overtime records for ${DateUtils.formatMonthDisplay(selectedMonth)}. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearMonthAttendance(selectedMonth)
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Attendance Edit Dialog
    if (selectedDateForEdit != null) {
        AttendanceEditDialog(
            date = selectedDateForEdit!!,
            initialRecord = selectedRecordForEdit,
            shifts = shifts,
            defaultShiftHours = userProfile.defaultShiftHours,
            onDismiss = {
                selectedDateForEdit = null
                selectedRecordForEdit = null
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
fun CalendarGridView(
    daysInMonth: List<String>,
    attendanceMap: Map<String, AttendanceRecordEntity>,
    holidayMap: Map<String, com.example.data.local.entity.HolidayRecordEntity>,
    selectedFilterStatus: AttendanceStatus?,
    onDateClick: (String) -> Unit
) {
    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Determine leading empty slots for month start offset
    val firstDayOffset = remember(daysInMonth) {
        if (daysInMonth.isNotEmpty()) {
            val dow = DateUtils.getDayOfWeek(daysInMonth.first()) // 1 = Sun, 2 = Mon ...
            dow - 1
        } else 0
    }

    val totalSlots = firstDayOffset + daysInMonth.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Weekday Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (dayName == "Sun") Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Empty offset slots
            items(firstDayOffset) {
                Box(modifier = Modifier.aspectRatio(0.85f))
            }

            // Days of the month
            items(daysInMonth) { dateStr ->
                val dayNumber = dateStr.split("-").last().toIntOrNull() ?: 1
                val isSunday = DateUtils.isSunday(dateStr)
                val holiday = holidayMap[dateStr]
                val record = attendanceMap[dateStr]

                val status = record?.status ?: if (holiday != null) {
                    AttendanceStatus.FESTIVAL_HOLIDAY
                } else if (isSunday) {
                    AttendanceStatus.WEEKLY_OFF
                } else {
                    AttendanceStatus.ABSENT
                }

                val matchesFilter = selectedFilterStatus == null || status == selectedFilterStatus

                val isToday = dateStr == DateUtils.getTodayDateString()

                Card(
                    modifier = Modifier
                        .aspectRatio(0.85f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isToday) 2.dp else 1.dp,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onDateClick(dateStr) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (matchesFilter) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 3.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Day Number & Today indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$dayNumber",
                                fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSunday) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                            )

                            if (record?.overtimeMinutes != null && record.overtimeMinutes > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(IndustrialAmber)
                                        .size(6.dp)
                                )
                            }
                        }

                        // Status Badge
                        StatusBadge(status = status, isCompact = true)

                        // OT or Work Hours Tag
                        if (record != null && record.overtimeMinutes > 0) {
                            Text(
                                text = "+${DateUtils.formatMinutesToDecimalHours(record.overtimeMinutes)}h OT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndustrialAmber
                            )
                        } else if (record != null && record.totalWorkMinutes > 0) {
                            Text(
                                text = "${record.totalWorkMinutes / 60}h",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceListView(
    daysInMonth: List<String>,
    attendanceMap: Map<String, AttendanceRecordEntity>,
    holidayMap: Map<String, com.example.data.local.entity.HolidayRecordEntity>,
    selectedFilterStatus: AttendanceStatus?,
    currencySymbol: String,
    onDateClick: (String) -> Unit
) {
    val filteredDays = remember(daysInMonth, selectedFilterStatus, attendanceMap, holidayMap) {
        if (selectedFilterStatus == null) daysInMonth
        else daysInMonth.filter { date ->
            val rec = attendanceMap[date]
            val hol = holidayMap[date]
            val isSun = DateUtils.isSunday(date)
            val st = rec?.status ?: if (hol != null) AttendanceStatus.FESTIVAL_HOLIDAY else if (isSun) AttendanceStatus.WEEKLY_OFF else AttendanceStatus.ABSENT
            st == selectedFilterStatus
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredDays) { date ->
            val record = attendanceMap[date]
            val holiday = holidayMap[date]
            val isSunday = DateUtils.isSunday(date)

            val status = record?.status ?: if (holiday != null) {
                AttendanceStatus.FESTIVAL_HOLIDAY
            } else if (isSunday) {
                AttendanceStatus.WEEKLY_OFF
            } else {
                AttendanceStatus.ABSENT
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDateClick(date) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DateUtils.formatFullDate(date),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (record?.inTime != null && record.outTime != null)
                                "In: ${DateUtils.formatTime12H(record.inTime)} • Out: ${DateUtils.formatTime12H(record.outTime)} • Work: ${DateUtils.formatMinutesToHoursMins(record.totalWorkMinutes)}"
                            else if (holiday != null) "Holiday: ${holiday.name}"
                            else if (isSunday) "Standard Weekly Off"
                            else "No punch record",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (record != null && record.overtimeMinutes > 0) {
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
                        StatusBadge(status = status)
                    }
                }
            }
        }
    }
}
