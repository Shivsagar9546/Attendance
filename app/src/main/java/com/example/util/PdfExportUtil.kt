package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.UserProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExportUtil {

    fun generateAndShareSalarySlipPdf(
        context: Context,
        profile: UserProfileEntity,
        summary: MonthlySalarySummary
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width at 72dpi
            val pageHeight = 842 // Standard A4 height at 72dpi
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawSalarySlip(canvas, profile, summary, pageWidth, pageHeight)

            pdfDocument.finishPage(page)

            // Save to cache directory
            val cacheDir = File(context.cacheDir, "payslips").apply { mkdirs() }
            val fileName = "Payslip_${profile.employeeId}_${summary.monthPrefix}.pdf"
            val file = File(cacheDir, fileName)

            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Open share chooser
            sharePdfFile(context, file, "Salary Slip - ${DateUtils.formatMonthDisplay(summary.monthPrefix)}")
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generating PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun drawSalarySlip(
        canvas: Canvas,
        profile: UserProfileEntity,
        summary: MonthlySalarySummary,
        width: Int,
        height: Int
    ) {
        val margin = 36f
        val contentWidth = width - (margin * 2)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 12f
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 11f
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235) // Electric Blue
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val boxBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249) // Slate 100
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val accentBoxPaint = Paint().apply {
            color = Color.rgb(220, 252, 231) // Green 100
            style = Paint.Style.FILL
        }

        var y = 50f

        // 1. Company Header Box
        canvas.drawRect(margin, y - 10, margin + contentWidth, y + 65, boxBgPaint)
        canvas.drawRect(margin, y - 10, margin + contentWidth, y + 65, borderPaint)

        canvas.drawText(profile.companyName.ifBlank { "Apex Precision Engineering Ltd." }, margin + 16, y + 16, titlePaint)
        canvas.drawText("SALARY & OVERTIME REGISTER PAYSLIP", margin + 16, y + 36, headerPaint)
        canvas.drawText("Pay Period: ${DateUtils.formatMonthDisplay(summary.monthPrefix)}", margin + 16, y + 54, subTitlePaint)

        y += 90f

        // 2. Employee Details Grid
        canvas.drawRect(margin, y, margin + contentWidth, y + 70, borderPaint)
        val col1 = margin + 12
        val col2 = margin + (contentWidth / 2) + 12

        canvas.drawText("Employee ID: ${profile.employeeId}", col1, y + 20, textPaint)
        canvas.drawText("Employee Name: ${profile.name}", col1, y + 40, textPaint)
        canvas.drawText("Department: ${profile.department}", col1, y + 60, textPaint)

        canvas.drawText("Designation: ${profile.designation}", col2, y + 20, textPaint)
        canvas.drawText("Standard Shift: ${profile.defaultShiftHours} Hours", col2, y + 40, textPaint)
        canvas.drawText("Payment Currency: ${profile.currencySymbol}", col2, y + 60, textPaint)

        y += 85f

        // 3. Attendance & OT Metrics Summary
        canvas.drawRect(margin, y, margin + contentWidth, y + 50, boxBgPaint)
        canvas.drawRect(margin, y, margin + contentWidth, y + 50, borderPaint)

        val cellW = contentWidth / 5f
        val labels = listOf("Total Days", "Present Days", "Absent", "Paid Leaves", "Total OT Hours")
        val values = listOf(
            "${summary.totalCalendarDays}",
            "${summary.presentDaysCount}",
            "${summary.absentDaysCount}",
            "${summary.paidLeaveDaysCount + summary.weeklyOffDaysCount + summary.festivalHolidayDaysCount}",
            "%.1f hrs".format(Locale.US, summary.totalOvertimeHours)
        )

        for (i in 0 until 5) {
            val cellX = margin + (i * cellW) + 8
            canvas.drawText(labels[i], cellX, y + 18, tableHeaderPaint)
            val valPaint = Paint(tableHeaderPaint).apply { color = Color.rgb(15, 23, 42); textSize = 12f }
            canvas.drawText(values[i], cellX, y + 38, valPaint)
        }

        y += 65f

        // 4. Earnings & Deductions Tables
        val halfW = (contentWidth - 10) / 2f
        val earnX = margin
        val dedX = margin + halfW + 10

        // Headers
        canvas.drawRect(earnX, y, earnX + halfW, y + 24, boxBgPaint)
        canvas.drawRect(earnX, y, earnX + halfW, y + 24, borderPaint)
        canvas.drawText("EARNINGS", earnX + 10, y + 16, headerPaint)

        canvas.drawRect(dedX, y, dedX + halfW, y + 24, boxBgPaint)
        canvas.drawRect(dedX, y, dedX + halfW, y + 24, borderPaint)
        val redHeader = Paint(headerPaint).apply { color = Color.rgb(220, 38, 38) }
        canvas.drawText("DEDUCTIONS", dedX + 10, y + 16, redHeader)

        y += 24f
        val startTableY = y

        val earningsList = listOf(
            "Basic Salary (Earned)" to summary.earnedBasicPay,
            "HRA Allowance" to summary.earnedHra,
            "Conveyance Allowance" to summary.earnedConveyance,
            "Special Allowance" to summary.earnedSpecialAllowance,
            "Other Allowances" to summary.earnedOtherAllowances,
            "Overtime Pay (${summary.totalOvertimeHours}h)" to summary.totalOvertimePay,
            "Bonuses & Incentives" to summary.totalBonusesAndIncentives
        )

        val deductionsList = listOf(
            "Provident Fund (PF)" to summary.pfDeduction,
            "ESI Insurance" to summary.esiDeduction,
            "Professional Tax (PT)" to summary.professionalTax,
            "Salary Advance Repayment" to summary.advanceDeductions,
            "Penalties / Late Fines" to summary.penaltyDeductions
        )

        val rowHeight = 22f
        val maxRows = maxOf(earningsList.size, deductionsList.size)

        for (i in 0 until maxRows) {
            val rowY = startTableY + (i * rowHeight)

            // Earnings
            if (i < earningsList.size) {
                val (name, amt) = earningsList[i]
                canvas.drawText(name, earnX + 8, rowY + 16, textPaint)
                val amtStr = SalaryCalculator.formatCurrency(amt, profile.currencySymbol)
                canvas.drawText(amtStr, earnX + halfW - textPaint.measureText(amtStr) - 8, rowY + 16, textPaint)
            }

            // Deductions
            if (i < deductionsList.size) {
                val (name, amt) = deductionsList[i]
                canvas.drawText(name, dedX + 8, rowY + 16, textPaint)
                val amtStr = SalaryCalculator.formatCurrency(amt, profile.currencySymbol)
                canvas.drawText(amtStr, dedX + halfW - textPaint.measureText(amtStr) - 8, rowY + 16, textPaint)
            }
        }

        val tableBottom = startTableY + (maxRows * rowHeight)
        canvas.drawRect(earnX, startTableY, earnX + halfW, tableBottom, borderPaint)
        canvas.drawRect(dedX, startTableY, dedX + halfW, tableBottom, borderPaint)

        y = tableBottom

        // Total Earnings & Total Deductions Row
        canvas.drawRect(earnX, y, earnX + halfW, y + 24, boxBgPaint)
        canvas.drawRect(earnX, y, earnX + halfW, y + 24, borderPaint)
        canvas.drawText("Gross Earnings:", earnX + 8, y + 16, tableHeaderPaint)
        val grossStr = SalaryCalculator.formatCurrency(summary.totalGrossSalary, profile.currencySymbol)
        canvas.drawText(grossStr, earnX + halfW - tableHeaderPaint.measureText(grossStr) - 8, y + 16, tableHeaderPaint)

        canvas.drawRect(dedX, y, dedX + halfW, y + 24, boxBgPaint)
        canvas.drawRect(dedX, y, dedX + halfW, y + 24, borderPaint)
        canvas.drawText("Total Deductions:", dedX + 8, y + 16, tableHeaderPaint)
        val dedStr = SalaryCalculator.formatCurrency(summary.totalDeductions, profile.currencySymbol)
        canvas.drawText(dedStr, dedX + halfW - tableHeaderPaint.measureText(dedStr) - 8, y + 16, tableHeaderPaint)

        y += 40f

        // 5. Net Salary Take Home Highlight Box
        canvas.drawRect(margin, y, margin + contentWidth, y + 45, accentBoxPaint)
        canvas.drawRect(margin, y, margin + contentWidth, y + 45, borderPaint)

        val netLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 128, 61) // Green 700
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val netAmtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 128, 61)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("NET TAKE-HOME SALARY PAYABLE:", margin + 16, y + 28, netLabelPaint)
        val netStr = SalaryCalculator.formatCurrency(summary.netTakeHomeSalary, profile.currencySymbol)
        canvas.drawText(netStr, margin + contentWidth - netAmtPaint.measureText(netStr) - 16, y + 30, netAmtPaint)

        y += 75f

        // 6. Signatures
        val sigLinePaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            strokeWidth = 1f
        }

        val sigCol1 = margin + 20
        val sigCol2 = margin + contentWidth - 180

        canvas.drawLine(sigCol1, y + 30, sigCol1 + 160, y + 30, sigLinePaint)
        canvas.drawText("Employee Signature", sigCol1 + 20, y + 46, subTitlePaint)

        canvas.drawLine(sigCol2, y + 30, sigCol2 + 160, y + 30, sigLinePaint)
        canvas.drawText("Authorized Signatory / HR", sigCol2 + 10, y + 46, subTitlePaint)

        // Footer Note
        val genDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        canvas.drawText("Generated on $genDate • Confidential Workforce Record • 100% Offline System", margin + 10, height - 30f, subTitlePaint)
    }

    private fun sharePdfFile(context: Context, file: File, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Payslip via"))
    }
}
