import React, { useState, useEffect, useRef } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const Expense = () => {
  const [expenses, setExpenses] = useState([]);
  const [budgets, setBudgets] = useState([]);
  const [formData, setFormData] = useState({
    subject: "",
    category: "",
    date: "",
    amount: "",
    currency: "",
  });
  const [editingExpense, setEditingExpense] = useState(null);
  const [error, setError] = useState("");
  const [user, setUser] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const formRef = useRef(null);
  const [categories] = useState([
    "Food", "Transportation", "Housing", "Utilities", 
    "Entertainment", "Healthcare", "Education", "Shopping", "Other"
  ]);
  const navigate = useNavigate();

  const apiUrl = "http://localhost:8080/api/expenses";
  const budgetApiUrl = "http://localhost:8080/api/budgets";

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

  // Fetch all expenses
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
          Authorization: `Bearer ${authToken}`
        }
      };

      const response = await axios.get(`${apiUrl}/getExpenses`, config);
      setExpenses(response.data);
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

  // Fetch all budgets
  const fetchBudgets = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) return;

      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      const response = await axios.get(`${budgetApiUrl}/getBudgets`, config);
      setBudgets(response.data);
    } catch (error) {
      console.error("Error fetching budgets:", error);
    }
  };

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  // Add a new expense
  const addExpense = async (e) => {
    e.preventDefault();
    
    // Prevent duplicate submissions
    if (isSubmitting) return;
    
    try {
      setIsSubmitting(true);
      
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`,
          'X-Submission-ID': Date.now().toString() // Add submission ID to prevent duplicates
        }
      };

      const response = await axios.post(`${apiUrl}/postExpense`, formData, config);
      
      // Update expenses state with new expense
      setExpenses(prev => {
        // Make sure we don't add duplicates
        const exists = prev.some(exp => exp.id === response.data.id);
        if (exists) return prev;
        return [...prev, response.data];
      });
      
      // Reset form
      setFormData({ subject: "", category: "", date: "", amount: "", currency: "" });
      
      // Refresh data to show updated total savings and budgets
      await fetchUserInfo();
      await fetchBudgets();
    } catch (error) {
      console.error("Error adding expense:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Update an expense
  const updateExpense = async (e) => {
    e.preventDefault();
    
    // Prevent duplicate submissions
    if (isSubmitting) return;
    
    try {
      setIsSubmitting(true);
      
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`,
          'X-Submission-ID': Date.now().toString() // Add submission ID to prevent duplicates
        }
      };

      const response = await axios.put(
        `${apiUrl}/putExpense/${editingExpense.id}`,
        formData,
        config
      );
      
      setExpenses(
        expenses.map((expense) =>
          expense.id === editingExpense.id ? response.data : expense
        )
      );
      
      setEditingExpense(null);
      setFormData({ subject: "", category: "", date: "", amount: "", currency: "" });
      
      // Refresh data to show updated total savings and budgets
      await fetchUserInfo();
      await fetchBudgets();
    } catch (error) {
      console.error("Error updating expense:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Delete an expense
  const deleteExpense = async (id) => {
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`,
          'X-Submission-ID': Date.now().toString() // Add submission ID to prevent duplicates
        }
      };

      await axios.delete(`${apiUrl}/deleteExpense/${id}`, config);
      setExpenses(expenses.filter((expense) => expense.id !== id));
      
      // Refresh data to show updated total savings and budgets
      await fetchUserInfo();
      await fetchBudgets();
    } catch (error) {
      console.error("Error deleting expense:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Set expense for editing
  const editExpense = (expense) => {
    setEditingExpense(expense);
    setFormData({
      subject: expense.subject,
      category: expense.category,
      date: expense.date,
      amount: expense.amount,
      currency: expense.currency,
    });
  };

  // Find budget for category
  const findBudgetForCategory = (category) => {
    return budgets.find(budget => budget.category === category);
  };

  useEffect(() => {
    const loadData = async () => {
      await fetchUserInfo();
      await fetchExpenses();
      await fetchBudgets();
    };
    
    loadData();
    
    // Prevent Enter key double submissions
    const form = formRef.current;
    if (form) {
      const preventDuplicateSubmit = (e) => {
        if (e.key === 'Enter' && isSubmitting) {
          e.preventDefault();
        }
      };
      
      form.addEventListener('keydown', preventDuplicateSubmit);
      return () => {
        if (form) {
          form.removeEventListener('keydown', preventDuplicateSubmit);
        }
      };
    }
  }, [navigate, isSubmitting]);

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
      <h1>Expense Management</h1>
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
        ref={formRef}
        onSubmit={editingExpense ? updateExpense : addExpense}
        style={{
          backgroundColor: "#f8f9fa",
          padding: "20px",
          borderRadius: "8px",
          marginBottom: "20px"
        }}
      >
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Subject:</label>
          <input
            type="text"
            name="subject"
            placeholder="What did you spend on?"
            value={formData.subject}
            onChange={handleInputChange}
            required
            disabled={isSubmitting}
            style={{ width: "100%", padding: "8px" }}
          />
        </div>
        
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Category:</label>
          <select
            name="category"
            value={formData.category}
            onChange={handleInputChange}
            required
            disabled={isSubmitting}
            style={{ width: "100%", padding: "8px" }}
          >
            <option value="">Select a category</option>
            {categories.map(category => (
              <option key={category} value={category}>{category}</option>
            ))}
          </select>
        </div>
        
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Date:</label>
          <input
            type="date"
            name="date"
            value={formData.date}
            onChange={handleInputChange}
            required
            disabled={isSubmitting}
            style={{ width: "100%", padding: "8px" }}
          />
        </div>
        
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Amount:</label>
          <input
            type="number"
            name="amount"
            placeholder="How much did you spend?"
            value={formData.amount}
            onChange={handleInputChange}
            required
            disabled={isSubmitting}
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
            disabled={isSubmitting}
            style={{ width: "100%", padding: "8px" }}
          />
        </div>
        
        <div>
          <button 
            type="submit"
            disabled={isSubmitting}
            style={{
              backgroundColor: editingExpense ? "#ffc107" : "#28a745",
              color: "white",
              padding: "10px 15px",
              border: "none",
              borderRadius: "4px",
              cursor: isSubmitting ? "not-allowed" : "pointer",
              marginRight: "10px",
              opacity: isSubmitting ? 0.7 : 1
            }}
          >
            {isSubmitting ? "Processing..." : (editingExpense ? "Update" : "Add")} Expense
          </button>
          
          {editingExpense && (
            <button
              type="button"
              disabled={isSubmitting}
              onClick={() => {
                setEditingExpense(null);
                setFormData({ subject: "", category: "", date: "", amount: "", currency: "" });
              }}
              style={{
                backgroundColor: "#6c757d",
                color: "white",
                padding: "10px 15px",
                border: "none",
                borderRadius: "4px",
                cursor: isSubmitting ? "not-allowed" : "pointer",
                opacity: isSubmitting ? 0.7 : 1
              }}
            >
              Cancel
            </button>
          )}
        </div>
      </form>

      <h2>Expense List</h2>
      {expenses.length === 0 ? (
        <p>No expense records found. Add your first expense record above!</p>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", marginTop: "10px" }}>
            <thead>
              <tr style={{ backgroundColor: "#f8f9fa" }}>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Subject</th>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Category</th>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Date</th>
                <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Amount</th>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Currency</th>
                <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Budget Status</th>
                <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {expenses.map((expense) => {
                const budget = findBudgetForCategory(expense.category);
                const budgetStatus = budget 
                  ? `${budget.totalSpent.toFixed(2)}/${budget.monthlyBudget.toFixed(2)}`
                  : "No budget";
                const isOverBudget = budget && budget.totalSpent > budget.monthlyBudget;
                
                return (
                  <tr key={expense.id}>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{expense.subject}</td>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{expense.category}</td>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{expense.date}</td>
                    <td style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>
                      {typeof expense.amount === 'number' ? expense.amount.toFixed(2) : expense.amount}
                    </td>
                    <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{expense.currency}</td>
                    <td style={{ 
                      padding: "10px", 
                      textAlign: "center", 
                      borderBottom: "1px solid #dee2e6",
                      color: isOverBudget ? "#dc3545" : "#28a745"
                    }}>
                      {budgetStatus}
                    </td>
                    <td style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>
                      <button
                        onClick={() => editExpense(expense)}
                        disabled={isSubmitting}
                        style={{
                          backgroundColor: "#ffc107",
                          color: "white",
                          padding: "5px 10px",
                          border: "none",
                          borderRadius: "4px",
                          cursor: isSubmitting ? "not-allowed" : "pointer",
                          marginRight: "5px",
                          opacity: isSubmitting ? 0.7 : 1
                        }}
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => deleteExpense(expense.id)}
                        disabled={isSubmitting}
                        style={{
                          backgroundColor: "#dc3545",
                          color: "white",
                          padding: "5px 10px",
                          border: "none",
                          borderRadius: "4px",
                          cursor: isSubmitting ? "not-allowed" : "pointer",
                          opacity: isSubmitting ? 0.7 : 1
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

      {/* Budget Summary Section */}
      {budgets.length > 0 && (
        <div style={{ marginTop: "40px" }}>
          <h2>Budget Summary</h2>
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", marginTop: "10px" }}>
              <thead>
                <tr style={{ backgroundColor: "#f8f9fa" }}>
                  <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Category</th>
                  <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Monthly Budget</th>
                  <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Total Spent</th>
                  <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Remaining</th>
                  <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Progress</th>
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
                        {budget.monthlyBudget.toFixed(2)} {budget.currency}
                      </td>
                      <td style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>
                        {budget.totalSpent.toFixed(2)} {budget.currency}
                      </td>
                      <td style={{ 
                        padding: "10px", 
                        textAlign: "right", 
                        borderBottom: "1px solid #dee2e6",
                        color: remaining < 0 ? "#dc3545" : "#28a745" 
                      }}>
                        {remaining.toFixed(2)} {budget.currency}
                      </td>
                      <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>
                        <div style={{ 
                          width: "100%", 
                          backgroundColor: "#e9ecef", 
                          borderRadius: "4px", 
                          overflow: "hidden" 
                        }}>
                          <div style={{
                            width: `${Math.min(percentSpent, 100)}%`,
                            height: "20px",
                            backgroundColor: progressColor,
                            textAlign: "center",
                            color: "white",
                            fontSize: "12px",
                            lineHeight: "20px"
                          }}>
                            {percentSpent.toFixed(0)}%
                          </div>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default Expense;