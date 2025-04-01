import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const Budget = () => {
  const [budgets, setBudgets] = useState([]);
  const [formData, setFormData] = useState({
    category: "",
    monthlyBudget: "",
    currency: "",
  });
  const [editingBudget, setEditingBudget] = useState(null);
  const [error, setError] = useState("");
  const [categories] = useState([
    "Food", "Transportation", "Housing", "Utilities", 
    "Entertainment", "Healthcare", "Education", "Shopping", "Other"
  ]);
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
          Authorization: `Bearer ${authToken}`
        }
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

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  // Add a new budget
  const addBudget = async (e) => {
    e.preventDefault();
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      // We don't include totalSpent as it will be calculated from linked expenses
      const budgetData = {
        category: formData.category,
        monthlyBudget: formData.monthlyBudget,
        currency: formData.currency
      };

      const response = await axios.post(`${apiUrl}/postBudget`, budgetData, config);
      setBudgets([...budgets, response.data]);
      setFormData({ category: "", monthlyBudget: "", currency: "" });
    } catch (error) {
      console.error("Error adding budget:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Update a budget
  const updateBudget = async (e) => {
    e.preventDefault();
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      // Only update category and monthlyBudget, totalSpent is managed by expenses
      const budgetData = {
        category: formData.category,
        monthlyBudget: formData.monthlyBudget,
        currency: formData.currency
      };

      const response = await axios.put(
        `${apiUrl}/putBudget/${editingBudget.id}`,
        budgetData,
        config
      );
      setBudgets(
        budgets.map((budget) =>
          budget.id === editingBudget.id ? response.data : budget
        )
      );
      setEditingBudget(null);
      setFormData({ category: "", monthlyBudget: "", currency: "" });
    } catch (error) {
      console.error("Error updating budget:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Delete a budget
  const deleteBudget = async (id) => {
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      await axios.delete(`${apiUrl}/deleteBudget/${id}`, config);
      setBudgets(budgets.filter((budget) => budget.id !== id));
    } catch (error) {
      console.error("Error deleting budget:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Set budget for editing
  const editBudget = (budget) => {
    setEditingBudget(budget);
    setFormData({
      category: budget.category,
      monthlyBudget: budget.monthlyBudget,
      currency: budget.currency,
    });
  };

  useEffect(() => {
    fetchBudgets();
  }, [navigate]);

  if (error) {
    return (
      <div>
        <h1>Error</h1>
        <p style={{ color: "red" }}>{error}</p>
      </div>
    );
  }

  return (
    <div style={{ padding: "20px" }}>
      <h1>Budget Management</h1>
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
      
      <div style={{ 
        padding: "15px", 
        backgroundColor: "#e3f2fd", 
        borderRadius: "8px", 
        marginBottom: "20px",
        border: "1px solid #bbdefb" 
      }}>
        <p><strong>How budgets work:</strong></p>
        <p>When you create a budget for a category, it will automatically track any expenses you've already created under that category.</p>
        <p>The "Total Spent" is calculated from your existing expenses in that category.</p>
      </div>
      
      <form 
        onSubmit={editingBudget ? updateBudget : addBudget}
        style={{
          backgroundColor: "#f8f9fa",
          padding: "20px",
          borderRadius: "8px",
          marginBottom: "20px"
        }}
      >
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Category:</label>
          <select
            name="category"
            value={formData.category}
            onChange={handleInputChange}
            required
            style={{ width: "100%", padding: "8px" }}
          >
            <option value="">Select a category</option>
            {categories.map(category => (
              <option key={category} value={category}>{category}</option>
            ))}
          </select>
        </div>
        
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Monthly Budget Amount:</label>
          <input
            type="number"
            name="monthlyBudget"
            placeholder="How much do you plan to spend?"
            value={formData.monthlyBudget}
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
              backgroundColor: editingBudget ? "#ffc107" : "#28a745",
              color: "white",
              padding: "10px 15px",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              marginRight: "10px"
            }}
          >
            {editingBudget ? "Update" : "Add"} Budget
          </button>
          
          {editingBudget && (
            <button
              type="button"
              onClick={() => {
                setEditingBudget(null);
                setFormData({ category: "", monthlyBudget: "", currency: "" });
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

      <h2>Budget List</h2>
      {budgets.length === 0 ? (
        <p>No budget records found. Add your first budget record above!</p>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", marginTop: "10px" }}>
            <thead>
              <tr style={{ backgroundColor: "#f8f9fa" }}>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Category</th>
                <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Monthly Budget</th>
                <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Total Spent</th>
                <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Remaining</th>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Currency</th>
                <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Progress</th>
                <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {budgets.map((budget) => {
                const remaining = budget.monthlyBudget - budget.totalSpent;
                const percentSpent = (budget.totalSpent / budget.monthlyBudget) * 100;
                const progressColor = percentSpent > 90 ? "#dc3545" : percentSpent > 70 ? "#ffc107" : "#28a745";
                
                return (
                  <tr key={budget.id}>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{budget.category}</td>
                    <td style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>
                      {typeof budget.monthlyBudget === 'number' ? budget.monthlyBudget.toFixed(2) : budget.monthlyBudget}
                    </td>
                    <td style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>
                      {typeof budget.totalSpent === 'number' ? budget.totalSpent.toFixed(2) : budget.totalSpent}
                    </td>
                    <td style={{ 
                      padding: "10px", 
                      textAlign: "right", 
                      borderBottom: "1px solid #dee2e6",
                      color: remaining < 0 ? "#dc3545" : "#28a745"
                    }}>
                      {typeof remaining === 'number' ? remaining.toFixed(2) : remaining}
                    </td>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{budget.currency}</td>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>
                      <div style={{ 
                        width: "100%", 
                        backgroundColor: "#e9ecef",
                        borderRadius: "4px",
                        overflow: "hidden"
                      }}>
                        <div 
                          style={{ 
                            width: `${Math.min(percentSpent, 100)}%`, 
                            height: "20px",
                            backgroundColor: progressColor,
                            textAlign: "center",
                            color: "white",
                            fontSize: "12px",
                            lineHeight: "20px"
                          }}
                        >
                          {percentSpent.toFixed(0)}%
                        </div>
                      </div>
                    </td>
                    <td style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>
                      <button
                        onClick={() => editBudget(budget)}
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
                        Edit
                      </button>
                      <button
                        onClick={() => deleteBudget(budget.id)}
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

export default Budget;