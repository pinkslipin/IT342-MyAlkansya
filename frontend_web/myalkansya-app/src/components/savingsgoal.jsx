import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const SavingsGoal = () => {
  const [savingsGoals, setSavingsGoals] = useState([]);
  const [formData, setFormData] = useState({
    goal: "",
    targetAmount: "",
    currentAmount: "0",
    targetDate: "",
    currency: "",
  });
  const [editingGoal, setEditingGoal] = useState(null);
  const [error, setError] = useState("");
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  const apiUrl = "http://localhost:8080/api/savings-goals";

  // Fetch user data to display total savings
  const fetchUserInfo = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) return;

      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      let response;
      try {
        response = await axios.get("http://localhost:8080/api/users/me", config);
      } catch (innerErr) {
        response = await axios.get("http://localhost:8080/user-info", config);
      }
      
      setUser(response.data);
    } catch (error) {
      console.error("Error fetching user info:", error);
    }
  };

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
          Authorization: `Bearer ${authToken}`
        }
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

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  // Add a new savings goal
  const addSavingsGoal = async (e) => {
    e.preventDefault();
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      const response = await axios.post(`${apiUrl}/postSavingsGoal`, formData, config);
      setSavingsGoals([...savingsGoals, response.data]);
      setFormData({
        goal: "",
        targetAmount: "",
        currentAmount: "0",
        targetDate: "",
        currency: "",
      });
    } catch (error) {
      console.error("Error adding savings goal:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Update a savings goal
  const updateSavingsGoal = async (e) => {
    e.preventDefault();
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      const response = await axios.put(
        `${apiUrl}/putSavingsGoal/${editingGoal.id}`,
        formData,
        config
      );
      setSavingsGoals(
        savingsGoals.map((goal) =>
          goal.id === editingGoal.id ? response.data : goal
        )
      );
      setEditingGoal(null);
      setFormData({
        goal: "",
        targetAmount: "",
        currentAmount: "0",
        targetDate: "",
        currency: "",
      });
    } catch (error) {
      console.error("Error updating savings goal:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Delete a savings goal
  const deleteSavingsGoal = async (id) => {
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      await axios.delete(`${apiUrl}/deleteSavingsGoal/${id}`, config);
      setSavingsGoals(savingsGoals.filter((goal) => goal.id !== id));
    } catch (error) {
      console.error("Error deleting savings goal:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Set savings goal for editing
  const editSavingsGoal = (goal) => {
    setEditingGoal(goal);
    setFormData({
      goal: goal.goal,
      targetAmount: goal.targetAmount,
      currentAmount: goal.currentAmount,
      targetDate: goal.targetDate,
      currency: goal.currency,
    });
  };

  // Calculate progress percentage for a savings goal
  const calculateProgress = (currentAmount, targetAmount) => {
    if (targetAmount <= 0) return 0;
    const progress = (currentAmount / targetAmount) * 100;
    return Math.min(100, Math.max(0, progress)); // Ensure between 0 and 100
  };

  useEffect(() => {
    fetchSavingsGoals();
    fetchUserInfo();
  }, [navigate]);

  if (error) {
    return (
      <div>
        <h1>Error</h1>
        <p style={{ color: "red" }}>{error}</p>
      </div>
    );
  }

  // Format total savings if user data is available
  const formattedSavings = user && user.totalSavings !== undefined ? 
    new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: user.currency || 'USD',
      minimumFractionDigits: 2
    }).format(user.totalSavings) : 'Loading...';

  return (
    <div style={{ padding: "20px" }}>
      <h1>Savings Goals</h1>
      <button 
        onClick={() => navigate("/")} 
        style={{ 
          marginBottom: "20px",
          padding: "8px 16px",
          backgroundColor: "#6c757d",
          color: "white",
          border: "none",
          borderRadius: "4px",
          cursor: "pointer"
        }}
      >
        Back to Home
      </button>
      
      {/* Display total savings */}
      {user && (
        <div style={{ 
          backgroundColor: '#f0f8ff', 
          padding: '15px', 
          borderRadius: '8px', 
          margin: '20px auto',
          maxWidth: '300px',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
        }}>
          <h3 style={{ margin: '0 0 10px 0', color: '#28a745' }}>Total Savings</h3>
          <p style={{ 
            fontSize: '24px', 
            fontWeight: 'bold', 
            margin: '0',
            color: user.totalSavings >= 0 ? '#28a745' : '#dc3545'
          }}>
            {formattedSavings}
          </p>
        </div>
      )}
      
      <form 
        onSubmit={editingGoal ? updateSavingsGoal : addSavingsGoal}
        style={{
          backgroundColor: "#f8f9fa",
          padding: "20px",
          borderRadius: "8px",
          marginBottom: "20px"
        }}
      >
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Goal Name:</label>
          <input
            type="text"
            name="goal"
            placeholder="What are you saving for?"
            value={formData.goal}
            onChange={handleInputChange}
            required
            style={{ width: "100%", padding: "8px" }}
          />
        </div>
        
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Target Amount:</label>
          <input
            type="number"
            name="targetAmount"
            placeholder="How much do you want to save?"
            value={formData.targetAmount}
            onChange={handleInputChange}
            required
            style={{ width: "100%", padding: "8px" }}
          />
        </div>
        
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Current Amount:</label>
          <input
            type="number"
            name="currentAmount"
            placeholder="How much have you saved so far?"
            value={formData.currentAmount}
            onChange={handleInputChange}
            required
            style={{ width: "100%", padding: "8px" }}
          />
        </div>
        
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Target Date:</label>
          <input
            type="date"
            name="targetDate"
            value={formData.targetDate}
            onChange={handleInputChange}
            required
            style={{ width: "100%", padding: "8px" }}
          />
        </div>
        
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Currency:</label>
          <input
            type="text"
            name="currency"
            placeholder="PHP, USD, etc."
            value={formData.currency}
            onChange={handleInputChange}
            required
            style={{ width: "100%", padding: "8px" }}
          />
        </div>
        
        <div>
          <button 
            type="submit"
            style={{
              backgroundColor: editingGoal ? "#ffc107" : "#28a745",
              color: "white",
              padding: "10px 15px",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              marginRight: "10px"
            }}
          >
            {editingGoal ? "Update" : "Add"} Savings Goal
          </button>
          
          {editingGoal && (
            <button
              type="button"
              onClick={() => {
                setEditingGoal(null);
                setFormData({
                  goal: "",
                  targetAmount: "",
                  currentAmount: "0",
                  targetDate: "",
                  currency: "",
                });
              }}
              style={{
                backgroundColor: "#6c757d",
                color: "white",
                padding: "10px 15px",
                border: "none",
                borderRadius: "4px",
                cursor: "pointer"
              }}
            >
              Cancel
            </button>
          )}
        </div>
      </form>

      <h2>Your Savings Goals</h2>
      {savingsGoals.length === 0 ? (
        <p>No savings goals found. Create your first savings goal above!</p>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", marginTop: "10px" }}>
            <thead>
              <tr style={{ backgroundColor: "#f8f9fa" }}>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Goal</th>
                <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Target Amount</th>
                <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Current Amount</th>
                <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Target Date</th>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Currency</th>
                <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Progress</th>
                <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {savingsGoals.map((goal) => {
                const progressPercentage = calculateProgress(goal.currentAmount, goal.targetAmount);
                const progressColor = progressPercentage > 90 ? "#28a745" : progressPercentage > 50 ? "#ffc107" : "#dc3545";
                
                // Calculate remaining amount
                const remaining = goal.targetAmount - goal.currentAmount;
                
                // Format date
                const targetDate = new Date(goal.targetDate);
                const formattedDate = targetDate.toLocaleDateString();
                
                // Calculate days remaining
                const today = new Date();
                const daysRemaining = Math.ceil((targetDate - today) / (1000 * 60 * 60 * 24));
                
                return (
                  <tr key={goal.id}>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{goal.goal}</td>
                    <td style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>
                      {typeof goal.targetAmount === 'number' ? goal.targetAmount.toFixed(2) : goal.targetAmount} {goal.currency}
                    </td>
                    <td style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>
                      {typeof goal.currentAmount === 'number' ? goal.currentAmount.toFixed(2) : goal.currentAmount} {goal.currency}
                    </td>
                    <td style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>
                      {formattedDate}
                      <div style={{ fontSize: "12px", color: daysRemaining < 0 ? "#dc3545" : "#6c757d" }}>
                        {daysRemaining < 0 ? `${Math.abs(daysRemaining)} days overdue` : `${daysRemaining} days left`}
                      </div>
                    </td>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{goal.currency}</td>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>
                      <div style={{ 
                        width: "100%", 
                        backgroundColor: "#e9ecef", 
                        borderRadius: "4px", 
                        overflow: "hidden" 
                      }}>
                        <div style={{
                          width: `${progressPercentage}%`,
                          height: "20px",
                          backgroundColor: progressColor,
                          textAlign: "center",
                          color: "white",
                          fontSize: "12px",
                          lineHeight: "20px"
                        }}>
                          {progressPercentage.toFixed(0)}%
                        </div>
                      </div>
                      <div style={{ fontSize: "12px", textAlign: "center", marginTop: "4px" }}>
                        {remaining > 0 ? 
                          `${remaining.toFixed(2)} ${goal.currency} to go` : 
                          `Goal achieved! 🎉`
                        }
                      </div>
                    </td>
                    <td style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>
                      <button
                        onClick={() => editSavingsGoal(goal)}
                        style={{
                          backgroundColor: "#ffc107",
                          color: "white",
                          padding: "5px 10px",
                          border: "none",
                          borderRadius: "4px",
                          cursor: "pointer",
                          marginRight: "5px"
                        }}
                      >
                        Add Savings
                      </button>
                      <button
                        onClick={() => deleteSavingsGoal(goal.id)}
                        style={{
                          backgroundColor: "#dc3545",
                          color: "white",
                          padding: "5px 10px",
                          border: "none",
                          borderRadius: "4px",
                          cursor: "pointer"
                        }}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default SavingsGoal;