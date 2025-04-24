import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import axios from "axios";
import trashIcon from "../assets/trash.png"; // Import the trash icon
import { getCurrencyName, fetchAvailableCurrencies } from "./getCurrency";

const EditExpense = () => {
  const [formData, setFormData] = useState({
    subject: "",
    category: "",
    date: "",
    amount: "",
    currency: "PHP",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [availableCurrencies, setAvailableCurrencies] = useState([]);
  const navigate = useNavigate();
  const { expenseId } = useParams();

  useEffect(() => {
    const initializeData = async () => {
      try {
        const authToken = localStorage.getItem("authToken");
        if (!authToken) {
          setError("You must be logged in to edit an expense.");
          navigate("/login");
          return;
        }

        const config = {
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        };

        // Fetch expense data
        const response = await axios.get(`http://localhost:8080/api/expenses/getExpense/${expenseId}`, config);
        
        // Format numeric values to 2 decimal places when setting initial data
        const data = response.data;
        if (data.amount) {
          data.amount = parseFloat(data.amount).toFixed(2);
        }
        
        setFormData(data);
        
        // Fetch available currencies
        await fetchAvailableCurrencies(setAvailableCurrencies);
      } catch (err) {
        console.error("Error fetching expense:", err);
        setError("Failed to fetch expense details. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    initializeData();
  }, [expenseId, navigate]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    
    // Handle amount field with 2 decimal places
    if (name === "amount") {
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

  const handleEditExpense = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to edit an expense.");
        return;
      }

      const config = {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      };

      // Convert string amount to number for submission
      const submissionData = {
        ...formData,
        amount: parseFloat(formData.amount)
      };

      await axios.put(`http://localhost:8080/api/expenses/putExpense/${expenseId}`, submissionData, config);
      navigate("/expense"); // Redirect to the Expense page after successful update
    } catch (err) {
      console.error("Error editing expense:", err);
      setError("Failed to edit expense. Please try again later.");
    }
  };

  const handleDeleteExpense = async () => {
    if (!window.confirm("Are you sure you want to delete this expense?")) {
      return;
    }

    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to delete an expense.");
        return;
      }

      const config = {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      };

      await axios.delete(`http://localhost:8080/api/expenses/deleteExpense/${expenseId}`, config);
      navigate("/expense"); // Redirect to the Expense page after successful deletion
    } catch (err) {
      console.error("Error deleting expense:", err);
      setError("Failed to delete expense. Please try again later.");
    }
  };

  if (loading) {
    return <p>Loading expense details...</p>;
  }

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
          <div className="bg-white p-12 rounded-lg shadow-md w-full max-w-7xl flex" style={{ height: "650px" }}>
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
                <h1 className="text-2xl font-bold text-[#18864F]">Edit Expense</h1>
              </div>

              <form onSubmit={handleEditExpense}>
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
                    onClick={() => navigate("/expense")}
                    className="bg-[#FEF6EA] text-[#18864F] font-bold py-2 px-4 rounded-md border border-[#18864F] hover:bg-[#EDFBE9] transition duration-300"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={handleDeleteExpense}
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
            </div>

            {/* Right Section: Empty for Future Use */}
            <div className="w-1/3 bg-[#F9F9F9] rounded-lg p-4 flex items-center justify-center">
              <p className="text-gray-500">Future content goes here</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditExpense;