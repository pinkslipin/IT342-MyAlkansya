import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar"; // Import the Sidebar component
import TopBar from "./topbar"; // Import the TopBar component

const HomePage = () => {
  const [user, setUser] = useState(null);
  const [totalExpenses, setTotalExpenses] = useState(0);
  const [totalBudget, setTotalBudget] = useState(0);
  const [totalSavings, setTotalSavings] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [profileImage, setProfileImage] = useState(null);
  const navigate = useNavigate();

  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  useEffect(() => {
    const fetchData = async () => {
      try {
        const authToken = localStorage.getItem("authToken");

        if (!authToken) {
          const urlParams = new URLSearchParams(window.location.search);
          const oauthToken = urlParams.get("token");

          if (oauthToken) {
            localStorage.setItem("authToken", oauthToken);
          } else {
            throw new Error("No authentication token found");
          }
        }

        const config = {
          withCredentials: true,
          headers: {
            Authorization: `Bearer ${authToken || localStorage.getItem("authToken")}`,
          },
        };

        // Fetch user data
        const userResponse = await axios.get("http://localhost:8080/api/users/me", config);
        setUser(userResponse.data);

        // Fetch total expenses
        const expensesResponse = await axios.get("http://localhost:8080/api/expenses/getExpenses", config);
        const totalExpensesSum = expensesResponse.data.reduce((sum, expense) => sum + expense.amount, 0);
        setTotalExpenses(totalExpensesSum);

        // Fetch total budget - update to use the new getCurrentMonthBudgets endpoint
        const budgetsResponse = await axios.get("http://localhost:8080/api/budgets/getCurrentMonthBudgets", config);
        const totalBudgetSum = budgetsResponse.data.reduce((sum, budget) => sum + budget.monthlyBudget, 0);
        setTotalBudget(totalBudgetSum);

        // Fetch total savings
        setTotalSavings(userResponse.data.totalSavings || 0);

        // Set profile image
        const userData = userResponse.data;
        if (userData) {
          const possiblePictureFields = [
            "picture",
            "profilePicture",
            "profile_picture",
            "avatar",
            "photo",
            "image",
            "imageUrl",
          ];

          for (const field of possiblePictureFields) {
            if (userData[field] && typeof userData[field] === "string") {
              setProfileImage(userData[field]);
              break;
            }
          }
        }

        setLoading(false);
      } catch (err) {
        console.error("Error fetching data:", err);
        setError("Failed to fetch data. Please log in again.");
        setLoading(false);
        setTimeout(() => {
          navigate("/login");
        }, 2000);
      }
    };

    fetchData();
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem("authToken");
    navigate("/login");
  };

  if (loading) {
    return (
      <div style={{ textAlign: "center", marginTop: "50px" }}>
        <h2>Loading your profile...</h2>
        <div style={{ marginTop: "20px" }}>Please wait while we retrieve your information</div>
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <h1>Error</h1>
        <p>{error}</p>
        <p>Redirecting to login...</p>
      </div>
    );
  }

  const displayName = user.name ? user.name : `${user.firstname || ""} ${user.lastname || ""}`.trim();
  const emailToDisplay = user.email || "";

  const formattedExpenses = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "PHP",
  }).format(totalExpenses);

  const formattedBudget = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "PHP",
  }).format(totalBudget);

  const formattedSavings = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "PHP",
  }).format(totalSavings);

  const imageToDisplay = profileImage || user.picture || user.profilePicture || defaultProfilePic;

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar
        user={user}
        profileImage={imageToDisplay}
        onLogout={handleLogout}
      />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="dashboard" />

        {/* Main Content */}
        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA]">
          {/* Header */}
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold text-[#18864F]">Data Analytics</h1>
            <div className="relative">
              <button className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-md hover:bg-yellow-500 transition duration-300">
                Weekly, Monthly, Yearly
              </button>
            </div>
          </div>

          {/* Summary Cards */}
          <div className="grid grid-cols-3 gap-6 mb-8">
            <div className="bg-white p-6 rounded-lg shadow-md text-center">
              <h3 className="text-lg font-bold text-[#18864F]">Total Savings</h3>
              <p className="text-2xl font-bold text-[#18864F]">{formattedSavings}</p>
            </div>
            <div className="bg-white p-6 rounded-lg shadow-md text-center">
              <h3 className="text-lg font-bold text-[#18864F]">Total Expenses</h3>
              <p className="text-2xl font-bold text-[#18864F]">{formattedExpenses}</p>
            </div>
            <div className="bg-white p-6 rounded-lg shadow-md text-center">
              <h3 className="text-lg font-bold text-[#18864F]">Total Budget</h3>
              <p className="text-2xl font-bold text-[#18864F]">{formattedBudget}</p>
            </div>
          </div>

          {/* Chart Section */}
          <div className="bg-white p-8 rounded-lg shadow-md flex items-center justify-center" style={{ height: "400px" }}>
            <p className="text-gray-500">Chart will be displayed here</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;