package edu.cit.myalkansya.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.myalkansya.dto.CurrencyConversionRequest;
import edu.cit.myalkansya.dto.CurrencyConversionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ExchangeRateService {
    
    private static final Logger logger = Logger.getLogger(ExchangeRateService.class.getName());
    
    @Value("${currency.api.key}")
    private String apiKey;
    
    @Value("${currency.api.url}")
    private String apiUrlTemplate;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Cache for exchange rates to reduce API calls
    private final Map<String, Map<String, Double>> ratesCache = new HashMap<>();
    private final Map<String, Long> cacheTimestamps = new HashMap<>();
    private static final long CACHE_DURATION = 1000 * 60 * 60; // 1 hour
    
    public ExchangeRateService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public CurrencyConversionResponse convertCurrency(CurrencyConversionRequest request) {
        try {
            logger.info("Converting " + request.getAmount() + " " + 
                      request.getFromCurrency() + " to " + request.getToCurrency());
            
            String fromCurrency = request.getFromCurrency().toUpperCase();
            String toCurrency = request.getToCurrency().toUpperCase();
            double amount = request.getAmount();
            
            // Check cache first
            double exchangeRate = getExchangeRate(fromCurrency, toCurrency);
            
            CurrencyConversionResponse response = new CurrencyConversionResponse();
            response.setFromCurrency(fromCurrency);
            response.setToCurrency(toCurrency);
            response.setAmount(amount);
            response.setExchangeRate(exchangeRate);
            response.setConvertedAmount(amount * exchangeRate);
            
            logger.info("Conversion successful: " + amount + " " + fromCurrency + 
                      " = " + response.getConvertedAmount() + " " + toCurrency);
            
            return response;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in convertCurrency: " + e.getMessage(), e);
            throw new RuntimeException("Conversion failed. Please try again.", e);
        }
    }
    
    public double getExchangeRate(String fromCurrency, String toCurrency) {
        // Check if we have a cached rate that's still valid
        String cacheKey = fromCurrency + "-" + toCurrency;
        if (ratesCache.containsKey(fromCurrency)) {
            Long timestamp = cacheTimestamps.get(fromCurrency);
            if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                Map<String, Double> rates = ratesCache.get(fromCurrency);
                if (rates.containsKey(toCurrency)) {
                    logger.info("Using cached exchange rate for " + cacheKey);
                    return rates.get(toCurrency);
                }
            }
        }
        
        // No valid cache, fetch from API
        String apiUrl = String.format(apiUrlTemplate, apiKey, fromCurrency);
        logger.info("Fetching exchange rate from API: " + apiUrl.replace(apiKey, "API_KEY"));
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            String responseBody = response.getBody();
            
            if (responseBody == null) {
                logger.severe("Empty response from exchange rate API");
                throw new RuntimeException("Empty response from exchange rate API");
            }
            
            logger.info("API Response received, status: " + response.getStatusCode());
            // For debugging only, don't log full response in production
            logger.fine("API Response body: " + responseBody);
            
            JsonNode root = objectMapper.readTree(responseBody);
            String result = root.path("result").asText();
            
            if ("success".equals(result)) {
                JsonNode conversionRates = root.path("conversion_rates");
                
                if (conversionRates.isMissingNode()) {
                    logger.severe("Missing conversion_rates in API response");
                    throw new RuntimeException("Invalid API response format");
                }
                
                if (!conversionRates.has(toCurrency)) {
                    logger.severe("Target currency not found in response: " + toCurrency);
                    throw new RuntimeException("Currency not supported: " + toCurrency);
                }
                
                double rate = conversionRates.path(toCurrency).asDouble();
                
                // Update cache
                Map<String, Double> rates = ratesCache.computeIfAbsent(fromCurrency, k -> new HashMap<>());
                rates.put(toCurrency, rate);
                cacheTimestamps.put(fromCurrency, System.currentTimeMillis());
                
                logger.info("Successfully got rate: 1 " + fromCurrency + " = " + rate + " " + toCurrency);
                return rate;
            } else {
                String errorType = root.has("error-type") ? root.path("error-type").asText() : "Unknown error";
                logger.severe("API Error: " + errorType);
                throw new RuntimeException("API Error: " + errorType);
            }
        } catch (JsonProcessingException e) {
            logger.log(Level.SEVERE, "Error parsing exchange rate API response: " + e.getMessage(), e);
            throw new RuntimeException("Error processing exchange rate data", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching exchange rates: " + e.getMessage(), e);
            throw new RuntimeException("Error fetching exchange rates: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Double> getAllRatesForCurrency(String baseCurrency) {
        baseCurrency = baseCurrency.toUpperCase();
        logger.info("Getting all rates for currency: " + baseCurrency);
        
        // Check cache first
        if (ratesCache.containsKey(baseCurrency)) {
            Long timestamp = cacheTimestamps.get(baseCurrency);
            if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                logger.info("Using cached exchange rates for " + baseCurrency);
                return ratesCache.get(baseCurrency);
            }
        }
        
        // No valid cache, fetch from API
        String apiUrl = String.format(apiUrlTemplate, apiKey, baseCurrency);
        logger.info("Fetching rates from API: " + apiUrl.replace(apiKey, "API_KEY"));
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            String responseBody = response.getBody();
            
            if (responseBody == null) {
                logger.severe("Empty response from exchange rate API");
                throw new RuntimeException("Empty response from exchange rate API");
            }
            
            logger.info("API Response received, status: " + response.getStatusCode());
            
            JsonNode root = objectMapper.readTree(responseBody);
            String result = root.path("result").asText();
            
            if ("success".equals(result)) {
                JsonNode conversionRates = root.path("conversion_rates");
                
                if (conversionRates.isMissingNode()) {
                    logger.severe("Missing conversion_rates in API response");
                    throw new RuntimeException("Invalid API response format");
                }
                
                Map<String, Double> rates = new HashMap<>();
                
                conversionRates.fields().forEachRemaining(entry -> 
                    rates.put(entry.getKey(), entry.getValue().asDouble()));
                
                // Update cache
                ratesCache.put(baseCurrency, rates);
                cacheTimestamps.put(baseCurrency, System.currentTimeMillis());
                
                logger.info("Successfully fetched " + rates.size() + " exchange rates for " + baseCurrency);
                return rates;
            } else {
                String errorType = root.has("error-type") ? root.path("error-type").asText() : "Unknown error";
                logger.severe("API Error: " + errorType);
                throw new RuntimeException("API Error: " + errorType);
            }
        } catch (JsonProcessingException e) {
            logger.log(Level.SEVERE, "Error parsing exchange rate API response: " + e.getMessage(), e);
            throw new RuntimeException("Error processing exchange rate data", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching exchange rates: " + e.getMessage(), e);
            throw new RuntimeException("Error fetching exchange rates: " + e.getMessage(), e);
        }
    }
    
    // Add a test endpoint method to verify API connectivity
    public Map<String, Object> testApiConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Format the URL without including the actual API key in logs
            String apiUrl = String.format(apiUrlTemplate, apiKey, "USD");
            String safeUrl = apiUrl.replace(apiKey, "API_KEY"); 
            logger.info("Testing API connection with URL: " + safeUrl);
            
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            result.put("statusCode", response.getStatusCodeValue());
            
            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String apiResult = root.path("result").asText();
                result.put("result", apiResult);
                
                if ("success".equals(apiResult)) {
                    JsonNode conversionRates = root.path("conversion_rates");
                    int ratesCount = 0;
                    if (!conversionRates.isMissingNode()) {
                        ratesCount = conversionRates.size();
                    }
                    result.put("ratesCount", ratesCount);
                    result.put("eurRate", conversionRates.has("EUR") ? conversionRates.path("EUR").asDouble() : "N/A");
                    result.put("status", "success");
                } else {
                    result.put("status", "error");
                    result.put("errorType", root.has("error-type") ? root.path("error-type").asText() : "Unknown");
                }
            } else {
                result.put("status", "error");
                result.put("message", "Empty response body");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "API connection test failed: " + e.getMessage(), e);
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("exceptionType", e.getClass().getName());
        }
        return result;
    }
    
    public Map<String, Double> getHistoricalRatesOrEstimate(String baseCurrency) {
        // For a real implementation, you would fetch historical data from an API that supports it
        // Since we're using a basic Exchange Rate API, we'll simulate historical data
        // In a production app, you would store historical rates in your database
        
        Map<String, Double> currentRates = getAllRatesForCurrency(baseCurrency);
        Map<String, Double> simulatedHistoricalRates = new HashMap<>();
        
        // Create simulated historical rates (random variations within a small range)
        Random random = new Random(System.currentTimeMillis());
        for (Map.Entry<String, Double> entry : currentRates.entrySet()) {
            // Random change between -3% and +3%
            double changePercent = (random.nextDouble() * 6) - 3;
            double historicalRate = entry.getValue() * (1 - (changePercent / 100));
            simulatedHistoricalRates.put(entry.getKey(), historicalRate);
        }
        
        return simulatedHistoricalRates;
    }
    
    /**
     * Get historical trends for a specific currency pair
     */
    public Map<String, Object> getCurrencyTrends(String fromCurrency, String toCurrency) {
        Map<String, Object> trends = new HashMap<>();
        
        // Current rate
        double currentRate = getExchangeRate(fromCurrency, toCurrency);
        trends.put("currentRate", currentRate);
        
        // Get simulated historical data for different periods
        trends.put("7days", getHistoricalTrend(fromCurrency, toCurrency, 7));
        trends.put("30days", getHistoricalTrend(fromCurrency, toCurrency, 30));
        trends.put("90days", getHistoricalTrend(fromCurrency, toCurrency, 90));
        
        return trends;
    }
    
    /**
     * Simulate historical trend data for a specific time period
     */
    private Map<String, Object> getHistoricalTrend(String fromCurrency, String toCurrency, int days) {
        // In a real application, you would fetch this from a database where you store historical rates
        // For demo purposes, we'll simulate some realistic trends
        
        double currentRate = getExchangeRate(fromCurrency, toCurrency);
        Random random = new Random(fromCurrency.hashCode() + toCurrency.hashCode() + days);
        
        // Different volatility based on time period (longer period = potentially more movement)
        double volatility;
        switch (days) {
            case 7:
                volatility = 0.5; // 0.5% typical weekly volatility
                break;
            case 30:
                volatility = 1.2; // 1.2% typical monthly volatility
                break;
            case 90:
                volatility = 2.5; // 2.5% typical quarterly volatility
                break;
            default:
                volatility = 1.0;
        }
        
        // Add some currency-specific volatility adjustment
        if (toCurrency.equals("BTC") || fromCurrency.equals("BTC")) {
            volatility *= 5; // Crypto is more volatile
        } else if (toCurrency.equals("JPY") || fromCurrency.equals("JPY") ||
                   toCurrency.equals("EUR") || fromCurrency.equals("EUR")) {
            volatility *= 0.8; // Major currencies tend to be less volatile
        }
        
        // Generate a somewhat realistic change percentage
        double changePercent = (random.nextDouble() * 2 - 1) * volatility;
        
        // Calculate historical rate based on the change percentage
        double historicalRate = currentRate / (1 + (changePercent / 100));
        
        // Also generate some intermittent points for a mini chart
        List<Double> trendPoints = new ArrayList<>();
        for (int i = days; i >= 0; i--) {
            // Create somewhat correlated movements (not just random)
            double progress = (double) i / days;
            double randomFactor = random.nextDouble() * 0.4 - 0.2; // Random noise ±0.2
            double pointRate = historicalRate + (currentRate - historicalRate) * progress + 
                              (currentRate * randomFactor * volatility / 100);
            trendPoints.add(Math.round(pointRate * 10000) / 10000.0); // Round to 4 decimal places
        }
        
        Map<String, Object> trend = new HashMap<>();
        trend.put("historicalRate", historicalRate);
        trend.put("changePercent", Math.round(changePercent * 100) / 100.0); // Round to 2 decimal places
        trend.put("trendPoints", trendPoints);
        
        return trend;
    }
}
