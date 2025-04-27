package com.example.myalkansyamobile.utils

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
}
