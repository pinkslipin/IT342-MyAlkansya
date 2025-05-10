package com.example.myalkansyamobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.installreferrer.BuildConfig
import com.example.myalkansyamobile.api.CategoryExpenseResponse
import com.example.myalkansyamobile.api.FinancialSummaryResponse
import com.example.myalkansyamobile.api.MonthlySummaryResponse
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.SavingsGoalProgressResponse
import com.example.myalkansyamobile.databinding.ActivityDashboardBinding
import com.example.myalkansyamobile.ui.ExportActivity
import com.example.myalkansyamobile.utils.ExportService
import com.example.myalkansyamobile.utils.SessionManager
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var exportService: ExportService

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // Calendar months are 0-indexed

    private val months = arrayOf(
        "All Months", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val years = arrayOf(
        "All Years",
        (currentYear - 2).toString(),
        (currentYear - 1).toString(),
        currentYear.toString(),
        (currentYear + 1).toString()
    )

    // Storage permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            exportData()
        } else {
            Toast.makeText(this, "Storage permission required for export", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        exportService = ExportService(this)

        setupFilterSpinners()
        setupClickListeners()
        loadDashboardData()
    }

    private fun setupFilterSpinners() {
        // Month spinner
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        binding.spinnerMonth.setSelection(currentMonth) // Default to current month

        // Year spinner
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        binding.spinnerYear.setSelection(3) // Current year is at index 3
    }

    private fun setupClickListeners() {
        binding.btnApplyFilter.setOnClickListener {
            loadDashboardData()
        }

        binding.btnResetFilters.setOnClickListener {
            binding.spinnerMonth.setSelection(currentMonth)
            binding.spinnerYear.setSelection(3) // Reset to current year
            loadDashboardData()
        }

        binding.btnExportData.setOnClickListener {
            // Replace direct export call with navigation to Export Activity
            val intent = Intent(this, ExportActivity::class.java)
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnViewAllGoals.setOnClickListener {
            val intent = Intent(this, SavingsGoalsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkStoragePermission() {
        // For Android 10+ we don't need runtime permission for our specific use case
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportData()
            return
        }

        // For Android 9 and below, check for storage permission
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                exportData()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                Toast.makeText(
                    this,
                    "Storage permission is needed to export data",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun exportData() {
        // Show loading dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Exporting Data")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        dialog.show()

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: ""
                if (token.isEmpty()) {
                    Toast.makeText(this@DashboardActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@launch
                }

                val bearerToken = "Bearer $token"

                // Fetch real user data for export
                val selectedMonth = binding.spinnerMonth.selectedItemPosition
                val selectedYear = if (binding.spinnerYear.selectedItemPosition == 0)
                    currentYear else binding.spinnerYear.selectedItem.toString().toInt()

                // Fetch incomes
                val incomes = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.incomeApiService.getIncomes(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error fetching incomes for export: ${e.message}")
                    emptyList<Any>()
                }

                // Fetch expenses
                val expenses = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.expenseApiService.getExpenses(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error fetching expenses for export: ${e.message}")
                    emptyList<Any>()
                }

                // Fetch budgets
                val budgets = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.budgetApiService.getUserBudgets(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error fetching budgets for export: ${e.message}")
                    emptyList<Any>()
                }

                // Fetch savings goals
                val savingsGoals = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.savingsGoalApiService.getAllSavingsGoals(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error fetching savings goals for export: ${e.message}")
                    emptyList<Any>()
                }

                // Fetch financial summary
                val financialSummary = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.analyticsApiService.getFinancialSummary(
                            bearerToken,
                            if (selectedMonth == 0) currentMonth else selectedMonth,
                            selectedYear
                        )
                    }
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error fetching financial summary for export: ${e.message}")
                    null
                }

                // Now export the real data
                exportService.exportAndShareFinancialData(
                    incomes = incomes,
                    expenses = expenses,
                    budgets = budgets,
                    savingsGoals = savingsGoals,
                    financialSummary = financialSummary
                )

                dialog.dismiss()
            } catch (e: Exception) {
                dialog.dismiss()
                Toast.makeText(this@DashboardActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun loadDashboardData() {
        showLoading(true)

        val token = sessionManager.getToken()
        val isValid = sessionManager.isTokenValid()
        Log.d("DashboardActivity", "Token exists: ${!token.isNullOrEmpty()}, Is valid (client-side): $isValid")

        lifecycleScope.launch {
            try {
                val validToken = if (isValid && !token.isNullOrEmpty()) token else null

                if (validToken.isNullOrEmpty()) {
                    Log.w("DashboardActivity", "Invalid token, redirecting to login")
                    handleExpiredToken()
                    return@launch
                }

                val selectedMonth = binding.spinnerMonth.selectedItemPosition
                val selectedYear = if (binding.spinnerYear.selectedItemPosition == 0)
                    currentYear // All years defaults to current year for API
                else
                    binding.spinnerYear.selectedItem.toString().toInt()

                val bearerToken = "Bearer $validToken"

                // First try a basic token validation to ensure our auth is working
                val isTokenValid = try {
                    withContext(Dispatchers.IO) {
                        sessionManager.testTokenValidityBasic(validToken)
                    }
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Token validation error: ${e.message}")
                    false
                }

                if (!isTokenValid) {
                    Log.w("DashboardActivity", "Token validation failed, redirecting to login")
                    handleExpiredToken()
                    return@launch
                }

                // Load analytics data with individual try/catch blocks for each call
                val monthlySummaryResult = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.analyticsApiService.getMonthlySummary(bearerToken, selectedYear)
                    }
                } catch (e: Exception) {
                    Log.w("DashboardActivity", "Monthly summary error: ${e.message}")
                    emptyList<MonthlySummaryResponse>()
                }

                val categoryDataResult = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.analyticsApiService.getExpenseCategories(
                            bearerToken,
                            if (selectedMonth == 0) currentMonth else selectedMonth,
                            selectedYear
                        )
                    }
                } catch (e: Exception) {
                    Log.w("DashboardActivity", "Category data error: ${e.message}")
                    emptyList<CategoryExpenseResponse>()
                }

                val financialSummaryResult = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.analyticsApiService.getFinancialSummary(
                            bearerToken,
                            if (selectedMonth == 0) currentMonth else selectedMonth,
                            selectedYear
                        )
                    }
                } catch (e: Exception) {
                    Log.w("DashboardActivity", "Financial summary error: ${e.message}")
                    FinancialSummaryResponse(
                        totalIncome = 0.0,
                        totalExpenses = 0.0,
                        totalBudget = 0.0,
                        totalSavings = 0.0,
                        netCashflow = 0.0,
                        budgetUtilization = 0.0,
                        savingsRate = 0.0,
                        currency = sessionManager.getCurrency() ?: "USD"
                    )
                }

                val savingsGoalsProgressResult = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.analyticsApiService.getSavingsGoalsProgress(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.w("DashboardActivity", "Savings goals error: ${e.message}")
                    emptyList<SavingsGoalProgressResponse>()
                }

                // Check if we got any real data
                val hasData = monthlySummaryResult.isNotEmpty() ||
                        categoryDataResult.isNotEmpty() ||
                        financialSummaryResult.totalIncome > 0 ||
                        financialSummaryResult.totalExpenses > 0 ||
                        savingsGoalsProgressResult.isNotEmpty()

                // Update UI with whatever data we managed to get
                updateFinancialSummaryCards(financialSummaryResult)
                setupMonthlyChart(monthlySummaryResult)
                setupCategoryPieChart(categoryDataResult)
                setupSavingsGoalsProgressChart(savingsGoalsProgressResult)

                showLoading(false)

                if (!hasData) {
                    Toast.makeText(
                        this@DashboardActivity,
                        "No financial data found for the selected period. Try adding some transactions.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Dashboard data load error: ${e.message}", e)

                // Check if this is an authentication error
                if (isAuthError(e)) {
                    handleExpiredToken()
                } else {
                    // Show empty data for non-auth errors
                    showEmptyData()
                    showLoading(false)

                    Toast.makeText(
                        this@DashboardActivity,
                        "Could not load financial data. Please try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showEmptyData() {
        try {
            // Use safe calls to avoid NPE
            updateFinancialSummaryCards(
                FinancialSummaryResponse(
                    totalIncome = 0.0,
                    totalExpenses = 0.0,
                    totalBudget = 0.0,
                    totalSavings = 0.0,
                    netCashflow = 0.0,
                    budgetUtilization = 0.0,
                    savingsRate = 0.0,
                    currency = sessionManager.getCurrency() ?: "USD"
                )
            )

            setupMonthlyChart(emptyList<MonthlySummaryResponse>())
            setupCategoryPieChart(emptyList<CategoryExpenseResponse>())
            setupSavingsGoalsProgressChart(emptyList<SavingsGoalProgressResponse>())

            // Show empty state message
            binding.tvNoSavingsGoals.visibility = View.VISIBLE
            binding.tvNoSavingsGoals.text = "No financial data found. Add transactions to see insights."
            binding.barChartSavingsGoals.visibility = View.GONE
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error showing empty data: ${e.message}", e)
        }
    }

    private fun isAuthError(e: Exception): Boolean {
        val errorMsg = e.message?.lowercase() ?: ""
        return errorMsg.contains("401") ||
                errorMsg.contains("403") ||
                errorMsg.contains("authentication required") ||
                errorMsg.contains("session expired") ||
                errorMsg.contains("unauthorized") ||
                errorMsg.contains("unauthenticated")
    }

    private fun handleExpiredToken() {
        showLoading(false)

        // Clear the expired token
        sessionManager.clearToken()

        // Show a friendly message
        Toast.makeText(
            this@DashboardActivity,
            "Your session has expired. Please sign in again.",
            Toast.LENGTH_LONG
        ).show()

        // Redirect to login
        val intent = Intent(this@DashboardActivity, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun handleDataLoadError(e: Exception) {
        showLoading(false)
        Log.e("DashboardActivity", "Data load error", e)

        val errorMessage = e.message ?: ""
        if (errorMessage.contains("Authentication required") ||
            errorMessage.contains("401") ||
            errorMessage.contains("Session expired")) {

            Toast.makeText(
                this@DashboardActivity,
                "Your session has expired. Please sign in again.",
                Toast.LENGTH_LONG
            ).show()

            sessionManager.clearToken()
            val intent = Intent(this@DashboardActivity, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
            return
        }

        val errorMsg = if (BuildConfig.DEBUG) {
            "Error: ${e.message}\n\n" +
                    "This might be because:\n" +
                    "1. Backend API is not implemented yet\n" +
                    "2. Server is unreachable\n" +
                    "3. Data format doesn't match expected format"
        } else {
            "Failed to load dashboard data. Please try again later."
        }

        AlertDialog.Builder(this)
            .setTitle("Data Load Error")
            .setMessage(errorMsg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateFinancialSummaryCards(summary: FinancialSummaryResponse) {
        val currencyFormat = NumberFormat.getCurrencyInstance()
        try {
            currencyFormat.currency = Currency.getInstance(summary.currency)
        } catch (e: Exception) {
            currencyFormat.currency = Currency.getInstance("USD")
        }

        // Format all financial values using user's preferred currency
        binding.tvTotalIncome.text = currencyFormat.format(summary.totalIncome)
        binding.tvTotalExpenses.text = currencyFormat.format(summary.totalExpenses)
        binding.tvTotalBudget.text = currencyFormat.format(summary.totalBudget)
        binding.tvTotalSavings.text = currencyFormat.format(summary.totalSavings)

        binding.tvNetCashflow.text = currencyFormat.format(summary.netCashflow)
        binding.tvNetCashflowLabel.text = if (summary.netCashflow >= 0) "Surplus" else "Deficit"
        binding.tvNetCashflow.setTextColor(if (summary.netCashflow >= 0)
            getColor(R.color.green_primary) else getColor(R.color.red))

        val budgetUtilizationPercentage = (summary.budgetUtilization * 100).roundToInt()
        binding.tvBudgetUtilization.text = "$budgetUtilizationPercentage%"
        binding.tvBudgetUtilizationLabel.text =
            if (summary.budgetUtilization <= 1.0) "Under budget" else "Over budget"
        binding.tvBudgetUtilization.setTextColor(
            if (summary.budgetUtilization <= 1.0) getColor(R.color.green_primary)
            else getColor(R.color.red)
        )

        val savingsRatePercentage = (summary.savingsRate * 100).roundToInt()
        binding.tvSavingsRate.text = "$savingsRatePercentage%"
        binding.tvSavingsRateLabel.text = "of income saved"
        binding.tvSavingsRate.setTextColor(getColor(R.color.green_primary))
    }

    private fun setupMonthlyChart(data: List<MonthlySummaryResponse>) {
        val barChart = binding.barChartMonthly
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setPinchZoom(false)
        barChart.isDoubleTapToZoomEnabled = false

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.setDrawZeroLine(true)
        leftAxis.setDrawAxisLine(false)

        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

        if (data.isEmpty()) {
            barChart.setNoDataText("No monthly data available")
            barChart.setNoDataTextColor(Color.GRAY)
            barChart.invalidate()
            return
        }

        xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.month })

        val incomeEntries = data.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.income.toFloat())
        }

        val expenseEntries = data.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.expenses.toFloat())
        }

        val incomeDataSet = BarDataSet(incomeEntries, "Income")
        incomeDataSet.color = getColor(R.color.green_primary)

        val expenseDataSet = BarDataSet(expenseEntries, "Expenses")
        expenseDataSet.color = getColor(R.color.yellow_primary)

        val barData = BarData(incomeDataSet, expenseDataSet)
        barData.barWidth = 0.3f

        val groupSpace = 0.4f
        val barSpace = 0.05f

        barChart.data = barData
        barChart.groupBars(0f, groupSpace, barSpace)
        barChart.invalidate()
    }

    private fun setupCategoryPieChart(data: List<CategoryExpenseResponse>) {
        val pieChart = binding.pieChartExpenses
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(false)
        
        // Enhanced legend configuration with better spacing
        pieChart.legend.apply {
            isEnabled = true
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            textSize = 12f
            xEntrySpace = 16f  // Increased spacing between legend entries
            yEntrySpace = 8f
            formSize = 12f
            formToTextSpace = 8f
            setWordWrapEnabled(true)
            maxSizePercent = 0.6f
        }
        
        pieChart.setUsePercentValues(true)
        
        // REMOVE center text entirely
        pieChart.setDrawCenterText(false)
        
        // Still keep the hole in the middle, but don't display any text
        pieChart.holeRadius = 40f
        pieChart.transparentCircleRadius = 45f
        pieChart.setExtraOffsets(24f, 24f, 24f, 24f)
        
        if (data.isEmpty()) {
            pieChart.setNoDataText("No expense categories found")
            pieChart.setNoDataTextColor(Color.GRAY)
            pieChart.invalidate()
            return
        }

        // Limit number of categories for better visibility on mobile
        val maxCategories = 6
        val sortedData = data.sortedByDescending { it.amount }
        
        val entries = if (sortedData.size > maxCategories) {
            // Display top categories and group the rest as "Others"
            val topCategories = sortedData.take(maxCategories - 1)
            val otherCategories = sortedData.drop(maxCategories - 1)
            val otherSum = otherCategories.sumOf { it.amount }
            
            topCategories.map { PieEntry(it.amount.toFloat(), it.category) } +
                    PieEntry(otherSum.toFloat(), "Others")
        } else {
            sortedData.map { PieEntry(it.amount.toFloat(), it.category) }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = getChartColors()
        dataSet.sliceSpace = 5f  // Increased distance between slices
        dataSet.selectionShift = 5f
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.4f
        dataSet.valueLinePart2Length = 0.5f
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieData.setValueTextSize(14f)  // Larger percentage text
        pieData.setValueTextColor(Color.BLACK)

        pieChart.data = pieData
        pieChart.highlightValues(null)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun setupSavingsGoalsProgressChart(data: List<SavingsGoalProgressResponse>) {
        val horizontalBarChart = binding.barChartSavingsGoals
        horizontalBarChart.description.isEnabled = false
        horizontalBarChart.setDrawGridBackground(false)
        horizontalBarChart.setDrawBarShadow(false)
        horizontalBarChart.setDrawValueAboveBar(true)
        horizontalBarChart.setPinchZoom(false)
        horizontalBarChart.isDoubleTapToZoomEnabled = false
        
        // Add extra offset for better spacing
        horizontalBarChart.setExtraOffsets(10f, 10f, 30f, 10f)

        if (data.isEmpty()) {
            binding.tvNoSavingsGoals.visibility = View.VISIBLE
            binding.tvNoSavingsGoals.text = "No savings goals found. Create a savings goal to track your progress."
            binding.barChartSavingsGoals.visibility = View.GONE
            return
        } else {
            binding.tvNoSavingsGoals.visibility = View.GONE
            horizontalBarChart.visibility = View.VISIBLE
        }

        // Sort goals by progress to show the most complete ones first
        val sortedGoals = data
            .filter { it.progress < 100 }  // Filter out completed goals
            .sortedByDescending { it.progress }
            .take(5)  // Show only top 5 goals for better visibility on mobile

        val xAxis = horizontalBarChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 100f
        xAxis.labelCount = 5
        xAxis.textSize = 11f

        val leftAxis = horizontalBarChart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawAxisLine(true)
        leftAxis.granularity = 1f
        leftAxis.textSize = 12f
        leftAxis.xOffset = 10f

        // Truncate goal names for better display on mobile
        val goalNames = sortedGoals.map {
            if (it.goal.length > 12) {
                "${it.goal.substring(0, 10)}..."
            } else {
                it.goal
            }
        }.toTypedArray()
        leftAxis.valueFormatter = IndexAxisValueFormatter(goalNames)

        val entries = sortedGoals.mapIndexed { index, goal ->
            BarEntry(index.toFloat(), goal.progress.toFloat())
        }

        val dataSet = BarDataSet(entries, "Progress (%)")
        dataSet.colors = sortedGoals.map { goal ->
            when {
                goal.progress > 90 -> Color.parseColor("#28A745")  // Green
                goal.progress > 50 -> Color.parseColor("#FFC107")  // Yellow
                else -> Color.parseColor("#A5D6B7")  // Light green
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        barData.setValueTextSize(12f)

        horizontalBarChart.data = barData
        horizontalBarChart.setFitBars(true)
        horizontalBarChart.animateY(1000)

        // Show values on bars
        horizontalBarChart.setDrawValueAboveBar(true)
        dataSet.setDrawValues(true)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }
        
        horizontalBarChart.legend.isEnabled = false
        horizontalBarChart.axisRight.isEnabled = false
        leftAxis.setLabelCount(sortedGoals.size, true)

        // Enable touch for more information
        horizontalBarChart.setTouchEnabled(true)
        horizontalBarChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    val index = it.x.toInt()
                    if (index in sortedGoals.indices) {
                        val goal = sortedGoals[index]
                        val percentFormatted = String.format("%.1f", goal.progress)
                        
                        val currencyFormat = NumberFormat.getCurrencyInstance()
                        try {
                            currencyFormat.currency = Currency.getInstance(sessionManager.getCurrency() ?: "USD")
                        } catch (ex: Exception) {
                            currencyFormat.currency = Currency.getInstance("USD")
                        }

                        val message = "${goal.goal}\n" +
                                "${currencyFormat.format(goal.currentAmount)} of " +
                                "${currencyFormat.format(goal.targetAmount)} " +
                                "($percentFormatted%)"
                        Toast.makeText(this@DashboardActivity, message, Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onNothingSelected() {
                // Do nothing
            }
        })

        horizontalBarChart.invalidate()
    }

    private fun getChartColors(): List<Int> = listOf(
        Color.parseColor("#18864F"),
        Color.parseColor("#FFC107"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#A5D6B7"),
        Color.parseColor("#EDFBE9"),
        Color.parseColor("#2E7D32"),
        Color.parseColor("#FEF6EA"),
        Color.parseColor("#DCE775"),
        Color.parseColor("#FFD54F"),
        Color.parseColor("#FFB74D"),
        Color.parseColor("#FF8A65"),
        Color.parseColor("#A1887F")
    )

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.nestedScrollView.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.nestedScrollView.visibility = View.VISIBLE
        }
    }
}
