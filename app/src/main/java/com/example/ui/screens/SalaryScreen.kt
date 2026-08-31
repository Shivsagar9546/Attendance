package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.SalaryAdjustmentEntity
import com.example.data.model.AdjustmentType
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import com.example.util.ExportReportUtil
import com.example.util.PdfExportUtil
import com.example.util.SalaryCalculator

@Composable
fun SalaryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val monthAdjustments by viewModel.monthAdjustments.collectAsState()
    val monthAttendance by viewModel.monthAttendance.collectAsState()
    val monthHolidays by viewModel.monthHolidays.collectAsState()

    var showAddAdjustmentDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_salary"),
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

        // 1. Payslip Hero Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_payslip_hero"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = userProfile.companyName.ifBlank { "Factory / Company Payslip" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Period: ${DateUtils.formatMonthDisplay(selectedMonth)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate200
                            )
                        }

                        Text(
                            text = userProfile.employeeId,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ElectricBlueLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate800)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    HorizontalDivider(color = Slate700)

                    // Net Salary Block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NET TAKE-HOME PAY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF6EE7B7)
                            )
                            Text(
                                text = SalaryCalculator.formatCurrency(monthlySummary.netTakeHomeSalary, userProfile.currencySymbol),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp
                                ),
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }

                    // Key Metrics
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate800)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gross Earnings", fontSize = 11.sp, color = Slate200)
                            Text(
                                SalaryCalculator.formatCurrency(monthlySummary.totalGrossSalary, userProfile.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(28.dp), color = Slate700)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Deductions", fontSize = 11.sp, color = Slate200)
                            Text(
                                SalaryCalculator.formatCurrency(monthlySummary.totalDeductions, userProfile.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusAbsentLight
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(28.dp), color = Slate700)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Payable Days", fontSize = 11.sp, color = Slate200)
                            Text(
                                "${monthlySummary.totalPayableDays} / ${monthlySummary.totalCalendarDays}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2. Export Actions Grid
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Export & Share Payslip",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = {
                            PdfExportUtil.generateAndShareSalarySlipPdf(context, userProfile, monthlySummary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_export_pdf"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download & Share PDF Salary Slip", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ExportReportUtil.exportAttendanceExcel(
                                    context, userProfile, selectedMonth, monthAttendance, monthHolidays, monthlySummary
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_excel"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel (.xls)", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                ExportReportUtil.exportAttendanceCsv(
                                    context, userProfile, selectedMonth, monthAttendance, monthHolidays, monthlySummary
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_csv"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV Sheet", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Earnings Breakdown Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = StatusPresentDark)
                            Text("Earnings Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Text(
                            SalaryCalculator.formatCurrency(monthlySummary.totalGrossSalary, userProfile.currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = StatusPresentDark)
                        )
                    }

                    HorizontalDivider()

                    SalaryItemRow("Basic Salary (Earned)", monthlySummary.earnedBasicPay, userProfile.currencySymbol)
                    SalaryItemRow("House Rent Allowance (HRA)", monthlySummary.earnedHra, userProfile.currencySymbol)
                    SalaryItemRow("Conveyance Allowance", monthlySummary.earnedConveyance, userProfile.currencySymbol)
                    SalaryItemRow("Special Allowance", monthlySummary.earnedSpecialAllowance, userProfile.currencySymbol)
                    SalaryItemRow("Other Allowances", monthlySummary.earnedOtherAllowances, userProfile.currencySymbol)
                    SalaryItemRow(
                        "Overtime Pay (${"%.1f".format(monthlySummary.totalOvertimeHours)} hrs)",
                        monthlySummary.totalOvertimePay,
                        userProfile.currencySymbol,
                        isHighlight = true
                    )
                    SalaryItemRow("Bonuses & Incentives", monthlySummary.totalBonusesAndIncentives, userProfile.currencySymbol)
                }
            }
        }

        // 4. Deductions Breakdown Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = StatusAbsentDark)
                            Text("Deductions Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Text(
                            SalaryCalculator.formatCurrency(monthlySummary.totalDeductions, userProfile.currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = StatusAbsentDark)
                        )
                    }

                    HorizontalDivider()

                    SalaryItemRow("Provident Fund (PF ${userProfile.pfPercentage}%)", monthlySummary.pfDeduction, userProfile.currencySymbol)
                    SalaryItemRow("ESI Insurance (${userProfile.esiPercentage}%)", monthlySummary.esiDeduction, userProfile.currencySymbol)
                    SalaryItemRow("Professional Tax (PT)", monthlySummary.professionalTax, userProfile.currencySymbol)
                    SalaryItemRow("Advance Repayments", monthlySummary.advanceDeductions, userProfile.currencySymbol)
                    SalaryItemRow("Penalties & Late Fines", monthlySummary.penaltyDeductions, userProfile.currencySymbol)
                }
            }
        }

        // 5. Salary Adjustments (Bonuses, Advances, Penalties) Manager
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Adjustments", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Bonuses, Advances & Deductions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = { showAddAdjustmentDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp)
                        }
                    }

                    if (monthAdjustments.isEmpty()) {
                        Text(
                            "No adjustments added for this month.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        monthAdjustments.forEach { adj ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(adj.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${adj.type.displayName} • ${adj.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${if (adj.type.isDeduction) "-" else "+"}${SalaryCalculator.formatCurrency(adj.amount, userProfile.currencySymbol)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (adj.type.isDeduction) StatusAbsentDark else StatusPresentDark
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteAdjustment(adj.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Slate500, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Adjustment Dialog
    if (showAddAdjustmentDialog) {
        AddAdjustmentDialog(
            monthYear = selectedMonth,
            onDismiss = { showAddAdjustmentDialog = false },
            onSave = { adj ->
                viewModel.saveAdjustment(adj)
                showAddAdjustmentDialog = false
            }
        )
    }
}

@Composable
fun SalaryItemRow(
    label: String,
    amount: Double,
    currencySymbol: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) IndustrialAmberDark else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = SalaryCalculator.formatCurrency(amount, currencySymbol),
            fontSize = 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) IndustrialAmberDark else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AddAdjustmentDialog(
    monthYear: String,
    onDismiss: () -> Unit,
    onSave: (SalaryAdjustmentEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AdjustmentType.BONUS) }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Add Salary Adjustment", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Festival Bonus, Diwali Advance)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Adjustment Category", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AdjustmentType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0) {
                                onSave(
                                    SalaryAdjustmentEntity(
                                        monthYear = monthYear,
                                        title = title,
                                        type = selectedType,
                                        amount = amt,
                                        date = DateUtils.getTodayDateString(),
                                        notes = notes
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Add Adjustment")
                    }
                }
            }
        }
    }
}
