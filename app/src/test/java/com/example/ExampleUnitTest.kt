package com.example

import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.HolidayRecordEntity
import com.example.data.local.entity.SalaryAdjustmentEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.AdjustmentType
import com.example.data.model.AttendanceStatus
import com.example.data.model.HolidayType
import com.example.data.model.OtRounding
import com.example.util.DateUtils
import com.example.util.SalaryCalculator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testDateUtils_durationAndFormatting() {
    val durationMinutes = DateUtils.calculateDurationMinutes("09:00", "18:00", 60)
    assertEquals(480, durationMinutes) // 9h - 1h break = 8h = 480 mins

    val formatted = DateUtils.formatMinutesToHoursMins(510)
    assertEquals("8h 30m", formatted)

    val time12h = DateUtils.formatTime12H("14:30")
    assertEquals("02:30 PM", time12h)
  }

  @Test
  fun testSalaryCalculator_overtimeRounding() {
    // 8h shift = 480 mins. Total work = 550 mins. Raw OT = 70 mins.
    // 70 % 15 = 10 (rem >= 8 -> rounds up to 75 mins)
    val otRounded15 = SalaryCalculator.calculateOvertimeMinutes(
      totalWorkMinutes = 550,
      shiftStandardHours = 8.0,
      rounding = OtRounding.ROUND_15_MIN,
      minThresholdMinutes = 15
    )
    assertEquals(75, otRounded15)

    // Raw OT under threshold (10 mins < 15 mins) -> 0 OT
    val otBelowThreshold = SalaryCalculator.calculateOvertimeMinutes(
      totalWorkMinutes = 490,
      shiftStandardHours = 8.0,
      rounding = OtRounding.ROUND_15_MIN,
      minThresholdMinutes = 15
    )
    assertEquals(0, otBelowThreshold)
  }

  @Test
  fun testSalaryCalculator_monthlyCalculation() {
    val profile = UserProfileEntity(
      basicPay = 26000.0,
      hra = 4000.0,
      conveyanceAllowance = 0.0,
      specialAllowance = 0.0,
      otherAllowances = 0.0,
      defaultShiftHours = 8.0,
      defaultOtMultiplier = 1.5,
      sundayOtMultiplier = 2.0,
      isPfActive = true,
      pfPercentage = 12.0,
      isEsiActive = false,
      professionalTax = 200.0
    )

    // Add 20 days Present with 2 hours OT each (120 mins OT at 1.5x)
    val attendance = mutableListOf<AttendanceRecordEntity>()
    for (day in 1..20) {
      val dateStr = "2026-08-%02d".format(day)
      attendance.add(
        AttendanceRecordEntity(
          date = dateStr,
          status = AttendanceStatus.PRESENT,
          totalWorkMinutes = 600,
          regularMinutes = 480,
          overtimeMinutes = 120,
          otMultiplier = 1.5
        )
      )
    }

    val holidays = listOf(
      HolidayRecordEntity(date = "2026-08-15", name = "Independence Day", type = HolidayType.NATIONAL, isPaid = true)
    )

    val adjustments = listOf(
      SalaryAdjustmentEntity(monthYear = "2026-08", title = "Bonus", type = AdjustmentType.BONUS, amount = 1000.0, date = "2026-08-10")
    )

    val summary = SalaryCalculator.calculateMonthlySummary(
      monthPrefix = "2026-08",
      profile = profile,
      attendanceList = attendance,
      holidaysList = holidays,
      adjustmentsList = adjustments
    )

    assertTrue("Net salary must be positive", summary.netTakeHomeSalary > 0)
    assertTrue("OT Pay must be calculated", summary.totalOvertimePay > 0)
    assertEquals(40.0, summary.totalOvertimeHours, 0.01) // 20 days * 2 hrs = 40 hrs OT
    assertTrue("Gross must include basic + OT + bonus", summary.totalGrossSalary > 30000.0)
  }
}

