package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.*
import com.example.data.model.AttendanceStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportReportUtil {

    /**
     * Exports full monthly attendance register as CSV
     */
    fun exportAttendanceCsv(
        context: Context,
        profile: UserProfileEntity,
        monthPrefix: String,
        attendanceList: List<AttendanceRecordEntity>,
        holidaysList: List<HolidayRecordEntity>,
        summary: MonthlySalarySummary
    ): File? {
        return try {
            val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val fileName = "Attendance_${profile.employeeId}_${monthPrefix}.csv"
            val file = File(cacheDir, fileName)

            val days = DateUtils.getDaysInMonth(monthPrefix)
            val attendanceMap = attendanceList.associateBy { it.date }
            val holidayMap = holidaysList.associateBy { it.date }

            val sb = StringBuilder()
            // Header Info
            sb.append("ATTENDANCE & OVERTIME MONTHLY REGISTER\n")
            sb.append("Company:,\"${profile.companyName}\"\n")
            sb.append("Employee ID:,\"${profile.employeeId}\",Name:,\"${profile.name}\",Department:,\"${profile.department}\"\n")
            sb.append("Month:,\"${DateUtils.formatMonthDisplay(monthPrefix)}\",Standard Shift:,\"${profile.defaultShiftHours} hrs\"\n\n")

            // Table Header
            sb.append("Date,Day,Status,In Time,Out Time,Break (min),Work Hours,OT Hours,OT Mult,Late (min),Early Exit (min),Notes\n")

            for (date in days) {
                val record = attendanceMap[date]
                val holiday = holidayMap[date]
                val isSunday = DateUtils.isSunday(date)
                val dayName = DateUtils.getDayOfWeekName(date)

                val status = record?.status ?: if (holiday != null) {
                    AttendanceStatus.FESTIVAL_HOLIDAY
                } else if (isSunday) {
                    AttendanceStatus.WEEKLY_OFF
                } else {
                    AttendanceStatus.ABSENT
                }

                val inTime = record?.inTime ?: if (status == AttendanceStatus.PRESENT) "09:00" else "--:--"
                val outTime = record?.outTime ?: if (status == AttendanceStatus.PRESENT) "18:00" else "--:--"
                val breakMin = record?.breakMinutes ?: 0
                val workHours = if (record != null) "%.2f".format(Locale.US, record.totalWorkMinutes / 60.0) else "0.00"
                val otHours = if (record != null) {
                    if (record.manualOtHoursOverride != null) "%.2f".format(Locale.US, record.manualOtHoursOverride)
                    else "%.2f".format(Locale.US, record.overtimeMinutes / 60.0)
                } else "0.00"
                val otMult = record?.otMultiplier ?: profile.defaultOtMultiplier
                val lateMin = record?.lateMinutes ?: 0
                val earlyMin = record?.earlyExitMinutes ?: 0
                val notes = record?.note ?: (holiday?.name ?: "")

                sb.append("\"$date\",\"$dayName\",\"${status.displayName}\",\"$inTime\",\"$outTime\",$breakMin,$workHours,$otHours,${otMult}x,$lateMin,$earlyMin,\"${notes.replace("\"", "\"\"")}\"\n")
            }

            // Summary Section
            sb.append("\nSUMMARY METRICS\n")
            sb.append("Total Days,${summary.totalCalendarDays},Present Days,${summary.presentDaysCount},Absent Days,${summary.absentDaysCount}\n")
            sb.append("Paid Leaves,${summary.paidLeaveDaysCount},Weekly Offs,${summary.weeklyOffDaysCount},Holidays,${summary.festivalHolidayDaysCount}\n")
            sb.append("Total OT Hours,${summary.totalOvertimeHours},Total OT Pay,${SalaryCalculator.formatCurrency(summary.totalOvertimePay, profile.currencySymbol)}\n")
            sb.append("Gross Salary,${SalaryCalculator.formatCurrency(summary.totalGrossSalary, profile.currencySymbol)},Total Deductions,${SalaryCalculator.formatCurrency(summary.totalDeductions, profile.currencySymbol)},Net Salary,${SalaryCalculator.formatCurrency(summary.netTakeHomeSalary, profile.currencySymbol)}\n")

            val fos = FileOutputStream(file)
            fos.write(sb.toString().toByteArray())
            fos.flush()
            fos.close()

            shareFile(context, file, "text/csv", "Share Attendance CSV Sheet")
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export CSV failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Exports full monthly attendance register formatted as an Excel-compatible XML Spreadsheet (.xls)
     */
    fun exportAttendanceExcel(
        context: Context,
        profile: UserProfileEntity,
        monthPrefix: String,
        attendanceList: List<AttendanceRecordEntity>,
        holidaysList: List<HolidayRecordEntity>,
        summary: MonthlySalarySummary
    ): File? {
        return try {
            val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val fileName = "Attendance_${profile.employeeId}_${monthPrefix}.xls"
            val file = File(cacheDir, fileName)

            val days = DateUtils.getDaysInMonth(monthPrefix)
            val attendanceMap = attendanceList.associateBy { it.date }
            val holidayMap = holidaysList.associateBy { it.date }

            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\"?>\n")
            sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n")
            sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
            sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
            sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")
            sb.append(" <Styles>\n")
            sb.append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\"><Font ss:FontName=\"Arial\" ss:Size=\"10\"/></Style>\n")
            sb.append("  <Style ss:ID=\"Title\"><Font ss:FontName=\"Arial\" ss:Size=\"14\" ss:Bold=\"1\" ss:Color=\"#0F172A\"/></Style>\n")
            sb.append("  <Style ss:ID=\"Header\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/><Interior ss:Color=\"#2563EB\" ss:Pattern=\"Solid\"/></Style>\n")
            sb.append("  <Style ss:ID=\"SummaryHeader\"><Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/><Interior ss:Color=\"#0F172A\" ss:Pattern=\"Solid\"/></Style>\n")
            sb.append("  <Style ss:ID=\"Bold\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\"/></Style>\n")
            sb.append("  <Style ss:ID=\"Present\"><Interior ss:Color=\"#DCFCE7\" ss:Pattern=\"Solid\"/><Font ss:Color=\"#15803D\" ss:Bold=\"1\"/></Style>\n")
            sb.append("  <Style ss:ID=\"Absent\"><Interior ss:Color=\"#FEE2E2\" ss:Pattern=\"Solid\"/><Font ss:Color=\"#B91C1C\" ss:Bold=\"1\"/></Style>\n")
            sb.append("  <Style ss:ID=\"Holiday\"><Interior ss:Color=\"#FCE7F3\" ss:Pattern=\"Solid\"/><Font ss:Color=\"#BE185D\" ss:Bold=\"1\"/></Style>\n")
            sb.append("  <Style ss:ID=\"WeeklyOff\"><Interior ss:Color=\"#E0F2FE\" ss:Pattern=\"Solid\"/><Font ss:Color=\"#0369A1\" ss:Bold=\"1\"/></Style>\n")
            sb.append(" </Styles>\n")
            sb.append(" <Worksheet ss:Name=\"Attendance Register\">\n")
            sb.append("  <Table>\n")

            // Title
            sb.append("   <Row><Cell ss:StyleID=\"Title\"><Data ss:Type=\"String\">${profile.companyName}</Data></Cell></Row>\n")
            sb.append("   <Row><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Monthly Attendance &amp; Overtime Sheet - ${DateUtils.formatMonthDisplay(monthPrefix)}</Data></Cell></Row>\n")
            sb.append("   <Row><Cell><Data ss:Type=\"String\">Employee: ${profile.name} (${profile.employeeId}) | Dept: ${profile.department}</Data></Cell></Row>\n")
            sb.append("   <Row></Row>\n")

            // Table Header
            sb.append("   <Row>\n")
            val headers = listOf("Date", "Day", "Status", "In Time", "Out Time", "Break (min)", "Work Hours", "OT Hours", "OT Mult", "Late (min)", "Early Exit (min)", "Notes")
            for (h in headers) {
                sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">$h</Data></Cell>\n")
            }
            sb.append("   </Row>\n")

            for (date in days) {
                val record = attendanceMap[date]
                val holiday = holidayMap[date]
                val isSunday = DateUtils.isSunday(date)
                val dayName = DateUtils.getDayOfWeekName(date)

                val status = record?.status ?: if (holiday != null) {
                    AttendanceStatus.FESTIVAL_HOLIDAY
                } else if (isSunday) {
                    AttendanceStatus.WEEKLY_OFF
                } else {
                    AttendanceStatus.ABSENT
                }

                val statusStyle = when (status) {
                    AttendanceStatus.PRESENT, AttendanceStatus.ON_DUTY -> "Present"
                    AttendanceStatus.ABSENT -> "Absent"
                    AttendanceStatus.WEEKLY_OFF -> "WeeklyOff"
                    AttendanceStatus.FESTIVAL_HOLIDAY -> "Holiday"
                    else -> "Default"
                }

                val inTime = record?.inTime ?: if (status == AttendanceStatus.PRESENT) "09:00" else "--:--"
                val outTime = record?.outTime ?: if (status == AttendanceStatus.PRESENT) "18:00" else "--:--"
                val breakMin = record?.breakMinutes ?: 0
                val workHours = if (record != null) record.totalWorkMinutes / 60.0 else 0.0
                val otHours = if (record != null) {
                    record.manualOtHoursOverride ?: (record.overtimeMinutes / 60.0)
                } else 0.0
                val otMult = record?.otMultiplier ?: profile.defaultOtMultiplier
                val lateMin = record?.lateMinutes ?: 0
                val earlyMin = record?.earlyExitMinutes ?: 0
                val notes = (record?.note ?: "") + if (holiday != null) " (${holiday.name})" else ""

                sb.append("   <Row>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$date</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$dayName</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"$statusStyle\"><Data ss:Type=\"String\">${status.displayName}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$inTime</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$outTime</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">$breakMin</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">$workHours</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">$otHours</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${otMult}x</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">$lateMin</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">$earlyMin</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${notes.replace("&", "&amp;").replace("<", "&lt;")}</Data></Cell>\n")
                sb.append("   </Row>\n")
            }

            // Summary Rows
            sb.append("   <Row></Row>\n")
            sb.append("   <Row><Cell ss:StyleID=\"SummaryHeader\" ss:MergeAcross=\"3\"><Data ss:Type=\"String\">PAYROLL &amp; ATTENDANCE MONTHLY SUMMARY</Data></Cell></Row>\n")
            sb.append("   <Row><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Total Calendar Days</Data></Cell><Cell><Data ss:Type=\"Number\">${summary.totalCalendarDays}</Data></Cell><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Total OT Hours</Data></Cell><Cell><Data ss:Type=\"Number\">${summary.totalOvertimeHours}</Data></Cell></Row>\n")
            sb.append("   <Row><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Present Days</Data></Cell><Cell><Data ss:Type=\"Number\">${summary.presentDaysCount}</Data></Cell><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Total OT Pay</Data></Cell><Cell><Data ss:Type=\"Number\">${summary.totalOvertimePay}</Data></Cell></Row>\n")
            sb.append("   <Row><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Absent Days</Data></Cell><Cell><Data ss:Type=\"Number\">${summary.absentDaysCount}</Data></Cell><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Gross Salary</Data></Cell><Cell><Data ss:Type=\"Number\">${summary.totalGrossSalary}</Data></Cell></Row>\n")
            sb.append("   <Row><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Paid Leaves &amp; Holidays</Data></Cell><Cell><Data ss:Type=\"Number\">${summary.paidLeaveDaysCount + summary.festivalHolidayDaysCount + summary.weeklyOffDaysCount}</Data></Cell><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Total Deductions</Data></Cell><Cell><Data ss:Type=\"Number\">${summary.totalDeductions}</Data></Cell></Row>\n")
            sb.append("   <Row><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Attendance Rate</Data></Cell><Cell><Data ss:Type=\"String\">${summary.attendancePercentage}%</Data></Cell><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Net Take Home Salary</Data></Cell><Cell ss:StyleID=\"Bold\"><Data ss:Type=\"Number\">${summary.netTakeHomeSalary}</Data></Cell></Row>\n")

            sb.append("  </Table>\n")
            sb.append(" </Worksheet>\n")
            sb.append("</Workbook>\n")

            val fos = FileOutputStream(file)
            fos.write(sb.toString().toByteArray())
            fos.flush()
            fos.close()

            shareFile(context, file, "application/vnd.ms-excel", "Share Excel Attendance Sheet")
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export Excel failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Exports entire Database as portable JSON Backup file
     */
    fun exportDatabaseJson(
        context: Context,
        profile: UserProfileEntity?,
        attendance: List<AttendanceRecordEntity>,
        shifts: List<ShiftConfigEntity>,
        holidays: List<HolidayRecordEntity>,
        adjustments: List<SalaryAdjustmentEntity>
    ): File? {
        return try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())
            root.put("appName", "Attendance & Overtime Calculator")

            // Profile
            profile?.let { p ->
                val pObj = JSONObject().apply {
                    put("employeeId", p.employeeId)
                    put("name", p.name)
                    put("designation", p.designation)
                    put("department", p.department)
                    put("companyName", p.companyName)
                    put("currencySymbol", p.currencySymbol)
                    put("basicPay", p.basicPay)
                    put("hra", p.hra)
                    put("conveyanceAllowance", p.conveyanceAllowance)
                    put("specialAllowance", p.specialAllowance)
                    put("otherAllowances", p.otherAllowances)
                    put("pfPercentage", p.pfPercentage)
                    put("isPfActive", p.isPfActive)
                    put("esiPercentage", p.esiPercentage)
                    put("isEsiActive", p.isEsiActive)
                    put("professionalTax", p.professionalTax)
                    put("defaultShiftHours", p.defaultShiftHours)
                    put("defaultOtMultiplier", p.defaultOtMultiplier)
                    put("sundayOtMultiplier", p.sundayOtMultiplier)
                    put("workingDaysMode", p.workingDaysMode.name)
                    put("otBaseMode", p.otBaseMode.name)
                    put("otRounding", p.otRounding.name)
                }
                root.put("userProfile", pObj)
            }

            // Attendance
            val attArray = JSONArray()
            for (att in attendance) {
                attArray.put(JSONObject().apply {
                    put("date", att.date)
                    put("status", att.status.name)
                    put("shiftId", att.shiftId)
                    put("inTime", att.inTime)
                    put("outTime", att.outTime)
                    put("breakMinutes", att.breakMinutes)
                    put("totalWorkMinutes", att.totalWorkMinutes)
                    put("regularMinutes", att.regularMinutes)
                    put("overtimeMinutes", att.overtimeMinutes)
                    put("otMultiplier", att.otMultiplier)
                    put("isLateArrival", att.isLateArrival)
                    put("lateMinutes", att.lateMinutes)
                    put("isEarlyExit", att.isEarlyExit)
                    put("earlyExitMinutes", att.earlyExitMinutes)
                    put("note", att.note)
                    if (att.manualOtHoursOverride != null) put("manualOtHoursOverride", att.manualOtHoursOverride)
                })
            }
            root.put("attendance", attArray)

            // Shifts
            val shiftsArray = JSONArray()
            for (s in shifts) {
                shiftsArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("code", s.code)
                    put("startTime", s.startTime)
                    put("endTime", s.endTime)
                    put("durationHours", s.durationHours)
                    put("breakMinutes", s.breakMinutes)
                    put("isNightShift", s.isNightShift)
                    put("colorHex", s.colorHex)
                })
            }
            root.put("shifts", shiftsArray)

            // Holidays
            val holArray = JSONArray()
            for (h in holidays) {
                holArray.put(JSONObject().apply {
                    put("date", h.date)
                    put("name", h.name)
                    put("type", h.type.name)
                    put("isPaid", h.isPaid)
                    put("colorHex", h.colorHex)
                })
            }
            root.put("holidays", holArray)

            // Adjustments
            val adjArray = JSONArray()
            for (a in adjustments) {
                adjArray.put(JSONObject().apply {
                    put("monthYear", a.monthYear)
                    put("title", a.title)
                    put("type", a.type.name)
                    put("amount", a.amount)
                    put("date", a.date)
                    put("notes", a.notes)
                })
            }
            root.put("adjustments", adjArray)

            val cacheDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(cacheDir, "AttendanceBackup_$timeStr.json")

            val fos = FileOutputStream(file)
            fos.write(root.toString(2).toByteArray())
            fos.flush()
            fos.close()

            shareFile(context, file, "application/json", "Share Database Backup File")
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Backup failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Parses JSON string and restores all data models
     */
    fun parseDatabaseJson(jsonString: String): BackupDataHolder {
        val root = JSONObject(jsonString)
        var profile: UserProfileEntity? = null
        val attendance = mutableListOf<AttendanceRecordEntity>()
        val shifts = mutableListOf<ShiftConfigEntity>()
        val holidays = mutableListOf<HolidayRecordEntity>()
        val adjustments = mutableListOf<SalaryAdjustmentEntity>()

        if (root.has("userProfile")) {
            val p = root.getJSONObject("userProfile")
            profile = UserProfileEntity(
                employeeId = p.optString("employeeId", "EMP-1001"),
                name = p.optString("name", "Worker"),
                designation = p.optString("designation", "Operator"),
                department = p.optString("department", "Production"),
                companyName = p.optString("companyName", "Company"),
                currencySymbol = p.optString("currencySymbol", "₹"),
                basicPay = p.optDouble("basicPay", 20000.0),
                hra = p.optDouble("hra", 4000.0),
                conveyanceAllowance = p.optDouble("conveyanceAllowance", 1500.0),
                specialAllowance = p.optDouble("specialAllowance", 2000.0),
                otherAllowances = p.optDouble("otherAllowances", 500.0),
                pfPercentage = p.optDouble("pfPercentage", 12.0),
                isPfActive = p.optBoolean("isPfActive", true),
                esiPercentage = p.optDouble("esiPercentage", 0.75),
                isEsiActive = p.optBoolean("isEsiActive", true),
                professionalTax = p.optDouble("professionalTax", 200.0),
                defaultShiftHours = p.optDouble("defaultShiftHours", 8.0),
                defaultOtMultiplier = p.optDouble("defaultOtMultiplier", 1.5),
                sundayOtMultiplier = p.optDouble("sundayOtMultiplier", 2.0)
            )
        }

        if (root.has("attendance")) {
            val arr = root.getJSONArray("attendance")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                attendance.add(
                    AttendanceRecordEntity(
                        date = o.getString("date"),
                        status = runCatching { AttendanceStatus.valueOf(o.getString("status")) }.getOrDefault(AttendanceStatus.PRESENT),
                        shiftId = o.optLong("shiftId", 1L),
                        inTime = o.optString("inTime", "09:00"),
                        outTime = o.optString("outTime", "18:00"),
                        breakMinutes = o.optInt("breakMinutes", 60),
                        totalWorkMinutes = o.optInt("totalWorkMinutes", 480),
                        regularMinutes = o.optInt("regularMinutes", 480),
                        overtimeMinutes = o.optInt("overtimeMinutes", 0),
                        otMultiplier = o.optDouble("otMultiplier", 1.5),
                        isLateArrival = o.optBoolean("isLateArrival", false),
                        lateMinutes = o.optInt("lateMinutes", 0),
                        isEarlyExit = o.optBoolean("isEarlyExit", false),
                        earlyExitMinutes = o.optInt("earlyExitMinutes", 0),
                        note = o.optString("note", ""),
                        manualOtHoursOverride = if (o.has("manualOtHoursOverride")) o.getDouble("manualOtHoursOverride") else null
                    )
                )
            }
        }

        if (root.has("shifts")) {
            val arr = root.getJSONArray("shifts")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                shifts.add(
                    ShiftConfigEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        code = o.optString("code", "S"),
                        startTime = o.getString("startTime"),
                        endTime = o.getString("endTime"),
                        durationHours = o.optDouble("durationHours", 8.0),
                        breakMinutes = o.optInt("breakMinutes", 30),
                        isNightShift = o.optBoolean("isNightShift", false),
                        colorHex = o.optString("colorHex", "#2563EB")
                    )
                )
            }
        }

        if (root.has("holidays")) {
            val arr = root.getJSONArray("holidays")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                holidays.add(
                    HolidayRecordEntity(
                        date = o.getString("date"),
                        name = o.getString("name"),
                        type = runCatching { com.example.data.model.HolidayType.valueOf(o.getString("type")) }.getOrDefault(com.example.data.model.HolidayType.FESTIVAL),
                        isPaid = o.optBoolean("isPaid", true),
                        colorHex = o.optString("colorHex", "#DB2777")
                    )
                )
            }
        }

        if (root.has("adjustments")) {
            val arr = root.getJSONArray("adjustments")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                adjustments.add(
                    SalaryAdjustmentEntity(
                        monthYear = o.getString("monthYear"),
                        title = o.getString("title"),
                        type = runCatching { com.example.data.model.AdjustmentType.valueOf(o.getString("type")) }.getOrDefault(com.example.data.model.AdjustmentType.BONUS),
                        amount = o.getDouble("amount"),
                        date = o.getString("date"),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        return BackupDataHolder(profile, attendance, shifts, holidays, adjustments)
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }
}

data class BackupDataHolder(
    val profile: UserProfileEntity?,
    val attendance: List<AttendanceRecordEntity>,
    val shifts: List<ShiftConfigEntity>,
    val holidays: List<HolidayRecordEntity>,
    val adjustments: List<SalaryAdjustmentEntity>
)
