import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import axios from "axios";
import { getCurrencyName, fetchAvailableCurrencies } from "./getCurrency";

const AddSavingGoal = () => {
  const [formData, setFormData] = useState({
    goal: "",
    targetAmount: "",
    currentAmount: "0",
    targetDate: "",
    currency: "", // Will be set from user's profile
  });
  const [userDefaultCurrency, setUserDefaultCurrency] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [converting, setConverting] = useState(false);
  const [availableCurrencies, setAvailableCurrencies] = useState([]);
  const [conversionMessage, setConversionMessage] = useState("");
  const navigate = useNavigate();
  const originalTargetAmount = useRef(null);
  const originalCurrentAmount = useRef(null);
  const originalCurrency = useRef(null);

  // Function to fetch user data and set the default currency
  const fetchUserData = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to add a savings goal.");
        setLoading(false);
        return;
      }

      console.log("Fetching user profile data...");
      const response = await axios.get("http://localhost:8080/api/users/me", {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      });

      // Check the actual currency value for debugging
      console.log("User profile data:", response.data);
      console.log("User currency value:", response.data?.currency);

      // Set the user's default currency
      const userCurrency = response.data?.currency || "PHP";
      setUserDefaultCurrency(userCurrency);
      
      // Set the form currency
      setFormData(prev => ({
        ...prev,
        currency: userCurrency
      }));
    } catch (err) {
      console.error("Error fetching user data:", err);
      // Continue with default currency if there's an error
      setFormData(prev => ({
        ...prev, 
        currency: "PHP"
      }));
      setUserDefaultCurrency("PHP");
    } finally {
      setLoading(false);
    }
  };

  // Initialize data when component loads
  useEffect(() => {
    const initializeData = async () => {
      await fetchAvailableCurrencies(setAvailableCurrencies);
      await fetchUserData();
    };
    
    initializeData();
  }, []);

  // Function to convert currency
  const convertCurrency = async (amount, fromCurrency, toCurrency) => {
    try {
      if (!amount || isNaN(amount) || amount <= 0) {
        return null;
      }

      setConverting(true);
      const authToken = localStorage.getItem("authToken");
      
      // Add more detailed debugging
      console.log(`Converting ${amount} from ${fromCurrency} to ${toCurrency}`);
      
      const response = await axios.get(
        `http://localhost:8080/api/currency/rate?from=${fromCurrency}&to=${toCurrency}`,
        {
          headers: { Authorization: `Bearer ${authToken}` }
        }
      );
      
      console.log("Conversion API response:", response.data);
      
      if (response.data && response.data.rate) {
        const convertedAmount = parseFloat(amount) * response.data.rate;
        return convertedAmount.toFixed(2);
      }
      return null;
    } catch (err) {
      // Log more detailed error information
      console.error("Currency conversion error:", err);
      if (err.response) {
        console.error("Error response:", err.response.data);
        console.error("Status code:", err.response.status);
      }
      return null;
    } finally {
      setConverting(false);
    }
  };

  const handleInputChange = async (e) => {
    const { name, value } = e.target;
    
    // Special handling for currency changes
    if (name === "currency") {
      const newCurrency = value;
      const currentTargetAmount = formData.targetAmount;
      const currentCurrentAmount = formData.currentAmount;
      
      // If amounts exist and currency is changed
      if ((currentTargetAmount || currentCurrentAmount) && newCurrency !== formData.currency) {
        // Save original values if this is the first change
        if (!originalCurrency.current) {
          originalTargetAmount.current = currentTargetAmount;
          originalCurrentAmount.current = currentCurrentAmount;
          originalCurrency.current = formData.currency;
        }
        
        // If changing back to original currency, restore original amounts
        if (newCurrency === originalCurrency.current) {
          setFormData({
            ...formData,
            targetAmount: originalTargetAmount.current,
            currentAmount: originalCurrentAmount.current,
            currency: newCurrency
          });
          setConversionMessage("");
          return;
        }
        
        // Convert to the new currency
        let convertedTargetAmount = null;
        let convertedCurrentAmount = null;
        
        if (currentTargetAmount) {
          convertedTargetAmount = await convertCurrency(
            currentTargetAmount, 
            formData.currency, 
            newCurrency
          );
        }
        
        if (currentCurrentAmount) {
          convertedCurrentAmount = await convertCurrency(
            currentCurrentAmount, 
            formData.currency, 
            newCurrency
          );
        }
        
        const updatedFormData = { ...formData, currency: newCurrency };
        
        if (convertedTargetAmount) {
          updatedFormData.targetAmount = convertedTargetAmount;
        }
        
        if (convertedCurrentAmount) {
          updatedFormData.currentAmount = convertedCurrentAmount;
        }
        
        setFormData(updatedFormData);
        
        setConversionMessage(
          `Amounts converted from ${formData.currency} to ${newCurrency}`
        );
        
        return;
      }
    }
    
    // For amount changes, clear the original values to allow new conversions
    if (name === "targetAmount" || name === "currentAmount") {
      originalTargetAmount.current = null;
      originalCurrentAmount.current = null;
      originalCurrency.current = null;
      setConversionMessage("");
    }
    
    // Standard input handling for other fields
    setFormData({ ...formData, [name]: value });
  };

  const handleAddSavingGoal = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to add a savings goal.");
        return;
      }

      // Create a copy of form data for submission
      const submissionData = { ...formData };
      
      // If the selected currency is different from user's default, convert amounts
      if (submissionData.currency !== userDefaultCurrency) {
        setConverting(true);
        
        try {
          const convertedTargetAmount = await convertCurrency(
            submissionData.targetAmount,
            submissionData.currency,
            userDefaultCurrency
          );
          
          const convertedCurrentAmount = await convertCurrency(
            submissionData.currentAmount,
            submissionData.currency,
            userDefaultCurrency
          );
          
          if (convertedTargetAmount && convertedCurrentAmount) {
            // Update the submission data with converted values
            submissionData.targetAmount = convertedTargetAmount;
            submissionData.currentAmount = convertedCurrentAmount;
            submissionData.currency = userDefaultCurrency;
          } else {
            // Try with hardcoded fallback rates for common currencies as a last resort
            const fallbackRates = {
              "USD_PHP": 56.5,
              "EUR_PHP": 61.2,
              "PHP_USD": 0.0177,
              "PHP_EUR": 0.0163,
              "EUR_USD": 1.08,
              "USD_EUR": 0.93
            };
            
            const rateKey = `${submissionData.currency}_${userDefaultCurrency}`;
            if (fallbackRates[rateKey]) {
              console.log(`Using fallback rate for ${rateKey}: ${fallbackRates[rateKey]}`);
              submissionData.targetAmount = (parseFloat(submissionData.targetAmount) * fallbackRates[rateKey]).toFixed(2);
              submissionData.currentAmount = (parseFloat(submissionData.currentAmount) * fallbackRates[rateKey]).toFixed(2);
              submissionData.currency = userDefaultCurrency;
            } else {
              setError("Currency conversion failed. Please try again or use your default currency.");
              setConverting(false);
              return;
            }
          }
        } catch (convErr) {
          console.error("Error during conversion process:", convErr);
          setError("Currency conversion system error. Please try again later or use your default currency.");
          setConverting(false);
          return;
        }
        setConverting(false);
      }

      const config = {
        headers: {
          Authorization: `Bearer ${authToken}`,
          'Content-Type': 'application/json'
        },
      };

      await axios.post("http://localhost:8080/api/savings-goals/postSavingsGoal", submissionData, config);
      navigate("/savingsgoal"); // Redirect to the Savings Goal page after successful addition
    } catch (err) {
      console.error("Error adding savings goal:", err);
      setError("Failed to add savings goal. Please try again later.");
    }
  };

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="savingsgoal" />

        {/* Main Content */}
        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA] flex justify-center items-center">
          {/* White Container */}
          <div className="bg-white p-12 rounded-lg shadow-md w-full max-w-7xl flex" style={{ height: "700px" }}>
            {/* Left Section: Form */}
            <div className="w-2/3 pr-8">
              <div className="flex items-center mb-6">
                <button
                  onClick={() => navigate("/savingsgoal")}
                  className="text-[#18864F] hover:text-green-700 mr-4"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-6 w-6"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M15 19l-7-7 7-7"
                    />
                  </svg>
                </button>
                <h1 className="text-2xl font-bold text-[#18864F]">Add Savings Goal</h1>
              </div>

              {loading ? (
                <div className="flex justify-center items-center h-64">
                  <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[#18864F]"></div>
                  <span className="ml-3 text-[#18864F]">Loading your currency preferences...</span>
                </div>
              ) : (
                <form onSubmit={handleAddSavingGoal}>
                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Goal</label>
                    <input
                      type="text"
                      name="goal"
                      value={formData.goal}
                      onChange={handleInputChange}
                      className="w-full max-w-lg p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                      placeholder="Enter your savings goal"
                      required
                    />
                  </div>

                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Target Amount</label>
                    <div className="flex max-w-lg">
                      <input
                        type="number"
                        name="targetAmount"
                        value={formData.targetAmount}
                        onChange={handleInputChange}
                        className="w-full p-3 border rounded-l-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                        placeholder="Enter target amount"
                        required
                        disabled={converting}
                      />
                      <select
                        name="currency"
                        value={formData.currency}
                        onChange={handleInputChange}
                        className="p-3 border rounded-r-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                        disabled={converting}
                      >
                        {availableCurrencies.length > 0 ? (
                          availableCurrencies.map(currency => (
                            <option key={currency.code} value={currency.code}>
                              {currency.code} - {currency.name}
                              {currency.code === userDefaultCurrency ? " (Default)" : ""}
                            </option>
                          ))
                        ) : (
                          <>
                            <option value="PHP">PHP - Philippine Peso</option>
                            <option value="USD">USD - US Dollar</option>
                            <option value="EUR">EUR - Euro</option>
                          </>
                        )}
                      </select>
                    </div>
                  </div>

                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Current Amount</label>
                    <input
                      type="number"
                      name="currentAmount"
                      value={formData.currentAmount}
                      onChange={handleInputChange}
                      className="w-full max-w-lg p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                      placeholder="Enter current amount saved"
                      required
                      disabled={converting}
                    />
                    
                    {converting && (
                      <div className="mt-2 text-blue-600 flex items-center">
                        <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-blue-600 mr-2"></div>
                        Converting...
                      </div>
                    )}
                    {conversionMessage && (
                      <div className="mt-2 text-green-600 text-sm">
                        {conversionMessage}
                      </div>
                    )}
                    
                    {formData.currency !== userDefaultCurrency && (formData.targetAmount || formData.currentAmount) && (
                      <div className="mt-2 text-amber-600 text-sm">
                        Note: Values will be automatically converted to {userDefaultCurrency} when saved.
                      </div>
                    )}
                  </div>

                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Target Date</label>
                    <input
                      type="date"
                      name="targetDate"
                      value={formData.targetDate}
                      onChange={handleInputChange}
                      className="w-full max-w-lg p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                      required
                    />
                  </div>

                  {error && <p className="text-red-500 text-sm mb-4">{error}</p>}

                  <div className="flex justify-between max-w-lg">
                    <button
                      type="submit"
                      className="bg-[#18864F] text-white font-bold py-2 px-4 rounded-md hover:bg-green-700 transition duration-300"
                      disabled={converting}
                    >
                      Add Savings Goal
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate("/savingsgoal")}
                      className="bg-[#FEF6EA] text-[#18864F] font-bold py-2 px-4 rounded-md border border-[#18864F] hover:bg-[#EDFBE9] transition duration-300"
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              )}
            </div>

            {/* Right Section: Information Panel */}
            <div className="w-1/3 bg-[#F9F9F9] rounded-lg p-6 flex flex-col justify-center">
              <div className="bg-[#18864F] text-white p-4 rounded-lg mb-4">
                <h3 className="text-lg font-semibold">Savings Goal Tips</h3>
                <p className="text-sm mt-2">
                  Setting clear, achievable goals helps you stay motivated on your savings journey.
                </p>
              </div>
              
              <div className="bg-blue-50 p-4 my-4 rounded">
                <div className="flex items-center mb-2">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-blue-600 mr-2" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                  </svg>
                  <h3 className="font-medium text-blue-900">Currency Info</h3>
                </div>
                <p className="text-sm text-blue-700">
                  Your default currency is <span className="font-bold">{userDefaultCurrency}</span>. All your financial data will be saved in this currency.
                </p>
              </div>
              
              <div className="bg-green-50 p-4 rounded">
                <h3 className="font-medium text-green-900 flex items-center">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                  </svg>
                  Progress Tracking
                </h3>
                <p className="text-sm text-green-700 mt-2">
                  Track your progress towards your goal and visualize how close you are to achieving it.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AddSavingGoal;