package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.cardview.widget.CardView
import com.example.myalkansyamobile.adapters.PopularCurrencyAdapter
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.model.CurrencyConversionRequest
import com.example.myalkansyamobile.model.PopularCurrency
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.NumberFormat
import java.util.*

class CurrencyConverterActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    
    // UI elements
    private lateinit var fromCurrencySpinner: Spinner
    private lateinit var toCurrencySpinner: Spinner
    private lateinit var amountEditText: EditText
    private lateinit var convertButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var rateTextView: TextView
    private lateinit var popularCurrenciesView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var backToHomeButton: Button
    private lateinit var swapCurrenciesButton: ImageButton
    
    // New UI elements for trends
    private lateinit var trendsSection: LinearLayout
    private lateinit var trend7daysCard: CardView
    private lateinit var trend30daysCard: CardView
    private lateinit var trend90daysCard: CardView
    
    // Currency data
    private val currencyCodes = arrayOf(
        "USD", "EUR", "JPY", "GBP", "AUD", "CAD", "CHF", "CNY", "HKD", "NZD", 
        "SEK", "KRW", "SGD", "NOK", "MXN", "INR", "RUB", "ZAR", "TRY", "BRL", 
        "PHP", "BTC"
    )
    
    private val currencyNames = mapOf(
        "USD" to "US Dollar",
        "EUR" to "Euro",
        "JPY" to "Japanese Yen",
        "GBP" to "British Pound",
        "AUD" to "Australian Dollar",
        "CAD" to "Canadian Dollar",
        "CHF" to "Swiss Franc",
        "CNY" to "Chinese Yuan",
        "HKD" to "Hong Kong Dollar",
        "NZD" to "New Zealand Dollar",
        "SEK" to "Swedish Krona",
        "KRW" to "South Korean Won",
        "SGD" to "Singapore Dollar",
        "NOK" to "Norwegian Krone",
        "MXN" to "Mexican Peso",
        "INR" to "Indian Rupee",
        "RUB" to "Russian Ruble",
        "ZAR" to "South African Rand",
        "TRY" to "Turkish Lira",
        "BRL" to "Brazilian Real",
        "PHP" to "Philippine Peso",
        "BTC" to "Bitcoin"
    )
    
    private val popularCurrencies = mutableListOf<PopularCurrency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_converter)
        
        // Initialize session manager
        sessionManager = SessionManager(this)
        
        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Currency Converter"
        
        try {
            // Initialize UI elements
            fromCurrencySpinner = findViewById(R.id.spinnerFromCurrency)
            toCurrencySpinner = findViewById(R.id.spinnerToCurrency)
            amountEditText = findViewById(R.id.etAmount)
            convertButton = findViewById(R.id.btnConvert)
            resultTextView = findViewById(R.id.tvResult)
            rateTextView = findViewById(R.id.tvRate)
            popularCurrenciesView = findViewById(R.id.lvPopularCurrencies)
            progressBar = findViewById(R.id.progressBar)
            errorTextView = findViewById(R.id.tvError)
            backToHomeButton = findViewById(R.id.btnBackToHome)
            swapCurrenciesButton = findViewById(R.id.btnSwapCurrencies)
            
            // Initialize trend UI elements
            trendsSection = findViewById(R.id.trendsSection)
            trend7daysCard = findViewById(R.id.trend7days)
            trend30daysCard = findViewById(R.id.trend30days)
            trend90daysCard = findViewById(R.id.trend90days)
            
            // Setup currency spinners
            setupCurrencySpinners()
            
            // Setup convert button
            convertButton.setOnClickListener {
                convertCurrency()
            }
            
            // Setup swap button
            swapCurrenciesButton.setOnClickListener {
                swapCurrencies()
            }
            
            // Setup back to home button
            backToHomeButton.setOnClickListener {
                navigateToHome()
            }
            
            // Fetch popular currencies
            fetchPopularCurrencies()
            
        } catch (e: Exception) {
            Log.e("CurrencyConverter", "Error initializing UI: ${e.message}", e)
            Toast.makeText(this, "Error initializing UI. Please try again.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    // Handle back button in action bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            navigateToHome()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    // Handle back button press
    override fun onBackPressed() {
        navigateToHome()
    }
    
    // Back to home button click handler
    fun onBackToHomeClick(view: View) {
        navigateToHome()
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomePageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
    
    private fun setupCurrencySpinners() {
        try {
            // Create array of currency display strings (e.g., "USD - US Dollar")
            val currencyDisplayList = currencyCodes.map { "$it - ${currencyNames[it] ?: it}" }.toTypedArray()
            
            // Create adapter for spinners
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyDisplayList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            
            // Set adapters to spinners
            fromCurrencySpinner.adapter = adapter
            toCurrencySpinner.adapter = adapter
            
            // Set default selections based on user preference
            val userCurrency = sessionManager.getCurrency() ?: "USD"
            val usdPosition = currencyCodes.indexOf("USD")
            val phpPosition = currencyCodes.indexOf("PHP")
            val userCurrencyPosition = currencyCodes.indexOf(userCurrency)
            
            // From is usually user's currency or USD
            if (userCurrencyPosition >= 0) {
                fromCurrencySpinner.setSelection(userCurrencyPosition)
            } else if (usdPosition >= 0) {
                fromCurrencySpinner.setSelection(usdPosition)
            }
            
            // To is usually PHP or a different currency than From
            if (phpPosition >= 0 && phpPosition != userCurrencyPosition) {
                toCurrencySpinner.setSelection(phpPosition)
            } else if (usdPosition >= 0 && userCurrencyPosition != usdPosition) {
                toCurrencySpinner.setSelection(usdPosition)
            } else if (currencyCodes.isNotEmpty()) {
                // Set to first currency that's different from the 'from' currency
                for (i in currencyCodes.indices) {
                    if (i != userCurrencyPosition) {
                        toCurrencySpinner.setSelection(i)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CurrencyConverter", "Error setting up spinners: ${e.message}", e)
            // Fallback to default behavior if setup fails
        }
    }
    
    private fun convertCurrency() {
        // Hide any previous errors and clear results
        errorTextView.visibility = View.GONE
        resultTextView.visibility = View.GONE
        rateTextView.visibility = View.GONE
        trendsSection.visibility = View.GONE
        
        val amountStr = amountEditText.text.toString()
        if (amountStr.isEmpty()) {
            showError("Please enter an amount")
            return
        }
        
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            showError("Please enter a valid amount")
            return
        }
        
        // Get selected currencies
        val fromCurrencyPosition = fromCurrencySpinner.selectedItemPosition
        val toCurrencyPosition = toCurrencySpinner.selectedItemPosition
        
        if (fromCurrencyPosition < 0 || toCurrencyPosition < 0) {
            showError("Please select currencies")
            return
        }
        
        val fromCurrency = currencyCodes[fromCurrencyPosition]
        val toCurrency = currencyCodes[toCurrencyPosition]
        
        // Log for debugging
        Log.d("CurrencyConverter", "Converting $amount from $fromCurrency to $toCurrency")
        
        // Show progress
        progressBar.visibility = View.VISIBLE
        
        // Create request
        val request = CurrencyConversionRequest(fromCurrency, toCurrency, amount)
        
        // Call API
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    showError("You need to be logged in")
                    return@launch
                }
                
                val response = RetrofitClient.currencyApiService.convertCurrency(
                    request, "Bearer $token"
                )
                
                // Log raw response for debugging
                Log.d("CurrencyConverter", "Response code: ${response.code()}")
                
                // Check if response was successful
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    
                    // Log successful result
                    Log.d("CurrencyConverter", "Conversion successful: $result")
                    
                    if (result.containsKey("conversion")) {
                        // New format with nested conversion object
                        @Suppress("UNCHECKED_CAST")
                        val conversion = result["conversion"] as Map<String, Any>
                        
                        val amount = (conversion["amount"] as Number).toDouble()
                        val convertedAmount = (conversion["convertedAmount"] as Number).toDouble()
                        val exchangeRate = (conversion["exchangeRate"] as Number).toDouble()
                        
                        // Format the result
                        val fromFormatter = NumberFormat.getCurrencyInstance(Locale.US)
                        fromFormatter.currency = Currency.getInstance(fromCurrency)
                        
                        val toFormatter = NumberFormat.getCurrencyInstance(Locale.US)
                        toFormatter.currency = Currency.getInstance(toCurrency)
                        
                        // Update UI
                        resultTextView.text = "${fromFormatter.format(amount)} = ${toFormatter.format(convertedAmount)}"
                        rateTextView.text = "1 $fromCurrency = ${exchangeRate.format(6)} $toCurrency"
                        
                        resultTextView.visibility = View.VISIBLE
                        rateTextView.visibility = View.VISIBLE
                        
                        // Process trends data if available
                        if (result.containsKey("trends")) {
                            @Suppress("UNCHECKED_CAST")
                            val trends = result["trends"] as Map<String, Any>
                            displayTrends(trends)
                        }
                    } else {
                        // Old format (direct values)
                        showError("Unexpected response format")
                    }
                } else {
                    // Handle unsuccessful response
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("CurrencyConverter", "Error response: $errorBody")
                    
                    if (errorBody.contains("<!DOCTYPE html>") || errorBody.contains("<html")) {
                        showError("Server returned HTML instead of JSON. Please check your connection.")
                    } else {
                        showError("Error: $errorBody")
                    }
                }
            } catch (e: HttpException) {
                Log.e("CurrencyConverter", "HTTP error: ${e.message()}", e)
                showError("Network error: ${e.message()}")
            } catch (e: Exception) {
                Log.e("CurrencyConverter", "Error: ${e.message}", e)
                showError("Error: ${e.message}")
                
                // Provide fallback calculation if possible
                val fallbackRate = getEstimatedRate(fromCurrency, toCurrency)
                if (fallbackRate > 0 && amount > 0) {
                    val convertedAmount = amount * fallbackRate
                    
                    // Format the result
                    val fromFormatter = NumberFormat.getCurrencyInstance(Locale.US)
                    fromFormatter.currency = Currency.getInstance(fromCurrency)
                    
                    val toFormatter = NumberFormat.getCurrencyInstance(Locale.US)
                    toFormatter.currency = Currency.getInstance(toCurrency)
                    
                    resultTextView.text = "${fromFormatter.format(amount)} ≈ ${toFormatter.format(convertedAmount)} (estimated)"
                    rateTextView.text = "1 $fromCurrency ≈ ${fallbackRate.format(6)} $toCurrency (estimated)"
                    
                    resultTextView.visibility = View.VISIBLE
                    rateTextView.visibility = View.VISIBLE
                }
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun fetchPopularCurrencies() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    // Just hide the popular currencies section
                    findViewById<TextView>(R.id.tvPopularCurrenciesHeader)?.visibility = View.GONE
                    return@launch
                }
                
                val response = RetrofitClient.currencyApiService.getPopularCurrencies("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val currencies = response.body()!!
                    if (currencies.isNotEmpty()) {
                        popularCurrencies.clear()
                        popularCurrencies.addAll(currencies)
                        
                        // Setup adapter
                        val adapter = PopularCurrencyAdapter(
                            this@CurrencyConverterActivity,
                            popularCurrencies
                        )
                        popularCurrenciesView.adapter = adapter
                    } else {
                        Log.d("CurrencyConverter", "No popular currencies returned")
                        // Use fallback data
                        useFallbackPopularCurrencies()
                    }
                } else {
                    Log.e("CurrencyConverter", "Failed to get popular currencies: ${response.code()}")
                    // Use fallback data
                    useFallbackPopularCurrencies()
                }
            } catch (e: Exception) {
                Log.e("CurrencyConverter", "Error fetching popular currencies: ${e.message}", e)
                // Use fallback data
                useFallbackPopularCurrencies()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun useFallbackPopularCurrencies() {
        // Create some fallback data
        val fallbackData = listOf(
            PopularCurrency("EUR", "Euro", 0.93, 0.25),
            PopularCurrency("GBP", "British Pound", 0.79, -0.15),
            PopularCurrency("JPY", "Japanese Yen", 149.8, 0.42),
            PopularCurrency("AUD", "Australian Dollar", 1.52, -0.33),
            PopularCurrency("PHP", "Philippine Peso", 56.5, 0.18)
        )
        
        popularCurrencies.clear()
        popularCurrencies.addAll(fallbackData)
        
        // Setup adapter
        val adapter = PopularCurrencyAdapter(
            this@CurrencyConverterActivity,
            popularCurrencies
        )
        popularCurrenciesView.adapter = adapter
    }
    
    private fun showError(message: String) {
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }
    
    // Helper function to get estimated exchange rates when API fails
    private fun getEstimatedRate(from: String, to: String): Double {
        val commonRates = mapOf(
            "USD" to 1.0,
            "EUR" to 0.93,
            "GBP" to 0.79,
            "JPY" to 149.8,
            "AUD" to 1.52,
            "CAD" to 1.37,
            "CHF" to 0.90,
            "CNY" to 7.24,
            "PHP" to 56.5
        )
        
        // If we have both rates against USD, we can calculate
        if (from == "USD" && commonRates.containsKey(to)) {
            return commonRates[to] ?: 0.0
        } else if (to == "USD" && commonRates.containsKey(from)) {
            return 1.0 / (commonRates[from] ?: 1.0)
        } else if (commonRates.containsKey(from) && commonRates.containsKey(to)) {
            // Cross rate calculation
            val fromToUsd = 1.0 / (commonRates[from] ?: 1.0)
            val usdToTo = commonRates[to] ?: 1.0
            return fromToUsd * usdToTo
        }
        
        return 0.0 // Can't estimate
    }
    
    // New method to display trends
    private fun displayTrends(trends: Map<String, Any>) {
        try {
            // Check if we have all three trend periods
            val periods = listOf("7days", "30days", "90days")
            val allPeriodsExist = periods.all { trends.containsKey(it) }
            
            if (!allPeriodsExist) {
                Log.d("CurrencyConverter", "Incomplete trend data")
                return
            }
            
            // Process 7 days trend
            @Suppress("UNCHECKED_CAST")
            val trend7days = trends["7days"] as Map<String, Any>
            setupTrendCard(trend7daysCard, "7 days", trend7days)
            
            // Process 30 days trend
            @Suppress("UNCHECKED_CAST")
            val trend30days = trends["30days"] as Map<String, Any>
            setupTrendCard(trend30daysCard, "30 days", trend30days)
            
            // Process 90 days trend
            @Suppress("UNCHECKED_CAST")
            val trend90days = trends["90days"] as Map<String, Any>
            setupTrendCard(trend90daysCard, "90 days", trend90days)
            
            // Make the trends section visible
            trendsSection.visibility = View.VISIBLE
            
        } catch (e: Exception) {
            Log.e("CurrencyConverter", "Error displaying trends: ${e.message}", e)
            // Don't show an error to the user, just log it
        }
    }
    
    private fun setupTrendCard(cardView: CardView, periodText: String, trendData: Map<String, Any>) {
        val periodTextView = cardView.findViewById<TextView>(R.id.tvPeriod)
        val changePercentTextView = cardView.findViewById<TextView>(R.id.tvChangePercent)
        val chartContainer = cardView.findViewById<LinearLayout>(R.id.chartContainer)
        
        // Set period text
        periodTextView.text = periodText
        
        // Set change percent with appropriate color and arrow
        val changePercent = (trendData["changePercent"] as Number).toDouble()
        val formattedPercent = String.format("%.2f%%", Math.abs(changePercent))
        
        if (changePercent > 0) {
            changePercentTextView.setTextColor(ContextCompat.getColor(this, R.color.green_700))
            changePercentTextView.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_arrow_up, 0, 0, 0)
            changePercentTextView.text = formattedPercent
        } else {
            changePercentTextView.setTextColor(ContextCompat.getColor(this, R.color.red_700))
            changePercentTextView.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_arrow_down, 0, 0, 0)
            changePercentTextView.text = formattedPercent
        }
        
        // Draw the chart
        @Suppress("UNCHECKED_CAST")
        val trendPoints = trendData["trendPoints"] as List<Double>
        drawTrendChart(chartContainer, trendPoints)
    }
    
    private fun drawTrendChart(container: LinearLayout, trendPoints: List<Double>) {
        // Clear previous chart bars
        container.removeAllViews()
        
        if (trendPoints.isEmpty()) return
        
        val maxPoint = trendPoints.maxOrNull() ?: 1.0
        val minPoint = trendPoints.minOrNull() ?: 0.0
        val range = maxPoint - minPoint
        
        // Ensure we don't divide by zero
        val safeRange = if (range < 0.0001) 0.0001 else range
        
        for (i in trendPoints.indices) {
            val point = trendPoints[i]
            val barView = layoutInflater.inflate(R.layout.view_trend_bar, container, false)
            
            // Calculate height percentage (20% minimum height, 100% maximum)
            val heightPercentage = 20 + ((point - minPoint) / safeRange * 80)
            
            // Set bar height
            val layoutParams = barView.layoutParams
            layoutParams.height = (heightPercentage * container.height / 100).toInt()
            
            // Last bar should be highlighted
            if (i == trendPoints.lastIndex) {
                barView.setBackgroundColor(ContextCompat.getColor(this, R.color.green_700))
            } else {
                barView.setBackgroundColor(ContextCompat.getColor(this, R.color.light_green_300))
            }
            
            container.addView(barView)
        }
    }
    
    // Extension function to format double
    private fun Double.format(digits: Int): String {
        return "%.${digits}f".format(this)
    }
    
    private fun swapCurrencies() {
        try {
            // Get current positions
            val fromPosition = fromCurrencySpinner.selectedItemPosition
            val toPosition = toCurrencySpinner.selectedItemPosition
            
            // Swap positions
            fromCurrencySpinner.setSelection(toPosition)
            toCurrencySpinner.setSelection(fromPosition)
            
            // Clear previous result since we've changed currencies
            resultTextView.visibility = View.GONE
            rateTextView.visibility = View.GONE
            
            // Animate the swap button
            swapCurrenciesButton.animate()
                .rotationBy(180f)
                .setDuration(300)
                .start()
            
            // Log the action
            Log.d("CurrencyConverter", "Currencies swapped: ${currencyCodes[toPosition]} <-> ${currencyCodes[fromPosition]}")
            
            // If amount is already entered, you might want to automatically convert with the new currencies
            if (amountEditText.text.toString().isNotEmpty()) {
                convertCurrency()
            }
        } catch (e: Exception) {
            Log.e("CurrencyConverter", "Error swapping currencies: ${e.message}", e)
            Toast.makeText(this, "Error swapping currencies", Toast.LENGTH_SHORT).show()
        }
    }
}
