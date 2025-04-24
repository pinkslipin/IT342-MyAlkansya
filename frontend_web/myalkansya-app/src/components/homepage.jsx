import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
// Import Chart.js components
import { 
  Chart as ChartJS, 
  CategoryScale, 
  LinearScale, 
  PointElement, 
  LineElement, 
  BarElement, 
  ArcElement,
  Title, 
  Tooltip, 
  Legend, 
  Filler 
} from 'chart.js';
import { Bar, Pie, Line } from 'react-chartjs-2';
import { exportSheets } from '../utils/excelExport';

// Register Chart.js components
ChartJS.register(
  CategoryScale, 
  LinearScale, 
  PointElement, 
  LineElement, 
  BarElement,
  ArcElement,
  Title, 
  Tooltip, 
  Legend,
  Filler
);

const HomePage = () => {
  const [user, setUser] = useState(null);
  const [totalExpenses, setTotalExpenses] = useState(0);
  const [totalIncome, setTotalIncome] = useState(0);
  const [totalBudget, setTotalBudget] = useState(0);
  const [totalSavings, setTotalSavings] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [profileImage, setProfileImage] = useState(null);
  const [savingsGoalsData, setSavingsGoalsData] = useState([]);
  const [filteredExpenses, setFilteredExpenses] = useState([]);
  const [filteredIncomes, setFilteredIncomes] = useState([]);
  const [filteredBudgets, setFilteredBudgets] = useState([]);

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
        // After filtering expenses
        setFilteredExpenses(filteredExpenses);

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
        // After filtering incomes
        setFilteredIncomes(filteredIncomes);

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
        // After filtering budgets
        setFilteredBudgets(filteredBudgets);

        setTotalSavings(userResponse.data.totalSavings || 0);

        // Generate monthly data for chart from raw expenses and income data
        generateMonthlyChartData(expensesResponse.data, incomesResponse.data, selectedYear);
        
        // Generate category data from expenses
        generateCategoryChartData(filteredExpenses);

        // Rest of your profile picture code
        const userData = userResponse.data;
        if (userData) {
          const possiblePictureFields = [
            "picture", "profilePicture", "profile_picture", 
            "avatar", "photo", "image", "imageUrl",
          ];

          for (const field of possiblePictureFields) {
            if (userData[field] && typeof userData[field] === "string") {
              setProfileImage(userData[field]);
              break;
            }
          }
        }

        // Add this in your first useEffect where you fetch data, after the budget data fetching
        const savingsGoalsResponse = await axios.get("http://localhost:8080/api/savings-goals/getSavingsGoals", config);
        setSavingsGoalsData(savingsGoalsResponse.data);

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

  // Add new state variables for chart data
  const [monthlyData, setMonthlyData] = useState({
    labels: [],
    income: [],
    expenses: []
  });
  const [categoryData, setCategoryData] = useState({
    labels: [],
    data: []
  });

  // Add a new useEffect to prepare chart data
  useEffect(() => {
    const prepareChartData = async () => {
      try {
        const authToken = localStorage.getItem("authToken");
        if (!authToken) return;
        
        const config = {
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        };
        
        // Get monthly data for the selected year
        const monthlyResponse = await axios.get(
          `http://localhost:8080/api/analytics/monthly-summary?year=${selectedYear}`,
          config
        );
        
        if (monthlyResponse.data && monthlyResponse.data.length > 0) {
          const labels = [];
          const incomeData = [];
          const expenseData = [];
          
          monthlyResponse.data.forEach(item => {
            labels.push(item.month);
            incomeData.push(item.income);
            expenseData.push(item.expenses);
          });
          
          setMonthlyData({
            labels,
            income: incomeData,
            expenses: expenseData
          });
          
          console.log("Monthly data processed:", { labels, incomeData, expenseData });
        } else {
          console.warn("No monthly data returned from API");
        }
        
        // Get expense categories data
        const categoriesResponse = await axios.get(
          `http://localhost:8080/api/analytics/expense-categories?month=${selectedMonth}&year=${selectedYear}`,
          config
        );
        
        if (categoriesResponse.data && categoriesResponse.data.length > 0) {
          const labels = [];
          const data = [];
          
          categoriesResponse.data.forEach(item => {
            labels.push(item.category);
            data.push(item.amount);
          });
          
          setCategoryData({
            labels,
            data
          });
          
          console.log("Category data processed:", { labels, data });
        } else {
          console.warn("No category data returned from API");
        }

        // Inside your prepareChartData function
        console.log("API URLs:", {
          monthlyUrl: `http://localhost:8080/api/analytics/monthly-summary?year=${selectedYear}`,
          categoriesUrl: `http://localhost:8080/api/analytics/expense-categories?month=${selectedMonth}&year=${selectedYear}`
        });

        // After your API calls
        console.log("Monthly response:", monthlyResponse);
        console.log("Categories response:", categoriesResponse);
      } catch (error) {
        console.error("Error fetching chart data:", error);
        if (error.response) {
          console.error("Response status:", error.response.status);
          console.error("Response data:", error.response.data);
        }
      }
    };
    
    if (!loading && user) {
      prepareChartData();
    }
  }, [selectedMonth, selectedYear, loading, user]);

  // Function to generate monthly chart data
  const generateMonthlyChartData = (expenses, incomes, year) => {
    const monthLabels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const incomeData = Array(12).fill(0);
    const expenseData = Array(12).fill(0);
    
    // Process income data
    incomes.forEach(income => {
      const incomeDate = new Date(income.date);
      const incomeYear = incomeDate.getFullYear();
      
      if (incomeYear === year || year === 0) {
        const month = incomeDate.getMonth(); // 0-based index
        incomeData[month] += income.amount;
      }
    });
    
    // Process expense data
    expenses.forEach(expense => {
      const expenseDate = new Date(expense.date);
      const expenseYear = expenseDate.getFullYear();
      
      if (expenseYear === year || year === 0) {
        const month = expenseDate.getMonth(); // 0-based index
        expenseData[month] += expense.amount;
      }
    });
    
    setMonthlyData({
      labels: monthLabels,
      income: incomeData,
      expenses: expenseData
    });
    
    console.log("Generated monthly data:", { 
      labels: monthLabels, 
      income: incomeData, 
      expenses: expenseData 
    });
  };

  // Function to generate category chart data
  const generateCategoryChartData = (expenses) => {
    // Group expenses by category
    const categoryMap = {};
    
    expenses.forEach(expense => {
      const category = expense.category || 'Uncategorized';
      if (!categoryMap[category]) {
        categoryMap[category] = 0;
      }
      categoryMap[category] += expense.amount;
    });
    
    // Convert to arrays for chart
    const categories = Object.keys(categoryMap);
    const amounts = categories.map(category => categoryMap[category]);
    
    setCategoryData({
      labels: categories,
      data: amounts
    });
    
    console.log("Generated category data:", { 
      labels: categories, 
      data: amounts 
    });
  };

  const handleLogout = () => {
    localStorage.removeItem("authToken");
    navigate("/login");
  };

  // Place this before the return statement, near other handler functions

  const handleExportDashboard = () => {
    try {
      // Get month name for the filename
      const monthName = selectedMonth > 0 
        ? months.find(m => m.value === selectedMonth)?.label || selectedMonth
        : "All";
      
      // Get year for the filename
      const yearText = selectedYear > 0 ? selectedYear.toString() : "All";
      
      const summaryRows = [
        { Metric: 'Total Income',   Value: totalIncome   },
        { Metric: 'Total Expenses', Value: totalExpenses },
        { Metric: 'Total Budget',   Value: totalBudget   },
        { Metric: 'Total Savings',  Value: totalSavings  },
      ];
    
      const sheets = [
        {
          name: 'Dashboard',
          columns: [
            { header: 'Metric', key: 'Metric' },
            { header: 'Value',  key: 'Value'  },
          ],
          data: summaryRows,
        },
        {
          name: 'Income',
          columns: [
            { header: 'Date',   key: 'date'   },
            { header: 'Source', key: 'source' },
            { header: 'Amount', key: 'amount' },
          ],
          data: filteredIncomes,
        },
        {
          name: 'Expenses',
          columns: [
            { header: 'Date',     key: 'date'     },
            { header: 'Category', key: 'category' },
            { header: 'Amount',   key: 'amount'   },
          ],
          data: filteredExpenses,
        },
        {
          name: 'Budget',
          columns: [
            { header: 'Category',       key: 'category'      },
            { header: 'Monthly Budget', key: 'monthlyBudget' },
            { header: 'Total Spent',    key: 'totalSpent'    },
            { header: 'Remaining',      key: 'remaining'     },
          ],
          data: filteredBudgets.map(b => ({
            category:      b.category,
            monthlyBudget: b.monthlyBudget,
            totalSpent:    b.totalSpent,
            remaining:     b.monthlyBudget - b.totalSpent,
          })),
        },
        {
          name: 'SavingsGoal',
          columns: [
            { header: 'Goal',           key: 'goal'         },
            { header: 'Target Amount',  key: 'targetAmount' },
            { header: 'Current Amount', key: 'currentAmount'},
            { header: 'Target Date',    key: 'targetDate'   },
            { header: 'Progress (%)',   key: 'progress'     },
          ],
          data: savingsGoalsData.map(g => ({
            goal:          g.goal,
            targetAmount:  g.targetAmount,
            currentAmount: g.currentAmount,
            targetDate:    new Date(g.targetDate).toLocaleDateString(),
            progress:      Math.round((g.currentAmount / g.targetAmount) * 100),
          })),
        },
      ];
    
      // Pass the options object as the third parameter
      exportSheets(
        sheets,
        `MyAlkansya_Report_${monthName}_${yearText}.xlsx`,
        { user, selectedMonth, selectedYear, months }
      );      
  
      
    } catch (error) {
      console.error("Export failed:", error);
      alert("Export failed. Please try again.");
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-t-2 border-b-2 border-[#18864F]"></div>
        <p className="mt-4 text-[#18864F] font-semibold">Loading dashboard...</p>
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
  
  // Get the user's preferred currency or default to PHP if not set
  const userCurrency = user.currency || "PHP";

  // Format all monetary values using the user's preferred currency
  const formattedIncome = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: userCurrency,
  }).format(totalIncome);

  const formattedExpenses = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: userCurrency,
  }).format(totalExpenses);

  const formattedBudget = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: userCurrency,
  }).format(totalBudget);

  // Use totalSavings directly in the formatter
