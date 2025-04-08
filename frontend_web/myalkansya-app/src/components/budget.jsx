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
  const navigate = useNavigate();

  const apiUrl = "http://localhost:8080/api/budgets";

  // Fetch all budgets
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

      const response = await axios.get(`${apiUrl}/getBudgets`, config);
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
  }, [navigate]);

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
              <p className="text-center text-gray-500 py-4">No budget records found.</p>
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

export default Budget;