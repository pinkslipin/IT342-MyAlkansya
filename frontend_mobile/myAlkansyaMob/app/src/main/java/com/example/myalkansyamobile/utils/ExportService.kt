package com.example.myalkansyamobile.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.myalkansyamobile.BudgetResponse
import com.example.myalkansyamobile.api.ExpenseResponse
import com.example.myalkansyamobile.api.FinancialSummaryResponse
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.api.SavingsGoalResponse
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service class to handle exporting data to CSV files
 */
class ExportService(private val context: Context) {

    /**
     * Exports financial data to CSV files and shares them
     * Uses generic Any type to accommodate different response types
     */
    suspend fun exportAndShareFinancialData(
        incomes: Any? = null,
        expenses: Any? = null,
        budgets: Any? = null,
        savingsGoals: Any? = null,
        financialSummary: FinancialSummaryResponse? = null
    ) {
        val uri = exportToCSV(incomes, expenses, budgets, savingsGoals, financialSummary)
        
        if (uri != null) {
            shareCSVFile(uri)
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to generate export file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Exports financial data to CSV file
     * @return URI of the created file or null if export failed
     */
    private suspend fun exportToCSV(
        incomes: Any? = null,
        expenses: Any? = null,
        budgets: Any? = null,
        savingsGoals: Any? = null,
        financialSummary: FinancialSummaryResponse? = null
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // Create file name with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "MyAlkansya_Export_$timestamp.csv"
            
            // Get the Downloads directory
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            // Create CSV writer
            val writer = csvWriter()
            
            // Get currency formatter
            val currency = financialSummary?.currency ?: "USD"
            val currencyFormat = NumberFormat.getCurrencyInstance()
            try {
                currencyFormat.currency = Currency.getInstance(currency)
            } catch (e: Exception) {
                currencyFormat.currency = Currency.getInstance("USD")
            }
            
            // Generate CSV content
            writer.open(file, append = false) {
                // Write header
                writeRow("MyAlkansya Financial Export - $timestamp")
                writeRow(listOf()) // empty row for spacing
                
                // Financial Summary Section
                writeRow("FINANCIAL SUMMARY")
                writeRow(listOf("Category", "Amount"))
                
                // Use actual financial summary data if available
                if (financialSummary != null) {
                    writeRow(listOf("Total Income", currencyFormat.format(financialSummary.totalIncome)))
                    writeRow(listOf("Total Expenses", currencyFormat.format(financialSummary.totalExpenses)))
                    writeRow(listOf("Total Budget", currencyFormat.format(financialSummary.totalBudget)))
                    writeRow(listOf("Total Savings", currencyFormat.format(financialSummary.totalSavings)))
                    writeRow(listOf("Net Cashflow", currencyFormat.format(financialSummary.netCashflow)))
                    writeRow(listOf("Budget Utilization", "${(financialSummary.budgetUtilization * 100).toInt()}%"))
                    writeRow(listOf("Savings Rate", "${(financialSummary.savingsRate * 100).toInt()}%"))
                } else {
                    writeRow(listOf("Total Income", "0.00"))
                    writeRow(listOf("Total Expenses", "0.00"))
                    writeRow(listOf("Total Budget", "0.00"))
                    writeRow(listOf("Total Savings", "0.00"))
                }
                writeRow(listOf()) // empty row for spacing
                
                // Income Section - Handle different potential types with safe casting
                if (incomes != null) {
                    writeRow("INCOME RECORDS")
                    writeRow(listOf("ID", "Source", "Amount", "Date", "Currency"))
                    
                    when (incomes) {
                        is List<*> -> {
                            for (income in incomes) {
                                when (income) {
                                    is Map<*, *> -> {
                                        val id = income["id"]?.toString() ?: ""
                                        val source = income["source"]?.toString() ?: ""
                                        val amount = income["amount"]?.toString() ?: "0.00"
                                        val date = income["date"]?.toString() ?: ""
                                        val incomeCurrency = income["currency"]?.toString() ?: currency
                                        writeRow(listOf(id, source, amount, date, incomeCurrency))
                                    }
                                    is Income -> {
                                        writeRow(listOf(
                                            income.id.toString(),
                                            income.source,
                                            income.amount.toString(),
                                            income.date.toString(),
                                            income.currency
                                        ))
                                    }
                                    else -> {
                                        // Try to extract fields by reflection
                                        val id = getFieldValue(income, "id") ?: ""
                                        val source = getFieldValue(income, "source") ?: ""
                                        val amount = getFieldValue(income, "amount") ?: "0.00"
                                        val date = getFieldValue(income, "date") ?: ""
                                        val incomeCurrency = getFieldValue(income, "currency") ?: currency
                                        writeRow(listOf(id, source, amount, date, incomeCurrency))
                                    }
                                }
                            }
                        }
                        else -> {
                            // Handle single item or other types
                            writeRow(listOf("N/A", "Unable to process", "0.00", "N/A", currency))
                        }
                    }
                    writeRow(listOf()) // empty row for spacing
                }
                
                // Expense Section - Handle different potential types with safe casting
                if (expenses != null) {
                    writeRow("EXPENSE RECORDS")
                    writeRow(listOf("ID", "Subject", "Category", "Amount", "Date", "Currency"))
                    
                    when (expenses) {
                        is List<*> -> {
                            for (expense in expenses) {
                                when (expense) {
                                    is Map<*, *> -> {
                                        val id = expense["id"]?.toString() ?: ""
                                        val subject = expense["subject"]?.toString() ?: ""
                                        val category = expense["category"]?.toString() ?: ""
                                        val amount = expense["amount"]?.toString() ?: "0.00"
                                        val date = expense["date"]?.toString() ?: ""
                                        val expenseCurrency = expense["currency"]?.toString() ?: currency
                                        writeRow(listOf(id, subject, category, amount, date, expenseCurrency))
                                    }
                                    is ExpenseResponse -> {
                                        writeRow(listOf(
                                            expense.id.toString(),
                                            expense.subject,
                                            expense.category,
                                            expense.amount.toString(),
                                            expense.date,
                                            expense.currency ?: currency
                                        ))
                                    }
                                    else -> {
                                        // Try to extract fields by reflection
                                        val id = getFieldValue(expense, "id") ?: ""
                                        val subject = getFieldValue(expense, "subject") ?: ""
                                        val category = getFieldValue(expense, "category") ?: ""
                                        val amount = getFieldValue(expense, "amount") ?: "0.00"
                                        val date = getFieldValue(expense, "date") ?: ""
                                        val expenseCurrency = getFieldValue(expense, "currency") ?: currency
                                        writeRow(listOf(id, subject, category, amount, date, expenseCurrency))
                                    }
                                }
                            }
                        }
                        else -> {
                            // Handle single item or other types
                            writeRow(listOf("N/A", "Unable to process", "N/A", "0.00", "N/A", currency))
                        }
                    }
                    writeRow(listOf()) // empty row for spacing
                }
                
                // Budget Section - Handle different potential types with safe casting
                if (budgets != null) {
                    writeRow("BUDGET RECORDS")
                    writeRow(listOf("ID", "Category", "Monthly Budget", "Total Spent", "Month", "Year"))
                    
                    when (budgets) {
                        is List<*> -> {
                            for (budget in budgets) {
                                when (budget) {
                                    is Map<*, *> -> {
                                        val id = budget["id"]?.toString() ?: ""
                                        val category = budget["category"]?.toString() ?: ""
                                        val monthlyBudget = budget["monthlyBudget"]?.toString() ?: "0.00"
                                        val totalSpent = budget["totalSpent"]?.toString() ?: "0.00"
                                        val month = budget["budgetMonth"]?.toString() ?: ""
                                        val year = budget["budgetYear"]?.toString() ?: ""
                                        writeRow(listOf(id, category, monthlyBudget, totalSpent, month, year))
                                    }
                                    is BudgetResponse -> {
                                        writeRow(listOf(
                                            budget.id.toString(),
                                            budget.category,
                                            budget.monthlyBudget.toString(),
                                            budget.totalSpent.toString(),
                                            budget.budgetMonth.toString(),
                                            budget.budgetYear.toString()
                                        ))
                                    }
                                    else -> {
                                        // Try to extract fields by reflection
                                        val id = getFieldValue(budget, "id") ?: ""
                                        val category = getFieldValue(budget, "category") ?: ""
                                        val monthlyBudget = getFieldValue(budget, "monthlyBudget") ?: "0.00"
                                        val totalSpent = getFieldValue(budget, "totalSpent") ?: "0.00"
                                        val month = getFieldValue(budget, "budgetMonth") ?: ""
                                        val year = getFieldValue(budget, "budgetYear") ?: ""
                                        writeRow(listOf(id, category, monthlyBudget, totalSpent, month, year))
                                    }
                                }
                            }
                        }
                        else -> {
                            // Handle single item or other types
                            writeRow(listOf("N/A", "Unable to process", "0.00", "0.00", "N/A", "N/A"))
                        }
                    }
                    writeRow(listOf()) // empty row for spacing
                }
                
                // Savings Goals Section - Handle different potential types with safe casting
                if (savingsGoals != null) {
                    writeRow("SAVINGS GOALS")
                    writeRow(listOf("ID", "Goal", "Target Amount", "Current Amount", "Target Date", "Progress (%)"))
                    
                    when (savingsGoals) {
                        is List<*> -> {
                            for (goal in savingsGoals) {
                                when (goal) {
                                    is Map<*, *> -> {
                                        val id = goal["id"]?.toString() ?: ""
                                        val name = goal["goal"]?.toString() ?: ""
                                        val targetAmount = goal["targetAmount"]?.toString() ?: "0.00"
                                        val currentAmount = goal["currentAmount"]?.toString() ?: "0.00"
                                        val targetDate = goal["targetDate"]?.toString() ?: ""
                                        val progress = ((currentAmount.toDoubleOrNull() ?: 0.0) / 
                                                      (targetAmount.toDoubleOrNull() ?: 1.0) * 100).toInt().toString() + "%"
                                        writeRow(listOf(id, name, targetAmount, currentAmount, targetDate, progress))
                                    }
                                    is SavingsGoalResponse -> {
                                        val progress = ((goal.currentAmount / goal.targetAmount) * 100).toInt().toString() + "%"
                                        writeRow(listOf(
                                            goal.id.toString(),
                                            goal.goal,
                                            goal.targetAmount.toString(),
                                            goal.currentAmount.toString(),
                                            goal.targetDate,
                                            progress
                                        ))
                                    }
                                    else -> {
                                        // Try to extract fields by reflection
                                        val id = getFieldValue(goal, "id") ?: ""
                                        val name = getFieldValue(goal, "goal") ?: ""
                                        val targetAmount = getFieldValue(goal, "targetAmount") ?: "0.00"
                                        val currentAmount = getFieldValue(goal, "currentAmount") ?: "0.00"
                                        val targetDate = getFieldValue(goal, "targetDate") ?: ""
                                        val progress = getFieldValue(goal, "progress") ?: "0%"
                                        writeRow(listOf(id, name, targetAmount, currentAmount, targetDate, progress))
                                    }
                                }
                            }
                        }
                        else -> {
                            // Handle single item or other types
                            writeRow(listOf("N/A", "Unable to process", "0.00", "0.00", "N/A", "0%"))
                        }
                    }
                    writeRow(listOf()) // empty row for spacing
                }
                
                // Footer with export info
                writeRow(listOf("Generated by MyAlkansya App on", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())))
            }
            
            // Create and return content URI using FileProvider
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            null
        }
    }
    
    /**
     * Share the exported CSV file
     */
    private fun shareCSVFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "MyAlkansya Financial Data Export")
            putExtra(Intent.EXTRA_TEXT, "Attached is your exported financial data from MyAlkansya.")
        }
        
        val chooser = Intent.createChooser(intent, "Share MyAlkansya Export")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
    
    /**
     * Helper method to extract field values using reflection
     */
    private fun getFieldValue(obj: Any?, fieldName: String): String? {
        if (obj == null) return null
        return try {
            val field = obj.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(obj)?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
