import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import editIcon from "/assets/edit.png"; // Import the edit icon

const Expense = () => {
  const [expenses, setExpenses] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10); // Set to 10 items per page
  const [error, setError] = useState("");
  
  // Get current month and year
  const currentDate = new Date();
  const currentMonthIndex = currentDate.getMonth() + 1; // JavaScript months are 0-indexed
  const currentYear = currentDate.getFullYear();
  
  const [selectedMonth, setSelectedMonth] = useState(currentMonthIndex); // Default to current month
  const [selectedYear, setSelectedYear] = useState(currentYear); // Default to current year
  const navigate = useNavigate();

  const apiUrl = "https://myalkansya-sia.as.r.appspot.com/api/expenses";

  // Generate array of months for dropdown
  const months = [
    { value: 0, label: "All Months" },
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

  // Generate array of years (current year - 2 to current year + 1)
  const years = [
    { value: 0, label: "All Years" },
    { value: currentYear - 2, label: (currentYear - 2).toString() },
    { value: currentYear - 1, label: (currentYear - 1).toString() },
    { value: currentYear, label: currentYear.toString() },
    { value: currentYear + 1, label: (currentYear + 1).toString() },
  ];

  // Fetch all expenses with client-side filtering
  const fetchExpenses = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to access this page");
        setTimeout(() => navigate("/login"), 2000);
        return;
      }

      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      };
      
      // Get all expenses from the backend
      const response = await axios.get(`${apiUrl}/getExpenses`, config);
      
      // Apply client-side filtering
      let filteredExpenses = response.data;
      
      if (selectedMonth > 0 || selectedYear > 0) {
        filteredExpenses = response.data.filter(expense => {
          // Parse the expense date string to a Date object
          const expenseDate = new Date(expense.date);
          
          // Get the month (1-12) and year from the date
          const expenseMonth = expenseDate.getMonth() + 1; // JavaScript months are 0-indexed
          const expenseYear = expenseDate.getFullYear();
          
          // Check if the expense matches the selected filters
          const monthMatches = selectedMonth === 0 || expenseMonth === selectedMonth;
          const yearMatches = selectedYear === 0 || expenseYear === selectedYear;
          
          // Return true only if both month and year match the filters
          return monthMatches && yearMatches;
        });
      }
      
      // Update state with filtered expenses
      setExpenses(filteredExpenses);
      setCurrentPage(1); // Reset to first page when filters change
    } catch (error) {
      console.error("Error fetching expenses:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      } else {
        setError("Error fetching expense data. Please try again later.");
      }
    }
  };

  useEffect(() => {
    fetchExpenses();
  }, [selectedMonth, selectedYear, navigate]);

  // Reset filters to current month and year
  const handleResetFilters = () => {
    setSelectedMonth(currentMonthIndex);
    setSelectedYear(currentYear);
  };

  // Pagination logic
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentExpenses = expenses.slice(indexOfFirstItem, indexOfLastItem);

  const totalPages = Math.ceil(expenses.length / itemsPerPage);

  const handleNextPage = () => {
    if (currentPage < totalPages) {
      setCurrentPage(currentPage + 1);
    }
  };

  const handlePreviousPage = () => {
    if (currentPage > 1) {
      setCurrentPage(currentPage - 1);
    }
  };

  if (error) {
    return (
      <div>
        <h1>Error</h1>
        <p style={{ color: "red" }}>{error}</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="expense" />

        {/* Main Content */}
        <div className="flex-1 p-6 ml-72 bg-[#FEF6EA]">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold text-[#18864F]">Expense Management</h1>
            <button
              onClick={() => navigate("/addexpense")}
              className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-md hover:bg-yellow-500 transition duration-300"
            >
              Add Expense
            </button>
          </div>
          
          {/* Month and Year Filter - Same styling as income.jsx */}
          <div className="mb-6 bg-white p-4 rounded-md shadow-sm">
            <div className="flex flex-col sm:flex-row justify-between items-center">
              <div className="flex flex-col sm:flex-row items-center mb-3 sm:mb-0">
                <h2 className="text-lg font-semibold text-[#18864F] mr-4">Filters</h2>
                <div className="flex flex-wrap gap-3">
                  <div className="flex items-center">
                    <label className="text-[#18864F] mr-2 font-medium">Month:</label>
                    <select
                      value={selectedMonth}
                      onChange={(e) => setSelectedMonth(Number(e.target.value))}
                      className="border border-gray-300 rounded-md py-1 px-3 bg-white text-[#18864F] focus:outline-none focus:ring-2 focus:ring-[#FFC107]"
                    >
                      {months.map((month) => (
                        <option key={month.value} value={month.value}>
                          {month.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="flex items-center">
                    <label className="text-[#18864F] mr-2 font-medium">Year:</label>
                    <select
                      value={selectedYear}
                      onChange={(e) => setSelectedYear(Number(e.target.value))}
                      className="border border-gray-300 rounded-md py-1 px-3 bg-white text-[#18864F] focus:outline-none focus:ring-2 focus:ring-[#FFC107]"
                    >
                      {years.map((year) => (
                        <option key={year.value} value={year.value}>
                          {year.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
              <button
                onClick={handleResetFilters}
                className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-md hover:bg-yellow-500 transition duration-300"
              >
                Reset Filters
              </button>
            </div>
            {(selectedMonth > 0 || selectedYear > 0) && (
              <div className="mt-3 px-2 py-1 bg-[#EDFBE9] text-[#18864F] rounded-md inline-block">
                <span className="font-medium">Active filters:</span>
                {selectedMonth > 0 && (
                  <span className="ml-2 px-2 py-0.5 bg-[#FFC107] rounded-md text-sm">
                    Month: {months.find(m => m.value === selectedMonth)?.label}
                  </span>
                )}
                {selectedYear > 0 && (
                  <span className="ml-2 px-2 py-0.5 bg-[#FFC107] rounded-md text-sm">
                    Year: {selectedYear}
                  </span>
                )}
              </div>
            )}
          </div>

          {/* Fixed Header - Adjust column names based on your expense fields */}
          <div className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-t-md mb-2">
            <div className="grid grid-cols-4">
              <div>Date</div>
              <div>Category</div>
              <div className="text-right">Amount</div>
              <div className="text-center">Actions</div>
            </div>
          </div>

          {/* Expense Data Container */}
          <div className="bg-white rounded-b-md shadow-md" style={{ height: "500px" }}>
            {currentExpenses.length === 0 ? (
              <p className="text-center text-gray-500 py-4">No expense records found.</p>
            ) : (
              currentExpenses.map((expense) => (
                <div
                  key={expense.id}
                  className="grid grid-cols-4 py-2 px-4 border-b last:border-none"
                >
                  <div>{expense.date}</div>
                  <div>{expense.category}</div>
                  <div className="text-right">
                    {new Intl.NumberFormat("en-US", {
                      style: "currency",
                      currency: expense.currency || "USD",
                    }).format(expense.amount)}
                  </div>
                  <div className="text-center">
                    <button
                      onClick={() => navigate(`/editexpense/${expense.id}`)}
                      className="hover:opacity-80"
                    >
                      <img
                        src={editIcon}
                        alt="Edit"
                        className="h-5 w-5 inline-block"
                      />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Pagination */}
          <div className="flex justify-end items-center mt-4">
            <button
              onClick={handlePreviousPage}
              disabled={currentPage === 1}
              className={`bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-l-md ${
                currentPage === 1 ? "opacity-50 cursor-not-allowed" : "hover:bg-yellow-500"
              }`}
            >
              &lt;
            </button>
            <div className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4">
              {currentPage} out of {totalPages}
            </div>
            <button
              onClick={handleNextPage}
              disabled={currentPage === totalPages}
              className={`bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-r-md ${
                currentPage === totalPages ? "opacity-50 cursor-not-allowed" : "hover:bg-yellow-500"
              }`}
            >
              &gt;
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Expense;