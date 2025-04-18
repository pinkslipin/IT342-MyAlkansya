import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import "./style.css";
import LandingPage from "./components/landingPage";
import Login from "./components/login";
import Register from "./components/register";
import HomePage from "./components/homepage";
import Income from "./components/income";
import Expense from "./components/expense";
import Budget from "./components/budget";
import SavingsGoal from "./components/savingsgoal";
import AddIncome from "./components/addIncome";
import EditIncome from "./components/editIncome";
import AddExpense from "./components/addExpense";
import EditExpense from "./components/editExpense";
import AddBudget from "./components/addBudget";
import EditBudget from "./components/editBudget";
import AddSavingGoal from "./components/addSavingGoal";
import EditSavingGoal from "./components/editSavingGoal";
import CurrencyConverter from "./components/currencyConverter";
import Profile from "./components/profile";

const root = ReactDOM.createRoot(document.getElementById("app"));
root.render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/home" element={<HomePage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/income" element={<Income />} />
        <Route path="/expense" element={<Expense />} />
        <Route path="/budget" element={<Budget />} />
        <Route path="/savingsgoal" element={<SavingsGoal />} />
        <Route path="/addincome" element={<AddIncome />} />
        <Route path="/editincome/:incomeId" element={<EditIncome />} />
        <Route path="/addexpense" element={<AddExpense />} />
        <Route path="/editexpense/:expenseId" element={<EditExpense />} />
        <Route path="/addbudget" element={<AddBudget />} />
        <Route path="/editbudget/:budgetId" element={<EditBudget />} />
        <Route path="/addsavingsgoal" element={<AddSavingGoal />} />
        <Route path="/editsavingsgoal/:goalId" element={<EditSavingGoal />} />
        <Route path="/currencyconverter" element={<CurrencyConverter />} />
        <Route path="/profile" element={<Profile />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);