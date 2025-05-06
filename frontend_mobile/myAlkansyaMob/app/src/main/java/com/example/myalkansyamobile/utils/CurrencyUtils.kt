package com.example.myalkansyamobile.utils

import android.util.Log
import com.example.myalkansyamobile.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * Utility class for currency-related operations
 */
object CurrencyUtils {

    // Map of currency codes to their names
    val currencyNames = mapOf(
        "USD" to "US Dollar",
        "EUR" to "Euro",
        "JPY" to "Japanese Yen",
        "GBP" to "British Pound",
        "PHP" to "Philippine Peso",
        "AUD" to "Australian Dollar",
        "CAD" to "Canadian Dollar",
        "CHF" to "Swiss Franc",
        "CNY" to "Chinese Yuan",
        "SGD" to "Singapore Dollar",
        "BTC" to "Bitcoin",
        "ETH" to "Ethereum",
        "XRP" to "Ripple",
        "LTC" to "Litecoin",
        "BCH" to "Bitcoin Cash",
        "AED" to "UAE Dirham",
        "ARS" to "Argentine Peso",
        "BGN" to "Bulgarian Lev",
        "BRL" to "Brazilian Real",
        "BSD" to "Bahamian Dollar",
        "CLP" to "Chilean Peso",
        "COP" to "Colombian Peso",
        "CZK" to "Czech Koruna",
        "DKK" to "Danish Krone",
        "DOP" to "Dominican Peso",
        "EGP" to "Egyptian Pound",
        "FJD" to "Fijian Dollar",
        "GTQ" to "Guatemalan Quetzal",
        "HKD" to "Hong Kong Dollar",
        "HRK" to "Croatian Kuna",
        "HUF" to "Hungarian Forint",
        "IDR" to "Indonesian Rupiah",
        "ILS" to "Israeli Shekel",
        "INR" to "Indian Rupee",
        "ISK" to "Icelandic Króna",
        "KRW" to "South Korean Won",
        "KZT" to "Kazakhstani Tenge",
        "MXN" to "Mexican Peso",
        "MYR" to "Malaysian Ringgit",
        "NOK" to "Norwegian Krone",
        "NZD" to "New Zealand Dollar",
        "PAB" to "Panamanian Balboa",
        "PEN" to "Peruvian Sol",
        "PKR" to "Pakistani Rupee",
        "PLN" to "Polish Złoty",
        "PYG" to "Paraguayan Guaraní",
        "RON" to "Romanian Leu",
        "RUB" to "Russian Ruble",
        "SAR" to "Saudi Riyal",
        "SEK" to "Swedish Krona",
        "THB" to "Thai Baht",
        "TRY" to "Turkish Lira",
        "TWD" to "Taiwan Dollar",
        "UAH" to "Ukrainian Hryvnia",
        "UYU" to "Uruguayan Peso",
        "ZAR" to "South African Rand"
    )

    // Currency codes for spinner
    val currencyCodes = listOf(
        "USD", "EUR", "JPY", "GBP", "PHP", "AUD", "CAD", "CHF", "CNY", "SGD",
        "AED", "ARS", "BGN", "BRL", "INR", "MXN", "RUB", "ZAR"
    )

    // Get currency name from code
    fun getCurrencyName(code: String): String {
        return currencyNames[code] ?: code
    }

    // Format amount with currency symbol
    fun formatWithCurrency(amount: Double, currencyCode: String): String {
        val currencyFormat = NumberFormat.getCurrencyInstance()
        try {
            currencyFormat.currency = Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            currencyFormat.currency = Currency.getInstance("USD")
        }
        return currencyFormat.format(amount)
    }
    
    // Format amount with just 2 decimal places
    fun formatAmount(amount: Double): String {
        return DecimalFormat("#,##0.00").format(amount)
    }

    // Get currency display text for spinners (e.g. "USD - US Dollar")
    fun getCurrencyDisplayText(currencyCode: String): String {
        val name = getCurrencyName(currencyCode)
        return "$currencyCode - $name"
    }

