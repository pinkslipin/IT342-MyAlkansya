import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import Login from './components/login';
import Register from './components/register';
import Home from './components/home';
import Budget from './components/budget';
import AddBudget from './components/addBudget';
import EditBudget from './components/editBudget';
import Income from './components/income';
import AddIncome from './components/addIncome';
import EditIncome from './components/editIncome';
import Expense from './components/expense';
import AddExpense from './components/addExpense';
import EditExpense from './components/editExpense';
import SavingsGoal from './components/savingsGoal';
import AddSavingGoal from './components/addSavingGoal';
import EditSavingGoal from './components/editSavingGoal';
import CurrencyConverter from './components/currencyConverter';
import Profile from './components/profile';
import './style.css'; // Ensure Tailwind CSS is imported

// Wrap the rendering logic in DOMContentLoaded listener
document.addEventListener('DOMContentLoaded', () => {
  const container = document.getElementById('root');
  if (container) {
    const root = ReactDOM.createRoot(container);
    root.render(
      <React.StrictMode>
        <Router>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            {/* Default route redirects to login if no token */}
            <Route path="/" element={localStorage.getItem('authToken') ? <Navigate to="/home" /> : <Navigate to="/login" />} />
            {/* Protected Routes */}
            <Route path="/home" element={localStorage.getItem('authToken') ? <Home /> : <Navigate to="/login" />} />
            <Route path="/budget" element={localStorage.getItem('authToken') ? <Budget /> : <Navigate to="/login" />} />
            <Route path="/add-budget" element={localStorage.getItem('authToken') ? <AddBudget /> : <Navigate to="/login" />} />
            <Route path="/edit-budget/:budgetId" element={localStorage.getItem('authToken') ? <EditBudget /> : <Navigate to="/login" />} />
            <Route path="/income" element={localStorage.getItem('authToken') ? <Income /> : <Navigate to="/login" />} />
            <Route path="/add-income" element={localStorage.getItem('authToken') ? <AddIncome /> : <Navigate to="/login" />} />
            <Route path="/edit-income/:incomeId" element={localStorage.getItem('authToken') ? <EditIncome /> : <Navigate to="/login" />} />
            <Route path="/expense" element={localStorage.getItem('authToken') ? <Expense /> : <Navigate to="/login" />} />
            <Route path="/add-expense" element={localStorage.getItem('authToken') ? <AddExpense /> : <Navigate to="/login" />} />
            <Route path="/edit-expense/:expenseId" element={localStorage.getItem('authToken') ? <EditExpense /> : <Navigate to="/login" />} />
            <Route path="/savingsgoal" element={localStorage.getItem('authToken') ? <SavingsGoal /> : <Navigate to="/login" />} />
            <Route path="/add-saving-goal" element={localStorage.getItem('authToken') ? <AddSavingGoal /> : <Navigate to="/login" />} />
            <Route path="/edit-saving-goal/:goalId" element={localStorage.getItem('authToken') ? <EditSavingGoal /> : <Navigate to="/login" />} />
            <Route path="/currency-converter" element={localStorage.getItem('authToken') ? <CurrencyConverter /> : <Navigate to="/login" />} />
            <Route path="/profile" element={localStorage.getItem('authToken') ? <Profile /> : <Navigate to="/login" />} />
          </Routes>
        </Router>
      </React.StrictMode>
    );
  } else {
    console.error("Failed to find the root element. Ensure an element with ID 'root' exists in your index.html.");
  }
});