const formattedSavings = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: userCurrency,
}).format(totalSavings); // Changed from displayedTotalSavings to totalSavings

  const imageToDisplay = profileImage || user.picture || user.profilePicture || defaultProfilePic;

  // Prepare chart configurations
  const barChartData = {
    labels: monthlyData.labels,
    datasets: [
      {
        label: 'Income',
        data: monthlyData.income,
        backgroundColor: 'rgba(24, 134, 79, 0.7)',
        borderColor: '#18864F',
        borderWidth: 1,
      },
      {
        label: 'Expenses',
        data: monthlyData.expenses,
        backgroundColor: 'rgba(255, 193, 7, 0.7)',
        borderColor: '#FFC107',
        borderWidth: 1,
      },
    ],
  };
  
  const barChartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: true,
        text: 'Monthly Income vs Expenses',
        font: {
          size: 16,
          weight: 'bold'
        }
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: `Amount (${userCurrency})`
        }
      }
    }
  };
  
  const pieChartData = {
    labels: categoryData.labels,
    datasets: [
      {
        data: categoryData.data,
        backgroundColor: [
          '#18864F',
          '#FFC107', 
          '#4CAF50', 
          '#A5D6B7',
          '#EDFBE9', 
          '#2E7D32', 
          '#FEF6EA'
        ],
        borderColor: '#FFFFFF',
        borderWidth: 1,
      },
    ],
  };
  
  const pieChartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'right',
      },
      title: {
        display: true,
        text: 'Expense Distribution',
        font: {
          size: 16,
          weight: 'bold'
        }
      },
    },
  };

  // Add this after your pieChartOptions configuration
  const savingsGoalsChartData = {
    labels: savingsGoalsData.map(goal => goal.goal),
    datasets: [
      {
        label: 'Progress',
        data: savingsGoalsData.map(goal => Math.min(100, (goal.currentAmount / goal.targetAmount) * 100)),
        backgroundColor: savingsGoalsData.map(goal => {
          const progress = (goal.currentAmount / goal.targetAmount) * 100;
          return progress > 90 ? 'rgba(40, 167, 69, 0.7)' : // green for almost complete
                 progress > 50 ? 'rgba(255, 193, 7, 0.7)' : // yellow for halfway
                 'rgba(220, 53, 69, 0.7)'; // red for just started
        }),
        borderColor: '#FFFFFF',
        borderWidth: 1,
        barThickness: 20,
      }
    ]
  };

  const savingsGoalsChartOptions = {
    indexAxis: 'y',
    responsive: true,
    plugins: {
      legend: {
        display: false,
      },
      title: {
        display: true,
        text: 'Savings Goals Progress',
        font: {
          size: 16,
          weight: 'bold'
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const goal = savingsGoalsData[context.dataIndex];
            const formattedCurrent = new Intl.NumberFormat('en-US', {
              style: 'currency',
              currency: goal.currency || 'PHP'
            }).format(goal.currentAmount);
            
            const formattedTarget = new Intl.NumberFormat('en-US', {
              style: 'currency',
              currency: goal.currency || 'PHP'
            }).format(goal.targetAmount);
            
            const percentage = Math.round((goal.currentAmount / goal.targetAmount) * 100);
            return [`Progress: ${percentage}%`, `${formattedCurrent} of ${formattedTarget}`];
          }
        }
      }
    },
    scales: {
      x: {
        beginAtZero: true,
        max: 100,
        title: {
          display: true,
          text: 'Completion Percentage'
        },
        ticks: {
          callback: function(value) {
            return value + '%';
          }
        }
      }
    }
  };

  // Before rendering charts
  console.log("Bar chart data:", barChartData);
  console.log("Pie chart data:", pieChartData);

  // First, add this function before the return statement to get top 5 closest goals to completion
  // Modify the getTopFiveClosestGoals function to exclude completed goals
