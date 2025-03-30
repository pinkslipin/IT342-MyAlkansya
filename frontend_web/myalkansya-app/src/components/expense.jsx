import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const Expense = () => {
  const [expenses, setExpenses] = useState([]);
  const [formData, setFormData] = useState({
    subject: "",
    category: "",
    date: "",
    amount: "",
    currency: "",
  });
  const [editingExpense, setEditingExpense] = useState(null);
  const [error, setError] = useState("");
  const [categories] = useState([
    "Food", "Transportation", "Housing", "Utilities", 
    "Entertainment", "Healthcare", "Education", "Shopping", "Other"
  ]);
  const navigate = useNavigate();

  const apiUrl = "http://localhost:8080/api/expenses";

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
        withCredentials: true, // Add this for OAuth cookies
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

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  // Add a new expense
  const addExpense = async (e) => {
    e.preventDefault();
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true, // Add this for OAuth cookies
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      const response = await axios.post(`${apiUrl}/postExpense`, formData, config);
      setExpenses([...expenses, response.data]);
      setFormData({ subject: "", category: "", date: "", amount: "", currency: "" });
    } catch (error) {
      console.error("Error adding expense:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Update an expense
  const updateExpense = async (e) => {
    e.preventDefault();
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true, // Add this for OAuth cookies
        headers: {
          Authorization: `Bearer ${authToken}`
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
    } catch (error) {
      console.error("Error updating expense:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Delete an expense
  const deleteExpense = async (id) => {
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true, // Add this for OAuth cookies
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      await axios.delete(`${apiUrl}/deleteExpense/${id}`, config);
      setExpenses(expenses.filter((expense) => expense.id !== id));
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

  useEffect(() => {
    fetchExpenses();
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
      
      <form 
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
              backgroundColor: editingExpense ? "#ffc107" : "#28a745",
              color: "white",
              padding: "10px 15px",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              marginRight: "10px"
            }}
          >
            {editingExpense ? "Update" : "Add"} Expense
          </button>
          
          {editingExpense && (
            <button
              type="button"
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
                cursor: "pointer"
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
                <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {expenses.map((expense) => (
                <tr key={expense.id}>
                  <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{expense.subject}</td>
                  <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{expense.category}</td>
                  <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{expense.date}</td>
                  <td style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>
                    {typeof expense.amount === 'number' ? expense.amount.toFixed(2) : expense.amount}
                  </td>
                  <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{expense.currency}</td>
                  <td style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>
                    <button
                      onClick={() => editExpense(expense)}
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
                      onClick={() => deleteExpense(expense.id)}
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
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default Expense;