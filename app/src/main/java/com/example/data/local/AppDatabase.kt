package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import com.example.data.model.HolidayType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        AttendanceRecordEntity::class,
        ShiftConfigEntity::class,
        HolidayRecordEntity::class,
        SalaryAdjustmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun shiftDao(): ShiftDao
    abstract fun holidayDao(): HolidayDao
    abstract fun salaryAdjustmentDao(): SalaryAdjustmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workforce_attendance_db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        prepopulateDefaults(database)
                    }
                }
            }
        }

        suspend fun prepopulateDefaults(database: AppDatabase) {
            // 1. Initial User Profile
            if (database.userProfileDao().getProfile() == null) {
                database.userProfileDao().insertOrUpdate(UserProfileEntity())
            }

            // 2. Default Shift Configurations
            val defaultShifts = listOf(
                ShiftConfigEntity(
                    id = 1L,
                    name = "General Shift",
                    code = "GS",
                    startTime = "09:00",
                    endTime = "18:00",
                    durationHours = 8.0,
                    breakMinutes = 60,
                    isNightShift = false,
                    colorHex = "#2563EB"
                ),
                ShiftConfigEntity(
                    id = 2L,
                    name = "Morning Shift (A)",
                    code = "MS",
                    startTime = "06:00",
                    endTime = "14:30",
                    durationHours = 8.0,
                    breakMinutes = 30,
                    isNightShift = false,
                    colorHex = "#0D9488"
                ),
                ShiftConfigEntity(
                    id = 3L,
                    name = "Evening Shift (B)",
                    code = "ES",
                    startTime = "14:00",
                    endTime = "22:30",
                    durationHours = 8.0,
                    breakMinutes = 30,
                    isNightShift = false,
                    colorHex = "#EA580C"
                ),
                ShiftConfigEntity(
                    id = 4L,
                    name = "Night Shift (C)",
                    code = "NS",
                    startTime = "22:00",
                    endTime = "06:30",
                    durationHours = 8.0,
                    breakMinutes = 30,
                    isNightShift = true,
                    colorHex = "#7C3AED"
                ),
                ShiftConfigEntity(
                    id = 5L,
                    name = "12-Hour Factory Duty",
                    code = "12H",
                    startTime = "08:00",
                    endTime = "20:00",
                    durationHours = 12.0,
                    breakMinutes = 60,
                    isNightShift = false,
                    colorHex = "#0284C7"
                )
            )
            database.shiftDao().insertAll(defaultShifts)

            // 3. Common Factory/Annual Holidays for year 2026
            val defaultHolidays = listOf(
                HolidayRecordEntity(date = "2026-01-01", name = "New Year's Day", type = HolidayType.NATIONAL, colorHex = "#2563EB"),
                HolidayRecordEntity(date = "2026-01-26", name = "Republic Day", type = HolidayType.NATIONAL, colorHex = "#EA580C"),
                HolidayRecordEntity(date = "2026-03-04", name = "Holi Festival", type = HolidayType.FESTIVAL, colorHex = "#DB2777"),
                HolidayRecordEntity(date = "2026-05-01", name = "International Labour Day", type = HolidayType.NATIONAL, colorHex = "#0D9488"),
                HolidayRecordEntity(date = "2026-08-15", name = "Independence Day", type = HolidayType.NATIONAL, colorHex = "#EA580C"),
                HolidayRecordEntity(date = "2026-10-02", name = "Gandhi Jayanti", type = HolidayType.NATIONAL, colorHex = "#16A34A"),
                HolidayRecordEntity(date = "2026-10-20", name = "Dussehra / Vijayadashami", type = HolidayType.FESTIVAL, colorHex = "#D97706"),
                HolidayRecordEntity(date = "2026-11-08", name = "Diwali (Deepavali)", type = HolidayType.FESTIVAL, colorHex = "#F59E0B"),
                HolidayRecordEntity(date = "2026-12-25", name = "Christmas", type = HolidayType.FESTIVAL, colorHex = "#DC2626")
            )
            database.holidayDao().insertAll(defaultHolidays)
        }
    }
}