const getTopFiveClosestGoals = () => {
  if (!savingsGoalsData || savingsGoalsData.length === 0) return [];
  
  // Only include goals with valid target and current amounts
  // AND exclude already completed goals (where currentAmount >= targetAmount)
  const validGoals = savingsGoalsData.filter(goal => 
    goal.targetAmount && 
    goal.targetAmount > 0 && 
    goal.currentAmount != null && 
    goal.currentAmount < goal.targetAmount
  );
  
  // Sort goals by completion percentage (highest first)
  return [...validGoals]
    .sort((a, b) => {
      const percentA = (a.currentAmount / a.targetAmount) * 100;
      const percentB = (b.currentAmount / b.targetAmount) * 100;
      return percentB - percentA;
    })
    .slice(0, 5); // Take only top 5
};

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

            <button
              onClick={handleExportDashboard}
              className="bg-[#18864F] text-white font-bold py-2 px-4 rounded-md hover:bg-green-700 transition duration-300"
            >
              Export to Excel
            </button>
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

          <div className="grid grid-cols-2 gap-6 mb-8">
            <div className="bg-white p-6 rounded-lg shadow-md flex flex-col">
              <Bar data={barChartData} options={barChartOptions} className="mb-4" />
              
              {/* Add savings goals chart below the monthly chart */}
              <div className="mt-4 border-t pt-4">
  <div className="flex justify-between items-center mb-2">
    <h4 className="text-sm font-bold text-[#18864F]">Almost There: Goals Nearing Completion</h4>
    <button
      onClick={() => navigate("/savingsgoal")}
      className="text-xs bg-[#FFC107] text-[#18864F] py-1 px-2 rounded-md hover:bg-yellow-500 transition duration-300"
    >
      View All
    </button>
  </div>
  
  {savingsGoalsData.length === 0 ? (
    <div className="text-center py-2 text-gray-500 text-sm">
      <p>No savings goals found.</p>
    </div>
  ) : (
    <div style={{ height: "180px" }}> {/* Increased height for better visibility */}
      <Bar 
        data={{
          labels: getTopFiveClosestGoals().map(goal => {
            // Shorten goal names if too long
            const name = goal.goal;
            return name.length > 15 ? name.substring(0, 15) + '...' : name;
          }),
          datasets: [{
            ...savingsGoalsChartData.datasets[0],
            data: getTopFiveClosestGoals().map(goal => Math.min(100, (goal.currentAmount / goal.targetAmount) * 100)),
            backgroundColor: getTopFiveClosestGoals().map(goal => {
              const progress = (goal.currentAmount / goal.targetAmount) * 100;
              return progress > 90 ? 'rgba(40, 167, 69, 0.7)' : 
                     progress > 50 ? 'rgba(255, 193, 7, 0.7)' : 
                     'rgba(220, 53, 69, 0.7)';
            }),
            barThickness: 15 // Made bars slightly thicker for better visibility
          }]
        }} 
        options={{
          ...savingsGoalsChartOptions,
          maintainAspectRatio: false,
          plugins: {
            ...savingsGoalsChartOptions.plugins,
            title: {
              display: false
            },
            tooltip: {
              ...savingsGoalsChartOptions.plugins.tooltip,
              displayColors: false
            }
          },
          scales: {
            y: {
              grid: {
                display: false
              },
              ticks: {
                font: {
                  size: 11 // Slightly larger font for y-axis labels
                }
              }
            },
            x: {
              beginAtZero: true,
              max: 100,
              grid: {
                display: false
              },
              ticks: {
                callback: function(value) {
                  return value + '%';
                },
                maxRotation: 0,
                font: {
                  size: 10 // Adjusted font size for x-axis
                }
              }
            }
          }
        }}
      />
    </div>
  )}
