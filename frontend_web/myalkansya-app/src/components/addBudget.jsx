import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import axios from "axios";
import { getCurrencyName, fetchAvailableCurrencies } from "./getCurrency";

const AddBudget = () => {
  const [formData, setFormData] = useState({
    category: "",
    monthlyBudget: "",
    currency: "", // Will be set from user's profile
    budgetMonth: new Date().getMonth() + 1, // Current month (1-12)
    budgetYear: new Date().getFullYear(), // Current year
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
        setError("You must be logged in to add a budget.");
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
      // Call the imported fetchAvailableCurrencies function
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
      const currentAmount = formData.monthlyBudget;
      
      // If amount exists and currency is changed
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
            monthlyBudget: originalAmount.current,
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
            monthlyBudget: convertedAmount,
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
    if (name === "monthlyBudget") {
      originalAmount.current = null;
      originalCurrency.current = null;
      setConversionMessage("");
    }
    
    // Standard input handling for other fields
    setFormData({ ...formData, [name]: value });
  };

  const handleAddBudget = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to add a budget.");
        return;
      }

      // Create a copy of form data for submission
      const submissionData = { ...formData };
      
      // If the selected currency is different from user's default, convert it
      if (submissionData.currency !== userDefaultCurrency) {
        setConverting(true);
        const convertedAmount = await convertCurrency(
          submissionData.monthlyBudget,
          submissionData.currency,
          userDefaultCurrency
        );
        
        if (convertedAmount) {
          // Store the original values for display
          const originalAmount = submissionData.monthlyBudget;
          const originalCurrency = submissionData.currency;
          
          // Update the submission data with converted values
          submissionData.monthlyBudget = convertedAmount;
          submissionData.currency = userDefaultCurrency;
          
          console.log(`Converted ${originalAmount} ${originalCurrency} to ${convertedAmount} ${userDefaultCurrency} for storage`);
        } else {
          setError("Currency conversion failed. Please try again or use your default currency.");
          setConverting(false);
          return;
        }
        setConverting(false);
      }

      console.log("Submitting budget with currency:", submissionData.currency);
      const config = {
        headers: {
          Authorization: `Bearer ${authToken}`,
          'Content-Type': 'application/json'
        },
      };

      await axios.post("http://localhost:8080/api/budgets/create", submissionData, config);
      navigate("/budget"); // Redirect to the Budget page after successful addition
    } catch (err) {
      console.error("Error adding budget:", err);
      if (err.response && err.response.data) {
        setError(err.response.data);
      } else {
        setError("Failed to add budget. Please try again later.");
      }
    }
  };

  // Generate array of months for dropdown
  const months = [
    { value: 1, label: "January" },
    { value: 2, label: "February" },
    { value: 3, label: "March" },
    { value: 4, label: "April" },
    { value: 5, label: "May" },
    { value: 6, label: "June" },
    { value: 7, label: "July" },
    { value: 8, label: "August" },
    { value: 9, label: "September" },
    { value: 10, label: "October" },
    { value: 11, label: "November" },
    { value: 12, label: "December" },
  ];

  // Generate array of years (current year - 1, current year, current year + 1)
  const currentYear = new Date().getFullYear();
  const years = [currentYear - 1, currentYear, currentYear + 1, currentYear + 2];

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="budget" />

        {/* Main Content */}
        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA] flex justify-center items-center">
          {/* White Container */}
          <div className="bg-white p-12 rounded-lg shadow-md w-full max-w-7xl flex" style={{ height: "700px" }}>
            {/* Left Section: Form */}
            <div className="w-2/3 pr-8">
              <div className="flex items-center mb-6">
                <button
                  onClick={() => navigate("/budget")}
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
                <h1 className="text-2xl font-bold text-[#18864F]">Add Budget</h1>
              </div>

              {loading ? (
                <div className="flex justify-center items-center h-64">
                  <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[#18864F]"></div>
                  <span className="ml-3 text-[#18864F]">Loading your currency preferences...</span>
                </div>
              ) : (
                <form onSubmit={handleAddBudget}>
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

                  {/* Month and Year Selection */}
                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Month and Year</label>
                    <div className="flex max-w-lg">
                      <select
                        name="budgetMonth"
                        value={formData.budgetMonth}
                        onChange={handleInputChange}
                        className="w-1/2 p-3 border rounded-l-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                        required
                      >
                        {months.map((month) => (
                          <option key={month.value} value={month.value}>
                            {month.label}
                          </option>
                        ))}
                      </select>
                      <select
                        name="budgetYear"
                        value={formData.budgetYear}
                        onChange={handleInputChange}
                        className="w-1/2 p-3 border rounded-r-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                        required
                      >
                        {years.map((year) => (
                          <option key={year} value={year}>
                            {year}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Monthly Budget</label>
                    <div className="flex max-w-lg">
                      <input
                        type="number"
                        name="monthlyBudget"
                        value={formData.monthlyBudget}
                        onChange={handleInputChange}
                        className="w-full p-3 border rounded-l-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                        placeholder="Enter monthly budget"
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
                    
                    {formData.currency !== userDefaultCurrency && formData.monthlyBudget && (
                      <div className="mt-2 text-amber-600 text-sm">
                        Note: This budget will be automatically converted to {userDefaultCurrency} when saved.
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
                      Add Budget
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate("/budget")}
                      className="bg-[#FEF6EA] text-[#18864F] font-bold py-2 px-4 rounded-md border border-[#18864F] hover:bg-[#EDFBE9] transition duration-300"
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              )}
            </div>

            {/* Right Section */}
            <div className="w-1/3 bg-[#F9F9F9] rounded-lg p-4 flex flex-col items-center justify-center">
              <div className="bg-[#18864F] text-white p-4 rounded-lg mb-4">
                <h3 className="text-lg font-semibold">Budget Planning</h3>
                <p className="text-sm mt-2">
                  Set a monthly budget for each spending category to keep track of your expenses.
                </p>
              </div>

              <div className="bg-amber-50 border-l-4 border-amber-500 p-4 my-4 w-full">
                <h3 className="font-medium text-amber-800">Currency Info</h3>
                <p className="text-sm text-amber-700 mt-2">
                  Your default currency is <span className="font-bold">{userDefaultCurrency}</span>. All your financial data will be saved in this currency.
                </p>
              </div>
              
              <div className="text-sm text-gray-600 mt-4">
                You can create one budget per category for each month.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AddBudget;