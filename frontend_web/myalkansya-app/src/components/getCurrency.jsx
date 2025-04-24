/**
 * Currency utilities for MyAlkansya
 * Contains reusable functions for currency handling across components
 */

/**
 * Maps currency codes to their full names
 * @param {string} code - Currency code (e.g. USD, PHP)
 * @returns {string} The full name of the currency
 */
export const getCurrencyName = (code) => {
  const currencyNames = {
    // Most popular currencies first
    USD: "US Dollar",
    EUR: "Euro",
    JPY: "Japanese Yen",
    GBP: "British Pound",
    PHP: "Philippine Peso",
    AUD: "Australian Dollar",
    CAD: "Canadian Dollar",
    CHF: "Swiss Franc",
    CNY: "Chinese Yuan",
    SGD: "Singapore Dollar",
    // Cryptocurrencies
    BTC: "Bitcoin",
    ETH: "Ethereum",
    XRP: "Ripple",
    LTC: "Litecoin",
    BCH: "Bitcoin Cash",
    // Remaining currencies alphabetically
    AED: "UAE Dirham",
    ARS: "Argentine Peso",
    BGN: "Bulgarian Lev",
    BRL: "Brazilian Real",
    BSD: "Bahamian Dollar",
    CLP: "Chilean Peso",
    COP: "Colombian Peso",
    CZK: "Czech Koruna",
    DKK: "Danish Krone",
    DOP: "Dominican Peso",
    EGP: "Egyptian Pound",
    FJD: "Fijian Dollar",
    GTQ: "Guatemalan Quetzal",
    HKD: "Hong Kong Dollar",
    HRK: "Croatian Kuna",
    HUF: "Hungarian Forint",
    IDR: "Indonesian Rupiah",
    ILS: "Israeli Shekel",
    INR: "Indian Rupee",
    ISK: "Icelandic Króna",
    KRW: "South Korean Won",
    KZT: "Kazakhstani Tenge",
    MXN: "Mexican Peso",
    MYR: "Malaysian Ringgit",
    NOK: "Norwegian Krone",
    NZD: "New Zealand Dollar",
    PAB: "Panamanian Balboa",
    PEN: "Peruvian Sol",
    PKR: "Pakistani Rupee",
    PLN: "Polish Złoty",
    PYG: "Paraguayan Guaraní",
    RON: "Romanian Leu",
    RUB: "Russian Ruble",
    SAR: "Saudi Riyal",
    SEK: "Swedish Krona",
    THB: "Thai Baht",
    TRY: "Turkish Lira",
    TWD: "Taiwan Dollar",
    UAH: "Ukrainian Hryvnia",
    UYU: "Uruguayan Peso",
    ZAR: "South African Rand"
  };
  
  return currencyNames[code] || code;
};

/**
 * Maps currency codes to their flag emojis
 * @param {string} code - Currency code (e.g. USD, PHP)
 * @returns {string} The emoji flag representing the currency
 */
export const getCurrencyEmoji = (code) => {
  const currencyEmojis = {
    // Most popular currencies first
    USD: "🇺🇸",
    EUR: "🇪🇺",
    JPY: "🇯🇵",
    GBP: "🇬🇧",
    PHP: "🇵🇭",
    AUD: "🇦🇺",
    CAD: "🇨🇦",
    CHF: "🇨🇭",
    CNY: "🇨🇳",
    SGD: "🇸🇬",
    // Cryptocurrencies
    BTC: "₿",
    ETH: "Ξ",
    XRP: "✕",
    LTC: "Ł",
    BCH: "Ƀ",
    // Remaining currencies alphabetically
    AED: "🇦🇪",
    ARS: "🇦🇷",
    BGN: "🇧🇬",
    BRL: "🇧🇷",
    BSD: "🇧🇸",
    CLP: "🇨🇱",
    COP: "🇨🇴",
    CZK: "🇨🇿",
    DKK: "🇩🇰",
    DOP: "🇩🇴",
    EGP: "🇪🇬",
    FJD: "🇫🇯",
    GTQ: "🇬🇹",
    HKD: "🇭🇰",
    HRK: "🇭🇷",
    HUF: "🇭🇺",
    IDR: "🇮🇩",
    ILS: "🇮🇱",
    INR: "🇮🇳",
    ISK: "🇮🇸",
    KRW: "🇰🇷",
    KZT: "🇰🇿",
    MXN: "🇲🇽",
    MYR: "🇲🇾",
    NOK: "🇳🇴",
    NZD: "🇳🇿",
    PAB: "🇵🇦",
    PEN: "🇵🇪",
    PKR: "🇵🇰",
    PLN: "🇵🇱",
    PYG: "🇵🇾",
    RON: "🇷🇴",
    RUB: "🇷🇺",
    SAR: "🇸🇦",
    SEK: "🇸🇪",
    THB: "🇹🇭",
    TRY: "🇹🇷",
    TWD: "🇹🇼",
    UAH: "🇺🇦",
    UYU: "🇺🇾",
    ZAR: "🇿🇦"
  };
  
  return currencyEmojis[code] || "💱";
};

/**
 * Fetches available currencies from the backend API
 * @param {function} setAvailableCurrencies - State setter function for currencies
 * @returns {Promise} - Promise that resolves when currencies are fetched
 */
export const fetchAvailableCurrencies = async (setAvailableCurrencies) => {
  try {
    console.log("Fetching available currencies...");
    const response = await fetch("http://localhost:8080/api/currency/rates/USD", {
      headers: { 
        'Accept': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error(`Failed to fetch currencies: ${response.status}`);
    }
    
    const data = await response.json();
      
    if (data) {
      // Define priority currencies in order we want them displayed
      const priorityCurrencies = [
        "USD", "EUR", "JPY", "GBP", "PHP", 
        "AUD", "CAD", "CHF", "CNY", "SGD",
        "BTC", "ETH", "XRP", "LTC", "BCH"
      ];

      const currencies = Object.keys(data)
        .filter(code => {
          const name = getCurrencyName(code);
          return name && name !== code;
        })
        .map(code => ({
          code,
          name: getCurrencyName(code),
          // Assign a priority value - lower number = higher priority
          priority: priorityCurrencies.indexOf(code) >= 0 
            ? priorityCurrencies.indexOf(code) 
            : 1000 // Non-priority currencies go at the end
        }))
        .sort((a, b) => a.priority - b.priority) // Sort by priority
        .map(({ code, name }) => ({ code, name })); // Remove priority from final objects
      
      console.log(`Loaded ${currencies.length} currencies, sorted by priority`);
      setAvailableCurrencies(currencies);
    }
  } catch (err) {
    console.error("Error fetching available currencies:", err);
    // Fallback to basic currencies if API fails
    setAvailableCurrencies([
      { code: "PHP", name: "Philippine Peso" },
      { code: "USD", name: "US Dollar" },
      { code: "EUR", name: "Euro" },
      { code: "JPY", name: "Japanese Yen" },
      { code: "GBP", name: "British Pound" },
      { code: "AUD", name: "Australian Dollar" },
      { code: "CAD", name: "Canadian Dollar" },
      { code: "SGD", name: "Singapore Dollar" },
      { code: "CNY", name: "Chinese Yuan" }
    ]);
  }
};