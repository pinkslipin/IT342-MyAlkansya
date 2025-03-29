import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import "./style.css";
import Login from "./components/login";
import Register from "./components/register";
import HomePage from "./components/homepage";
import Income from "./components/income";
import Expense from "./components/expense"; // Import the Expense component

const root = ReactDOM.createRoot(document.getElementById("app"));
root.render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/income" element={<Income />} /> {/* Add this line */}
        <Route path="/expense" element={<Expense />} /> {/* Add this line */}
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);