</div>
            </div>
            
            <div className="bg-white p-6 rounded-lg shadow-md">
              <Pie data={pieChartData} options={pieChartOptions} />
            </div>
          </div>

          <div className="bg-white p-6 rounded-lg shadow-md">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-bold text-[#18864F]">Financial Summary</h3>
              <div className="text-sm text-gray-500">
                {selectedMonth > 0 
                  ? `${months.find(m => m.value === selectedMonth)?.label} ${selectedYear}` 
                  : `Year ${selectedYear}`}
              </div>
            </div>
            <div className="flex space-x-4">
              <div className="flex-1 p-4 border rounded-md">
                <h4 className="text-sm font-semibold text-gray-600">Income vs Expenses</h4>
                <p className="text-xl font-bold" style={{ color: totalIncome > totalExpenses ? '#18864F' : '#dc3545' }}>
                  {new Intl.NumberFormat("en-US", {
                    style: "currency",
                    currency: userCurrency,
                  }).format(totalIncome - totalExpenses)}
                </p>
                <div className="text-sm">{totalIncome > totalExpenses ? 'Surplus' : 'Deficit'}</div>
              </div>
              <div className="flex-1 p-4 border rounded-md">
                <h4 className="text-sm font-semibold text-gray-600">Budget Utilization</h4>
                <p className="text-xl font-bold" style={{ color: totalExpenses <= totalBudget ? '#18864F' : '#dc3545' }}>
                  {totalBudget > 0 ? Math.round((totalExpenses / totalBudget) * 100) : 0}%
                </p>
                <div className="text-sm">{totalExpenses <= totalBudget ? 'Under budget' : 'Over budget'}</div>
              </div>
              <div className="flex-1 p-4 border rounded-md">
                <h4 className="text-sm font-semibold text-gray-600">Savings Rate</h4>
                <p className="text-xl font-bold text-[#18864F]">
                  {totalIncome > 0 ? Math.round(((totalIncome - totalExpenses) / totalIncome) * 100) : 0}%
                </p>
                <div className="text-sm">of income saved</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;