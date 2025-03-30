import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const Income = () => {
  const [incomes, setIncomes] = useState([]);
  const [formData, setFormData] = useState({
    source: "",
    date: "",
    amount: "",
    currency: "",
  });
  const [editingIncome, setEditingIncome] = useState(null);
  const [error, setError] = useState("");
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  const apiUrl = "http://localhost:8080/api/incomes";

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

  // Fetch all incomes
  const fetchIncomes = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to access this page");
        setTimeout(() => navigate("/login"), 2000);
        return;
      }

      const config = {
        withCredentials: true, // Add for OAuth cookies
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      const response = await axios.get(`${apiUrl}/getIncomes`, config);
      setIncomes(response.data);
    } catch (error) {
      console.error("Error fetching incomes:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      } else {
        setError("Error fetching income data. Please try again later.");
      }
    }
  };

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  // Add a new income
  const addIncome = async (e) => {
    e.preventDefault();
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true, // Add for OAuth cookies
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      const response = await axios.post(`${apiUrl}/postIncome`, formData, config);
      setIncomes([...incomes, response.data]);
      setFormData({ source: "", date: "", amount: "", currency: "" });
      
      // Refresh user data to show updated total savings
      fetchUserInfo();
    } catch (error) {
      console.error("Error adding income:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Update an income
  const updateIncome = async (e) => {
    e.preventDefault();
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true, // Add for OAuth cookies
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      const response = await axios.put(
        `${apiUrl}/putIncome/${editingIncome.id}`,
        formData,
        config
      );
      setIncomes(
        incomes.map((income) =>
          income.id === editingIncome.id ? response.data : income
        )
      );
      setEditingIncome(null);
      setFormData({ source: "", date: "", amount: "", currency: "" });
      
      // Refresh user data to show updated total savings
      fetchUserInfo();
    } catch (error) {
      console.error("Error updating income:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Delete an income
  const deleteIncome = async (id) => {
    try {
      const authToken = localStorage.getItem("authToken");
      const config = {
        withCredentials: true, // Add for OAuth cookies
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      await axios.delete(`${apiUrl}/deleteIncome/${id}`, config);
      setIncomes(incomes.filter((income) => income.id !== id));
      
      // Refresh user data to show updated total savings
      fetchUserInfo();
    } catch (error) {
      console.error("Error deleting income:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      }
    }
  };

  // Set income for editing
  const editIncome = (income) => {
    setEditingIncome(income);
    setFormData({
      source: income.source,
      date: income.date,
      amount: income.amount,
      currency: income.currency,
    });
  };

  useEffect(() => {
    fetchIncomes();
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
      <h1>Income Management</h1>
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
        onSubmit={editingIncome ? updateIncome : addIncome}
        style={{
          backgroundColor: "#f8f9fa",
          padding: "20px",
          borderRadius: "8px",
          marginBottom: "20px"
        }}
      >
        <div style={{ marginBottom: "10px" }}>
          <label style={{ display: "block", marginBottom: "5px" }}>Source:</label>
          <input
            type="text"
            name="source"
            placeholder="Where did your income come from?"
            value={formData.source}
            onChange={handleInputChange}
            required
            style={{ width: "100%", padding: "8px" }}
          />
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
            placeholder="How much did you receive?"
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
              backgroundColor: editingIncome ? "#ffc107" : "#28a745",
              color: "white",
              padding: "10px 15px",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              marginRight: "10px"
            }}
          >
            {editingIncome ? "Update" : "Add"} Income
          </button>
          
          {editingIncome && (
            <button
              type="button"
              onClick={() => {
                setEditingIncome(null);
                setFormData({ source: "", date: "", amount: "", currency: "" });
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

      <h2>Income List</h2>
      {incomes.length === 0 ? (
        <p>No income records found. Add your first income record above!</p>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", marginTop: "10px" }}>
            <thead>
              <tr style={{ backgroundColor: "#f8f9fa" }}>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Source</th>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Date</th>
                <th style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>Amount</th>
                <th style={{ padding: "10px", textAlign: "left", borderBottom: "1px solid #dee2e6" }}>Currency</th>
                <th style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {incomes.map((income) => (
                <tr key={income.id}>
                  <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{income.source}</td>
                  <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{income.date}</td>
                  <td style={{ padding: "10px", textAlign: "right", borderBottom: "1px solid #dee2e6" }}>
                    {typeof income.amount === 'number' ? income.amount.toFixed(2) : income.amount}
                  </td>
                  <td style={{ padding: "10px", borderBottom: "1px solid #dee2e6" }}>{income.currency}</td>
                  <td style={{ padding: "10px", textAlign: "center", borderBottom: "1px solid #dee2e6" }}>
                    <button
                      onClick={() => editIncome(income)}
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
                      onClick={() => deleteIncome(income.id)}
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

export default Income;