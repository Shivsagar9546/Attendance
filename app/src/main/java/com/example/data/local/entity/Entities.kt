package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AdjustmentType
import com.example.data.model.AttendanceStatus
import com.example.data.model.HolidayType
import com.example.data.model.OtBaseMode
import com.example.data.model.OtRounding
import com.example.data.model.WorkingDaysMode

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val employeeId: String = "EMP-1001",
    val name: String = "Rajesh Kumar",
    val designation: String = "Senior Machine Operator",
    val department: String = "Production / Assembly",
    val companyName: String = "Apex Precision Engineering Ltd.",
    val currencySymbol: String = "₹",
    
    // Earnings Structure
    val basicPay: Double = 22000.0,
    val hra: Double = 4500.0,
    val conveyanceAllowance: Double = 1800.0,
    val specialAllowance: Double = 2500.0,
    val otherAllowances: Double = 1000.0,
    
    // Deductions Structure
    val pfPercentage: Double = 12.0, // 12% of basic
    val isPfActive: Boolean = true,
    val esiPercentage: Double = 0.75, // 0.75% of gross
    val isEsiActive: Boolean = true,
    val professionalTax: Double = 200.0,
    
    // Shift & Overtime Config
    val defaultShiftHours: Double = 8.0,
    val standardWeeklyOffDay: Int = 1, // Calendar.SUNDAY = 1
    val defaultOtMultiplier: Double = 1.5,
    val sundayOtMultiplier: Double = 2.0,
    val workingDaysMode: WorkingDaysMode = WorkingDaysMode.CALENDAR_DAYS,
    val otBaseMode: OtBaseMode = OtBaseMode.BASIC_PAY,
    val otRounding: OtRounding = OtRounding.ROUND_15_MIN,
    val minOtThresholdMinutes: Int = 15,
    
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["date"], unique = true)]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val shiftId: Long = 1L,
    val inTime: String? = "09:00", // "HH:mm"
    val outTime: String? = "18:00", // "HH:mm"
    val breakMinutes: Int = 60,
    val totalWorkMinutes: Int = 480,
    val regularMinutes: Int = 480,
    val overtimeMinutes: Int = 0,
    val otMultiplier: Double = 1.0,
    val isLateArrival: Boolean = false,
    val lateMinutes: Int = 0,
    val isEarlyExit: Boolean = false,
    val earlyExitMinutes: Int = 0,
    val note: String = "",
    val manualOtHoursOverride: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shift_configs")
data class ShiftConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String, // "G", "M", "E", "N"
    val startTime: String, // "09:00"
    val endTime: String, // "17:30"
    val durationHours: Double = 8.0,
    val breakMinutes: Int = 30,
    val isNightShift: Boolean = false,
    val colorHex: String = "#2563EB"
)

@Entity(
    tableName = "holiday_records",
    indices = [Index(value = ["date"], unique = true)]
)
data class HolidayRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val name: String,
    val type: HolidayType = HolidayType.FESTIVAL,
    val isPaid: Boolean = true,
    val colorHex: String = "#DB2777"
)

@Entity(tableName = "salary_adjustments")
data class SalaryAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthYear: String, // "YYYY-MM"
    val title: String,
    val type: AdjustmentType,
    val amount: Double,
    val date: String, // "YYYY-MM-DD"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
