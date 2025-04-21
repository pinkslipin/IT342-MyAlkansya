import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import editIcon from "../assets/edit.png"; // Import the edit icon

const SavingsGoal = () => {
  const [savingsGoals, setSavingsGoals] = useState([]);
  const [error, setError] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(8); // Set to 10 items per page
  const navigate = useNavigate();

  const apiUrl = "http://localhost:8080/api/savings-goals";

  // Fetch all savings goals
  const fetchSavingsGoals = async () => {
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

      const response = await axios.get(`${apiUrl}/getSavingsGoals`, config);
      setSavingsGoals(response.data);
    } catch (error) {
      console.error("Error fetching savings goals:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      } else {
        setError("Error fetching savings goals data. Please try again later.");
      }
    }
  };

  useEffect(() => {
    fetchSavingsGoals();
  }, [navigate]);

  const calculateProgress = (currentAmount, targetAmount) => {
    if (targetAmount <= 0) return 0;
    const progress = (currentAmount / targetAmount) * 100;
    return Math.min(100, Math.max(0, progress)); // Ensure between 0 and 100
  };

  // Pagination logic
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentSavingsGoals = savingsGoals.slice(indexOfFirstItem, indexOfLastItem);

  const totalPages = Math.ceil(savingsGoals.length / itemsPerPage);

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
        <Sidebar activePage="savingsgoal" />

        {/* Main Content */}
        <div className="flex-1 p-6 ml-72 bg-[#FEF6EA]">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold text-[#18864F]">Savings Goals</h1>
            <button
              onClick={() => navigate("/addsavingsgoal")}
              className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-md hover:bg-yellow-500 transition duration-300"
            >
              Add Savings Goal
            </button>
          </div>

          {/* Fixed Header */}
          <div className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-t-md mb-2">
            <div className="grid grid-cols-6">
              <div>Goal</div>
              <div className="text-right">Target Amount</div>
              <div className="text-right">Current Amount</div>
              <div className="text-center">Target Date</div>
              <div className="text-center">Progress</div>
              <div className="text-center">Actions</div>
            </div>
          </div>

          {/* Savings Goals Data Container */}
          <div className="bg-white rounded-b-md shadow-md" style={{ height: "500px" }}>
            {currentSavingsGoals.length === 0 ? (
              <p className="text-center text-gray-500 py-4">No savings goals found.</p>
            ) : (
              currentSavingsGoals.map((goal) => {
                const progressPercentage = calculateProgress(goal.currentAmount, goal.targetAmount);
                const progressColor =
                  progressPercentage > 90 ? "#28a745" : progressPercentage > 50 ? "#ffc107" : "#dc3545";

                const remaining = goal.targetAmount - goal.currentAmount;
                const targetDate = new Date(goal.targetDate);
                const formattedDate = targetDate.toLocaleDateString();
                const daysRemaining = Math.ceil((targetDate - new Date()) / (1000 * 60 * 60 * 24));

                return (
                  <div
                    key={goal.id}
                    className="grid grid-cols-6 py-2 px-4 border-b last:border-none"
                  >
                    <div>{goal.goal}</div>
                    <div className="text-right">
                      {new Intl.NumberFormat("en-US", {
                        style: "currency",
                        currency: goal.currency || "USD",
                      }).format(goal.targetAmount)}
                    </div>
                    <div className="text-right">
                      {new Intl.NumberFormat("en-US", {
                        style: "currency",
                        currency: goal.currency || "USD",
                      }).format(goal.currentAmount)}
                    </div>
                    <div className="text-center">
                      {formattedDate}
                      <div
                        className="text-sm"
                        style={{ color: daysRemaining < 0 ? "#dc3545" : "#6c757d" }}
                      >
                        {daysRemaining < 0
                          ? `${Math.abs(daysRemaining)} days overdue`
                          : `${daysRemaining} days left`}
                      </div>
                    </div>
                    <div className="text-center">
                      <div
                        style={{
                          width: "150px",
                          backgroundColor: "#e9ecef",
                          borderRadius: "4px",
                          overflow: "hidden",
                          margin: "0 auto",
                        }}
                      >
                        <div
                          style={{
                            width: `${progressPercentage}%`,
                            height: "20px",
                            backgroundColor: progressColor,
                            textAlign: "center",
                            color: "white",
                            fontSize: "12px",
                            lineHeight: "20px",
                          }}
                        >
                          {progressPercentage.toFixed(0)}%
                        </div>
                      </div>
                      <div className="text-sm mt-1">
                        {remaining > 0
                          ? `${remaining.toFixed(2)} ${goal.currency} to go`
                          : "Goal achieved! 🎉"}
                      </div>
                    </div>
                    <div className="text-center">
                      <button
                        onClick={() => navigate(`/editsavingsgoal/${goal.id}`)}
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

export default SavingsGoal;