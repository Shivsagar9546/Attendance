package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val database: AppDatabase) {

    // Profile
    val userProfile: Flow<UserProfileEntity?> = database.userProfileDao().getProfileFlow()
    suspend fun getUserProfileDirect(): UserProfileEntity =
        database.userProfileDao().getProfile() ?: UserProfileEntity().also {
            database.userProfileDao().insertOrUpdate(it)
        }
    suspend fun saveUserProfile(profile: UserProfileEntity) =
        database.userProfileDao().insertOrUpdate(profile)

    // Attendance
    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>> =
        database.attendanceDao().getAttendanceForMonth(monthPrefix)

    fun getAttendanceByDateFlow(date: String): Flow<AttendanceRecordEntity?> =
        database.attendanceDao().getAttendanceByDateFlow(date)

    suspend fun getAttendanceByDate(date: String): AttendanceRecordEntity? =
        database.attendanceDao().getAttendanceByDate(date)

    fun getRecentAttendance(limit: Int = 15): Flow<List<AttendanceRecordEntity>> =
        database.attendanceDao().getRecentAttendance(limit)

    fun getAllAttendance(): Flow<List<AttendanceRecordEntity>> =
        database.attendanceDao().getAllAttendance()

    suspend fun saveAttendanceRecord(record: AttendanceRecordEntity): Long =
        database.attendanceDao().insertOrUpdate(record)

    suspend fun saveAllAttendanceRecords(records: List<AttendanceRecordEntity>) =
        database.attendanceDao().insertAll(records)

    suspend fun deleteAttendance(record: AttendanceRecordEntity) =
        database.attendanceDao().delete(record)

    suspend fun deleteAttendanceByDate(date: String) =
        database.attendanceDao().deleteByDate(date)

    suspend fun deleteMonthAttendance(monthPrefix: String) =
        database.attendanceDao().deleteMonthAttendance(monthPrefix)

    // Shifts
    val allShifts: Flow<List<ShiftConfigEntity>> = database.shiftDao().getAllShifts()
    suspend fun getShiftById(id: Long): ShiftConfigEntity? = database.shiftDao().getShiftById(id)
    suspend fun saveShift(shift: ShiftConfigEntity): Long = database.shiftDao().insertOrUpdate(shift)
    suspend fun deleteShift(shift: ShiftConfigEntity) = database.shiftDao().delete(shift)

    // Holidays
    val allHolidays: Flow<List<HolidayRecordEntity>> = database.holidayDao().getAllHolidays()
    fun getHolidaysForYear(yearPrefix: String): Flow<List<HolidayRecordEntity>> =
        database.holidayDao().getHolidaysForYear(yearPrefix)
    fun getHolidaysForMonth(monthPrefix: String): Flow<List<HolidayRecordEntity>> =
        database.holidayDao().getHolidaysForMonth(monthPrefix)
    suspend fun saveHoliday(holiday: HolidayRecordEntity): Long =
        database.holidayDao().insertOrUpdate(holiday)
    suspend fun deleteHoliday(holiday: HolidayRecordEntity) =
        database.holidayDao().delete(holiday)

    // Adjustments
    fun getAdjustmentsForMonth(monthYear: String): Flow<List<SalaryAdjustmentEntity>> =
        database.salaryAdjustmentDao().getAdjustmentsForMonth(monthYear)
    val allAdjustments: Flow<List<SalaryAdjustmentEntity>> = database.salaryAdjustmentDao().getAllAdjustments()
    suspend fun saveAdjustment(adjustment: SalaryAdjustmentEntity): Long =
        database.salaryAdjustmentDao().insertOrUpdate(adjustment)
    suspend fun deleteAdjustment(adjustment: SalaryAdjustmentEntity) =
        database.salaryAdjustmentDao().delete(adjustment)
    suspend fun deleteAdjustmentById(id: Long) =
        database.salaryAdjustmentDao().deleteById(id)

    // Clear and restore
    suspend fun restoreAllData(
        profile: UserProfileEntity?,
        attendance: List<AttendanceRecordEntity>,
        shifts: List<ShiftConfigEntity>,
        holidays: List<HolidayRecordEntity>,
        adjustments: List<SalaryAdjustmentEntity>
    ) {
        if (profile != null) database.userProfileDao().insertOrUpdate(profile)
        if (attendance.isNotEmpty()) database.attendanceDao().insertAll(attendance)
        if (shifts.isNotEmpty()) database.shiftDao().insertAll(shifts)
        if (holidays.isNotEmpty()) database.holidayDao().insertAll(holidays)
        if (adjustments.isNotEmpty()) database.salaryAdjustmentDao().insertAll(adjustments)
    }
}
