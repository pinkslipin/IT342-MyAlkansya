package com.example.myalkansyamobile.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.myalkansyamobile.BudgetResponse
import com.example.myalkansyamobile.api.ExpenseResponse
import com.example.myalkansyamobile.api.FinancialSummaryResponse
import com.example.myalkansyamobile.api.SavingsGoalResponse
import com.example.myalkansyamobile.model.Income
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class GoogleSheetsHelper(private val context: Context) {
    private val TAG = "GoogleSheetsHelper"
    private lateinit var googleSignInClient: GoogleSignInClient
    private val sessionManager = SessionManager(context)

    init {
        try {
            // Configure sign-in to request the user's ID, email address, and basic profile
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

            // Build a GoogleSignInClient with the options
            googleSignInClient = GoogleSignIn.getClient(context, gso)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Google Sign-In: ${e.message}", e)
        }
    }

    // Custom exception to wrap the Intent required for permission
    class SheetsPermissionRequiredException(val permissionIntent: Intent) : Exception("Additional Google Sheets permissions required")

    // Check if user is signed in with Google
    fun isUserSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    // Get Google sign-in intent
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // Format amounts with the correct currency symbol
    private fun formatCurrencyAmount(amount: Double, currencyCode: String): String {
        Log.d(TAG, "Formatting currency amount in Google Sheets: $amount $currencyCode")
        // Use CurrencyUtils for consistent formatting across the app
        return CurrencyUtils.formatWithProperCurrency(amount, currencyCode)
    }

    // Create and populate a Google Sheet
    suspend fun createAndPopulateSheet(
        incomes: List<*>? = null,
        expenses: List<*>? = null,
        budgets: List<*>? = null,
        savingsGoals: List<*>? = null,
        financialSummary: FinancialSummaryResponse? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null

            // Set up credentials
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(SheetsScopes.SPREADSHEETS)
            )
            credential.selectedAccount = lastAccount.account

            // Create Sheets service
            val sheetsService = Sheets.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("MyAlkansya").build()

            // Create timestamp for sheet name
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
            val title = "MyAlkansya_Export_$timestamp"

            // Create a spreadsheet with a first sheet named "Summary" 
            val sheetNames = listOf("Summary", "Income", "Expenses", "Budget", "SavingsGoal")
            val sheets = sheetNames.map { name ->
                Sheet().setProperties(
                    SheetProperties()
                        .setTitle(name)
                        .setGridProperties(GridProperties().setColumnCount(26).setRowCount(1000))
                )
            }

            val spreadsheet = Spreadsheet()
                .setProperties(SpreadsheetProperties().setTitle(title))
                .setSheets(sheets)

            // This is where the permission exception might be thrown
            val createdSpreadsheet = try {
                sheetsService.spreadsheets().create(spreadsheet).execute()
            } catch (e: UserRecoverableAuthIOException) {
                // Wrap and rethrow the exception with the permission intent
                throw SheetsPermissionRequiredException(e.intent)
            }

            val spreadsheetId = createdSpreadsheet.spreadsheetId

            Log.d(TAG, "Created spreadsheet with ID: $spreadsheetId")

            // Get user's preferred currency
            val userCurrency = sessionManager.getCurrency() ?: "PHP"
            val currency = financialSummary?.currency ?: userCurrency

            Log.d(TAG, "Using currency for sheet: $currency")

            // Make sure we have the newly created sheets available from the API
            delay(500) // Short delay to ensure sheet creation is processed

            // Populate Summary sheet
            addFinancialSummary(spreadsheetId, sheetsService, financialSummary, currency)

            // Populate Income sheet if data exists
            if (incomes != null) {
                val incomeValues = mutableListOf<List<Any>>()

                // Add header row
                incomeValues.add(listOf("INCOME RECORDS"))
                incomeValues.add(listOf<Any>()) // Empty row
                incomeValues.add(listOf("ID", "Source", "Date", "Amount", "Currency")) // Removed any Progress column

                Log.d(TAG, "Processing incomes list with ${incomes.size} items")

                // Process the income data based on its type
                for (income in incomes) {
                    try {
                        Log.d(TAG, "Processing income item of type: ${income?.javaClass?.name}")
                        when (income) {
                            is Map<*, *> -> {
                                val id = income["id"]?.toString() ?: ""
                                val source = income["source"]?.toString() ?: ""
                                val date = income["date"]?.toString() ?: ""
                                val amount = income["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                                val incomeCurrency = income["currency"]?.toString() ?: currency

                                incomeValues.add(listOf(id, source, date, formatCurrencyAmount(amount, incomeCurrency), incomeCurrency)) // No progress value
                                Log.d(TAG, "Added income from Map: $id, $source, $amount")
                            }
                            is Income -> {
                                incomeValues.add(listOf(
                                    income.id.toString(),
                                    income.source,
                                    income.date,
                                    formatCurrencyAmount(income.amount, income.currency ?: currency),
                                    income.currency ?: currency
                                )) // No progress value
                                Log.d(TAG, "Added income from Income object: ${income.id}, ${income.source}, ${income.amount}")
                            }
                            else -> {
                                // Try to extract fields by reflection
                                val id = getFieldValue(income, "id") ?: ""
                                val source = getFieldValue(income, "source") ?: ""
                                val date = getFieldValue(income, "date") ?: ""
                                val amount = getFieldValue(income, "amount")?.toDoubleOrNull() ?: 0.0
                                val incomeCurrency = getFieldValue(income, "currency") ?: currency

                                incomeValues.add(listOf(id, source, date, formatCurrencyAmount(amount, incomeCurrency), incomeCurrency)) // No progress value
                                Log.d(TAG, "Added income via reflection: $id, $source, $amount")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing income item: ${e.message}", e)
                        // Continue with next item rather than failing entire export
                    }
                }

                // Update the Income sheet with the collected data
                updateSheetValues(sheetsService, spreadsheetId, "'Income'!A1", incomeValues)

                // Apply formatting to the Income sheet
                val rowCount = incomeValues.size
                formatDataSheet(sheetsService, spreadsheetId, "Income", rowCount)
            } else {
                Log.w(TAG, "Income list is null, skipping income export")
            }

            // Populate Expenses sheet if data exists
            if (expenses != null) {
                val expenseValues = mutableListOf<List<Any>>()

                // Add header row
                expenseValues.add(listOf("EXPENSE RECORDS"))
                expenseValues.add(listOf<Any>()) // Empty row
                expenseValues.add(listOf("ID", "Subject", "Category", "Date", "Amount", "Currency"))

                // Process the expense data based on its type
                for (expense in expenses) {
                    when (expense) {
                        is Map<*, *> -> {
                            val id = expense["id"]?.toString() ?: ""
                            val subject = expense["subject"]?.toString() ?: ""
                            val category = expense["category"]?.toString() ?: ""
                            val date = expense["date"]?.toString() ?: ""
                            val amount = expense["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                            val expenseCurrency = expense["currency"]?.toString() ?: currency

                            expenseValues.add(listOf(id, subject, category, date, formatCurrencyAmount(amount, expenseCurrency), expenseCurrency))
                        }
                        is ExpenseResponse -> {
                            expenseValues.add(listOf(
                                expense.id.toString(),
                                expense.subject,
                                expense.category,
                                expense.date,
                                formatCurrencyAmount(expense.amount, expense.currency ?: currency),
                                expense.currency ?: currency
                            ))
                        }
                        else -> {
                            // Try to extract fields by reflection
                            val id = getFieldValue(expense, "id") ?: ""
                            val subject = getFieldValue(expense, "subject") ?: ""
                            val category = getFieldValue(expense, "category") ?: ""
                            val date = getFieldValue(expense, "date") ?: ""
                            val amount = getFieldValue(expense, "amount")?.toDoubleOrNull() ?: 0.0
                            val expenseCurrency = getFieldValue(expense, "currency") ?: currency

                            expenseValues.add(listOf(id, subject, category, date, formatCurrencyAmount(amount, expenseCurrency), expenseCurrency))
                        }
                    }
                }

                // Update the Expenses sheet with the collected data
                updateSheetValues(sheetsService, spreadsheetId, "'Expenses'!A1", expenseValues)

                // Apply formatting to the Expenses sheet
                val rowCount = expenseValues.size
                formatDataSheet(sheetsService, spreadsheetId, "Expenses", rowCount)
            }

            // Populate Budget sheet if data exists
            if (budgets != null) {
                val budgetValues = mutableListOf<List<Any>>()

                // Add header row
                budgetValues.add(listOf("BUDGET RECORDS"))
                budgetValues.add(listOf<Any>()) // Empty row
                budgetValues.add(listOf("ID", "Category", "Monthly Budget", "Total Spent", "Month", "Year")) // Removed Progress column

                Log.d(TAG, "Processing budgets list with ${budgets.size} items")

                // Process the budget data based on its type
                for (budget in budgets) {
                    try {
                        Log.d(TAG, "Processing budget item of type: ${budget?.javaClass?.name}")
                        when (budget) {
                            is Map<*, *> -> {
                                val id = budget["id"]?.toString() ?: ""
                                val category = budget["category"]?.toString() ?: ""
                                val monthlyBudget = budget["monthlyBudget"]?.toString()?.toDoubleOrNull() ?: 0.0
                                val totalSpent = budget["totalSpent"]?.toString()?.toDoubleOrNull() ?: 0.0
                                val month = budget["budgetMonth"]?.toString() ?: ""
                                val year = budget["budgetYear"]?.toString() ?: ""

                                // No longer add progress formula
                                budgetValues.add(listOf(id, category, formatCurrencyAmount(monthlyBudget, currency), formatCurrencyAmount(totalSpent, currency), month, year))
                                Log.d(TAG, "Added budget from Map: $id, $category, $monthlyBudget")
                            }
                            is BudgetResponse -> {
                                // No longer add progress formula
                                budgetValues.add(listOf(
                                    budget.id.toString(),
                                    budget.category,
                                    formatCurrencyAmount(budget.monthlyBudget, currency),
                                    formatCurrencyAmount(budget.totalSpent, currency),
                                    budget.budgetMonth.toString(),
                                    budget.budgetYear.toString()
                                ))
                                Log.d(TAG, "Added budget from BudgetResponse: ${budget.id}, ${budget.category}, ${budget.monthlyBudget}")
                            }
                            else -> {
                                // Try to extract fields by reflection
                                val id = getFieldValue(budget, "id") ?: ""
                                val category = getFieldValue(budget, "category") ?: ""
                                val monthlyBudget = getFieldValue(budget, "monthlyBudget")?.toDoubleOrNull() ?: 0.0
                                val totalSpent = getFieldValue(budget, "totalSpent")?.toDoubleOrNull() ?: 0.0
                                val month = getFieldValue(budget, "budgetMonth") ?: ""
                                val year = getFieldValue(budget, "budgetYear") ?: ""

                                // No longer add progress formula
                                budgetValues.add(listOf(id, category, formatCurrencyAmount(monthlyBudget, currency), formatCurrencyAmount(totalSpent, currency), month, year))
                                Log.d(TAG, "Added budget via reflection: $id, $category, $monthlyBudget")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing budget item: ${e.message}", e)
                        // Continue with next item rather than failing entire export
                    }
                }

                // Update the Budget sheet with the collected data
                updateSheetValues(sheetsService, spreadsheetId, "'Budget'!A1", budgetValues)

                // Apply formatting to the Budget sheet
                val rowCount = budgetValues.size
                formatBudgetSheet(sheetsService, spreadsheetId, rowCount)
            } else {
                Log.w(TAG, "Budget list is null, skipping budget export")
            }

            // Populate SavingsGoal sheet if data exists
            if (savingsGoals != null) {
                val savingsValues = mutableListOf<List<Any>>()

                // Add header row
                savingsValues.add(listOf("SAVINGS GOALS"))
                savingsValues.add(listOf<Any>()) // Empty row
                savingsValues.add(listOf("ID", "Goal", "Target Amount", "Current Amount", "Target Date")) // Removed Progress column

                // Process the savings goals data based on its type
                for (goal in savingsGoals) {
                    when (goal) {
                        is Map<*, *> -> {
                            val id = goal["id"]?.toString() ?: ""
                            val name = goal["goal"]?.toString() ?: ""
                            val targetAmount = goal["targetAmount"]?.toString()?.toDoubleOrNull() ?: 0.0
                            val currentAmount = goal["currentAmount"]?.toString()?.toDoubleOrNull() ?: 0.0
                            val targetDate = goal["targetDate"]?.toString() ?: ""

                            // No longer add progress formula
                            savingsValues.add(listOf(id, name, formatCurrencyAmount(targetAmount, currency), formatCurrencyAmount(currentAmount, currency), targetDate))
                        }
                        is SavingsGoalResponse -> {
                            // No longer add progress formula
                            savingsValues.add(listOf(
                                goal.id.toString(),
                                goal.goal,
                                formatCurrencyAmount(goal.targetAmount, currency),
                                formatCurrencyAmount(goal.currentAmount, currency),
                                goal.targetDate
                            ))
                        }
                        else -> {
                            // Try to extract fields by reflection
                            val id = getFieldValue(goal, "id") ?: ""
                            val name = getFieldValue(goal, "goal") ?: ""
                            val targetAmount = getFieldValue(goal, "targetAmount")?.toDoubleOrNull() ?: 0.0
                            val currentAmount = getFieldValue(goal, "currentAmount")?.toDoubleOrNull() ?: 0.0
                            val targetDate = getFieldValue(goal, "targetDate") ?: ""

                            // No longer add progress formula
                            savingsValues.add(listOf(id, name, formatCurrencyAmount(targetAmount, currency), formatCurrencyAmount(currentAmount, currency), targetDate))
                        }
                    }
                }

                // Update the SavingsGoal sheet with the collected data
                updateSheetValues(sheetsService, spreadsheetId, "'SavingsGoal'!A1", savingsValues)

                // Apply formatting to the SavingsGoal sheet
                val rowCount = savingsValues.size
                formatSavingsSheet(sheetsService, spreadsheetId, rowCount)
            }

            // Footer with export info
            updateSheetValues(sheetsService, spreadsheetId, "'Summary'!A12", listOf<List<Any>>(
                listOf<Any>(""),
                listOf<Any>("Generated by MyAlkansya App on", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            ))

            // Return the URL to the spreadsheet
            "https://docs.google.com/spreadsheets/d/$spreadsheetId/edit"
        } catch (e: SheetsPermissionRequiredException) {
            // Let this propagate up to the activity
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Google Sheet", e)
            null
        }
    }

    // Helper method to get sheet ID by name
    private fun getSheetIdByName(sheetsService: Sheets, spreadsheetId: String, sheetName: String): Int? {
        try {
            val response = sheetsService.spreadsheets().get(spreadsheetId).execute()
            val sheet = response.sheets.find { it.properties.title == sheetName }
            return sheet?.properties?.sheetId
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sheet ID by name: ${e.message}", e)
            return null
        }
    }

    // Update sheet values with proper sheet name quoting
    private fun updateSheetValues(
        sheetsService: Sheets,
        spreadsheetId: String,
        range: String,
        values: List<List<Any>>
    ) {
        try {
            val body = ValueRange().setValues(values)
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute()
            Log.d(TAG, "Successfully updated range $range")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating values in range $range: ${e.message}", e)
            throw e
        }
    }

    // Format the summary sheet
    private fun formatSummarySheet(sheetsService: Sheets, spreadsheetId: String) {
        // ... existing code ...
    }

    // Format data sheets with better error handling
    private fun formatDataSheet(sheetsService: Sheets, spreadsheetId: String, sheetName: String, rowCount: Int) {
        try {
            val sheetId = getSheetIdByName(sheetsService, spreadsheetId, sheetName) ?: return

            val requests = mutableListOf<Request>()

            // Format header rows (first 3 rows)
            requests.add(Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(0)
                        .setEndRowIndex(3))
                    .setCell(CellData()
                        .setUserEnteredFormat(CellFormat()
                            .setTextFormat(TextFormat()
                                .setBold(true)
                                .setFontSize(12))))
                    .setFields("userEnteredFormat(textFormat)")
            ))

            // Execute requests in smaller batches to avoid server errors
            try {
                val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
                Log.d(TAG, "Applied basic formatting to $sheetName sheet")
            } catch (e: Exception) {
                Log.e(TAG, "Error applying basic formatting to $sheetName sheet: ${e.message}", e)
                // Continue with remaining formatting - don't exit early
            }

            // Additional formatting in separate batches
            val additionalRequests = mutableListOf<Request>()

            // Apply background color to header
            additionalRequests.add(Request().setUpdateCells(
                UpdateCellsRequest()
                    .setRange(GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(0)
                        .setEndRowIndex(1))
                    .setRows(listOf(RowData()
                        .setValues(listOf(CellData()
                            .setUserEnteredFormat(CellFormat()
                                .setBackgroundColor(Color()
                                    .setRed(0.98f)
                                    .setGreen(0.79f)
                                    .setBlue(0.28f)))))))
                    .setFields("userEnteredFormat.backgroundColor")
            ))

            // Execute this batch separately
            try {
                val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(additionalRequests)
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
                Log.d(TAG, "Applied header color to $sheetName sheet")
            } catch (e: Exception) {
                Log.e(TAG, "Error applying header color to $sheetName sheet: ${e.message}", e)
                // Continue with remaining formatting
            }

            // Format currency columns in separate batches if applicable
            if (sheetName == "Income" || sheetName == "Expenses" || sheetName == "Budget" || sheetName == "SavingsGoal") {
                try {
                    val currencyRequests = mutableListOf<Request>()
                    // Assuming amount is in column D for incomes and E for expenses
                    val amountColumnIndex = when (sheetName) {
                        "Income" -> 3  // Column D
                        "Expenses" -> 4 // Column E
                        "Budget" -> 2 // Column C - Monthly Budget
                        "SavingsGoal" -> 2 // Column C - Target Amount
                        else -> 3 
                    }

                    currencyRequests.add(Request().setRepeatCell(
                        RepeatCellRequest()
                            .setRange(GridRange()
                                .setSheetId(sheetId)
                                .setStartRowIndex(3)
                                .setEndRowIndex(rowCount)
                                .setStartColumnIndex(amountColumnIndex)
                                .setEndColumnIndex(amountColumnIndex + 1))
                            .setCell(CellData()
                                .setUserEnteredFormat(CellFormat()
                                    .setNumberFormat(NumberFormat()
                                        .setType("CURRENCY"))))
                            .setFields("userEnteredFormat.numberFormat")
                    ))

                    val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(currencyRequests)
                    sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
                    Log.d(TAG, "Applied currency formatting to $sheetName sheet")
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying currency formatting to $sheetName sheet: ${e.message}", e)
                }
            }

            // Auto-resize columns as last batch
            try {
                val resizeRequests = mutableListOf<Request>()
                resizeRequests.add(Request().setAutoResizeDimensions(
                    AutoResizeDimensionsRequest()
                        .setDimensions(DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("COLUMNS")
                            .setStartIndex(0)
                            .setEndIndex(10))
                ))

                val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(resizeRequests)
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
                Log.d(TAG, "Resized columns in $sheetName sheet")
            } catch (e: Exception) {
                Log.e(TAG, "Error resizing columns in $sheetName sheet: ${e.message}", e)
            }

            Log.d(TAG, "Formatted $sheetName sheet successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting $sheetName sheet: ${e.message}", e)
        }
    }

    // Format budget sheet with conditional formatting for progress column
    private fun formatBudgetSheet(sheetsService: Sheets, spreadsheetId: String, rowCount: Int) {
        try {
            // First apply the same basic formatting as other sheets to ensure consistency
            formatDataSheet(sheetsService, spreadsheetId, "Budget", rowCount)

            // Apply additional budget-specific formatting
            val sheetId = getSheetIdByName(sheetsService, spreadsheetId, "Budget") ?: return

            // Apply number formatting to budget and spent columns (columns C and D)
            val currencyFormatRequest = BatchUpdateSpreadsheetRequest().setRequests(
                listOf(
                    Request().setRepeatCell(
                        RepeatCellRequest()
                            .setRange(
                                GridRange()
                                    .setSheetId(sheetId)
                                    .setStartRowIndex(3) // Skip headers
                                    .setEndRowIndex(rowCount)
                                    .setStartColumnIndex(2) // Column C
                                    .setEndColumnIndex(4) // Column D (exclusive)
                            )
                            .setCell(
                                CellData()
                                    .setUserEnteredFormat(
                                        CellFormat()
                                            .setNumberFormat(
                                                NumberFormat()
                                                    .setType("CURRENCY")
                                            )
                                    )
                            )
                            .setFields("userEnteredFormat.numberFormat")
                    )
                )
            )

            sheetsService.spreadsheets().batchUpdate(spreadsheetId, currencyFormatRequest).execute()

        } catch (e: Exception) {
            Log.e(TAG, "Error formatting Budget sheet: ${e.message}", e)
        }
    }

    // Format savings goals sheet
    private fun formatSavingsSheet(sheetsService: Sheets, spreadsheetId: String, rowCount: Int) {
        try {
            // First apply basic formatting from the data sheet method
            formatDataSheet(sheetsService, spreadsheetId, "SavingsGoal", rowCount)

            val sheetId = getSheetIdByName(sheetsService, spreadsheetId, "SavingsGoal") ?: return

            val requests = mutableListOf<Request>()

            // Format currency columns (Target Amount and Current Amount)
            requests.add(Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(3)
                        .setEndRowIndex(rowCount)
                        .setStartColumnIndex(2) // Column C - Target Amount
                        .setEndColumnIndex(4)) // Column D - Current Amount
                    .setCell(CellData()
                        .setUserEnteredFormat(CellFormat()
                            .setNumberFormat(NumberFormat()
                                .setType("CURRENCY"))))
                    .setFields("userEnteredFormat.numberFormat")
            ))

            // Execute all formatting requests
            val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
            sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()

            Log.d(TAG, "Formatted SavingsGoal sheet successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting SavingsGoal sheet: ${e.message}", e)
        }
    }

    // Helper method to extract field values using reflection
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

    /**
     * Add financial summary to sheet
     */
    private fun addFinancialSummary(
        spreadsheetId: String,
        sheetsService: Sheets,
        financialSummary: FinancialSummaryResponse?,
        currency: String
    ) {
        // Use currency format consistently
        val rows = mutableListOf<List<Any>>()
        rows.add(listOf("FINANCIAL SUMMARY"))
        rows.add(listOf("Category", "Amount"))

        if (financialSummary != null) {
            rows.add(listOf("Total Income", formatCurrencyAmount(financialSummary.totalIncome, currency)))
            rows.add(listOf("Total Expenses", formatCurrencyAmount(financialSummary.totalExpenses, currency)))
            rows.add(listOf("Total Budget", formatCurrencyAmount(financialSummary.totalBudget, currency)))
            rows.add(listOf("Total Savings", formatCurrencyAmount(financialSummary.totalSavings, currency)))
            rows.add(listOf("Net Cashflow", formatCurrencyAmount(financialSummary.netCashflow, currency)))
            rows.add(listOf("Budget Utilization", "${(financialSummary.budgetUtilization * 100).toInt()}%"))
            rows.add(listOf("Savings Rate", "${(financialSummary.savingsRate * 100).toInt()}%"))
        } else {
            rows.add(listOf("Total Income", formatCurrencyAmount(0.0, currency)))
            rows.add(listOf("Total Expenses", formatCurrencyAmount(0.0, currency)))
            rows.add(listOf("Total Budget", formatCurrencyAmount(0.0, currency)))
            rows.add(listOf("Total Savings", formatCurrencyAmount(0.0, currency)))
        }

        updateSheetValues(sheetsService, spreadsheetId, "'Summary'!A1", rows)
    }
}
