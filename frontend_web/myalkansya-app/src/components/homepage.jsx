import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";

const HomePage = () => {
  const [user, setUser] = useState(null);
  const [totalExpenses, setTotalExpenses] = useState(0);
  const [totalIncome, setTotalIncome] = useState(0);
  const [totalBudget, setTotalBudget] = useState(0);
  const [totalSavings, setTotalSavings] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [profileImage, setProfileImage] = useState(null);

  const navigate = useNavigate();

  // Get current month and year
  const currentDate = new Date();
  const currentMonthIndex = currentDate.getMonth() + 1; // JavaScript months are 0-indexed
  const currentYear = currentDate.getFullYear();

  const [selectedMonth, setSelectedMonth] = useState(currentMonthIndex);
  const [selectedYear, setSelectedYear] = useState(currentYear);

  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  const months = [
    { value: 0, label: "All Months" },
    { value: 1, label: "January" },
    { value: 2, label: "February" },
    { value: 3, label: "March" },
    { value: 4, label: "April" },
    { value: 5, label: "May" },
    { value: 6, label: "June" },
    { value: 7, label: "July" },
    { value: 8, label: "August" },
    { value: 9, label: "September" },
    { value: 10, label: "October" },
    { value: 11, label: "November" },
    { value: 12, label: "December" },
  ];

  const years = [
    { value: 0, label: "All Years" },
    { value: currentYear - 2, label: (currentYear - 2).toString() },
    { value: currentYear - 1, label: (currentYear - 1).toString() },
    { value: currentYear, label: currentYear.toString() },
    { value: currentYear + 1, label: (currentYear + 1).toString() },
  ];

  const handleResetFilters = () => {
    setSelectedMonth(currentMonthIndex);
    setSelectedYear(currentYear);
  };

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

        const userResponse = await axios.get("http://localhost:8080/api/users/me", config);
        setUser(userResponse.data);

        const expensesResponse = await axios.get("http://localhost:8080/api/expenses/getExpenses", config);
        let filteredExpenses = expensesResponse.data;
        if (selectedMonth > 0 || selectedYear > 0) {
          filteredExpenses = filteredExpenses.filter(expense => {
            const expenseDate = new Date(expense.date);
            const expenseMonth = expenseDate.getMonth() + 1;
            const expenseYear = expenseDate.getFullYear();

            const monthMatches = selectedMonth === 0 || expenseMonth === selectedMonth;
            const yearMatches = selectedYear === 0 || expenseYear === selectedYear;

            return monthMatches && yearMatches;
          });
        }
        const totalExpensesSum = filteredExpenses.reduce((sum, expense) => sum + expense.amount, 0);
        setTotalExpenses(totalExpensesSum);

        const incomesResponse = await axios.get("http://localhost:8080/api/incomes/getIncomes", config);
        let filteredIncomes = incomesResponse.data;
        if (selectedMonth > 0 || selectedYear > 0) {
          filteredIncomes = filteredIncomes.filter(income => {
            const incomeDate = new Date(income.date);
            const incomeMonth = incomeDate.getMonth() + 1;
            const incomeYear = incomeDate.getFullYear();

            const monthMatches = selectedMonth === 0 || incomeMonth === selectedMonth;
            const yearMatches = selectedYear === 0 || incomeYear === selectedYear;

            return monthMatches && yearMatches;
          });
        }
        const totalIncomeSum = filteredIncomes.reduce((sum, income) => sum + income.amount, 0);
        setTotalIncome(totalIncomeSum);

        const budgetsResponse = await axios.get("http://localhost:8080/api/budgets/user", config);
        let filteredBudgets = budgetsResponse.data;
        if (selectedMonth > 0 || selectedYear > 0) {
          filteredBudgets = filteredBudgets.filter(budget => {
            const budgetMonth = budget.budgetMonth;
            const budgetYear = budget.budgetYear;
            
            const monthMatches = selectedMonth === 0 || budgetMonth === selectedMonth;
            const yearMatches = selectedYear === 0 || budgetYear === selectedYear;
            
            return monthMatches && yearMatches;
          });
        }
        const totalBudgetSum = filteredBudgets.reduce((sum, budget) => sum + budget.monthlyBudget, 0);
        setTotalBudget(totalBudgetSum);

        setTotalSavings(userResponse.data.totalSavings || 0);

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
  }, [navigate, selectedMonth, selectedYear]);

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

  const formattedIncome = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "PHP",
  }).format(totalIncome);

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
      <TopBar
        user={user}
        profileImage={imageToDisplay}
        onLogout={handleLogout}
      />

      <div className="flex flex-1 mt-16">
        <Sidebar activePage="dashboard" />

        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA]">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold text-[#18864F]">Data Analytics</h1>
          </div>

          <div className="mb-6 bg-white p-4 rounded-md shadow-sm">
            <div className="flex flex-col sm:flex-row justify-between items-center">
              <div className="flex flex-col sm:flex-row items-center mb-3 sm:mb-0">
                <h2 className="text-lg font-semibold text-[#18864F] mr-4">Filters</h2>
                <div className="flex flex-wrap gap-3">
                  <div className="flex items-center">
                    <label className="text-[#18864F] mr-2 font-medium">Month:</label>
                    <select
                      value={selectedMonth}
                      onChange={(e) => setSelectedMonth(Number(e.target.value))}
                      className="border border-gray-300 rounded-md py-1 px-3 bg-white text-[#18864F] focus:outline-none focus:ring-2 focus:ring-[#FFC107]"
                    >
                      {months.map((month) => (
                        <option key={month.value} value={month.value}>
                          {month.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="flex items-center">
                    <label className="text-[#18864F] mr-2 font-medium">Year:</label>
                    <select
                      value={selectedYear}
                      onChange={(e) => setSelectedYear(Number(e.target.value))}
                      className="border border-gray-300 rounded-md py-1 px-3 bg-white text-[#18864F] focus:outline-none focus:ring-2 focus:ring-[#FFC107]"
                    >
                      {years.map((year) => (
                        <option key={year.value} value={year.value}>
                          {year.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
              <button
                onClick={handleResetFilters}
                className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-md hover:bg-yellow-500 transition duration-300"
              >
                Reset Filters
              </button>
            </div>

            {(selectedMonth > 0 || selectedYear > 0) && (
              <div className="mt-3 px-2 py-1 bg-[#EDFBE9] text-[#18864F] rounded-md inline-block">
                <span className="font-medium">Active filters:</span>
                {selectedMonth > 0 && (
                  <span className="ml-2 px-2 py-0.5 bg-[#FFC107] rounded-md text-sm">
                    Month: {months.find(m => m.value === selectedMonth)?.label}
                  </span>
                )}
                {selectedYear > 0 && (
                  <span className="ml-2 px-2 py-0.5 bg-[#FFC107] rounded-md text-sm">
                    Year: {selectedYear}
                  </span>
                )}
              </div>
            )}
          </div>

          <div className="grid grid-cols-4 gap-6 mb-8">
            <div className="bg-white p-6 rounded-lg shadow-md text-center">
              <h3 className="text-lg font-bold text-[#18864F]">Total Income</h3>
              <p className="text-2xl font-bold text-[#18864F]">{formattedIncome}</p>
            </div>
            <div className="bg-white p-6 rounded-lg shadow-md text-center">
              <h3 className="text-lg font-bold text-[#18864F]">Total Expenses</h3>
              <p className="text-2xl font-bold text-[#18864F]">{formattedExpenses}</p>
            </div>
            <div className="bg-white p-6 rounded-lg shadow-md text-center">
              <h3 className="text-lg font-bold text-[#18864F]">Total Budget</h3>
              <p className="text-2xl font-bold text-[#18864F]">{formattedBudget}</p>
            </div>
            <div className="bg-white p-6 rounded-lg shadow-md text-center">
              <h3 className="text-lg font-bold text-[#18864F]">Total Savings</h3>
              <p className="text-2xl font-bold text-[#18864F]">{formattedSavings}</p>
            </div>
          </div>

          <div className="bg-white p-8 rounded-lg shadow-md flex items-center justify-center" style={{ height: "400px" }}>
            <p className="text-gray-500">Chart will be displayed here</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;