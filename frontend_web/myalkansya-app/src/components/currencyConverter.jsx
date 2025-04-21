import React, { useState, useEffect } from "react";
import Sidebar from "./sidebar";
import TopBar from "./topbar";

// Add this near the top of your file, outside the component
const apiCache = {
  popularCurrencies: null,
  popularCurrenciesTimestamp: null,
  cacheDuration: 15 * 60 * 1000 // 15 minutes in milliseconds
};

const CurrencyConverter = () => {
  // Define currencyNames at the component level, outside of any function
  const currencyNames = {
    AED: "UAE Dirham",
    ARS: "Argentine Peso",
    AUD: "Australian Dollar",
    BGN: "Bulgarian Lev",
    BRL: "Brazilian Real",
    BSD: "Bahamian Dollar",
    CAD: "Canadian Dollar",
    CHF: "Swiss Franc",
    CLP: "Chilean Peso",
    CNY: "Chinese Yuan",
    COP: "Colombian Peso",
    CZK: "Czech Koruna",
    DKK: "Danish Krone",
    DOP: "Dominican Peso",
    EGP: "Egyptian Pound",
    EUR: "Euro",
    FJD: "Fijian Dollar",
    GBP: "British Pound",
    GTQ: "Guatemalan Quetzal",
    HKD: "Hong Kong Dollar",
    HRK: "Croatian Kuna",
    HUF: "Hungarian Forint",
    IDR: "Indonesian Rupiah",
    ILS: "Israeli Shekel",
    INR: "Indian Rupee",
    ISK: "Icelandic Króna",
    JPY: "Japanese Yen",
    KRW: "South Korean Won",
    KZT: "Kazakhstani Tenge",
    MXN: "Mexican Peso",
    MYR: "Malaysian Ringgit",
    NOK: "Norwegian Krone",
    NZD: "New Zealand Dollar",
    PAB: "Panamanian Balboa",
    PEN: "Peruvian Sol",
    PHP: "Philippine Peso",
    PKR: "Pakistani Rupee",
    PLN: "Polish Złoty",
    PYG: "Paraguayan Guaraní",
    RON: "Romanian Leu",
    RUB: "Russian Ruble",
    SAR: "Saudi Riyal",
    SEK: "Swedish Krona",
    SGD: "Singapore Dollar",
    THB: "Thai Baht",
    TRY: "Turkish Lira",
    TWD: "Taiwan Dollar",
    UAH: "Ukrainian Hryvnia",
    USD: "US Dollar",
    UYU: "Uruguayan Peso",
    ZAR: "South African Rand",
    BTC: "Bitcoin",
    ETH: "Ethereum",
    XRP: "Ripple",
    LTC: "Litecoin",
    BCH: "Bitcoin Cash"
  };

  const [amount, setAmount] = useState("");
  const [fromCurrency, setFromCurrency] = useState("USD");
  const [toCurrency, setToCurrency] = useState("PHP");
  const [conversionRate, setConversionRate] = useState(null);
  const [conversionResult, setConversionResult] = useState("");
  const [availableCurrencies, setAvailableCurrencies] = useState([]);
  const [popularCurrencies, setPopularCurrencies] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [conversionData, setConversionData] = useState(null);

  // API base URL - adjust this to match your backend location
  const API_BASE_URL = "http://localhost:8080";

  // Fetch available currencies when component mounts
  useEffect(() => {
    const fetchCurrencies = async () => {
      try {
        setIsLoading(true);
        
        // Add logging to debug API call
        console.log("Attempting to fetch currencies from:", `${API_BASE_URL}/api/currency/rates/USD`);
        
        const response = await fetch(`${API_BASE_URL}/api/currency/rates/USD`, {
          headers: {
            'Accept': 'application/json'
          }
        });
        
        // Log raw response
        console.log("API Response status:", response.status);
        
        // Check if response is actually JSON
        const contentType = response.headers.get("content-type");
        if (!contentType || !contentType.includes("application/json")) {
          console.error("Non-JSON response from API");
          throw new Error("Server returned non-JSON response. Check API endpoint.");
        }
        
        if (!response.ok) {
          const errorText = await response.text();
          throw new Error(`API error (${response.status}): ${errorText}`);
        }
        
        const data = await response.json();
        console.log("Currencies data:", data);
        
        // Create an array of currency objects from the rates
        // Filter to include only currencies that have a name in our list
        const currencies = Object.keys(data)
          .filter(code => currencyNames[code]) // Only include currencies with defined names
          .map(code => ({
            code,
            name: getCurrencyName(code)
          }));
        
        if (currencies.length > 0) {
          console.log("Found currencies:", currencies.length);
          setAvailableCurrencies(currencies);
        } else {
          console.warn("No currencies found in API response, using fallbacks");
          throw new Error("No currencies found in API response");
        }
      } catch (error) {
        console.error("Error fetching currencies:", error);
        setError(`Failed to load all currencies: ${error.message}`);
        
        // Always use these fallback currencies to ensure the component works
        const fallbackCurrencies = [
          { code: "USD", name: "US Dollar" },
          { code: "EUR", name: "Euro" },
          { code: "JPY", name: "Japanese Yen" },
          { code: "GBP", name: "British Pound" },
          { code: "PHP", name: "Philippine Peso" },
          { code: "AUD", name: "Australian Dollar" },
          { code: "CAD", name: "Canadian Dollar" },
          { code: "CHF", name: "Swiss Franc" },
          { code: "CNY", name: "Chinese Yuan" },
          { code: "SGD", name: "Singapore Dollar" }
        ];
        
        setAvailableCurrencies(fallbackCurrencies);
      } finally {
        setIsLoading(false);
      }
    };

    const fetchPopularCurrencies = async () => {
      try {
        // Check if we have valid cached data
        const now = Date.now();
        if (apiCache.popularCurrencies && 
            apiCache.popularCurrenciesTimestamp && 
            now - apiCache.popularCurrenciesTimestamp < apiCache.cacheDuration) {
          // Use cached data if it's less than cacheDuration old
          console.log("Using cached popular currencies data");
          setPopularCurrencies(apiCache.popularCurrencies);
          return;
        }

        const response = await fetch(`${API_BASE_URL}/api/currency/popular`, {
          headers: {
            'Accept': 'application/json'
          }
        });
        
        if (!response.ok) {
          // If endpoint doesn't exist yet, silently fail and use dummy data
          throw new Error(`API error (${response.status})`);
        }
        
        const data = await response.json();
        
        // Cache the response and timestamp
        apiCache.popularCurrencies = data;
        apiCache.popularCurrenciesTimestamp = now;
        
        setPopularCurrencies(data);
      } catch (error) {
        console.error("Error fetching popular currencies:", error);
        
        // Use fallback data if no cached data available
        if (!apiCache.popularCurrencies) {
          const fallbackData = [
            { code: "EUR", name: "Euro", rate: 0.93, changePercent: 0.25 },
            { code: "GBP", name: "British Pound", rate: 0.79, changePercent: -0.15 },
            { code: "JPY", name: "Japanese Yen", rate: 149.8, changePercent: 0.42 },
            { code: "AUD", name: "Australian Dollar", rate: 1.52, changePercent: -0.33 },
            { code: "PHP", name: "Philippine Peso", rate: 56.5, changePercent: 0.18 }
          ];
          apiCache.popularCurrencies = fallbackData;
          apiCache.popularCurrenciesTimestamp = now;
          setPopularCurrencies(fallbackData);
        }
      }
    };

    fetchCurrencies();
    fetchPopularCurrencies();
  }, []);

  // Fetch exchange rate whenever currencies change
  useEffect(() => {
    const fetchExchangeRate = async () => {
      if (fromCurrency && toCurrency) {
        try {
          const response = await fetch(`${API_BASE_URL}/api/currency/rate?from=${fromCurrency}&to=${toCurrency}`, {
            headers: {
              'Accept': 'application/json'
            }
          });
          
          // Check if response is actually JSON
          const contentType = response.headers.get("content-type");
          if (!contentType || !contentType.includes("application/json")) {
            throw new Error("Server returned non-JSON response. Check API endpoint.");
          }
          
          if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`API error (${response.status}): ${errorText}`);
          }
          
          const data = await response.json();
          console.log("Exchange rate data:", data);
          setConversionRate(data.rate);
        } catch (error) {
          console.error("Error fetching exchange rate:", error);
          // Don't show error to user for this one, just log it
          // We'll use a fallback rate instead
          setConversionRate(null);
        }
      }
    };

    fetchExchangeRate();
  }, [fromCurrency, toCurrency]);

  const handleConvert = async () => {
    if (!amount || isNaN(amount) || amount <= 0) {
      setError("Please enter a valid amount");
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/currency/convert`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify({
          fromCurrency,
          toCurrency,
          amount: parseFloat(amount)
        })
      });
      
      // Check if response is actually JSON
      const contentType = response.headers.get("content-type");
      if (!contentType || !contentType.includes("application/json")) {
        const text = await response.text();
        console.error("Non-JSON response:", text);
        throw new Error("Server returned non-JSON response. Check API endpoint.");
      }

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || `Error code: ${response.status}`);
      }

      const data = await response.json();
      console.log("Conversion response:", data);
      
      // Store the full data including trends
      setConversionData(data);
      
      // Extract the basic conversion result
      if (data.conversion) {
        // New format with enhanced response
        setConversionResult(`${data.conversion.convertedAmount.toFixed(2)} ${toCurrency}`);
        setConversionRate(data.conversion.exchangeRate);
      } else {
        // Old format for backwards compatibility
        setConversionResult(`${data.convertedAmount.toFixed(2)} ${toCurrency}`);
        setConversionRate(data.exchangeRate);
      }
    } catch (error) {
      setError(error.message || "Conversion failed. Please try again.");
      console.error("Error during conversion:", error);
      
      // If no specific result, provide a fallback calculation (just for UI)
      if (!conversionResult && conversionRate) {
        const estimatedAmount = (parseFloat(amount) * conversionRate).toFixed(2);
        setConversionResult(`~${estimatedAmount} ${toCurrency} (estimated)`);
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Helper function to get currency names - now just uses the object defined at component level
  const getCurrencyName = (code) => {
    return currencyNames[code] || code;
  };

  // Helper function for currency emojis - expanded with all supported currencies
const getCurrencyEmoji = (code) => {
  const currencyEmojis = {
    AED: "🇦🇪",
    ARS: "🇦🇷",
    AUD: "🇦🇺",
    BGN: "🇧🇬",
    BRL: "🇧🇷",
    BSD: "🇧🇸",
    CAD: "🇨🇦",
    CHF: "🇨🇭",
    CLP: "🇨🇱",
    CNY: "🇨🇳",
    COP: "🇨🇴",
    CZK: "🇨🇿",
    DKK: "🇩🇰",
    DOP: "🇩🇴",
    EGP: "🇪🇬",
    EUR: "🇪🇺",
    FJD: "🇫🇯",
    GBP: "🇬🇧",
    GTQ: "🇬🇹",
    HKD: "🇭🇰",
    HRK: "🇭🇷",
    HUF: "🇭🇺",
    IDR: "🇮🇩",
    ILS: "🇮🇱",
    INR: "🇮🇳",
    ISK: "🇮🇸",
    JPY: "🇯🇵",
    KRW: "🇰🇷",
    KZT: "🇰🇿",
    MXN: "🇲🇽",
    MYR: "🇲🇾",
    NOK: "🇳🇴",
    NZD: "🇳🇿",
    PAB: "🇵🇦",
    PEN: "🇵🇪",
    PHP: "🇵🇭",
    PKR: "🇵🇰",
    PLN: "🇵🇱",
    PYG: "🇵🇾",
    RON: "🇷🇴",
    RUB: "🇷🇺",
    SAR: "🇸🇦",
    SEK: "🇸🇪",
    SGD: "🇸🇬",
    THB: "🇹🇭",
    TRY: "🇹🇷",
    TWD: "🇹🇼",
    UAH: "🇺🇦",
    USD: "🇺🇸",
    UYU: "🇺🇾",
    ZAR: "🇿🇦",
    BTC: "₿",
    ETH: "Ξ",
    XRP: "✕",
    LTC: "Ł",
    BCH: "Ƀ"
  };
  
  // For debugging - log when emoji isn't found
  if (!currencyEmojis[code]) {
    console.log(`No emoji found for currency: ${code}`);
  }
  
  return currencyEmojis[code] || "💱";
};

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="currencyconverter" />

        {/* Main Content */}
        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA]">
          <h1 className="text-3xl font-bold text-[#18864F] mb-6">Currency Converter</h1>

          {/* API status indicator */}
          {error && error.includes("API") && (
            <div className="bg-yellow-100 border border-yellow-400 text-yellow-800 px-4 py-3 rounded mb-4 flex items-center">
              <svg className="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
              API connection issue. Using offline mode.
            </div>
          )}

          {/* Converter Section */}
          <div className="bg-white p-6 rounded-lg shadow-md mb-6">
            {error && !error.includes("API") && (
              <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                {error}
              </div>
            )}
            
            <div className="grid grid-cols-3 gap-4 items-center">
              {/* Enter Amount */}
              <div>
                <label className="block text-[#18864F] font-bold mb-2">Enter Amount</label>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="w-full p-3 border rounded-md bg-[#FEF6EA] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  placeholder="Enter amount"
                />
              </div>

              {/* From Currency */}
              <div>
                <label className="block text-[#18864F] font-bold mb-2">From</label>
                <div className="flex items-center gap-2">
                  <span className="text-2xl" style={{ minWidth: '28px', display: 'inline-block' }}>
                    {getCurrencyEmoji(fromCurrency)}
                  </span>
                  <select
                    value={fromCurrency}
                    onChange={(e) => setFromCurrency(e.target.value)}
                    className="w-full p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  >
                    {availableCurrencies.map(currency => (
                      <option key={currency.code} value={currency.code}>
                        {currency.code} - {currency.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* To Currency */}
              <div>
                <label className="block text-[#18864F] font-bold mb-2">To</label>
                <div className="flex items-center gap-2">
                  <span className="text-2xl" style={{ minWidth: '28px', display: 'inline-block' }}>
                    {getCurrencyEmoji(toCurrency)}
                  </span>
                  <select
                    value={toCurrency}
                    onChange={(e) => setToCurrency(e.target.value)}
                    className="w-full p-3 border rounded-md bg-[#18864F] text-white font-bold focus:outline-none focus:ring-2 focus:ring-[#FFC107]"
                  >
                    {availableCurrencies.map(currency => (
                      <option key={currency.code} value={currency.code}>
                        {currency.code} - {currency.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* Convert Button */}
            <div className="flex justify-center mt-6">
              <button
                onClick={handleConvert}
                disabled={isLoading}
                className={`${
                  isLoading ? "bg-gray-400" : "bg-[#18864F] hover:bg-green-700"
                } text-white font-bold py-2 px-6 rounded-md transition duration-300`}
              >
                {isLoading ? "Converting..." : "Convert"}
              </button>
            </div>
          </div>

          {/* Conversion Result */}
          <div className="bg-white p-6 rounded-lg shadow-md">
            <h2 className="text-2xl font-bold text-[#18864F] mb-4">
              {fromCurrency} to {toCurrency}
            </h2>
            <p className="text-lg text-[#18864F] font-bold">
              {conversionResult || "Enter an amount to see the result"}
            </p>
            {conversionRate && (
              <p className="text-sm text-gray-500 mt-2">
                Exchange rate: 1 {fromCurrency} = {conversionRate.toFixed(6)} {toCurrency}
              </p>
            )}

            {/* Historical Trends */}
            {conversionData && conversionData.trends && (
              <div className="mt-4">
                <h3 className="text-lg font-semibold text-[#18864F] mb-2">Historical Trends</h3>
                <div className="grid grid-cols-3 gap-4 mt-2">
                  {/* 7-day trend */}
                  <div className="bg-[#FEF6EA] p-3 rounded-lg">
                    <div className="flex justify-between items-center">
                      <span className="text-gray-600 text-sm">7 days</span>
                      {conversionData.trends["7days"].changePercent > 0 ? (
                        <span className="text-green-600 font-medium flex items-center">
                          <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 15l7-7 7 7"></path>
                          </svg>
                          {Math.abs(conversionData.trends["7days"].changePercent).toFixed(2)}%
                        </span>
                      ) : (
                        <span className="text-red-600 font-medium flex items-center">
                          <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path>
                          </svg>
                          {Math.abs(conversionData.trends["7days"].changePercent).toFixed(2)}%
                        </span>
                      )}
                    </div>
                    <div className="h-10 mt-1 w-full">
                      {/* Simple chart visualization using trend points */}
                      <div className="flex items-end h-full w-full">
                        {conversionData.trends["7days"].trendPoints.map((point, index) => (
                          <div
                            key={index}
                            className={`flex-1 mx-px ${
                              index === conversionData.trends["7days"].trendPoints.length - 1
                                ? "bg-[#18864F]"
                                : "bg-[#A5D6B7]"
                            }`}
                            style={{
                              height: `${Math.max(20, Math.min(100, (point / conversionData.trends["7days"].trendPoints[0]) * 80))}%`,
                            }}
                          ></div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* 30-day trend */}
                  <div className="bg-[#FEF6EA] p-3 rounded-lg">
                    <div className="flex justify-between items-center">
                      <span className="text-gray-600 text-sm">30 days</span>
                      {conversionData.trends["30days"].changePercent > 0 ? (
                        <span className="text-green-600 font-medium flex items-center">
                          <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 15l7-7 7 7"></path>
                          </svg>
                          {Math.abs(conversionData.trends["30days"].changePercent).toFixed(2)}%
                        </span>
                      ) : (
                        <span className="text-red-600 font-medium flex items-center">
                          <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path>
                          </svg>
                          {Math.abs(conversionData.trends["30days"].changePercent).toFixed(2)}%
                        </span>
                      )}
                    </div>
                    <div className="h-10 mt-1 w-full">
                      <div className="flex items-end h-full w-full">
                        {conversionData.trends["30days"].trendPoints.map((point, index) => (
                          <div
                            key={index}
                            className={`flex-1 mx-px ${
                              index === conversionData.trends["30days"].trendPoints.length - 1
                                ? "bg-[#18864F]"
                                : "bg-[#A5D6B7]"
                            }`}
                            style={{
                              height: `${Math.max(20, Math.min(100, (point / conversionData.trends["30days"].trendPoints[0]) * 80))}%`,
                            }}
                          ></div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* 90-day trend */}
                  <div className="bg-[#FEF6EA] p-3 rounded-lg">
                    <div className="flex justify-between items-center">
                      <span className="text-gray-600 text-sm">90 days</span>
                      {conversionData.trends["90days"].changePercent > 0 ? (
                        <span className="text-green-600 font-medium flex items-center">
                          <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 15l7-7 7 7"></path>
                          </svg>
                          {Math.abs(conversionData.trends["90days"].changePercent).toFixed(2)}%
                        </span>
                      ) : (
                        <span className="text-red-600 font-medium flex items-center">
                          <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path>
                          </svg>
                          {Math.abs(conversionData.trends["90days"].changePercent).toFixed(2)}%
                        </span>
                      )}
                    </div>
                    <div className="h-10 mt-1 w-full">
                      <div className="flex items-end h-full w-full">
                        {conversionData.trends["90days"].trendPoints.map((point, index) => (
                          <div
                            key={index}
                            className={`flex-1 mx-px ${
                              index === conversionData.trends["90days"].trendPoints.length - 1
                                ? "bg-[#18864F]"
                                : "bg-[#A5D6B7]"
                            }`}
                            style={{
                              height: `${Math.max(20, Math.min(100, (point / conversionData.trends["90days"].trendPoints[0]) * 80))}%`,
                            }}
                          ></div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
            
            <p className="text-sm text-gray-500 mt-4">
              Data is updated hourly from ExchangeRate-API.
            </p>
          </div>

          {/* Popular Currencies Table */}
          <div className="bg-white p-6 rounded-lg shadow-md mt-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-2xl font-bold text-[#18864F]">Popular Currencies</h2>
              {apiCache.popularCurrenciesTimestamp && (
                <span className="text-xs text-gray-500">
                  Last updated: {new Date(apiCache.popularCurrenciesTimestamp).toLocaleTimeString()}
                </span>
              )}
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full bg-white">
                <thead>
                  <tr className="bg-[#FEF6EA] text-[#18864F]">
                    <th className="py-3 px-4 text-left">Currency</th>
                    <th className="py-3 px-4 text-right">Rate (USD)</th>
                    <th className="py-3 px-4 text-right">Change (1w)</th>
                  </tr>
                </thead>
                <tbody>
                  {popularCurrencies.map((currency) => (
                    <tr key={currency.code} className="border-b hover:bg-gray-50">
                      <td className="py-3 px-4">
                        <div className="flex items-center">
                          <span className="text-xl mr-2" style={{ minWidth: '28px', display: 'inline-block' }}>
                            {getCurrencyEmoji(currency.code)}
                          </span>
                          <div>
                            <div className="font-medium">{currency.code}</div>
                            <div className="text-gray-500 text-sm">{currency.name}</div>
                          </div>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-right font-medium">
                        {currency.rate.toFixed(4)}
                      </td>
                      <td className={`py-3 px-4 text-right font-medium ${
                        currency.changePercent > 0 
                          ? 'text-green-600' 
                          : currency.changePercent < 0 
                            ? 'text-red-600' 
                            : 'text-gray-600'
                      }`}>
                        <div className="flex items-center justify-end">
                          {currency.changePercent > 0 ? (
                            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 15l7-7 7 7"></path>
                            </svg>
                          ) : currency.changePercent < 0 ? (
                            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path>
                            </svg>
                          ) : (
                            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 12h14"></path>
                            </svg>
                          )}
                          {Math.abs(currency.changePercent).toFixed(2)}%
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CurrencyConverter;