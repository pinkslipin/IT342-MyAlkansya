import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import editIcon from "/assets/edit.png"; // Import the edit icon


const SavingsGoal = () => {
  const [savingsGoals, setSavingsGoals] = useState([]);
  const [error, setError] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(8); // Set to 10 items per page
  const navigate = useNavigate();
  const [showAddAmount, setShowAddAmount] = useState(null); // goalId or null
  const [addAmountValue, setAddAmountValue] = useState("");
  const [expandedGoal, setExpandedGoal] = useState(null); // goalId or null
  const [goalExpenses, setGoalExpenses] = useState({}); // {goalId: [expenses]}
  const [loadingExpenses, setLoadingExpenses] = useState({});

  const apiUrl = "https://myalkansya-sia.as.r.appspot.com/api/savings-goals";

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

  // Fetch expenses for a savings goal
  const fetchGoalExpenses = async (goal) => {
    setLoadingExpenses((prev) => ({ ...prev, [goal.id]: true }));
    try {
      const authToken = localStorage.getItem("authToken");
      const config = { headers: { Authorization: `Bearer ${authToken}` } };
      // Fetch expenses with subject = goal.goal and category = "Savings Goal"
      const res = await axios.get(
        `https://myalkansya-sia.as.r.appspot.com/api/expenses/getExpensesByGoal?goal=${encodeURIComponent(goal.goal)}`,
        config
      );
      setGoalExpenses((prev) => ({ ...prev, [goal.id]: res.data }));
    } catch (err) {
      setGoalExpenses((prev) => ({ ...prev, [goal.id]: [] }));
    } finally {
      setLoadingExpenses((prev) => ({ ...prev, [goal.id]: false }));
    }
  };

  // Handle add amount submit
  const handleAddAmount = async (goal) => {
    try {
      const authToken = localStorage.getItem("authToken");
      const config = { headers: { Authorization: `Bearer ${authToken}` } };
      await axios.post(
        "https://myalkansya-sia.as.r.appspot.com/api/expenses/postExpense",
        {
          subject: goal.goal,
          category: "Savings Goal",
          amount: parseFloat(addAmountValue),
          currency: goal.currency,
          date: new Date().toISOString().slice(0, 10),
        },
        config
      );
      setShowAddAmount(null);
      setAddAmountValue("");
      // Optionally, refresh savings goals and expenses
      fetchSavingsGoals();
      fetchGoalExpenses(goal);
    } catch (err) {
      alert("Failed to add amount: " + (err.response?.data || err.message));
    }
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
                    className="border-b last:border-none"
                  >
                    <div className="grid grid-cols-6 py-2 px-4">
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
                          onClick={() => setShowAddAmount(goal.id)}
                          className="bg-[#18864F] text-white font-bold py-1 px-3 rounded hover:bg-green-700 mr-2"
                        >
                          Add Amount
                        </button>
                        <button
                          onClick={() => {
                            setExpandedGoal(expandedGoal === goal.id ? null : goal.id);
                            if (expandedGoal !== goal.id) fetchGoalExpenses(goal);
                          }}
                          className="bg-[#FFC107] text-[#18864F] font-bold py-1 px-3 rounded hover:bg-yellow-500"
                        >
                          {expandedGoal === goal.id ? "Hide" : "Show"} History
                        </button>
                        <button
                          onClick={() => navigate(`/editsavingsgoal/${goal.id}`)}
                          className="hover:opacity-80 ml-2"
                        >
                          <img
                            src={editIcon}
                            alt="Edit"
                            className="h-5 w-5 inline-block"
                          />
                        </button>
                      </div>
                    </div>
                    {/* Add Amount Modal */}
                    {showAddAmount === goal.id && (
                      <div className="bg-[#EDFBE9] p-4 rounded-md my-2 flex items-center gap-2">
                        <input
                          type="number"
                          min="0.01"
                          step="0.01"
                          value={addAmountValue}
                          onChange={(e) => setAddAmountValue(e.target.value)}
                          className="p-2 border rounded text-[#18864F] font-bold"
                          placeholder="Enter amount"
                        />
                        <button
                          onClick={() => handleAddAmount(goal)}
                          className="bg-[#18864F] text-white font-bold py-1 px-3 rounded hover:bg-green-700"
                        >
                          Confirm
                        </button>
                        <button
                          onClick={() => setShowAddAmount(null)}
                          className="ml-2 text-[#dc3545] font-bold"
                        >
                          Cancel
                        </button>
                      </div>
                    )}
                    {/* Dropdown for expense history */}
                    {expandedGoal === goal.id && (
                      <div className="bg-gray-50 px-8 py-2 border-t border-gray-200">
                        <h3 className="text-sm font-semibold text-[#18864F] mb-2">Savings Additions History</h3>
                        {loadingExpenses[goal.id] ? (
                          <div className="text-center py-4">
                            <div className="inline-block animate-spin rounded-full h-6 w-6 border-t-2 border-b-2 border-[#18864F]"></div>
                            <p className="text-sm text-gray-600 mt-2">Loading...</p>
                          </div>
                        ) : (goalExpenses[goal.id]?.length > 0 ? (
                          <div>
                            <div className="grid grid-cols-4 text-sm font-medium text-gray-600 mb-1 px-2">
                              <div>Date</div>
                              <div>Subject</div>
                              <div className="text-right">Amount</div>
                              <div className="text-right">Currency</div>
                            </div>
                            {goalExpenses[goal.id].map(exp => (
                              <div key={exp.id} className="grid grid-cols-4 text-sm border-b border-gray-200 px-2 py-1">
                                <div>{exp.date}</div>
                                <div>{exp.subject}</div>
                                <div className="text-right">
                                  {new Intl.NumberFormat("en-US", {
                                    style: "currency",
                                    currency: exp.currency || goal.currency,
                                  }).format(exp.amount)}
                                </div>
                                <div className="text-right">{exp.currency}</div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <p className="text-sm text-gray-500 py-2">No additions found for this goal.</p>
                        ))}
                      </div>
                    )}
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