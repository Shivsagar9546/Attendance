package com.example.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.ShiftConfigEntity
import com.example.data.model.AttendanceStatus
import com.example.util.DateUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceEditDialog(
    date: String,
    initialRecord: AttendanceRecordEntity?,
    shifts: List<ShiftConfigEntity>,
    defaultShiftHours: Double,
    onDismiss: () -> Unit,
    onSave: (
        date: String,
        status: AttendanceStatus,
        shiftId: Long,
        inTime: String?,
        outTime: String?,
        breakMinutes: Int,
        manualOtHours: Double?,
        otMultiplier: Double,
        isLate: Boolean,
        lateMinutes: Int,
        isEarlyExit: Boolean,
        earlyExitMinutes: Int,
        notes: String
    ) -> Unit,
    onDelete: (date: String) -> Unit
) {
    val context = LocalContext.current
    var selectedStatus by remember { mutableStateOf(initialRecord?.status ?: AttendanceStatus.PRESENT) }
    var selectedShiftId by remember { mutableStateOf(initialRecord?.shiftId ?: (shifts.firstOrNull()?.id ?: 1L)) }
    var inTime by remember { mutableStateOf(initialRecord?.inTime ?: "09:00") }
    var outTime by remember { mutableStateOf(initialRecord?.outTime ?: "18:00") }
    var breakMinutes by remember { mutableIntStateOf(initialRecord?.breakMinutes ?: 60) }
    var otMultiplier by remember { mutableDoubleStateOf(initialRecord?.otMultiplier ?: 1.5) }
    var isManualOt by remember { mutableStateOf(initialRecord?.manualOtHoursOverride != null) }
    var manualOtText by remember { mutableStateOf(initialRecord?.manualOtHoursOverride?.toString() ?: "") }
    var isLate by remember { mutableStateOf(initialRecord?.isLateArrival ?: false) }
    var lateMinutesText by remember { mutableStateOf(initialRecord?.lateMinutes?.takeIf { it > 0 }?.toString() ?: "") }
    var isEarlyExit by remember { mutableStateOf(initialRecord?.isEarlyExit ?: false) }
    var earlyExitText by remember { mutableStateOf(initialRecord?.earlyExitMinutes?.takeIf { it > 0 }?.toString() ?: "") }
    var note by remember { mutableStateOf(initialRecord?.note ?: "") }

    // Live calculated hours
    val calculatedMinutes = remember(selectedStatus, inTime, outTime, breakMinutes) {
        if (selectedStatus.isWorkDay && inTime.isNotBlank() && outTime.isNotBlank()) {
            DateUtils.calculateDurationMinutes(inTime, outTime, breakMinutes)
        } else if (selectedStatus == AttendanceStatus.HALF_DAY) {
            (defaultShiftHours * 30).toInt()
        } else if (selectedStatus.isWorkDay) {
            (defaultShiftHours * 60).toInt()
        } else {
            0
        }
    }

    val calculatedOtMinutes = remember(calculatedMinutes, defaultShiftHours) {
        val reg = (defaultShiftHours * 60).toInt()
        if (calculatedMinutes > reg) calculatedMinutes - reg else 0
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("dialog_attendance_edit"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mark Attendance",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = DateUtils.formatFullDate(date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Status Selector Chips
                    Text(
                        text = "Attendance Status",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AttendanceStatus.values()) { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = {
                                    selectedStatus = status
                                    if (status == AttendanceStatus.HALF_DAY) {
                                        outTime = "13:30"
                                    } else if (status == AttendanceStatus.WEEKLY_OFF || status == AttendanceStatus.ABSENT) {
                                        inTime = ""
                                        outTime = ""
                                    } else if (inTime.isBlank()) {
                                        inTime = "09:00"
                                        outTime = "18:00"
                                    }
                                },
                                label = { Text(status.displayName, fontSize = 12.sp) },
                                leadingIcon = if (selectedStatus == status) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    if (selectedStatus.isWorkDay) {
                        // 2. Shift Selector
                        Text(
                            text = "Shift",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(shifts) { shift ->
                                FilterChip(
                                    selected = selectedShiftId == shift.id,
                                    onClick = {
                                        selectedShiftId = shift.id
                                        inTime = shift.startTime
                                        outTime = shift.endTime
                                        breakMinutes = shift.breakMinutes
                                    },
                                    label = { Text("${shift.name} (${shift.startTime}-${shift.endTime})", fontSize = 12.sp) }
                                )
                            }
                        }

                        // 3. In-Time & Out-Time Time Pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TimePickerBox(
                                label = "In Time (Punch In)",
                                time = inTime,
                                onTimePicked = { inTime = it },
                                modifier = Modifier.weight(1f)
                            )
                            TimePickerBox(
                                label = "Out Time (Punch Out)",
                                time = outTime,
                                onTimePicked = { outTime = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 4. Break Duration Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lunch / Tea Break: ${breakMinutes} mins",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0, 30, 45, 60).forEach { mins ->
                                    SuggestionChip(
                                        onClick = { breakMinutes = mins },
                                        label = { Text("${mins}m", fontSize = 11.sp) },
                                        colors = if (breakMinutes == mins)
                                            SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        else SuggestionChipDefaults.suggestionChipColors()
                                    )
                                }
                            }
                        }

                        // 5. Work Hours Live Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Work", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        DateUtils.formatMinutesToHoursMins(calculatedMinutes),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Standard Shift", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${defaultShiftHours}h",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Overtime (OT)", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        DateUtils.formatMinutesToHoursMins(calculatedOtMinutes),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (calculatedOtMinutes > 0) Color(0xFFEA580C) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // 6. Overtime Multiplier & Override
                        Text(
                            text = "Overtime (OT) Multiplier Rate",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(1.0 to "1.0x (Normal)", 1.25 to "1.25x", 1.5 to "1.5x (Std)", 2.0 to "2.0x (Double)").forEach { (rate, label) ->
                                FilterChip(
                                    selected = otMultiplier == rate,
                                    onClick = { otMultiplier = rate },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Manual OT hours override
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isManualOt,
                                onCheckedChange = { isManualOt = it }
                            )
                            Text("Manual OT Hours Override", style = MaterialTheme.typography.bodyMedium)
                        }

                        if (isManualOt) {
                            OutlinedTextField(
                                value = manualOtText,
                                onValueChange = { manualOtText = it },
                                label = { Text("Custom OT Hours (e.g. 2.5)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 7. Late Arrival & Early Exit Flags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isLate, onCheckedChange = { isLate = it })
                                    Text("Late Arrival", fontSize = 13.sp)
                                }
                                if (isLate) {
                                    OutlinedTextField(
                                        value = lateMinutesText,
                                        onValueChange = { lateMinutesText = it },
                                        label = { Text("Late (mins)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isEarlyExit, onCheckedChange = { isEarlyExit = it })
                                    Text("Early Exit", fontSize = 13.sp)
                                }
                                if (isEarlyExit) {
                                    OutlinedTextField(
                                        value = earlyExitText,
                                        onValueChange = { earlyExitText = it },
                                        label = { Text("Early (mins)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 8. Notes
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Notes / Supervisor Remarks (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (initialRecord != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(date)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete")
                        }
                    }

                    Button(
                        onClick = {
                            val manualOt = if (isManualOt) manualOtText.toDoubleOrNull() else null
                            val lateMin = if (isLate) lateMinutesText.toIntOrNull() ?: 0 else 0
                            val earlyMin = if (isEarlyExit) earlyExitText.toIntOrNull() ?: 0 else 0

                            onSave(
                                date,
                                selectedStatus,
                                selectedShiftId,
                                inTime.ifBlank { null },
                                outTime.ifBlank { null },
                                breakMinutes,
                                manualOt,
                                otMultiplier,
                                isLate,
                                lateMin,
                                isEarlyExit,
                                earlyMin,
                                note
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("btn_save_attendance")
                    ) {
                        Text("Save Attendance")
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerBox(
    label: String,
    time: String,
    onTimePicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val initialHour = try { time.split(":")[0].toInt() } catch (e: Exception) { 9 }
    val initialMinute = try { time.split(":")[1].toInt() } catch (e: Exception) { 0 }

    val timePickerDialog = remember(time) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = "%02d:%02d".format(Locale.US, hourOfDay, minute)
                onTimePicked(formatted)
            },
            initialHour,
            initialMinute,
            true // 24-hour mode
        )
    }

    Card(
        modifier = modifier
            .clickable { timePickerDialog.show() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (time.isNotBlank()) DateUtils.formatTime12H(time) else "--:--",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Pick Time",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
