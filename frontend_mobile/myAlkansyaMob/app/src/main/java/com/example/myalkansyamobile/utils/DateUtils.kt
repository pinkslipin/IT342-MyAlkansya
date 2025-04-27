package com.example.myalkansyamobile.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for handling date formatting and operations
 */
object DateUtils {
    /**
     * Format a date string to a more readable format
     * @param dateStr Date in format yyyy-MM-dd
     * @return Formatted date string (e.g., "Jan 01, 2023")
     */
    fun formatDate(dateStr: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr) ?: return dateStr
            return outputFormat.format(date)
        } catch (e: Exception) {
            return dateStr
        }
    }

    /**
     * Get today's date in format yyyy-MM-dd
     */
    fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    /**
     * Parse a date string into a Calendar object
     */
    fun parseDate(dateStr: String): Calendar? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = format.parse(dateStr)
            if (date != null) {
                Calendar.getInstance().apply {
                    time = date
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
