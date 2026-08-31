package com.example.util

import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.HolidayRecordEntity
import com.example.data.local.entity.SalaryAdjustmentEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.OtBaseMode
import com.example.data.model.OtRounding
import com.example.data.model.WorkingDaysMode
import java.util.*
import kotlin.math.roundToInt

data class MonthlySalarySummary(
    val monthPrefix: String,
    val totalCalendarDays: Int,
    val effectiveWorkingDays: Double,
    
    // Counts
    val presentDaysCount: Double, // Present + On Duty + Extra Duty + Night Shift (1.0 each)
    val halfDaysCount: Int,
    val absentDaysCount: Int,
    val paidLeaveDaysCount: Int, // PL + CL + SL
    val weeklyOffDaysCount: Int,
    val festivalHolidayDaysCount: Int,
    val totalPayableDays: Double, // presentDays + halfDays*0.5 + paidLeaves + weeklyOffs + holidays
    
    // Work & Overtime Hours
    val totalRegularMinutes: Int,
    val totalOvertimeMinutes: Int,
    val totalOvertimeHours: Double,
    val singleOtHours: Double,
    val doubleOtHours: Double,
    
    // Salary Rates
    val hourlyRate: Double,
    val perDayRate: Double,
    
    // Earnings
    val baseGrossSalary: Double,
    val earnedBasicPay: Double,
    val earnedHra: Double,
    val earnedConveyance: Double,
    val earnedSpecialAllowance: Double,
    val earnedOtherAllowances: Double,
    val earnedFixedEarnings: Double,
    val totalOvertimePay: Double,
    val totalBonusesAndIncentives: Double,
    val totalGrossSalary: Double,
    
    // Deductions
    val pfDeduction: Double,
    val esiDeduction: Double,
    val professionalTax: Double,
    val advanceDeductions: Double,
    val penaltyDeductions: Double,
    val totalDeductions: Double,
    
    // Net
    val netTakeHomeSalary: Double,
    val attendancePercentage: Float
)

object SalaryCalculator {

    fun calculateOvertimeMinutes(
        totalWorkMinutes: Int,
        shiftStandardHours: Double,
        rounding: OtRounding = OtRounding.ROUND_15_MIN,
        minThresholdMinutes: Int = 15
    ): Int {
        val regularShiftMinutes = (shiftStandardHours * 60).toInt()
        val rawOtMinutes = totalWorkMinutes - regularShiftMinutes
        if (rawOtMinutes < minThresholdMinutes) return 0

        return when (rounding) {
            OtRounding.EXACT -> rawOtMinutes
            OtRounding.ROUND_15_MIN -> {
                val rem = rawOtMinutes % 15
                if (rem >= 8) rawOtMinutes + (15 - rem) else rawOtMinutes - rem
            }
            OtRounding.ROUND_30_MIN -> {
                val rem = rawOtMinutes % 30
                if (rem >= 15) rawOtMinutes + (30 - rem) else rawOtMinutes - rem
            }
        }
    }

