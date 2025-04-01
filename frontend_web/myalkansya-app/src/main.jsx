import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import "./style.css";
import Login from "./components/login";
import Register from "./components/register";
import HomePage from "./components/homepage";
import Income from "./components/income";
import Expense from "./components/expense";
import Budget from "./components/budget";
import SavingsGoal from "./components/savingsgoal";

const root = ReactDOM.createRoot(document.getElementById("app"));
root.render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/income" element={<Income />} />
        <Route path="/expense" element={<Expense />} />
        <Route path="/budget" element={<Budget />} />
        <Route path="/savingsgoal" element={<SavingsGoal />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);