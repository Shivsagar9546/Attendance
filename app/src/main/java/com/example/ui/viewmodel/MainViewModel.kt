package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.model.AttendanceStatus
import com.example.data.repository.AttendanceRepository
import com.example.util.DateUtils
import com.example.util.ExportReportUtil
import com.example.util.MonthlySalarySummary
import com.example.util.SalaryCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = AttendanceRepository(db)
    }

    // Selected Month (Format: "YYYY-MM")
    private val _selectedMonth = MutableStateFlow(DateUtils.getCurrentMonthPrefix())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // Filter query for Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // User Profile
    val userProfile: StateFlow<UserProfileEntity> = repository.userProfile
        .map { it ?: UserProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileEntity())

    // All Shifts
    val shifts: StateFlow<List<ShiftConfigEntity>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Holidays
    val holidays: StateFlow<List<HolidayRecordEntity>> = repository.allHolidays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Attendance Records Flow
    val monthAttendance: StateFlow<List<AttendanceRecordEntity>> = _selectedMonth
        .flatMapLatest { month -> repository.getAttendanceForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Holidays Flow
    val monthHolidays: StateFlow<List<HolidayRecordEntity>> = _selectedMonth
        .flatMapLatest { month -> repository.getHolidaysForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Adjustments Flow
    val monthAdjustments: StateFlow<List<SalaryAdjustmentEntity>> = _selectedMonth
        .flatMapLatest { month -> repository.getAdjustmentsForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent Attendance for Dashboard stream
    val recentAttendance: StateFlow<List<AttendanceRecordEntity>> = repository.getRecentAttendance(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's Attendance Record
    val todayAttendance: StateFlow<AttendanceRecordEntity?> = repository
        .getAttendanceByDateFlow(DateUtils.getTodayDateString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Calculated Monthly Summary
    val monthlySummary: StateFlow<MonthlySalarySummary> = combine(
        _selectedMonth,
        userProfile,
        monthAttendance,
        monthHolidays,
        monthAdjustments
    ) { month, profile, attendance, holidays, adjustments ->
        SalaryCalculator.calculateMonthlySummary(
            monthPrefix = month,
            profile = profile,
            attendanceList = attendance,
            holidaysList = holidays,
            adjustmentsList = adjustments
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SalaryCalculator.calculateMonthlySummary(
            DateUtils.getCurrentMonthPrefix(),
            UserProfileEntity(),
            emptyList(),
            emptyList(),
            emptyList()
        )
    )

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun previousMonth() {
        _selectedMonth.value = DateUtils.getPreviousMonth(_selectedMonth.value)
    }

    fun nextMonth() {
        _selectedMonth.value = DateUtils.getNextMonth(_selectedMonth.value)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Quick 1-Tap Punch In for Today
    fun punchInToday(shiftId: Long = 1L) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = DateUtils.getTodayDateString()
            val nowTime = DateUtils.getCurrentTime24()
            val existing = repository.getAttendanceByDate(today)

            val record = existing?.copy(
                inTime = nowTime,
                status = AttendanceStatus.PRESENT
            ) ?: AttendanceRecordEntity(
                date = today,
                status = AttendanceStatus.PRESENT,
                shiftId = shiftId,
                inTime = nowTime,
                outTime = null,
                totalWorkMinutes = 0,
                overtimeMinutes = 0
            )
            repository.saveAttendanceRecord(record)
        }
    }

    // Quick 1-Tap Punch Out for Today
    fun punchOutToday() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = DateUtils.getTodayDateString()
            val nowTime = DateUtils.getCurrentTime24()
            val existing = repository.getAttendanceByDate(today)
            val profile = repository.getUserProfileDirect()

            if (existing != null && !existing.inTime.isNullOrBlank()) {
                val totalMinutes = DateUtils.calculateDurationMinutes(existing.inTime, nowTime, existing.breakMinutes)
                val regularMinutes = (profile.defaultShiftHours * 60).toInt().coerceAtMost(totalMinutes)
                val otMinutes = SalaryCalculator.calculateOvertimeMinutes(
                    totalWorkMinutes = totalMinutes,
                    shiftStandardHours = profile.defaultShiftHours,
                    rounding = profile.otRounding,
                    minThresholdMinutes = profile.minOtThresholdMinutes
                )

                val updated = existing.copy(
                    outTime = nowTime,
                    totalWorkMinutes = totalMinutes,
                    regularMinutes = regularMinutes,
                    overtimeMinutes = otMinutes,
                    status = if (totalMinutes < (profile.defaultShiftHours * 60 / 2)) AttendanceStatus.HALF_DAY else AttendanceStatus.PRESENT
                )
                repository.saveAttendanceRecord(updated)
            } else {
                // If punch in was missing, record 8h default ending now
                val record = AttendanceRecordEntity(
                    date = today,
                    status = AttendanceStatus.PRESENT,
                    inTime = "09:00",
                    outTime = nowTime,
                    totalWorkMinutes = 480,
                    regularMinutes = 480,
                    overtimeMinutes = 0
                )
                repository.saveAttendanceRecord(record)
            }
        }
    }

    // Quick Mark Date Status
    fun quickMarkDateStatus(date: String, status: AttendanceStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getAttendanceByDate(date)
            val profile = repository.getUserProfileDirect()
            val isWork = status.isWorkDay

            val totalMins = if (status == AttendanceStatus.HALF_DAY) {
                (profile.defaultShiftHours * 30).toInt()
            } else if (isWork) {
                (profile.defaultShiftHours * 60).toInt()
            } else 0

            val record = existing?.copy(
                status = status,
                totalWorkMinutes = totalMins,
                regularMinutes = totalMins,
                overtimeMinutes = if (!isWork) 0 else existing.overtimeMinutes
            ) ?: AttendanceRecordEntity(
                date = date,
                status = status,
                inTime = if (isWork) "09:00" else null,
                outTime = if (status == AttendanceStatus.HALF_DAY) "13:30" else if (isWork) "18:00" else null,
                breakMinutes = if (isWork) 60 else 0,
                totalWorkMinutes = totalMins,
                regularMinutes = totalMins,
                overtimeMinutes = 0
            )

            repository.saveAttendanceRecord(record)
        }
    }

    // Full Save of Attendance Record from Dialog
    fun saveAttendanceRecord(
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
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = repository.getUserProfileDirect()
            val totalMinutes = if (status.isWorkDay && !inTime.isNullOrBlank() && !outTime.isNullOrBlank()) {
                DateUtils.calculateDurationMinutes(inTime, outTime, breakMinutes)
            } else if (status == AttendanceStatus.HALF_DAY) {
                (profile.defaultShiftHours * 30).toInt()
            } else if (status.isWorkDay) {
                (profile.defaultShiftHours * 60).toInt()
            } else {
                0
            }

            val regularMinutes = (profile.defaultShiftHours * 60).toInt().coerceAtMost(totalMinutes)
            val calculatedOt = SalaryCalculator.calculateOvertimeMinutes(
                totalWorkMinutes = totalMinutes,
                shiftStandardHours = profile.defaultShiftHours,
                rounding = profile.otRounding,
                minThresholdMinutes = profile.minOtThresholdMinutes
            )

            val finalOtMinutes = if (manualOtHours != null) (manualOtHours * 60).toInt() else calculatedOt

            val existing = repository.getAttendanceByDate(date)
            val entity = AttendanceRecordEntity(
                id = existing?.id ?: 0L,
                date = date,
                status = status,
                shiftId = shiftId,
                inTime = inTime,
                outTime = outTime,
                breakMinutes = breakMinutes,
                totalWorkMinutes = totalMinutes,
                regularMinutes = regularMinutes,
                overtimeMinutes = finalOtMinutes,
                otMultiplier = otMultiplier,
                isLateArrival = isLate,
                lateMinutes = lateMinutes,
                isEarlyExit = isEarlyExit,
                earlyExitMinutes = earlyExitMinutes,
                note = notes,
                manualOtHoursOverride = manualOtHours
            )

            repository.saveAttendanceRecord(entity)
        }
    }

    fun deleteAttendance(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAttendanceByDate(date)
        }
    }

    fun batchMarkMonthSundaysAsWeeklyOff(monthPrefix: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val days = DateUtils.getDaysInMonth(monthPrefix)
            val sundays = days.filter { DateUtils.isSunday(it) }
            val existing = repository.getAttendanceByDate(sundays.firstOrNull() ?: "")
            val list = sundays.map { date ->
                AttendanceRecordEntity(
                    date = date,
                    status = AttendanceStatus.WEEKLY_OFF,
                    inTime = null,
                    outTime = null,
                    breakMinutes = 0,
                    totalWorkMinutes = 0,
                    regularMinutes = 0,
                    overtimeMinutes = 0
                )
            }
            repository.saveAllAttendanceRecords(list)
        }
    }

    fun batchFillMonthAsPresent(monthPrefix: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val days = DateUtils.getDaysInMonth(monthPrefix)
            val profile = repository.getUserProfileDirect()
            val list = mutableListOf<AttendanceRecordEntity>()

            for (date in days) {
                if (DateUtils.isSunday(date)) {
                    list.add(
                        AttendanceRecordEntity(
                            date = date,
                            status = AttendanceStatus.WEEKLY_OFF,
                            inTime = null,
                            outTime = null,
                            breakMinutes = 0,
                            totalWorkMinutes = 0,
                            regularMinutes = 0,
                            overtimeMinutes = 0
                        )
                    )
                } else {
                    list.add(
                        AttendanceRecordEntity(
                            date = date,
                            status = AttendanceStatus.PRESENT,
                            inTime = "09:00",
                            outTime = "18:00",
                            breakMinutes = 60,
                            totalWorkMinutes = 480,
                            regularMinutes = 480,
                            overtimeMinutes = 0,
                            otMultiplier = profile.defaultOtMultiplier
                        )
                    )
                }
            }
            repository.saveAllAttendanceRecords(list)
        }
    }

    fun clearMonthAttendance(monthPrefix: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMonthAttendance(monthPrefix)
        }
    }

    // Profile updates
    fun updateProfile(profile: UserProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveUserProfile(profile)
        }
    }

    // Shift updates
    fun saveShift(shift: ShiftConfigEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveShift(shift)
        }
    }

    fun deleteShift(shift: ShiftConfigEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteShift(shift)
        }
    }

    // Holiday updates
    fun saveHoliday(holiday: HolidayRecordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveHoliday(holiday)
        }
    }

    fun deleteHoliday(holiday: HolidayRecordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHoliday(holiday)
        }
    }

    // Salary Adjustments
    fun saveAdjustment(adjustment: SalaryAdjustmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAdjustment(adjustment)
        }
    }

    fun deleteAdjustment(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAdjustmentById(id)
        }
    }

    // Restore Data from JSON
    fun restoreFromJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val holder = ExportReportUtil.parseDatabaseJson(jsonString)
                repository.restoreAllData(
                    holder.profile,
                    holder.attendance,
                    holder.shifts,
                    holder.holidays,
                    holder.adjustments
                )
                onResult(true, "Successfully restored ${holder.attendance.size} attendance records, ${holder.shifts.size} shifts, ${holder.holidays.size} holidays!")
            } catch (e: Exception) {
                onResult(false, "Restore failed: ${e.localizedMessage}")
            }
        }
    }
}
