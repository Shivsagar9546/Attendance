package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.local.entity.HolidayRecordEntity
import com.example.data.local.entity.ShiftConfigEntity
import com.example.data.model.HolidayType
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftsHolidaysScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val shifts by viewModel.shifts.collectAsState()
    val holidays by viewModel.holidays.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddShiftDialog by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<ShiftConfigEntity?>(null) }
    var showAddHolidayDialog by remember { mutableStateOf(false) }
    var editingHoliday by remember { mutableStateOf<HolidayRecordEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_shifts_holidays")
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Factory Shifts (${shifts.size})") },
                icon = { Icon(Icons.Default.Schedule, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Annual Holidays (${holidays.size})") },
                icon = { Icon(Icons.Default.Celebration, contentDescription = null) }
            )
        }

        if (selectedTab == 0) {
            // Shifts List Tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Configured Shifts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Button(
                            onClick = {
                                editingShift = null
                                showAddShiftDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Shift", fontSize = 12.sp)
                        }
                    }
                }

                items(shifts) { shift ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (shift.isNightShift) Slate800 else ElectricBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shift.code,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(shift.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        if (shift.isNightShift) {
                                            Text(
                                                "Night",
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Slate800)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${DateUtils.formatTime12H(shift.startTime)} to ${DateUtils.formatTime12H(shift.endTime)} • ${shift.durationHours}h (${shift.breakMinutes}m break)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingShift = shift
                                        showAddShiftDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ElectricBlue, modifier = Modifier.size(20.dp))
                                }
                                if (shifts.size > 1) {
                                    IconButton(onClick = { viewModel.deleteShift(shift) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusAbsentDark, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Holidays List Tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Official Holidays", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Button(
                            onClick = {
                                editingHoliday = null
                                showAddHolidayDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Holiday", fontSize = 12.sp)
                        }
                    }
                }

                items(holidays) { hol ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(hol.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${DateUtils.formatFullDate(hol.date)} • ${hol.type.displayName} • ${if (hol.isPaid) "Paid Holiday" else "Unpaid"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingHoliday = hol
                                        showAddHolidayDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ElectricBlue, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { viewModel.deleteHoliday(hol) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusAbsentDark, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Shift Editor Dialog
    if (showAddShiftDialog) {
        ShiftEditorDialog(
            initial = editingShift,
            onDismiss = { showAddShiftDialog = false },
            onSave = { shift ->
                viewModel.saveShift(shift)
                showAddShiftDialog = false
            }
        )
    }

    // Holiday Editor Dialog
    if (showAddHolidayDialog) {
        HolidayEditorDialog(
            initial = editingHoliday,
            onDismiss = { showAddHolidayDialog = false },
            onSave = { holiday ->
                viewModel.saveHoliday(holiday)
                showAddHolidayDialog = false
            }
        )
    }
}

@Composable
fun ShiftEditorDialog(
    initial: ShiftConfigEntity?,
    onDismiss: () -> Unit,
    onSave: (ShiftConfigEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "General Shift") }
    var code by remember { mutableStateOf(initial?.code ?: "G") }
    var startTime by remember { mutableStateOf(initial?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(initial?.endTime ?: "18:00") }
    var durationHoursText by remember { mutableStateOf(initial?.durationHours?.toString() ?: "8.0") }
    var breakMinutesText by remember { mutableStateOf(initial?.breakMinutes?.toString() ?: "60") }
    var isNightShift by remember { mutableStateOf(initial?.isNightShift ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initial == null) "Add Shift" else "Edit Shift",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Shift Name") },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.take(2).uppercase() },
                        label = { Text("Code") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationHoursText,
                        onValueChange = { durationHoursText = it },
                        label = { Text("Standard Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = breakMinutesText,
                        onValueChange = { breakMinutesText = it },
                        label = { Text("Break (mins)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isNightShift, onCheckedChange = { isNightShift = it })
                    Text("Night Shift (Crosses Midnight)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dur = durationHoursText.toDoubleOrNull() ?: 8.0
                            val brk = breakMinutesText.toIntOrNull() ?: 30
                            onSave(
                                ShiftConfigEntity(
                                    id = initial?.id ?: 0L,
                                    name = name,
                                    code = code,
                                    startTime = startTime,
                                    endTime = endTime,
                                    durationHours = dur,
                                    breakMinutes = brk,
                                    isNightShift = isNightShift
                                )
                            )
                        }
                    ) {
                        Text("Save Shift")
                    }
                }
            }
        }
    }
}

@Composable
fun HolidayEditorDialog(
    initial: HolidayRecordEntity?,
    onDismiss: () -> Unit,
    onSave: (HolidayRecordEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: DateUtils.getTodayDateString()) }
    var selectedType by remember { mutableStateOf(initial?.type ?: HolidayType.FESTIVAL) }
    var isPaid by remember { mutableStateOf(initial?.isPaid ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initial == null) "Add Holiday" else "Edit Holiday",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Holiday Name (e.g. Labor Day, Diwali)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Holiday Type", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HolidayType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                    Text("Paid Holiday (Count in Payable Days)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && date.isNotBlank()) {
                                onSave(
                                    HolidayRecordEntity(
                                        date = date,
                                        name = name,
                                        type = selectedType,
                                        isPaid = isPaid
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Save Holiday")
                    }
                }
            }
        }
    }
}
