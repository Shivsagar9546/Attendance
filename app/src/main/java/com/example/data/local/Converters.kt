package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.*

class AppTypeConverters {
    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus?): String? = status?.name

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus? =
        value?.let { runCatching { AttendanceStatus.valueOf(it) }.getOrDefault(AttendanceStatus.PRESENT) }

    @TypeConverter
    fun fromHolidayType(type: HolidayType?): String? = type?.name

    @TypeConverter
    fun toHolidayType(value: String?): HolidayType? =
        value?.let { runCatching { HolidayType.valueOf(it) }.getOrDefault(HolidayType.FESTIVAL) }

    @TypeConverter
    fun fromAdjustmentType(type: AdjustmentType?): String? = type?.name

    @TypeConverter
    fun toAdjustmentType(value: String?): AdjustmentType? =
        value?.let { runCatching { AdjustmentType.valueOf(it) }.getOrDefault(AdjustmentType.BONUS) }

    @TypeConverter
    fun fromWorkingDaysMode(mode: WorkingDaysMode?): String? = mode?.name

    @TypeConverter
    fun toWorkingDaysMode(value: String?): WorkingDaysMode? =
        value?.let { runCatching { WorkingDaysMode.valueOf(it) }.getOrDefault(WorkingDaysMode.CALENDAR_DAYS) }

    @TypeConverter
    fun fromOtBaseMode(mode: OtBaseMode?): String? = mode?.name

    @TypeConverter
    fun toOtBaseMode(value: String?): OtBaseMode? =
        value?.let { runCatching { OtBaseMode.valueOf(it) }.getOrDefault(OtBaseMode.BASIC_PAY) }

    @TypeConverter
    fun fromOtRounding(rounding: OtRounding?): String? = rounding?.name

    @TypeConverter
    fun toOtRounding(value: String?): OtRounding? =
        value?.let { runCatching { OtRounding.valueOf(it) }.getOrDefault(OtRounding.ROUND_15_MIN) }
}
