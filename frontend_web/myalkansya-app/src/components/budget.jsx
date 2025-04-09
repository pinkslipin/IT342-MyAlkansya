import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import editIcon from "../assets/edit.png"; // Import the edit icon

const Budget = () => {
  const [budgets, setBudgets] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10); // Set to 10 items per page
  const [error, setError] = useState("");
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1); // Current month (1-12)
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear()); // Current year
  const navigate = useNavigate();

  const apiUrl = "http://localhost:8080/api/budgets";

  // Fetch budgets for the selected month and year
  const fetchBudgets = async () => {
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

      // Use the specific endpoint for month/year filtering
      const response = await axios.get(
        `${apiUrl}/getBudgetsByMonth/${selectedMonth}/${selectedYear}`, 
        config
      );
      setBudgets(response.data);
    } catch (error) {
      console.error("Error fetching budgets:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      } else {
        setError("Error fetching budget data. Please try again later.");
      }
    }
  };

  useEffect(() => {
    fetchBudgets();
  }, [selectedMonth, selectedYear, navigate]);

  // Pagination logic
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentBudgets = budgets.slice(indexOfFirstItem, indexOfLastItem);

  const totalPages = Math.ceil(budgets.length / itemsPerPage);

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
        <Sidebar activePage="budget" />

        {/* Main Content */}
        <div className="flex-1 p-6 ml-72 bg-[#FEF6EA]">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold text-[#18864F]">Budget Management</h1>
            <button
              onClick={() => navigate("/addbudget")}
              className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-md hover:bg-yellow-500 transition duration-300"
            >
              Add Budget
            </button>
          </div>

          {/* Month and Year Filter */}
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
                        <option key={year} value={year}>
                          {year}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
              <button
                onClick={() => {
                  setSelectedMonth(new Date().getMonth() + 1);
                  setSelectedYear(new Date().getFullYear());
                }}
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
                    Month: {months.find((m) => m.value === selectedMonth)?.label}
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

          {/* Fixed Header */}
          <div className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-t-md mb-2">
            <div className="grid grid-cols-6">
              <div>Category</div>
              <div className="text-right">Monthly Budget</div>
              <div className="text-right">Total Spent</div>
              <div className="text-right">Remaining</div>
              <div className="text-center">Spending Progress</div>
              <div className="text-center">Actions</div>
            </div>
          </div>

          {/* Budget Data Container */}
          <div className="bg-white rounded-b-md shadow-md" style={{ height: "500px" }}>
            {currentBudgets.length === 0 ? (
              <p className="text-center text-gray-500 py-4">
                No budget records found for {months.find(m => m.value === selectedMonth)?.label} {selectedYear}.
              </p>
            ) : (
              currentBudgets.map((budget) => {
                const remaining = budget.monthlyBudget - budget.totalSpent;
                const percentSpent = (budget.totalSpent / budget.monthlyBudget) * 100;
                const progressColor =
                  percentSpent > 90 ? "#dc3545" : percentSpent > 70 ? "#ffc107" : "#28a745";

                return (
                  <div
                    key={budget.id}
                    className="grid grid-cols-6 py-2 px-4 border-b last:border-none"
                  >
                    <div>{budget.category}</div>
                    <div className="text-right">
                      {new Intl.NumberFormat("en-US", {
                        style: "currency",
                        currency: budget.currency || "USD",
                      }).format(budget.monthlyBudget)}
                    </div>
                    <div className="text-right">
                      {new Intl.NumberFormat("en-US", {
                        style: "currency",
                        currency: budget.currency || "USD",
                      }).format(budget.totalSpent)}
                    </div>
                    <div
                      className="text-right"
                      style={{ color: remaining < 0 ? "#dc3545" : "#28a745" }}
                    >
                      {new Intl.NumberFormat("en-US", {
                        style: "currency",
                        currency: budget.currency || "USD",
                      }).format(remaining)}
                    </div>
                    <div className="text-center">
                      <div
                        style={{
                          width: "150px", // Adjusted width for a shorter progress bar
                          backgroundColor: "#e9ecef",
                          borderRadius: "4px",
                          overflow: "hidden",
                          margin: "0 auto",
                        }}
                      >
                        <div
                          style={{
                            width: `${Math.min(percentSpent, 100)}%`,
                            height: "20px",
                            backgroundColor: progressColor,
                            textAlign: "center",
                            color: "white",
                            fontSize: "12px",
                            lineHeight: "20px",
                          }}
                        >
                          {percentSpent.toFixed(0)}%
                        </div>
                      </div>
                    </div>
                    <div className="text-center">
                      <button
                        onClick={() => navigate(`/editbudget/${budget.id}`)}
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
                );
              })
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
              {currentPage} out of {totalPages || 1}
            </div>
            <button
              onClick={handleNextPage}
              disabled={currentPage === totalPages || totalPages === 0}
              className={`bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-r-md ${
                currentPage === totalPages || totalPages === 0 ? "opacity-50 cursor-not-allowed" : "hover:bg-yellow-500"
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

export default Budget;