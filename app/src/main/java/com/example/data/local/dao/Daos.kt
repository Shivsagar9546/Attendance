package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date LIMIT 1")
    suspend fun getAttendanceByDate(date: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE date = :date LIMIT 1")
    fun getAttendanceByDateFlow(date: String): Flow<AttendanceRecordEntity?>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC LIMIT :limit")
    fun getRecentAttendance(limit: Int = 10): Flow<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceRecordEntity>)

    @Delete
    suspend fun delete(record: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM attendance_records WHERE date LIKE :monthPrefix || '%'")
    suspend fun deleteMonthAttendance(monthPrefix: String)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shift_configs ORDER BY id ASC")
    fun getAllShifts(): Flow<List<ShiftConfigEntity>>

    @Query("SELECT * FROM shift_configs WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): ShiftConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(shift: ShiftConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shifts: List<ShiftConfigEntity>)

    @Delete
    suspend fun delete(shift: ShiftConfigEntity)

    @Query("DELETE FROM shift_configs")
    suspend fun clearAll()
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holiday_records ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<HolidayRecordEntity>>

    @Query("SELECT * FROM holiday_records WHERE date LIKE :yearPrefix || '%' ORDER BY date ASC")
    fun getHolidaysForYear(yearPrefix: String): Flow<List<HolidayRecordEntity>>

    @Query("SELECT * FROM holiday_records WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getHolidaysForMonth(monthPrefix: String): Flow<List<HolidayRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(holiday: HolidayRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holidays: List<HolidayRecordEntity>)

    @Delete
    suspend fun delete(holiday: HolidayRecordEntity)

    @Query("DELETE FROM holiday_records")
    suspend fun clearAll()
}

@Dao
interface SalaryAdjustmentDao {
    @Query("SELECT * FROM salary_adjustments WHERE monthYear = :monthYear ORDER BY date ASC")
    fun getAdjustmentsForMonth(monthYear: String): Flow<List<SalaryAdjustmentEntity>>

    @Query("SELECT * FROM salary_adjustments ORDER BY date DESC")
    fun getAllAdjustments(): Flow<List<SalaryAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(adjustment: SalaryAdjustmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(adjustments: List<SalaryAdjustmentEntity>)

    @Delete
    suspend fun delete(adjustment: SalaryAdjustmentEntity)

    @Query("DELETE FROM salary_adjustments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM salary_adjustments")
    suspend fun clearAll()
}
