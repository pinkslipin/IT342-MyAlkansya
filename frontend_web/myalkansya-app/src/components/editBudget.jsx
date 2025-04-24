import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import axios from "axios";
import trashIcon from "../assets/trash.png"; // Import the trash icon
import { getCurrencyName, fetchAvailableCurrencies } from "./getCurrency";

const EditBudget = () => {
  const [formData, setFormData] = useState({
    category: "",
    monthlyBudget: "",
    currency: "PHP",
    budgetMonth: new Date().getMonth() + 1,
    budgetYear: new Date().getFullYear(),
    totalSpent: 0
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [availableCurrencies, setAvailableCurrencies] = useState([]);
  const navigate = useNavigate();
  const { budgetId } = useParams();

  useEffect(() => {
    const initializeData = async () => {
      try {
        const authToken = localStorage.getItem("authToken");
        if (!authToken) {
          setError("You must be logged in to edit a budget.");
          navigate("/login");
          return;
        }

        const config = {
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        };

        // Fetch budget data
        const response = await axios.get(`http://localhost:8080/api/budgets/${budgetId}`, config);
        
        // Format numeric values to 2 decimal places when setting initial data
        const data = response.data;
        if (data.monthlyBudget) {
          data.monthlyBudget = parseFloat(data.monthlyBudget).toFixed(2);
        }
        if (data.totalSpent) {
          data.totalSpent = parseFloat(data.totalSpent).toFixed(2);
        }
        
        setFormData(data);
        
        // Fetch available currencies
        await fetchAvailableCurrencies(setAvailableCurrencies);
      } catch (err) {
        console.error("Error fetching budget:", err);
        setError("Failed to fetch budget details. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    initializeData();
  }, [budgetId, navigate]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    
    // Handle amount fields with 2 decimal places
    if (name === "monthlyBudget") {
      // For empty inputs, just set to empty string
      if (value === "") {
        setFormData({ ...formData, [name]: "" });
        return;
      }
      
      // For numeric inputs, format with 2 decimal places
      const numValue = parseFloat(value);
      if (!isNaN(numValue)) {
        // Store as a string with 2 decimal places to preserve trailing zeros
        setFormData({ ...formData, [name]: numValue.toFixed(2) });
        return;
      }
    }
    
    // For other fields, no special formatting
    setFormData({ ...formData, [name]: value });
  };

  const handleEditBudget = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to edit a budget.");
        return;
      }

      const config = {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      };

      // Convert string amounts to numbers for submission
      const submissionData = {
        ...formData,
        monthlyBudget: parseFloat(formData.monthlyBudget),
        totalSpent: parseFloat(formData.totalSpent || 0)
      };

      await axios.put(`http://localhost:8080/api/budgets/update/${budgetId}`, submissionData, config);
      navigate("/budget"); // Redirect to the Budget page after successful update
    } catch (err) {
      console.error("Error editing budget:", err);
      if (err.response && err.response.data) {
        setError(err.response.data);
      } else {
        setError("Failed to edit budget. Please try again later.");
      }
    }
  };

  const handleDeleteBudget = async () => {
    if (!window.confirm("Are you sure you want to delete this budget?")) {
      return;
    }

    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to delete a budget.");
        return;
      }

      const config = {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      };

      await axios.delete(`http://localhost:8080/api/budgets/delete/${budgetId}`, config);
      navigate("/budget"); // Redirect to the Budget page after successful deletion
    } catch (err) {
      console.error("Error deleting budget:", err);
      setError("Failed to delete budget. Please try again later.");
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

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen bg-[#FEF6EA]">
        <p className="text-[#18864F] font-bold text-xl">Loading budget details...</p>
      </div>
    );
  }

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
                <h1 className="text-2xl font-bold text-[#18864F]">Edit Budget</h1>
              </div>

              {loading ? (
                <div className="flex justify-center items-center h-64">
                  <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[#18864F]"></div>
                  <span className="ml-3 text-[#18864F]">Loading budget and currency data...</span>
                </div>
              ) : (
                <form onSubmit={handleEditBudget}>
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
                        step="0.01" // This restricts input to 2 decimal places
                        required
                      />
                      <select
                        name="currency"
                        value={formData.currency}
                        onChange={handleInputChange}
                        className="p-3 border rounded-r-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                      >
                        {availableCurrencies.length > 0 ? (
                          availableCurrencies.map(currency => (
                            <option key={currency.code} value={currency.code}>
                              {currency.code} - {currency.name}
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

                  {/* Display Total Spent (read-only) */}
                  <div className="mb-4">
                    <label className="block text-[#18864F] font-bold mb-2">Total Spent</label>
                    <div className="p-3 border rounded-md bg-gray-100 text-gray-700 max-w-lg">
                      {new Intl.NumberFormat("en-US", {
                        style: "currency",
                        currency: formData.currency || "PHP",
                      }).format(formData.totalSpent)}
                    </div>
                  </div>

                  {error && <p className="text-red-500 text-sm mb-4">{error}</p>}

                  <div className="flex justify-between max-w-lg">
                    <button
                      type="submit"
                      className="bg-[#18864F] text-white font-bold py-2 px-4 rounded-md hover:bg-green-700 transition duration-300"
                    >
                      Save Changes
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate("/budget")}
                      className="bg-[#FEF6EA] text-[#18864F] font-bold py-2 px-4 rounded-md border border-[#18864F] hover:bg-[#EDFBE9] transition duration-300"
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      onClick={handleDeleteBudget}
                      className="hover:opacity-80"
                    >
                      <img
                        src={trashIcon}
                        alt="Delete"
                        className="h-6 w-6"
                      />
                    </button>
                  </div>
                </form>
              )}
            </div>

            {/* Right Section */}
            <div className="w-1/3 bg-[#F9F9F9] rounded-lg p-4 flex flex-col items-center justify-center">
              <div className="mb-4 text-center">
                <h3 className="font-bold text-[#18864F] text-xl mb-2">Budget Details</h3>
                <p className="text-gray-700 mb-4">
                  Edit your budget for this category. Any expenses in this category for the selected month will be tracked against this budget.
                </p>
                <p className="text-sm text-gray-500 mt-4">
                  Note: Changing the month or category may affect which expenses are tracked against this budget.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditBudget;