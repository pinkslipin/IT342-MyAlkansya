import React from "react";
import { useNavigate } from "react-router-dom";
import { useUser } from "./userContext";

const Sidebar = ({ activePage }) => {
  const navigate = useNavigate();
  const { user, profileImage } = useUser();
  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  return (
    <div className="fixed top-16 left-0 h-[calc(100%-4rem)] w-72 bg-white shadow-md flex flex-col z-40">
      {/* Logo */}
      <div className="p-4">
        <img
          src="/assets/myAlkansyaLogo.png"
          alt="Logo"
          className="w-20 h-20 mx-auto"
        />
        <hr className="mt-4 border-t border-gray-300" />
      </div>

      {/* Navigation Links */}
      <div className="flex flex-col gap-3 px-6 mt-4 flex-grow">
        <button
          onClick={() => navigate("/home")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "dashboard" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <img 
            src="/assets/home.png" 
            alt="Home" 
            className="h-6 w-6"
          />
          Dashboard
        </button>

        <button
          onClick={() => navigate("/income")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "income" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <img 
            src="/assets/income.png" 
            alt="Income" 
            className="h-6 w-6"
          />
          Income Management
        </button>

        <button
          onClick={() => navigate("/expense")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "expense" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <img 
            src="/assets/expense.png" 
            alt="Expense" 
            className="h-6 w-6"
          />
          Expense Management
        </button>

        <button
          onClick={() => navigate("/budget")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "budget" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <img 
            src="/assets/budget.png" 
            alt="Budget" 
            className="h-6 w-6"
          />
          Budgeting
        </button>

        <button
          onClick={() => navigate("/savingsgoal")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "savingsgoal" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <img 
            src="/assets/savingsGoal.png" 
            alt="Savings Goal" 
            className="h-6 w-6"
          />
          Savings Goal
        </button>

        {/* New Currency Converter Button */}
        <button
          onClick={() => navigate("/currencyconverter")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "currencyconverter" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <img 
            src="/assets/converter.png" 
            alt="Currency Converter" 
            className="h-6 w-6"
          />
          Currency Converter
        </button>
      </div>

      {/* User Info and Logout */}
      <div 
        className={`p-4 border-t cursor-pointer ${
          activePage === "profile" ? "bg-[#EDFBE9]" : ""
        }`} 
        onClick={() => navigate("/profile")}
      >
        <div className="flex items-center gap-3">
          <img
            src={profileImage || defaultProfilePic}
            alt="User"
            className="w-12 h-12 rounded-full"
            onError={(e) => {
              e.target.src = defaultProfilePic;
              e.target.onerror = null;
            }}
          />
          <div>
            <p className={`font-bold ${activePage === "profile" ? "text-[#18864F]" : "text-gray-700"}`}>
              {user ? `${user.firstname} ${user.lastname}` : "Loading..."}
            </p>
            <p className="text-sm text-gray-500" style={{ fontSize: "12px" }}>
              {user ? user.email : ""}
            </p>
          </div>
        </div>
        <button
          onClick={(e) => {
            e.stopPropagation(); // Prevent triggering the parent div's onClick
            localStorage.removeItem("authToken");
            navigate("/login");
          }}
          className="mt-4 w-full bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-md hover:bg-yellow-500 transition duration-300"
        >
          Logout
        </button>
      </div>
    </div>
  );
};

export default Sidebar;