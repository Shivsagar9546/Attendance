package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.MonthlySalarySummary
import com.example.util.SalaryCalculator

@Composable
fun MonthlyKpiOverview(
    summary: MonthlySalarySummary,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Card: Net Take Home Pay & Working Days
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_net_take_home"),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Slate900,
                                Slate800,
                                Color(0xFF0D9488)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ESTIMATED NET SALARY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Slate200
                            )
                            Text(
                                text = SalaryCalculator.formatCurrency(summary.netTakeHomeSalary, currencySymbol),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp
                                ),
                                color = Color.White
                            )
                        }

                        // Circular Present Percentage Badge
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "%.0f%%".format(summary.attendancePercentage),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF6EE7B7)
                                )
                                Text(
                                    text = "Rate",
                                    fontSize = 9.sp,
                                    color = Slate200
                                )
                            }
                        }
                    }

                    // Progress Bar for Attendance
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { summary.attendancePercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF34D399),
                            trackColor = Color(0x33FFFFFF),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Present: ${summary.presentDaysCount} / ${summary.totalCalendarDays} Days",
                                fontSize = 12.sp,
                                color = Slate200
                            )
                            Text(
                                text = "Gross: ${SalaryCalculator.formatCurrency(summary.totalGrossSalary, currencySymbol)}",
                                fontSize = 12.sp,
                                color = Slate200,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Grid for Quick Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Overtime Card
            MiniStatCard(
                title = "Total OT Hours",
                value = "%.1f hrs".format(summary.totalOvertimeHours),
                subValue = SalaryCalculator.formatCurrency(summary.totalOvertimePay, currencySymbol),
                icon = Icons.Default.Timer,
                iconTint = IndustrialAmber,
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )

            // Present Days Card
            MiniStatCard(
                title = "Payable Days",
                value = "${summary.totalPayableDays} Days",
                subValue = "${summary.absentDaysCount} Absent",
                icon = Icons.Default.CheckCircle,
                iconTint = StatusPresentDark,
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Gross Earnings Card
            MiniStatCard(
                title = "Earned Gross",
                value = SalaryCalculator.formatCurrency(summary.totalGrossSalary, currencySymbol),
                subValue = "Base + OT + Bonus",
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = ElectricBlue,
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )

            // Deductions Card
            MiniStatCard(
                title = "Total Deductions",
                value = SalaryCalculator.formatCurrency(summary.totalDeductions, currencySymbol),
                subValue = "PF, ESI & Advances",
                icon = Icons.Default.TrendingDown,
                iconTint = StatusAbsentDark,
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MiniStatCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
