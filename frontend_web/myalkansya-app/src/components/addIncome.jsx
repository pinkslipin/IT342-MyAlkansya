import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import axios from "axios";
import { getCurrencyName, fetchAvailableCurrencies } from "./getCurrency";

const AddIncome = () => {
  const [formData, setFormData] = useState({
    source: "",
    date: "",
    amount: "",
    currency: "", // Will be set from user's profile
  });
  const [userDefaultCurrency, setUserDefaultCurrency] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [converting, setConverting] = useState(false);
  const [availableCurrencies, setAvailableCurrencies] = useState([]);
  const [conversionMessage, setConversionMessage] = useState("");
  const navigate = useNavigate();
  const originalAmount = useRef(null);
  const originalCurrency = useRef(null);

  // Function to fetch user data and set the default currency
  const fetchUserData = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to add income.");
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
      
      const response = await axios.get(
        `http://localhost:8080/api/currency/rate?from=${fromCurrency}&to=${toCurrency}`,
        {
          headers: { Authorization: `Bearer ${authToken}` }
        }
      );
      
      if (response.data && response.data.rate) {
        const convertedAmount = parseFloat(amount) * response.data.rate;
        return convertedAmount.toFixed(2);
      }
      return null;
    } catch (err) {
      console.error("Currency conversion error:", err);
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
      const currentAmount = formData.amount;
      
      // If amount exists, currency is changed, and it's different from default
      if (currentAmount && newCurrency !== formData.currency) {
        // Save original values if this is the first change
        if (!originalAmount.current) {
          originalAmount.current = currentAmount;
          originalCurrency.current = formData.currency;
        }
        
        // If changing back to original currency, restore original amount
        if (newCurrency === originalCurrency.current) {
          setFormData({
            ...formData,
            amount: originalAmount.current,
            currency: newCurrency
          });
          setConversionMessage("");
          return;
        }
        
        // Convert to the new currency
        const convertedAmount = await convertCurrency(
          currentAmount, 
          formData.currency, 
          newCurrency
        );
        
        if (convertedAmount) {
          setFormData({
            ...formData,
            amount: convertedAmount,
            currency: newCurrency
          });
          
          setConversionMessage(
            `Converted ${currentAmount} ${formData.currency} to ${convertedAmount} ${newCurrency}`
          );
        } else {
          // Just change the currency if conversion fails
          setFormData({
            ...formData,
            currency: newCurrency
          });
          setConversionMessage("Currency changed, but conversion failed. Amount may need manual adjustment.");
        }
        return;
      }
    }
    
    // For amount changes, clear the original values to allow new conversions
    if (name === "amount") {
      originalAmount.current = null;
      originalCurrency.current = null;
      setConversionMessage("");
    }
    
    // Standard input handling for other fields
    setFormData({ ...formData, [name]: value });
  };

  const handleAddIncome = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to add income.");
        return;
      }

      // Create a copy of form data for submission
      const submissionData = { ...formData };
      
      // If the selected currency is different from user's default, convert it
      if (submissionData.currency !== userDefaultCurrency) {
        setConverting(true);
        const convertedAmount = await convertCurrency(
          submissionData.amount,
          submissionData.currency,
          userDefaultCurrency
        );
        
        if (convertedAmount) {
          // Store the original values for display
          const originalAmount = submissionData.amount;
          const originalCurrency = submissionData.currency;
          
          // Update the submission data with converted values
          submissionData.amount = convertedAmount;
          submissionData.currency = userDefaultCurrency;
          
          console.log(`Converted ${originalAmount} ${originalCurrency} to ${convertedAmount} ${userDefaultCurrency} for storage`);
        } else {
          setError("Currency conversion failed. Please try again or use your default currency.");
          setConverting(false);
          return;
        }
        setConverting(false);
      }

      console.log("Submitting income with currency:", submissionData.currency);
      const config = {
        headers: {
          Authorization: `Bearer ${authToken}`,
          'Content-Type': 'application/json'
        },
      };

      await axios.post("http://localhost:8080/api/incomes/postIncome", submissionData, config);
      navigate("/income"); // Redirect to the Income page after successful addition
    } catch (err) {
      console.error("Error adding income:", err);
      setError("Failed to add income. Please try again later.");
    }
  };

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="income" />

        {/* Main Content */}
        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA] flex justify-center items-center">
          {/* White Container */}
          <div className="bg-white p-12 rounded-lg shadow-md w-full max-w-7xl flex" style={{ height: "650px" }}>
            {/* Left Section: Form */}
            <div className="w-2/3 pr-8">
              <div className="flex items-center mb-6">
                <button
                  onClick={() => navigate("/income")}
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
                <h1 className="text-2xl font-bold text-[#18864F]">Add Income</h1>
              </div>

              {loading ? (
                <div className="flex justify-center items-center h-64">
                  <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[#18864F]"></div>
                  <span className="ml-3 text-[#18864F]">Loading...</span>
                </div>
              ) : (
                <form onSubmit={handleAddIncome}>
                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Source</label>
                    <input
                      type="text"
                      name="source"
                      value={formData.source}
                      onChange={handleInputChange}
                      className="w-full max-w-lg p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                      placeholder="Enter income source"
                      required
                    />
                  </div>

                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Date</label>
                    <input
                      type="date"
                      name="date"
                      value={formData.date}
                      onChange={handleInputChange}
                      className="w-full max-w-lg p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                      required
                    />
                  </div>

                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Amount</label>
                    <div className="flex max-w-lg">
                      <input
                        type="number"
                        name="amount"
                        value={formData.amount}
                        onChange={handleInputChange}
                        className="w-full p-3 border rounded-l-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                        placeholder="Enter amount"
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
                    
                    {/* Add this new information section */}
                    {formData.currency !== userDefaultCurrency && formData.amount && (
                      <div className="mt-2 text-amber-600 text-sm">
                        Note: This income will be automatically converted to {userDefaultCurrency} when saved.
                      </div>
                    )}
                  </div>

                  {error && <p className="text-red-500 text-sm mb-4">{error}</p>}

                  <div className="flex justify-between max-w-lg">
                    <button
                      type="submit"
                      className="bg-[#18864F] text-white font-bold py-2 px-4 rounded-md hover:bg-green-700 transition duration-300"
                      disabled={converting}
                    >
                      Add Income
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate("/income")}
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
                <h3 className="text-lg font-semibold">Currency Conversion</h3>
                <p className="text-sm mt-2">
                  You can enter income in any currency. The amount will be automatically 
                  converted to your default currency ({userDefaultCurrency}) when saved.
                </p>
              </div>
              <div>
                <h3 className="font-medium text-[#18864F]">Your default currency:</h3>
                <p className="text-lg font-bold">{userDefaultCurrency}</p>
                <p className="text-sm text-gray-600 mt-4">
                  All financial data is stored in your default currency.
                  You can change your default currency in your profile settings.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AddIncome;