    // Convert currency amount
    suspend fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        authToken: String
    ): Double? = withContext(Dispatchers.IO) {
        try {
            if (fromCurrency == toCurrency) return@withContext amount
            
            val response = RetrofitClient.currencyApiService.getExchangeRate(
                fromCurrency, 
                toCurrency,
                "Bearer $authToken"
            )
            
            if (response.isSuccessful && response.body() != null) {
                val rate = response.body()?.get("rate") ?: return@withContext null
                val convertedAmount = amount * rate
                return@withContext BigDecimal(convertedAmount)
                    .setScale(2, RoundingMode.HALF_EVEN)
                    .toDouble()
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    // Get currency flag emoji
    fun getCurrencyEmoji(code: String): String {
        return when (code) {
            "USD" -> "🇺🇸"
            "EUR" -> "🇪🇺"
            "JPY" -> "🇯🇵"
            "GBP" -> "🇬🇧"
            "PHP" -> "🇵🇭"
            "AUD" -> "🇦🇺"
            "CAD" -> "🇨🇦"
            "CHF" -> "🇨🇭"
            "CNY" -> "🇨🇳"
            "SGD" -> "🇸🇬"
            "AED" -> "🇦🇪"
            "ARS" -> "🇦🇷"
            "BRL" -> "🇧🇷"
            "INR" -> "🇮🇳"
            "MXN" -> "🇲🇽"
            "RUB" -> "🇷🇺"
            "ZAR" -> "🇿🇦"
            "BTC" -> "₿"
            "ETH" -> "Ξ"
            else -> "💱"
        }
    }
    
    /**
     * Direct mapping of currency codes to their symbols for consistent display
     */
    private val currencySymbols = mapOf(
        "PHP" to "₱",
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "CNY" to "¥",
        "INR" to "₹",
        "KRW" to "₩",
        "BTC" to "₿",
        "THB" to "฿",
        "RUB" to "₽",
        "TRY" to "₺",
        "MYR" to "RM",
        "SGD" to "S$",
        "HKD" to "HK$",
        "AUD" to "A$",
        "CAD" to "C$",
        "NZD" to "NZ$",
        "ZAR" to "R",
        "BRL" to "R$",
        "MXN" to "Mex$",
        "CHF" to "Fr",
        "SEK" to "kr",
        "NOK" to "kr",
        "DKK" to "kr",
        "IDR" to "Rp",
        "AED" to "د.إ",
        "ARS" to "$",
        "BGN" to "лв",
        "BSD" to "B$",
        "CLP" to "$",
        "COP" to "$",
        "CZK" to "Kč",
        "DOP" to "RD$",
        "EGP" to "E£",
        "FJD" to "FJ$",
        "GTQ" to "Q",
        "HRK" to "kn",
        "HUF" to "Ft",
        "ILS" to "₪",
        "ISK" to "kr",
        "KZT" to "₸",
        "PAB" to "B/.",
        "PEN" to "S/.",
        "PKR" to "₨",
        "PLN" to "zł",
        "PYG" to "₲",
        "RON" to "lei",
        "SAR" to "﷼",
        "TWD" to "NT$",
        "UAH" to "₴",
        "UYU" to "SU"
        // Add more as needed
    )
    
    /**
     * Get currency symbol directly (more reliable than locale-based)
     */
    fun getCurrencySymbol(currencyCode: String): String {
        val code = currencyCode.uppercase()
        // Log for debugging
        Log.d("CurrencyUtils", "Getting symbol for currency: $code")
        val symbol = currencySymbols[code]
        
        if (symbol == null) {
            Log.w("CurrencyUtils", "WARNING: No symbol found for currency $code, defaulting to code itself")
            return code
        }
        
        Log.d("CurrencyUtils", "Symbol found for $code: $symbol")
        return symbol
    }
    
    /**
     * Get a properly configured NumberFormat for a specific currency
     * This handles locale selection to get the right currency symbols
     */
    fun getCurrencyFormatter(currencyCode: String): NumberFormat {
        // Select appropriate locale for the currency to get correct symbols
        val locale = when (currencyCode) {
            "PHP" -> Locale("fil", "PH") // Filipino locale for Philippine Peso
            "USD" -> Locale.US
            "EUR" -> Locale.GERMANY
            "GBP" -> Locale.UK
            "JPY" -> Locale.JAPAN
            "CNY" -> Locale.CHINA
            "AUD" -> Locale("en", "AU")
            "CAD" -> Locale("en", "CA")
            "INR" -> Locale("en", "IN")
            "MXN" -> Locale("es", "MX")
            "BRL" -> Locale("pt", "BR")
            "RUB" -> Locale("ru", "RU")
            "ZAR" -> Locale("en", "ZA")
            "CHF" -> Locale("de", "CH")
            "SGD" -> Locale("en", "SG")
            "HKD" -> Locale("zh", "HK")
            "NZD" -> Locale("en", "NZ")
            "SEK" -> Locale("sv", "SE")
            "KRW" -> Locale("ko", "KR")
            "NOK" -> Locale("no", "NO")
            "TRY" -> Locale("tr", "TR")
            else -> Locale.US // Default to US if no specific locale
        }
        
        val formatter = NumberFormat.getCurrencyInstance(locale)
        try {
            formatter.currency = Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            Log.w("CurrencyUtils", "Error setting currency $currencyCode: ${e.message}")
        }
        
        return formatter
    }
    
    /**
     * Format amount with proper currency symbol based on currency code
     * This version uses direct symbol mapping rather than locale-based formatting
     * to ensure consistent display across platforms
     */
    fun formatWithProperCurrency(amount: Double, currencyCode: String): String {
        // Use our direct symbol mapping instead of formatter
        val code = currencyCode.uppercase()
        val symbol = getCurrencySymbol(code)
        val formatter = DecimalFormat("#,##0.00")
        val formatted = "$symbol${formatter.format(amount)}"
        
        // Log for debugging
        Log.d("CurrencyUtils", "Formatted $amount $code as: $formatted")
        return formatted
    }
    
    /**
     * Simple format with currency code (for CSV export)
     * Format: "PHP 1,000.00" instead of "₱1,000.00"
     */
    fun formatWithCurrencyCode(amount: Double, currencyCode: String): String {
        val formatter = DecimalFormat("#,##0.00")
        return "$currencyCode ${formatter.format(amount)}"
    }
}
