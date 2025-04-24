import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import axios from "axios";
import { getCurrencyName, fetchAvailableCurrencies } from "./getCurrency";

const AddExpense = () => {
  const [formData, setFormData] = useState({
    subject: "",
    category: "",
    date: "",
    amount: "",
    currency: "", // Start with empty string so we can tell if it's been set
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

  // Clean up this function - just focus on setting the user's currency
  const fetchUserCurrency = async () => {
    try {
      setLoading(true);
      
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to add an expense.");
        setLoading(false);
        return;
      }

      // Get user profile to get their currency
      const userResponse = await axios.get("http://localhost:8080/api/users/me", {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      });

      // Check the actual currency value for debugging
      console.log("User profile data:", userResponse.data);
      console.log("User currency value:", userResponse.data?.currency);

      // Set the user's default currency
      const userCurrency = userResponse.data?.currency || "PHP";
      setUserDefaultCurrency(userCurrency);
      
      // Set the user's currency in the form
      setFormData(prev => ({
        ...prev,
        currency: userCurrency
      }));
    } catch (err) {
      console.error("Error fetching user currency:", err);
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
      // Call the imported fetchAvailableCurrencies and pass the state setter
      await fetchAvailableCurrencies(setAvailableCurrencies);
      await fetchUserCurrency();
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

  const handleAddExpense = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to add an expense.");
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
          // Update the submission data with converted values
          submissionData.amount = convertedAmount;
          submissionData.currency = userDefaultCurrency;
        } else {
          setError("Currency conversion failed. Please try again or use your default currency.");
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

      await axios.post("http://localhost:8080/api/expenses/postExpense", submissionData, config);
      navigate("/expense"); // Redirect to the Expense page after successful addition
    } catch (err) {
      console.error("Error adding expense:", err);
      setError("Failed to add expense. Please try again later.");
    }
  };

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="expense" />

        {/* Main Content */}
        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA] flex justify-center items-center">
          {/* White Container */}
          <div className="bg-white p-12 rounded-lg shadow-md w-full max-w-7xl flex" style={{ height: "700px" }}>
            {/* Left Section: Form */}
            <div className="w-2/3 pr-8">
              <div className="flex items-center mb-6">
                <button
                  onClick={() => navigate("/expense")}
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
                <h1 className="text-2xl font-bold text-[#18864F]">Add Expense</h1>
              </div>

              {loading ? (
                <div className="flex justify-center items-center h-64">
                  <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[#18864F]"></div>
                  <span className="ml-3 text-[#18864F]">Loading your currency preferences...</span>
                </div>
              ) : (
                <form onSubmit={handleAddExpense}>
                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Subject</label>
                    <input
                      type="text"
                      name="subject"
                      value={formData.subject}
                      onChange={handleInputChange}
                      className="w-full max-w-lg p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                      placeholder="Enter expense subject"
                      required
                    />
                  </div>

                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Category</label>
                    <select
                      name="category"
                      value={formData.category}
                      onChange={handleInputChange}
                      className="w-full max-w-lg p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                      required
                    >
                      <option value="" disabled>
                        Select a category
                      </option>
                      <option value="Food">Food</option>
                      <option value="Transportation">Transportation</option>
                      <option value="Housing">Housing</option>
                      <option value="Utilities">Utilities</option>
                      <option value="Entertainment">Entertainment</option>
                      <option value="Healthcare">Healthcare</option>
                      <option value="Education">Education</option>
                      <option value="Shopping">Shopping</option>
                      <option value="Other">Other</option>
                    </select>
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
                  </div>

                  {error && <p className="text-red-500 text-sm mb-4">{error}</p>}

                  <div className="flex justify-between max-w-lg">
                    <button
                      type="submit"
                      className="bg-[#18864F] text-white font-bold py-2 px-4 rounded-md hover:bg-green-700 transition duration-300"
                      disabled={converting}
                    >
                      Add Expense
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate("/expense")}
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
                <h3 className="text-lg font-semibold">Expense Details</h3>
                <p className="text-sm mt-2">
                  Track your expenses in your preferred currency. Your default currency is <span className="font-bold">{userDefaultCurrency}</span>.
                </p>
              </div>
              
              <div className="bg-amber-50 border-l-4 border-amber-500 p-4 my-4">
                <h3 className="font-medium text-amber-800">Auto-Conversion</h3>
                <p className="text-sm text-amber-700 mt-2">
                  When you change currencies, amounts are automatically converted using current exchange rates.
                </p>
              </div>
              
              <div className="bg-blue-50 border-l-4 border-blue-500 p-4">
                <h3 className="font-medium text-blue-800">Tip</h3>
                <p className="text-sm text-blue-700 mt-2">
                  Categorizing your expenses helps you create accurate budgets and track spending patterns.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AddExpense;