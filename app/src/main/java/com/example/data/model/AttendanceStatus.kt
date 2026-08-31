package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class AttendanceStatus(
    val code: String,
    val displayName: String,
    val shortName: String,
    val dayCredit: Double, // 1.0 = Full day, 0.5 = Half day, 0.0 = Absent
    val isPaid: Boolean,
    val isWorkDay: Boolean
) {
    PRESENT("P", "Present", "Present (P)", 1.0, true, true),
    ABSENT("A", "Absent", "Absent (A)", 0.0, false, false),
    HALF_DAY("HD", "Half Day", "Half Day (HD)", 0.5, true, true),
    PAID_LEAVE("PL", "Paid Leave", "Paid Leave (PL)", 1.0, true, false),
    CASUAL_LEAVE("CL", "Casual Leave", "Casual Leave (CL)", 1.0, true, false),
    SICK_LEAVE("SL", "Sick Leave", "Sick Leave (SL)", 1.0, true, false),
    WEEKLY_OFF("WO", "Weekly Off", "Weekly Off (WO)", 1.0, true, false),
    FESTIVAL_HOLIDAY("FH", "Festival Holiday", "Festival Holiday (FH)", 1.0, true, false),
    NIGHT_SHIFT("NS", "Night Shift", "Night Shift (NS)", 1.0, true, true),
    EXTRA_DUTY("ED", "Extra Duty", "Extra Duty (ED)", 1.0, true, true),
    ON_DUTY("OD", "On Duty", "On Duty (OD)", 1.0, true, true);

    fun getBadgeColors(isDark: Boolean = false): Pair<Color, Color> {
        return when (this) {
            PRESENT, EXTRA_DUTY, ON_DUTY -> if (isDark) Pair(Color(0xFF064E3B), Color(0xFF6EE7B7)) else Pair(StatusPresentBg, StatusPresentText)
            ABSENT -> if (isDark) Pair(Color(0xFF7F1D1D), Color(0xFFFCA5A5)) else Pair(StatusAbsentBg, StatusAbsentText)
            HALF_DAY -> if (isDark) Pair(Color(0xFF581C87), Color(0xFFD8B4FE)) else Pair(StatusHalfDayBg, StatusHalfDayText)
            PAID_LEAVE, CASUAL_LEAVE, SICK_LEAVE -> if (isDark) Pair(Color(0xFF78350F), Color(0xFFFDE68A)) else Pair(StatusLeaveBg, StatusLeaveText)
            WEEKLY_OFF -> if (isDark) Pair(Color(0xFF0C4A6E), Color(0xFF7DD3FC)) else Pair(StatusWeeklyOffBg, StatusWeeklyOffText)
            FESTIVAL_HOLIDAY -> if (isDark) Pair(Color(0xFF831843), Color(0xFFF9A8D4)) else Pair(StatusHolidayBg, StatusHolidayText)
            NIGHT_SHIFT -> if (isDark) Pair(Color(0xFF312E81), Color(0xFFA5B4FC)) else Pair(StatusNightShiftBg, StatusNightShiftText)
        }
    }
}

enum class HolidayType(val displayName: String) {
    NATIONAL("National Holiday"),
    FESTIVAL("Festival Holiday"),
    COMPANY("Company Holiday"),
    OPTIONAL("Optional Leave")
}

enum class AdjustmentType(val displayName: String, val isDeduction: Boolean) {
    ADVANCE_DEDUCTION("Advance Repayment", true),
    PENALTY("Fine / Penalty", true),
    PF_EXTRA("Extra PF Deduction", true),
    OTHER_DEDUCTION("Other Deduction", true),
    BONUS("Festival / Annual Bonus", false),
    INCENTIVE("Performance Incentive", false),
    OVERTIME_BONUS("Special OT Bonus", false),
    TRAVEL_REIMBURSEMENT("Travel Allowance", false),
    OTHER_ALLOWANCE("Other Allowance", false)
}

enum class OtRounding(val displayName: String, val minutes: Int) {
    EXACT("Exact Minutes", 1),
    ROUND_15_MIN("Nearest 15 Mins", 15),
    ROUND_30_MIN("Nearest 30 Mins", 30)
}

enum class WorkingDaysMode(val displayName: String) {
    CALENDAR_DAYS("Actual Calendar Days in Month (28-31)"),
    FIXED_26("Fixed 26 Working Days"),
    FIXED_30("Fixed 30 Days Standard"),
    EXCLUDE_WEEKLY_OFF("Calendar Days minus Weekly Offs")
}

enum class OtBaseMode(val displayName: String) {
    BASIC_PAY("Basic Pay Only"),
    GROSS_SALARY("Gross Salary (Basic + Allowances)")
}
