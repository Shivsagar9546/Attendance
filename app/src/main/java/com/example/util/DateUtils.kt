package com.example.util

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object DateUtils {
    val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val MONTH_FORMAT = SimpleDateFormat("yyyy-MM", Locale.US)
    val MONTH_DISPLAY_FORMAT = SimpleDateFormat("MMMM yyyy", Locale.US)
    val SHORT_DATE_FORMAT = SimpleDateFormat("dd MMM", Locale.US)
    val FULL_DATE_DISPLAY_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
    val DAY_NAME_FORMAT = SimpleDateFormat("EEE", Locale.US)
    val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.US)
    val TIME_12H_FORMAT = SimpleDateFormat("hh:mm a", Locale.US)

    fun getTodayDateString(): String = DATE_FORMAT.format(Date())

    fun getCurrentMonthPrefix(): String = MONTH_FORMAT.format(Date())

    fun formatMonthDisplay(monthPrefix: String): String {
        return try {
            val date = MONTH_FORMAT.parse(monthPrefix)
            if (date != null) MONTH_DISPLAY_FORMAT.format(date) else monthPrefix
        } catch (e: Exception) {
            monthPrefix
        }
    }

    fun formatFullDate(dateStr: String): String {
        return try {
            val date = DATE_FORMAT.parse(dateStr)
            if (date != null) FULL_DATE_DISPLAY_FORMAT.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatShortDate(dateStr: String): String {
        return try {
            val date = DATE_FORMAT.parse(dateStr)
            if (date != null) SHORT_DATE_FORMAT.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getDayOfWeekName(dateStr: String): String {
        return try {
            val date = DATE_FORMAT.parse(dateStr)
            if (date != null) DAY_NAME_FORMAT.format(date) else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getDayOfWeek(dateStr: String): Int {
        return try {
            val cal = Calendar.getInstance()
            val date = DATE_FORMAT.parse(dateStr)
            if (date != null) {
                cal.time = date
                cal.get(Calendar.DAY_OF_WEEK)
            } else 1
        } catch (e: Exception) {
            1
        }
    }

    fun isSunday(dateStr: String): Boolean {
        return getDayOfWeek(dateStr) == Calendar.SUNDAY
    }

    fun getDaysInMonth(monthPrefix: String): List<String> {
        val days = mutableListOf<String>()
        try {
            val cal = Calendar.getInstance()
            val date = MONTH_FORMAT.parse(monthPrefix) ?: return emptyList()
            cal.time = date
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in 1..maxDay) {
                cal.set(Calendar.DAY_OF_MONTH, i)
                days.add(DATE_FORMAT.format(cal.time))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return days
    }

    fun getPreviousMonth(monthPrefix: String): String {
        return try {
            val cal = Calendar.getInstance()
            cal.time = MONTH_FORMAT.parse(monthPrefix) ?: Date()
            cal.add(Calendar.MONTH, -1)
            MONTH_FORMAT.format(cal.time)
        } catch (e: Exception) {
            monthPrefix
        }
    }

    fun getNextMonth(monthPrefix: String): String {
        return try {
            val cal = Calendar.getInstance()
            cal.time = MONTH_FORMAT.parse(monthPrefix) ?: Date()
            cal.add(Calendar.MONTH, 1)
            MONTH_FORMAT.format(cal.time)
        } catch (e: Exception) {
            monthPrefix
        }
    }

    /**
     * Calculates duration in minutes between inTime and outTime ("HH:mm")
     * Supports overnight shifts (e.g. 22:00 to 06:00)
     */
    fun calculateDurationMinutes(inTime: String?, outTime: String?, breakMinutes: Int = 0): Int {
        if (inTime.isNullOrBlank() || outTime.isNullOrBlank()) return 0
        return try {
            val inParts = inTime.split(":").map { it.toInt() }
            val outParts = outTime.split(":").map { it.toInt() }

            val inTotalMin = inParts[0] * 60 + inParts[1]
            var outTotalMin = outParts[0] * 60 + outParts[1]

            if (outTotalMin < inTotalMin) {
                // Overnight shift crosses midnight
                outTotalMin += 24 * 60
            }

            val grossMinutes = outTotalMin - inTotalMin
            val netMinutes = grossMinutes - breakMinutes
            if (netMinutes > 0) netMinutes else 0
        } catch (e: Exception) {
            0
        }
    }

    fun formatMinutesToHoursMins(minutes: Int): String {
        if (minutes <= 0) return "0h 0m"
        val hours = minutes / 60
        val mins = minutes % 60
        return if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
    }

    fun formatMinutesToDecimalHours(minutes: Int): Double {
        return String.format(Locale.US, "%.2f", minutes / 60.0).toDoubleOrNull() ?: 0.0
    }

    fun formatTime12H(time24: String?): String {
        if (time24.isNullOrBlank()) return "--:--"
        return try {
            val date = TIME_FORMAT.parse(time24)
            if (date != null) TIME_12H_FORMAT.format(date) else time24
        } catch (e: Exception) {
            time24
        }
    }

    fun getCurrentTime24(): String {
        return TIME_FORMAT.format(Date())
    }
}
