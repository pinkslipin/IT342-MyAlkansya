package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.dto.CurrencyConversionRequest;
import edu.cit.myalkansya.dto.CurrencyConversionResponse;
import edu.cit.myalkansya.service.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    private static final Logger logger = Logger.getLogger(CurrencyController.class.getName());

    @Autowired
    private ExchangeRateService exchangeRateService;
    
    @PostMapping("/convert")
    public ResponseEntity<?> convertCurrency(@RequestBody CurrencyConversionRequest request) {
        try {
            if (request.getFromCurrency() == null || request.getToCurrency() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Currency codes cannot be null"));
            }
            
            if (request.getAmount() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Amount must be greater than zero"));
            }
            
            // Get basic conversion result
            CurrencyConversionResponse response = exchangeRateService.convertCurrency(request);
            
            // Get historical trends and enhance the response
            Map<String, Object> enhancedResponse = new HashMap<>();
            enhancedResponse.put("conversion", response);
            
            // Add historical trends
            Map<String, Object> trends = exchangeRateService.getCurrencyTrends(
                request.getFromCurrency(), request.getToCurrency());
            enhancedResponse.put("trends", trends);
            
            return ResponseEntity.ok(enhancedResponse);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in currency conversion: " + e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/rate")
    public ResponseEntity<?> getExchangeRate(
            @RequestParam String from, 
            @RequestParam String to) {
        try {
            double rate = exchangeRateService.getExchangeRate(from.toUpperCase(), to.toUpperCase());
            return ResponseEntity.ok(Map.of("rate", rate));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting exchange rate: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/rates/{currency}")
    public ResponseEntity<?> getAllRates(@PathVariable String currency) {
        try {
            Map<String, Double> rates = exchangeRateService.getAllRatesForCurrency(currency.toUpperCase());
            return ResponseEntity.ok(rates);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting all rates: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> testApiConnection() {
        Map<String, Object> result = exchangeRateService.testApiConnection();
        if ("error".equals(result.get("status"))) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/popular")
    public ResponseEntity<?> getPopularCurrencies() {
        try {
            // Base currency for comparing popular currencies
            String baseCurrency = "USD";
            
            // List of popular currencies to display
            List<String> popularCurrencyCodes = List.of(
                "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "INR", "PHP", "BTC"
            );
            
            // Get current rates for all these currencies
            Map<String, Double> currentRates = exchangeRateService.getAllRatesForCurrency(baseCurrency);
            
            // Get historical rates (7 days ago) if available, otherwise use slightly modified rates
            Map<String, Double> historicalRates = exchangeRateService.getHistoricalRatesOrEstimate(baseCurrency);
            
            // Build response with rates and change percentages
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (String code : popularCurrencyCodes) {
                if (currentRates.containsKey(code)) {
                    Map<String, Object> currencyData = new HashMap<>();
                    currencyData.put("code", code);
                    currencyData.put("name", getCurrencyName(code));
                    currencyData.put("rate", currentRates.get(code));
                    
                    // Calculate percentage change
                    double currentRate = currentRates.get(code);
                    double historicalRate = historicalRates.getOrDefault(code, currentRate * 0.98); // Fallback if no historical data
                    double changePercent = ((currentRate - historicalRate) / historicalRate) * 100;
                    
                    currencyData.put("changePercent", Math.round(changePercent * 100) / 100.0); // Round to 2 decimal places
                    result.add(currencyData);
                }
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting popular currencies: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Helper method to get currency names
    private String getCurrencyName(String code) {
        Map<String, String> currencyNames = Map.ofEntries(
            Map.entry("USD", "US Dollar"),
            Map.entry("EUR", "Euro"),
            Map.entry("JPY", "Japanese Yen"),
            Map.entry("GBP", "British Pound"),
            Map.entry("AUD", "Australian Dollar"),
            Map.entry("CAD", "Canadian Dollar"),
            Map.entry("CHF", "Swiss Franc"),
            Map.entry("CNY", "Chinese Yuan"),
            Map.entry("INR", "Indian Rupee"),
            Map.entry("PHP", "Philippine Peso"),
            Map.entry("BTC", "Bitcoin")
        );
        return currencyNames.getOrDefault(code, code);
    }
}
