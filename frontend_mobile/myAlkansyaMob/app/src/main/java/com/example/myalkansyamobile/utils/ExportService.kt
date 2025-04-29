package com.example.myalkansyamobile.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
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
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service class to handle exporting financial data
 * Primary export is now to Google Sheets with CSV as fallback
 */
class ExportService(private val context: Context) {
    private val sheetsHelper = GoogleSheetsHelper(context)
    private val TAG = "ExportService"
    private val sessionManager = SessionManager(context)

    // Request code for Google Sign-in
    companion object {
        const val RC_SIGN_IN = 9001
    }

    /**
     * Exports financial data to Google Sheets if possible, otherwise falls back to CSV
     * Accepts Any? type parameters to handle various response types
     */
    suspend fun exportAndShareFinancialData(
        incomes: Any? = null,
        expenses: Any? = null,
        budgets: Any? = null,
        savingsGoals: Any? = null,
        financialSummary: FinancialSummaryResponse? = null
    ) {
        // Check if user is signed in with Google
        if (sheetsHelper.isUserSignedIn()) {
            // Try exporting to Google Sheets
            val sheetsUrl = exportToGoogleSheets(
                incomes = incomes,
                expenses = expenses,
                budgets = budgets,
                savingsGoals = savingsGoals,
                financialSummary = financialSummary
            )

            if (sheetsUrl != null) {
                // Open the Google Sheet in browser
                openGoogleSheet(sheetsUrl)
                return
            }
        }

        // If Google Sheets export failed or user not signed in, fall back to CSV
        val uri = exportToCSV(
            incomes = incomes,
            expenses = expenses,
            budgets = budgets,
            savingsGoals = savingsGoals,
            financialSummary = financialSummary
        )

        if (uri != null) {
            shareCSVFile(uri)
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to generate export file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Export data specifically to Google Sheets
     * This method can throw GoogleSheetsHelper.SheetsPermissionRequiredException
     * which contains an Intent that must be launched to get additional permissions
     *
     * Modified to accept Any? type parameters
     */
    suspend fun exportToGoogleSheets(
        incomes: Any? = null,
        expenses: Any? = null,
        budgets: Any? = null,
        savingsGoals: Any? = null,
        financialSummary: FinancialSummaryResponse? = null
    ): String? = withContext(Dispatchers.IO) {
        // Cast the Any? params to List<*>? safely for Google Sheets helper
        val incomesList = toSafeList(incomes)
        val expensesList = toSafeList(expenses)
        val budgetsList = toSafeList(budgets)
        val savingsGoalsList = toSafeList(savingsGoals)

        // No try-catch here - let the exception propagate to the activity
        sheetsHelper.createAndPopulateSheet(
            incomesList, expensesList, budgetsList, savingsGoalsList, financialSummary
        )
    }

    /**
     * Safely converts Any? to List<*>?
     * Handles various types of input including retrofit responses, collections, and single objects
     */
    private fun toSafeList(data: Any?): List<*>? {
        return when {
            data == null -> null
            data is List<*> -> data
            data is Array<*> -> data.toList()
            data is Collection<*> -> data.toList()
            // For retrofit responses that might be wrapped
            data.javaClass.simpleName.contains("Response") -> {
                try {
                    val bodyField = data.javaClass.getDeclaredField("body")
                    bodyField.isAccessible = true
                    val body = bodyField.get(data)
                    if (body is List<*>) body else listOf(data)
                } catch (e: Exception) {
                    Log.w("ExportService", "Failed to extract list from response: ${e.message}")
                    listOf(data)  // Fallback to treating as single object
                }
            }
            else -> listOf(data)  // If it's a single object, wrap it in a list
        }
    }

    /**
     * Export data specifically to CSV file
     * Modified to accept Any? type parameters
     */
    suspend fun exportToCSV(
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

            // Get currency formatter using user's preferred currency
            val userCurrency = sessionManager.getCurrency() ?: "PHP"
            val currency = financialSummary?.currency ?: userCurrency
            
            // Log the currency that will be used for export
            Log.d("ExportService", "Exporting data with currency: $currency")
            Log.d("ExportService", "User's preferred currency: $userCurrency")
            
            // Verify that the currency symbol is available
            val symbol = CurrencyUtils.getCurrencySymbol(currency)
            Log.d("ExportService", "Currency symbol that will be used: $symbol")
            
            val currencyFormat = NumberFormat.getCurrencyInstance()
            try {
                currencyFormat.currency = Currency.getInstance(currency)
            } catch (e: Exception) {
                currencyFormat.currency = Currency.getInstance(userCurrency)
                Log.e("ExportService", "Error setting currency $currency, falling back to $userCurrency", e)
            }

            // Convert the Any? params to List<*>? safely
            val incomesList = toSafeList(incomes)
            val expensesList = toSafeList(expenses)
            val budgetsList = toSafeList(budgets)
            val savingsGoalsList = toSafeList(savingsGoals)

            // Generate CSV content
            writer.open(file, append = false) {
                // Write header
                writeRow("MyAlkansya Financial Export - $timestamp")
                writeRow(listOf("Currency", currency))
                writeRow(listOf()) // empty row for spacing

                // Financial Summary Section
                writeRow("FINANCIAL SUMMARY")
                writeRow(listOf("Category", "Amount"))

                // Use actual financial summary data if available
                if (financialSummary != null) {
                    // Replace direct currencyFormat.format() calls with formatAmountWithCurrency
                    writeRow(listOf("Total Income", formatAmountWithCurrency(financialSummary.totalIncome, currency, currencyFormat, currency)))
                    writeRow(listOf("Total Expenses", formatAmountWithCurrency(financialSummary.totalExpenses, currency, currencyFormat, currency)))
                    writeRow(listOf("Total Budget", formatAmountWithCurrency(financialSummary.totalBudget, currency, currencyFormat, currency)))
                    writeRow(listOf("Total Savings", formatAmountWithCurrency(financialSummary.totalSavings, currency, currencyFormat, currency)))
                    writeRow(listOf("Net Cashflow", formatAmountWithCurrency(financialSummary.netCashflow, currency, currencyFormat, currency)))
                    writeRow(listOf("Budget Utilization", "${(financialSummary.budgetUtilization * 100).toInt()}%"))
                    writeRow(listOf("Savings Rate", "${(financialSummary.savingsRate * 100).toInt()}%"))
                } else {
                    // Also update the default values
                    writeRow(listOf("Total Income", formatAmountWithCurrency(0.00, currency, currencyFormat, currency)))
                    writeRow(listOf("Total Expenses", formatAmountWithCurrency(0.00, currency, currencyFormat, currency)))
                    writeRow(listOf("Total Budget", formatAmountWithCurrency(0.00, currency, currencyFormat, currency)))
                    writeRow(listOf("Total Savings", formatAmountWithCurrency(0.00, currency, currencyFormat, currency)))
                }
                writeRow(listOf()) // empty row for spacing

                // Income Section - Handle different potential types with safe casting
                if (incomesList != null && incomesList.isNotEmpty()) {
                    writeRow("INCOME RECORDS")
                    writeRow(listOf("ID", "Source", "Amount", "Date", "Currency"))

                    for (income in incomesList) {
                        when (income) {
                            is Map<*, *> -> {
                                val id = income["id"]?.toString() ?: ""
                                val source = income["source"]?.toString() ?: ""
                                val amount = income["amount"]?.toString()?.toDoubleOrNull() ?: 0.00
                                val date = income["date"]?.toString() ?: ""
                                val incomeCurrency = income["currency"]?.toString() ?: currency

                                // Format the amount using currency
                                val formattedAmount = formatAmountWithCurrency(amount, incomeCurrency, currencyFormat, currency)
                                writeRow(listOf(id, source, formattedAmount, date, incomeCurrency))
                            }
                            is Income -> {
                                val formattedAmount = formatAmountWithCurrency(
                                    income.amount,
                                    income.currency,
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(
                                    income.id.toString(),
                                    income.source,
                                    formattedAmount,
                                    income.date.toString(),
                                    income.currency
                                ))
                            }
                            else -> {
                                // Try to extract fields by reflection
                                val id = getFieldValue(income, "id") ?: ""
                                val source = getFieldValue(income, "source") ?: ""
                                val amount = getFieldValue(income, "amount")?.toString()?.toDoubleOrNull() ?: 0.00
                                val date = getFieldValue(income, "date") ?: ""
                                val incomeCurrency = getFieldValue(income, "currency") ?: currency

                                val formattedAmount = formatAmountWithCurrency(
                                    amount,
                                    incomeCurrency.toString(),
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(id, source, formattedAmount, date, incomeCurrency))
                            }
                        }
                    }
                    writeRow(listOf()) // empty row for spacing
                }

                // Expense Section with proper currency formatting
                if (expensesList != null && expensesList.isNotEmpty()) {
                    writeRow("EXPENSE RECORDS")
                    writeRow(listOf("ID", "Subject", "Category", "Amount", "Date", "Currency"))

                    for (expense in expensesList) {
                        when (expense) {
                            is Map<*, *> -> {
                                val id = expense["id"]?.toString() ?: ""
                                val subject = expense["subject"]?.toString() ?: ""
                                val category = expense["category"]?.toString() ?: ""
                                val amount = expense["amount"]?.toString()?.toDoubleOrNull() ?: 0.00
                                val date = expense["date"]?.toString() ?: ""
                                val expenseCurrency = expense["currency"]?.toString() ?: currency

                                val formattedAmount = formatAmountWithCurrency(
                                    amount,
                                    expenseCurrency,
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(id, subject, category, formattedAmount, date, expenseCurrency))
                            }
                            is ExpenseResponse -> {
                                val formattedAmount = formatAmountWithCurrency(
                                    expense.amount,
                                    expense.currency ?: currency,
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(
                                    expense.id.toString(),
                                    expense.subject,
                                    expense.category,
                                    formattedAmount,
                                    expense.date,
                                    expense.currency ?: currency
                                ))
                            }
                            else -> {
                                // Try to extract fields by reflection
                                val id = getFieldValue(expense, "id") ?: ""
                                val subject = getFieldValue(expense, "subject") ?: ""
                                val category = getFieldValue(expense, "category") ?: ""
                                val amount = getFieldValue(expense, "amount")?.toString()?.toDoubleOrNull() ?: 0.00
                                val date = getFieldValue(expense, "date") ?: ""
                                val expenseCurrency = getFieldValue(expense, "currency") ?: currency

                                val formattedAmount = formatAmountWithCurrency(
                                    amount,
                                    expenseCurrency.toString(),
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(id, subject, category, formattedAmount, date, expenseCurrency))
                            }
                        }
                    }
                    writeRow(listOf()) // empty row for spacing
                }

                // Budget Section - Handle different potential types with safe casting
                if (budgetsList != null && budgetsList.isNotEmpty()) {
                    writeRow("BUDGET RECORDS")
                    writeRow(listOf("ID", "Category", "Monthly Budget", "Total Spent", "Month", "Year"))

                    for (budget in budgetsList) {
                        when (budget) {
                            is Map<*, *> -> {
                                val id = budget["id"]?.toString() ?: ""
                                val category = budget["category"]?.toString() ?: ""
                                val monthlyBudget = budget["monthlyBudget"]?.toString()?.toDoubleOrNull() ?: 0.00
                                val totalSpent = budget["totalSpent"]?.toString()?.toDoubleOrNull() ?: 0.00
                                val month = budget["budgetMonth"]?.toString() ?: ""
                                val year = budget["budgetYear"]?.toString() ?: ""

                                val formattedMonthlyBudget = formatAmountWithCurrency(
                                    monthlyBudget,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                val formattedTotalSpent = formatAmountWithCurrency(
                                    totalSpent,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(id, category, formattedMonthlyBudget, formattedTotalSpent, month, year))
                            }
                            is BudgetResponse -> {
                                val formattedMonthlyBudget = formatAmountWithCurrency(
                                    budget.monthlyBudget,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                val formattedTotalSpent = formatAmountWithCurrency(
                                    budget.totalSpent,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(
                                    budget.id.toString(),
                                    budget.category,
                                    formattedMonthlyBudget,
                                    formattedTotalSpent,
                                    budget.budgetMonth.toString(),
                                    budget.budgetYear.toString()
                                ))
                            }
                            else -> {
                                // Try to extract fields by reflection
                                val id = getFieldValue(budget, "id") ?: ""
                                val category = getFieldValue(budget, "category") ?: ""
                                val monthlyBudget = getFieldValue(budget, "monthlyBudget")?.toString()?.toDoubleOrNull() ?: 0.00
                                val totalSpent = getFieldValue(budget, "totalSpent")?.toString()?.toDoubleOrNull() ?: 0.00
                                val month = getFieldValue(budget, "budgetMonth") ?: ""
                                val year = getFieldValue(budget, "budgetYear") ?: ""

                                val formattedMonthlyBudget = formatAmountWithCurrency(
                                    monthlyBudget,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                val formattedTotalSpent = formatAmountWithCurrency(
                                    totalSpent,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(id, category, formattedMonthlyBudget, formattedTotalSpent, month, year))
                            }
                        }
                    }
                    writeRow(listOf()) // empty row for spacing
                }

                // Savings Goals Section - Handle different potential types with safe casting
                if (savingsGoalsList != null && savingsGoalsList.isNotEmpty()) {
                    writeRow("SAVINGS GOALS")
                    writeRow(listOf("ID", "Goal", "Target Amount", "Current Amount", "Target Date", "Progress (%)"))

                    for (goal in savingsGoalsList) {
                        when (goal) {
                            is Map<*, *> -> {
                                val id = goal["id"]?.toString() ?: ""
                                val name = goal["goal"]?.toString() ?: ""
                                val targetAmount = goal["targetAmount"]?.toString()?.toDoubleOrNull() ?: 0.00
                                val currentAmount = goal["currentAmount"]?.toString()?.toDoubleOrNull() ?: 0.00
                                val targetDate = goal["targetDate"]?.toString() ?: ""
                                val progress = ((currentAmount / targetAmount) * 100).toInt().toString() + "%"

                                val formattedTargetAmount = formatAmountWithCurrency(
                                    targetAmount,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                val formattedCurrentAmount = formatAmountWithCurrency(
                                    currentAmount,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(id, name, formattedTargetAmount, formattedCurrentAmount, targetDate, progress))
                            }
                            is SavingsGoalResponse -> {
                                val progress = ((goal.currentAmount / goal.targetAmount) * 100).toInt().toString() + "%"
                                val formattedTargetAmount = formatAmountWithCurrency(
                                    goal.targetAmount,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                val formattedCurrentAmount = formatAmountWithCurrency(
                                    goal.currentAmount,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(
                                    goal.id.toString(),
                                    goal.goal,
                                    formattedTargetAmount,
                                    formattedCurrentAmount,
                                    goal.targetDate,
                                    progress
                                ))
                            }
                            else -> {
                                // Try to extract fields by reflection
                                val id = getFieldValue(goal, "id") ?: ""
                                val name = getFieldValue(goal, "goal") ?: ""
                                val targetAmount = getFieldValue(goal, "targetAmount")?.toString()?.toDoubleOrNull() ?: 0.00
                                val currentAmount = getFieldValue(goal, "currentAmount")?.toString()?.toDoubleOrNull() ?: 0.00
                                val targetDate = getFieldValue(goal, "targetDate") ?: ""
                                val progress = ((currentAmount / targetAmount) * 100).toInt().toString() + "%"

                                val formattedTargetAmount = formatAmountWithCurrency(
                                    targetAmount,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                val formattedCurrentAmount = formatAmountWithCurrency(
                                    currentAmount,
                                    currency,
                                    currencyFormat,
                                    currency
                                )
                                writeRow(listOf(id, name, formattedTargetAmount, formattedCurrentAmount, targetDate, progress))
                            }
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
                Toast.makeText(context, "CSV Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            null
        }
    }

    /**
     * Format an amount with the appropriate currency symbol
     * 
     * @param amount The amount to format
     * @param amountCurrency The currency of the amount
     * @param formatter The number formatter to use (not used anymore)
     * @param defaultCurrency The default currency to use if amountCurrency is null
     * @return Formatted amount with correct currency symbol
     */
    private fun formatAmountWithCurrency(
        amount: Double, 
        amountCurrency: String?, 
        formatter: NumberFormat,
        defaultCurrency: String
    ): String {
        val currencyToUse = amountCurrency ?: defaultCurrency
        
        // Log the currency being used for debugging
        Log.d("ExportService", "Formatting amount $amount with currency: $currencyToUse")
        
        // Skip the NumberFormat parameter and use CurrencyUtils directly
        // This ensures we use consistent formatting across the app
        return CurrencyUtils.formatWithProperCurrency(amount, currencyToUse)
    }

    /**
     * Get Google Sign-in intent for activity to start
     */
    fun getGoogleSignInIntent(): Intent {
        return sheetsHelper.getGoogleSignInIntent()
    }

    /**
     * Check if user is signed into Google
     */
    fun isUserSignedInWithGoogle(): Boolean {
        return sheetsHelper.isUserSignedIn()
    }

    /**
     * Opens a Google Sheet URL
     */
    private fun openGoogleSheet(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        // Show success message
        Toast.makeText(
            context,
            "Financial data exported to Google Sheets",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Share the exported CSV file
     */
    fun shareCSVFile(uri: Uri) {
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
