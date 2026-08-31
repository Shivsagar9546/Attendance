package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.OtBaseMode
import com.example.data.model.OtRounding
import com.example.data.model.WorkingDaysMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.ExportReportUtil
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentProfile by viewModel.userProfile.collectAsState()
    val allAttendance by viewModel.monthAttendance.collectAsState()
    val allShifts by viewModel.shifts.collectAsState()
    val allHolidays by viewModel.holidays.collectAsState()
    val allAdjustments by viewModel.monthAdjustments.collectAsState()

    // Form states
    var employeeId by remember(currentProfile) { mutableStateOf(currentProfile.employeeId) }
    var name by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var designation by remember(currentProfile) { mutableStateOf(currentProfile.designation) }
    var department by remember(currentProfile) { mutableStateOf(currentProfile.department) }
    var companyName by remember(currentProfile) { mutableStateOf(currentProfile.companyName) }
    var currencySymbol by remember(currentProfile) { mutableStateOf(currentProfile.currencySymbol) }

    // Salary states
    var basicPay by remember(currentProfile) { mutableStateOf(currentProfile.basicPay.toString()) }
    var hra by remember(currentProfile) { mutableStateOf(currentProfile.hra.toString()) }
    var conveyance by remember(currentProfile) { mutableStateOf(currentProfile.conveyanceAllowance.toString()) }
    var specialAllowance by remember(currentProfile) { mutableStateOf(currentProfile.specialAllowance.toString()) }
    var otherAllowances by remember(currentProfile) { mutableStateOf(currentProfile.otherAllowances.toString()) }

    // Deductions states
    var isPfActive by remember(currentProfile) { mutableStateOf(currentProfile.isPfActive) }
    var pfPercentage by remember(currentProfile) { mutableStateOf(currentProfile.pfPercentage.toString()) }
    var isEsiActive by remember(currentProfile) { mutableStateOf(currentProfile.isEsiActive) }
    var esiPercentage by remember(currentProfile) { mutableStateOf(currentProfile.esiPercentage.toString()) }
    var professionalTax by remember(currentProfile) { mutableStateOf(currentProfile.professionalTax.toString()) }

    // OT Rules
    var defaultShiftHours by remember(currentProfile) { mutableStateOf(currentProfile.defaultShiftHours.toString()) }
    var defaultOtMultiplier by remember(currentProfile) { mutableStateOf(currentProfile.defaultOtMultiplier.toString()) }
    var sundayOtMultiplier by remember(currentProfile) { mutableStateOf(currentProfile.sundayOtMultiplier.toString()) }
    var workingDaysMode by remember(currentProfile) { mutableStateOf(currentProfile.workingDaysMode) }
    var otBaseMode by remember(currentProfile) { mutableStateOf(currentProfile.otBaseMode) }
    var otRounding by remember(currentProfile) { mutableStateOf(currentProfile.otRounding) }
    var minOtThresholdMinutes by remember(currentProfile) { mutableStateOf(currentProfile.minOtThresholdMinutes.toString()) }

    // JSON file restore launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                reader.close()
                inputStream?.close()

                viewModel.restoreFromJson(jsonString) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_settings"),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Worker Profile & Salary Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Configure salary components, statutory deductions, shift rules, and manage 100% offline data backups.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Worker Information
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Worker Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Worker Full Name") },
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = employeeId,
                            onValueChange = { employeeId = it },
                            label = { Text("Worker ID") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            label = { Text("Designation") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department / Plant") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company / Factory Name") },
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Salary Structure
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Monthly Salary Structure", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedTextField(
                        value = basicPay,
                        onValueChange = { basicPay = it },
                        label = { Text("Basic Pay ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hra,
                            onValueChange = { hra = it },
                            label = { Text("HRA ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = conveyance,
                            onValueChange = { conveyance = it },
                            label = { Text("Conveyance ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = specialAllowance,
                            onValueChange = { specialAllowance = it },
                            label = { Text("Special Allowance ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = otherAllowances,
                            onValueChange = { otherAllowances = it },
                            label = { Text("Other Allowances ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Statutory Deductions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Statutory Deductions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    // PF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isPfActive, onCheckedChange = { isPfActive = it })
                            Text("Provident Fund (PF)", fontWeight = FontWeight.Medium)
                        }
                        if (isPfActive) {
                            OutlinedTextField(
                                value = pfPercentage,
                                onValueChange = { pfPercentage = it },
                                label = { Text("PF %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }

                    // ESI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isEsiActive, onCheckedChange = { isEsiActive = it })
                            Text("ESI Insurance", fontWeight = FontWeight.Medium)
                        }
                        if (isEsiActive) {
                            OutlinedTextField(
                                value = esiPercentage,
                                onValueChange = { esiPercentage = it },
                                label = { Text("ESI %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }

                    // Professional Tax
                    OutlinedTextField(
                        value = professionalTax,
                        onValueChange = { professionalTax = it },
                        label = { Text("Professional Tax ($currencySymbol per month)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 4. Overtime & Working Days Rules
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Overtime & Calculation Rules", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = defaultShiftHours,
                            onValueChange = { defaultShiftHours = it },
                            label = { Text("Shift Std Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minOtThresholdMinutes,
                            onValueChange = { minOtThresholdMinutes = it },
                            label = { Text("Min OT (mins)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = defaultOtMultiplier,
                            onValueChange = { defaultOtMultiplier = it },
                            label = { Text("Normal OT Mult") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sundayOtMultiplier,
                            onValueChange = { sundayOtMultiplier = it },
                            label = { Text("Sunday/Holiday Mult") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Working days mode
                    Text("Working Days Mode", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WorkingDaysMode.values().forEach { mode ->
                            FilterChip(
                                selected = workingDaysMode == mode,
                                onClick = { workingDaysMode = mode },
                                label = { Text(mode.displayName, fontSize = 10.sp) }
                            )
                        }
                    }

                    // OT Base Mode
                    Text("OT Base Pay Calculation", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OtBaseMode.values().forEach { mode ->
                            FilterChip(
                                selected = otBaseMode == mode,
                                onClick = { otBaseMode = mode },
                                label = { Text(mode.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    // OT Rounding
                    Text("OT Rounding Interval", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OtRounding.values().forEach { r ->
                            FilterChip(
                                selected = otRounding == r,
                                onClick = { otRounding = r },
                                label = { Text(r.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Save Settings Button
        item {
            Button(
                onClick = {
                    val updated = currentProfile.copy(
                        employeeId = employeeId,
                        name = name,
                        designation = designation,
                        department = department,
                        companyName = companyName,
                        currencySymbol = currencySymbol,
                        basicPay = basicPay.toDoubleOrNull() ?: 20000.0,
                        hra = hra.toDoubleOrNull() ?: 4000.0,
                        conveyanceAllowance = conveyance.toDoubleOrNull() ?: 1500.0,
                        specialAllowance = specialAllowance.toDoubleOrNull() ?: 2000.0,
                        otherAllowances = otherAllowances.toDoubleOrNull() ?: 500.0,
                        isPfActive = isPfActive,
                        pfPercentage = pfPercentage.toDoubleOrNull() ?: 12.0,
                        isEsiActive = isEsiActive,
                        esiPercentage = esiPercentage.toDoubleOrNull() ?: 0.75,
                        professionalTax = professionalTax.toDoubleOrNull() ?: 200.0,
                        defaultShiftHours = defaultShiftHours.toDoubleOrNull() ?: 8.0,
                        defaultOtMultiplier = defaultOtMultiplier.toDoubleOrNull() ?: 1.5,
                        sundayOtMultiplier = sundayOtMultiplier.toDoubleOrNull() ?: 2.0,
                        workingDaysMode = workingDaysMode,
                        otBaseMode = otBaseMode,
                        otRounding = otRounding,
                        minOtThresholdMinutes = minOtThresholdMinutes.toIntOrNull() ?: 15
                    )
                    viewModel.updateProfile(updated)
                    Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_settings"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile & Rules", fontWeight = FontWeight.Bold)
            }
        }

        // 5. Offline Backup & Restore
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("100% Offline Data Backup & Restore", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Export a portable JSON backup of all your attendance logs, shifts, holidays, and settings to keep your data safe without cloud dependency.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                ExportReportUtil.exportDatabaseJson(
                                    context, currentProfile, allAttendance, allShifts, allHolidays, allAdjustments
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("application/json") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Backup", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
