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
  const navigate = useNavigate();

  const apiUrl = "http://localhost:8080/api/incomes";

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
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      const response = await axios.post(`${apiUrl}/postIncome`, formData, config);
      setIncomes([...incomes, response.data]);
      setFormData({ source: "", date: "", amount: "", currency: "" });
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
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      };

      await axios.delete(`${apiUrl}/deleteIncome/${id}`, config);
      setIncomes(incomes.filter((income) => income.id !== id));
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
    <div>
      <h1>Income Management</h1>
      <button onClick={() => navigate("/")} style={{ marginBottom: "20px" }}>Back to Home</button>
      
      <form onSubmit={editingIncome ? updateIncome : addIncome}>
        <input
          type="text"
          name="source"
          placeholder="Source"
          value={formData.source}
          onChange={handleInputChange}
          required
        />
        <input
          type="date"
          name="date"
          value={formData.date}
          onChange={handleInputChange}
          required
        />
        <input
          type="number"
          name="amount"
          placeholder="Amount"
          value={formData.amount}
          onChange={handleInputChange}
          required
        />
        <input
          type="text"
          name="currency"
          placeholder="Currency"
          value={formData.currency}
          onChange={handleInputChange}
          required
        />
        <button type="submit">{editingIncome ? "Update" : "Add"} Income</button>
        {editingIncome && (
          <button
            type="button"
            onClick={() => {
              setEditingIncome(null);
              setFormData({ source: "", date: "", amount: "", currency: "" });
            }}
          >
            Cancel
          </button>
        )}
      </form>

      <h2>Income List</h2>
      {incomes.length === 0 ? (
        <p>No income records found. Add your first income record above!</p>
      ) : (
        <table border="1" style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Source</th>
              <th>Date</th>
              <th>Amount</th>
              <th>Currency</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {incomes.map((income) => (
              <tr key={income.id}>
                <td>{income.id}</td>
                <td>{income.source}</td>
                <td>{income.date}</td>
                <td>{income.amount}</td>
                <td>{income.currency}</td>
                <td>
                  <button onClick={() => editIncome(income)}>Edit</button>
                  <button onClick={() => deleteIncome(income.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default Income;