    fun calculateMonthlySummary(
        monthPrefix: String,
        profile: UserProfileEntity,
        attendanceList: List<AttendanceRecordEntity>,
        holidaysList: List<HolidayRecordEntity>,
        adjustmentsList: List<SalaryAdjustmentEntity>
    ): MonthlySalarySummary {
        val daysInMonth = DateUtils.getDaysInMonth(monthPrefix)
        val totalCalendarDays = daysInMonth.size.coerceAtLeast(28)

        // Count calendar weekly offs (Sundays)
        val calendarSundaysCount = daysInMonth.count { DateUtils.isSunday(it) }

        // Effective working days based on mode
        val effectiveWorkingDays: Double = when (profile.workingDaysMode) {
            WorkingDaysMode.CALENDAR_DAYS -> totalCalendarDays.toDouble()
            WorkingDaysMode.FIXED_26 -> 26.0
            WorkingDaysMode.FIXED_30 -> 30.0
            WorkingDaysMode.EXCLUDE_WEEKLY_OFF -> (totalCalendarDays - calendarSundaysCount).toDouble().coerceAtLeast(1.0)
        }

        val attendanceMap = attendanceList.associateBy { it.date }
        val holidayMap = holidaysList.associateBy { it.date }

        var presentCount = 0.0
        var halfDaysCount = 0
        var absentCount = 0
        var paidLeaveCount = 0
        var weeklyOffCount = 0
        var festivalHolidayCount = 0

        var totalRegMinutes = 0
        var totalOtMinutes = 0
        var totalOtEarnings = 0.0
        var singleOtHours = 0.0
        var doubleOtHours = 0.0

        // Base salary for OT rate
        val fixedMonthlyGross = profile.basicPay + profile.hra + profile.conveyanceAllowance +
                profile.specialAllowance + profile.otherAllowances

        val otBaseSalary = if (profile.otBaseMode == OtBaseMode.GROSS_SALARY) fixedMonthlyGross else profile.basicPay
        val shiftHours = profile.defaultShiftHours.coerceAtLeast(1.0)
        val workingDaysForRate = effectiveWorkingDays.coerceAtLeast(1.0)

        // Standard hourly rate formula: Base / (WorkingDays * ShiftHours)
        val hourlyRate = otBaseSalary / (workingDaysForRate * shiftHours)
        val perDayRate = otBaseSalary / workingDaysForRate

        for (dayStr in daysInMonth) {
            val record = attendanceMap[dayStr]
            val holiday = holidayMap[dayStr]
            val isSunday = DateUtils.isSunday(dayStr)

            val status = record?.status ?: if (holiday != null) {
                AttendanceStatus.FESTIVAL_HOLIDAY
            } else if (isSunday) {
                AttendanceStatus.WEEKLY_OFF
            } else {
                // Not marked yet, default to Absent or unmarked
                AttendanceStatus.ABSENT
            }

            when (status) {
                AttendanceStatus.PRESENT, AttendanceStatus.ON_DUTY, AttendanceStatus.EXTRA_DUTY -> presentCount += 1.0
                AttendanceStatus.NIGHT_SHIFT -> presentCount += 1.0
                AttendanceStatus.HALF_DAY -> {
                    presentCount += 0.5
                    halfDaysCount++
                }
                AttendanceStatus.ABSENT -> absentCount++
                AttendanceStatus.PAID_LEAVE, AttendanceStatus.CASUAL_LEAVE, AttendanceStatus.SICK_LEAVE -> paidLeaveCount++
                AttendanceStatus.WEEKLY_OFF -> weeklyOffCount++
                AttendanceStatus.FESTIVAL_HOLIDAY -> festivalHolidayCount++
            }

            // OT Calculation
            if (record != null) {
                totalRegMinutes += record.regularMinutes
                
                val recordOtMinutes = if (record.manualOtHoursOverride != null) {
                    (record.manualOtHoursOverride * 60).toInt()
                } else {
                    record.overtimeMinutes
                }

                if (recordOtMinutes > 0) {
                    totalOtMinutes += recordOtMinutes
                    val otHours = recordOtMinutes / 60.0
                    val multiplier = if (isSunday || status == AttendanceStatus.WEEKLY_OFF || status == AttendanceStatus.FESTIVAL_HOLIDAY) {
                        profile.sundayOtMultiplier.coerceAtLeast(1.0)
                    } else {
                        record.otMultiplier.coerceAtLeast(profile.defaultOtMultiplier)
                    }

                    if (multiplier >= 1.9) {
                        doubleOtHours += otHours
                    } else {
                        singleOtHours += otHours
                    }

                    totalOtEarnings += (otHours * hourlyRate * multiplier)
                }
            }
        }

        // Total payable days
        val totalPayableDays = (presentCount + paidLeaveCount + weeklyOffCount + festivalHolidayCount)
            .coerceAtMost(totalCalendarDays.toDouble())

        val attendanceRatio = if (effectiveWorkingDays > 0) {
            (totalPayableDays / effectiveWorkingDays).coerceIn(0.0, 1.0)
        } else 1.0

        // Prorated fixed earnings based on payable days
        val earnedBasicPay = profile.basicPay * attendanceRatio
        val earnedHra = profile.hra * attendanceRatio
        val earnedConveyance = profile.conveyanceAllowance * attendanceRatio
        val earnedSpecialAllowance = profile.specialAllowance * attendanceRatio
        val earnedOtherAllowances = profile.otherAllowances * attendanceRatio
        val earnedFixedEarnings = earnedBasicPay + earnedHra + earnedConveyance + earnedSpecialAllowance + earnedOtherAllowances

        // Adjustments: Bonuses, Advances, Fines
        var totalBonuses = 0.0
        var totalAdvances = 0.0
        var totalPenalties = 0.0

        for (adj in adjustmentsList) {
            if (adj.type.isDeduction) {
                if (adj.type == com.example.data.model.AdjustmentType.PENALTY) {
                    totalPenalties += adj.amount
                } else {
                    totalAdvances += adj.amount
                }
            } else {
                totalBonuses += adj.amount
            }
        }

        val totalGrossSalary = earnedFixedEarnings + totalOtEarnings + totalBonuses

        // Deductions
        val pfDeduction = if (profile.isPfActive) {
            (earnedBasicPay * (profile.pfPercentage / 100.0))
        } else 0.0

        val esiDeduction = if (profile.isEsiActive && totalGrossSalary <= 21000.0) { // Standard ESI threshold
            (totalGrossSalary * (profile.esiPercentage / 100.0))
        } else if (profile.isEsiActive) {
            (totalGrossSalary * (profile.esiPercentage / 100.0))
        } else 0.0

        val professionalTax = profile.professionalTax
        val totalDeductions = pfDeduction + esiDeduction + professionalTax + totalAdvances + totalPenalties

        val netTakeHomeSalary = (totalGrossSalary - totalDeductions).coerceAtLeast(0.0)

        val attendancePercentage = if (effectiveWorkingDays > 0) {
            ((presentCount / effectiveWorkingDays) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        return MonthlySalarySummary(
            monthPrefix = monthPrefix,
            totalCalendarDays = totalCalendarDays,
            effectiveWorkingDays = effectiveWorkingDays,
            presentDaysCount = presentCount,
            halfDaysCount = halfDaysCount,
            absentDaysCount = absentCount,
            paidLeaveDaysCount = paidLeaveCount,
            weeklyOffDaysCount = weeklyOffCount,
            festivalHolidayDaysCount = festivalHolidayCount,
            totalPayableDays = totalPayableDays,
            totalRegularMinutes = totalRegMinutes,
            totalOvertimeMinutes = totalOtMinutes,
            totalOvertimeHours = totalOtMinutes / 60.0,
            singleOtHours = singleOtHours,
            doubleOtHours = doubleOtHours,
            hourlyRate = hourlyRate,
            perDayRate = perDayRate,
            baseGrossSalary = fixedMonthlyGross,
            earnedBasicPay = earnedBasicPay,
            earnedHra = earnedHra,
            earnedConveyance = earnedConveyance,
            earnedSpecialAllowance = earnedSpecialAllowance,
            earnedOtherAllowances = earnedOtherAllowances,
            earnedFixedEarnings = earnedFixedEarnings,
            totalOvertimePay = totalOtEarnings,
            totalBonusesAndIncentives = totalBonuses,
            totalGrossSalary = totalGrossSalary,
            pfDeduction = pfDeduction,
            esiDeduction = esiDeduction,
            professionalTax = professionalTax,
            advanceDeductions = totalAdvances,
            penaltyDeductions = totalPenalties,
            totalDeductions = totalDeductions,
            netTakeHomeSalary = netTakeHomeSalary,
            attendancePercentage = attendancePercentage
        )
    }

    fun formatCurrency(amount: Double, symbol: String = "₹"): String {
        return "$symbol%,.2f".format(Locale.US, amount)
    }

    fun formatCurrencyRounded(amount: Double, symbol: String = "₹"): String {
        return "$symbol%,d".format(Locale.US, amount.roundToInt())
    }
}
