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
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
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

    // Custom exception to wrap the Intent required for permission
    class SheetsPermissionRequiredException(val permissionIntent: Intent) : Exception("Additional Google Sheets permissions required")

    // Check if user is signed in with Google
    fun isUserSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        
        // Check if user has the sheets scope
        return account != null && GoogleSignIn.hasPermissions(
            account,
            Scope(SheetsScopes.SPREADSHEETS)
        )
    }

    // Get Google sign-in intent
    fun getGoogleSignInIntent(): Intent {
        // Only request the Sheets scope specifically
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .build()
        
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        return googleSignInClient.signInIntent
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
            
            // Get currency formatter
            val currency = financialSummary?.currency ?: "USD"
            val currencyFormat = NumberFormat.getCurrencyInstance()
            try {
                currencyFormat.currency = Currency.getInstance(currency)
            } catch (e: Exception) {
                currencyFormat.currency = Currency.getInstance("USD")
            }
            
            // Make sure we have the newly created sheets available from the API
            delay(500) // Short delay to ensure sheet creation is processed
            
            // Populate Summary sheet
            val summaryValues = mutableListOf<List<Any>>(
                listOf<Any>("MyAlkansya Financial Export - $timestamp"),
                listOf<Any>(""), // Empty row
                listOf<Any>("FINANCIAL SUMMARY"),
                listOf<Any>("Category", "Amount")
            )
            
            if (financialSummary != null) {
                summaryValues.add(listOf<Any>("Total Income", currencyFormat.format(financialSummary.totalIncome)))
                summaryValues.add(listOf<Any>("Total Expenses", currencyFormat.format(financialSummary.totalExpenses)))
                summaryValues.add(listOf<Any>("Total Budget", currencyFormat.format(financialSummary.totalBudget)))
                summaryValues.add(listOf<Any>("Total Savings", currencyFormat.format(financialSummary.totalSavings)))
                summaryValues.add(listOf<Any>("Net Cashflow", currencyFormat.format(financialSummary.netCashflow)))
                summaryValues.add(listOf<Any>("Budget Utilization", "${(financialSummary.budgetUtilization * 100).toInt()}%"))
                summaryValues.add(listOf<Any>("Savings Rate", "${(financialSummary.savingsRate * 100).toInt()}%"))
            } else {
                summaryValues.add(listOf<Any>("Total Income", "0.00"))
                summaryValues.add(listOf<Any>("Total Expenses", "0.00"))
                summaryValues.add(listOf<Any>("Total Budget", "0.00"))
                summaryValues.add(listOf<Any>("Total Savings", "0.00"))
            }
            
            // Add summary data using the sheet ID instead of name
            val sheetId = getSheetIdByName(sheetsService, spreadsheetId, "Summary")
            if (sheetId != null) {
                updateSheetValues(sheetsService, spreadsheetId, "'Summary'!A1", summaryValues)
                formatSummarySheet(sheetsService, spreadsheetId)
            } else {
                Log.e(TAG, "Summary sheet not found")
            }
            
            // Populate Income sheet if data exists
            if (incomes != null) {
                val incomeValues = mutableListOf<List<Any>>()
                
                // Add header row
                incomeValues.add(listOf("INCOME RECORDS"))
                incomeValues.add(listOf<Any>()) // Empty row
                incomeValues.add(listOf("ID", "Source", "Date", "Amount", "Currency"))
                
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
                                val amount = income["amount"]?.toString() ?: "0.00"
                                val incomeCurrency = income["currency"]?.toString() ?: currency
                                
                                incomeValues.add(listOf(id, source, date, amount, incomeCurrency))
                                Log.d(TAG, "Added income from Map: $id, $source, $amount")
                            }
                            is Income -> {
                                incomeValues.add(listOf(
                                    income.id.toString(),
                                    income.source,
                                    income.date,
                                    income.amount.toString(),
                                    income.currency ?: currency
                                ))
                                Log.d(TAG, "Added income from Income object: ${income.id}, ${income.source}, ${income.amount}")
                            }
                            else -> {
                                // Try to extract fields by reflection
                                val id = getFieldValue(income, "id") ?: ""
                                val source = getFieldValue(income, "source") ?: ""
                                val date = getFieldValue(income, "date") ?: ""
                                val amount = getFieldValue(income, "amount") ?: "0.00"
                                val incomeCurrency = getFieldValue(income, "currency") ?: currency
                                
                                incomeValues.add(listOf(id, source, date, amount, incomeCurrency))
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
                            val amount = expense["amount"]?.toString() ?: "0.00"
                            val expenseCurrency = expense["currency"]?.toString() ?: currency
                            
                            expenseValues.add(listOf(id, subject, category, date, amount, expenseCurrency))
                        }
                        is ExpenseResponse -> {
                            expenseValues.add(listOf(
                                expense.id.toString(),
                                expense.subject,
                                expense.category,
                                expense.date,
                                expense.amount.toString(),
                                expense.currency ?: currency
                            ))
                        }
                        else -> {
                            // Try to extract fields by reflection
                            val id = getFieldValue(expense, "id") ?: ""
                            val subject = getFieldValue(expense, "subject") ?: ""
                            val category = getFieldValue(expense, "category") ?: ""
                            val date = getFieldValue(expense, "date") ?: ""
                            val amount = getFieldValue(expense, "amount") ?: "0.00"
                            val expenseCurrency = getFieldValue(expense, "currency") ?: currency
                            
                            expenseValues.add(listOf(id, subject, category, date, amount, expenseCurrency))
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
                budgetValues.add(listOf("ID", "Category", "Monthly Budget", "Total Spent", "Month", "Year", "Progress"))
                
                Log.d(TAG, "Processing budgets list with ${budgets.size} items")
                
                // Process the budget data based on its type
                for (budget in budgets) {
                    try {
                        Log.d(TAG, "Processing budget item of type: ${budget?.javaClass?.name}")
                        when (budget) {
                            is Map<*, *> -> {
                                val id = budget["id"]?.toString() ?: ""
                                val category = budget["category"]?.toString() ?: ""
                                val monthlyBudget = budget["monthlyBudget"]?.toString() ?: "0.00"
                                val totalSpent = budget["totalSpent"]?.toString() ?: "0.00"
                                val month = budget["budgetMonth"]?.toString() ?: ""
                                val year = budget["budgetYear"]?.toString() ?: ""
                                
                                // Calculate progress as a formula
                                val progressFormula = "IFERROR(E${budgetValues.size + 1}/D${budgetValues.size + 1}, 0)"
                                
                                budgetValues.add(listOf(id, category, monthlyBudget, totalSpent, month, year, progressFormula))
                                Log.d(TAG, "Added budget from Map: $id, $category, $monthlyBudget")
                            }
                            is BudgetResponse -> {
                                // Calculate progress as a formula
                                val progressFormula = "IFERROR(E${budgetValues.size + 1}/D${budgetValues.size + 1}, 0)"
                                
                                budgetValues.add(listOf(
                                    budget.id.toString(),
                                    budget.category,
                                    budget.monthlyBudget.toString(),
                                    budget.totalSpent.toString(),
                                    budget.budgetMonth.toString(),
                                    budget.budgetYear.toString(),
                                    progressFormula
                                ))
                                Log.d(TAG, "Added budget from BudgetResponse: ${budget.id}, ${budget.category}, ${budget.monthlyBudget}")
                            }
                            else -> {
                                // Try to extract fields by reflection
                                val id = getFieldValue(budget, "id") ?: ""
                                val category = getFieldValue(budget, "category") ?: ""
                                val monthlyBudget = getFieldValue(budget, "monthlyBudget") ?: "0.00"
                                val totalSpent = getFieldValue(budget, "totalSpent") ?: "0.00"
                                val month = getFieldValue(budget, "budgetMonth") ?: ""
                                val year = getFieldValue(budget, "budgetYear") ?: ""
                                
                                // Calculate progress as a formula
                                val progressFormula = "IFERROR(E${budgetValues.size + 1}/D${budgetValues.size + 1}, 0)"
                                
                                budgetValues.add(listOf(id, category, monthlyBudget, totalSpent, month, year, progressFormula))
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
                savingsValues.add(listOf("ID", "Goal", "Target Amount", "Current Amount", "Target Date", "Progress"))
                
                // Process the savings goals data based on its type
                for (goal in savingsGoals) {
                    when (goal) {
                        is Map<*, *> -> {
                            val id = goal["id"]?.toString() ?: ""
                            val name = goal["goal"]?.toString() ?: ""
                            val targetAmount = goal["targetAmount"]?.toString() ?: "0.00"
                            val currentAmount = goal["currentAmount"]?.toString() ?: "0.00"
                            val targetDate = goal["targetDate"]?.toString() ?: ""
                            
                            // Calculate progress as a formula
                            val progressFormula = "IFERROR(D${savingsValues.size + 1}/C${savingsValues.size + 1}, 0)"
                            
                            savingsValues.add(listOf(id, name, targetAmount, currentAmount, targetDate, progressFormula))
                        }
                        is SavingsGoalResponse -> {
                            // Calculate progress as a formula
                            val progressFormula = "IFERROR(D${savingsValues.size + 1}/C${savingsValues.size + 1}, 0)"
                            
                            savingsValues.add(listOf(
                                goal.id.toString(),
                                goal.goal,
                                goal.targetAmount.toString(),
                                goal.currentAmount.toString(),
                                goal.targetDate,
                                progressFormula
                            ))
                        }
                        else -> {
                            // Try to extract fields by reflection
                            val id = getFieldValue(goal, "id") ?: ""
                            val name = getFieldValue(goal, "goal") ?: ""
                            val targetAmount = getFieldValue(goal, "targetAmount") ?: "0.00"
                            val currentAmount = getFieldValue(goal, "currentAmount") ?: "0.00"
                            val targetDate = getFieldValue(goal, "targetDate") ?: ""
                            
                            // Calculate progress as a formula
                            val progressFormula = "IFERROR(D${savingsValues.size + 1}/C${savingsValues.size + 1}, 0)"
                            
                            savingsValues.add(listOf(id, name, targetAmount, currentAmount, targetDate, progressFormula))
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
            // First apply basic formatting from the data sheet method
            formatDataSheet(sheetsService, spreadsheetId, "Budget", rowCount)
            
            val sheetId = getSheetIdByName(sheetsService, spreadsheetId, "Budget") ?: return
            
            val requests = mutableListOf<Request>()
            
            // Format currency columns (Monthly Budget and Total Spent)
            requests.add(Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(3)
                        .setEndRowIndex(rowCount)
                        .setStartColumnIndex(2) // Column C - Monthly Budget
                        .setEndColumnIndex(4)) // Column D - Total Spent
                    .setCell(CellData()
                        .setUserEnteredFormat(CellFormat()
                            .setNumberFormat(NumberFormat()
                                .setType("CURRENCY"))))
                    .setFields("userEnteredFormat.numberFormat")
            ))
            
            // Format the progress column as percentage
            requests.add(Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(3)
                        .setEndRowIndex(rowCount)
                        .setStartColumnIndex(6) // Column G - Progress
                        .setEndColumnIndex(7))
                    .setCell(CellData()
                        .setUserEnteredFormat(CellFormat()
                            .setNumberFormat(NumberFormat()
                                .setType("PERCENT")
                                .setPattern("0.00%"))))
                    .setFields("userEnteredFormat.numberFormat")
            ))
            
            // Define the range for conditional formatting
            val progressRange = GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(3)
                .setEndRowIndex(rowCount)
                .setStartColumnIndex(6)
                .setEndColumnIndex(7)
            
            // Add conditional formatting rules for the progress column
            // Rule 1: Red for > 100% (over budget)
            requests.add(Request().setAddConditionalFormatRule(
                AddConditionalFormatRuleRequest()
                    .setRule(ConditionalFormatRule()
                        .setRanges(listOf(progressRange))
                        .setBooleanRule(BooleanRule()
                            .setCondition(BooleanCondition()
                                .setType("NUMBER_GREATER")
                                .setValues(listOf(ConditionValue().setUserEnteredValue("1"))))
                            .setFormat(CellFormat()
                                .setBackgroundColor(Color().setRed(0.9f).setGreen(0.4f).setBlue(0.4f))
                                .setTextFormat(TextFormat().setForegroundColor(Color().setRed(1.0f).setGreen(1.0f).setBlue(1.0f))))))
            ))
            
            // Rule 2: Yellow for > 80% (approaching budget)
            requests.add(Request().setAddConditionalFormatRule(
                AddConditionalFormatRuleRequest()
                    .setRule(ConditionalFormatRule()
                        .setRanges(listOf(progressRange))
                        .setBooleanRule(BooleanRule()
                            .setCondition(BooleanCondition()
                                .setType("NUMBER_BETWEEN")
                                .setValues(listOf(
                                    ConditionValue().setUserEnteredValue("0.8"),
                                    ConditionValue().setUserEnteredValue("1")
                                )))
                            .setFormat(CellFormat()
                                .setBackgroundColor(Color().setRed(1.0f).setGreen(0.85f).setBlue(0.4f)))))
            ))
            
            // Rule 3: Green for < 80% (well within budget)
            requests.add(Request().setAddConditionalFormatRule(
                AddConditionalFormatRuleRequest()
                    .setRule(ConditionalFormatRule()
                        .setRanges(listOf(progressRange))
                        .setBooleanRule(BooleanRule()
                            .setCondition(BooleanCondition()
                                .setType("NUMBER_LESS")
                                .setValues(listOf(ConditionValue().setUserEnteredValue("0.8"))))
                            .setFormat(CellFormat()
                                .setBackgroundColor(Color().setRed(0.5f).setGreen(0.8f).setBlue(0.5f)))))
            ))
            
            // Execute all formatting requests
            val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
            sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
            
            Log.d(TAG, "Formatted Budget sheet successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting Budget sheet: ${e.message}", e)
        }
    }
    
    // Format savings goals sheet with progress formatting
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
            
            // Format the progress column as percentage
            requests.add(Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(3)
                        .setEndRowIndex(rowCount)
                        .setStartColumnIndex(5) // Column F - Progress
                        .setEndColumnIndex(6))
                    .setCell(CellData()
                        .setUserEnteredFormat(CellFormat()
                            .setNumberFormat(NumberFormat()
                                .setType("PERCENT")
                                .setPattern("0.00%"))))
                    .setFields("userEnteredFormat.numberFormat")
            ))
            
            // Define the range for conditional formatting
            val progressRange = GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(3)
                .setEndRowIndex(rowCount)
                .setStartColumnIndex(5)
                .setEndColumnIndex(6)
            
            // Add conditional formatting rules for the progress column
            // Rule 1: Green for > 90% (almost at goal)
            requests.add(Request().setAddConditionalFormatRule(
                AddConditionalFormatRuleRequest()
                    .setRule(ConditionalFormatRule()
                        .setRanges(listOf(progressRange))
                        .setBooleanRule(BooleanRule()
                            .setCondition(BooleanCondition()
                                .setType("NUMBER_GREATER")
                                .setValues(listOf(ConditionValue().setUserEnteredValue("0.9"))))
                            .setFormat(CellFormat()
                                .setBackgroundColor(Color().setRed(0.4f).setGreen(0.8f).setBlue(0.4f))
                                .setTextFormat(TextFormat().setForegroundColor(Color().setRed(0.0f).setGreen(0.0f).setBlue(0.0f))))))
            ))
            
            // Rule 2: Yellow for 50-90% (good progress)
            requests.add(Request().setAddConditionalFormatRule(
                AddConditionalFormatRuleRequest()
                    .setRule(ConditionalFormatRule()
                        .setRanges(listOf(progressRange))
                        .setBooleanRule(BooleanRule()
                            .setCondition(BooleanCondition()
                                .setType("NUMBER_BETWEEN")
                                .setValues(listOf(
                                    ConditionValue().setUserEnteredValue("0.5"),
                                    ConditionValue().setUserEnteredValue("0.9")
                                )))
                            .setFormat(CellFormat()
                                .setBackgroundColor(Color().setRed(1.0f).setGreen(0.85f).setBlue(0.4f)))))
            ))
            
            // Rule 3: Red for < 50% (needs attention)
            requests.add(Request().setAddConditionalFormatRule(
                AddConditionalFormatRuleRequest()
                    .setRule(ConditionalFormatRule()
                        .setRanges(listOf(progressRange))
                        .setBooleanRule(BooleanRule()
                            .setCondition(BooleanCondition()
                                .setType("NUMBER_LESS")
                                .setValues(listOf(ConditionValue().setUserEnteredValue("0.5"))))
                            .setFormat(CellFormat()
                                .setBackgroundColor(Color().setRed(0.9f).setGreen(0.4f).setBlue(0.4f)))))
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
}
