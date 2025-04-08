import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import editIcon from "../assets/edit.png"; // Import the edit icon

const Expense = () => {
  const [expenses, setExpenses] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10); // Set to 10 items per page
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const apiUrl = "http://localhost:8080/api/expenses";

  // Fetch all expenses
  const fetchExpenses = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to access this page");
        setTimeout(() => navigate("/login"), 2000);
        return;
      }

      const config = {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      };

      const response = await axios.get(`${apiUrl}/getExpenses`, config);
      setExpenses(response.data);
    } catch (error) {
      console.error("Error fetching expenses:", error);
      if (error.response && error.response.status === 401) {
        setError("Your session has expired. Please login again.");
        setTimeout(() => navigate("/login"), 2000);
      } else {
        setError("Error fetching expense data. Please try again later.");
      }
    }
  };

  useEffect(() => {
    fetchExpenses();
  }, [navigate]);

  // Pagination logic
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentExpenses = expenses.slice(indexOfFirstItem, indexOfLastItem);

  const totalPages = Math.ceil(expenses.length / itemsPerPage);

  const handleNextPage = () => {
    if (currentPage < totalPages) {
      setCurrentPage(currentPage + 1);
    }
  };

  const handlePreviousPage = () => {
    if (currentPage > 1) {
      setCurrentPage(currentPage - 1);
    }
  };

  if (error) {
    return (
      <div>
        <h1>Error</h1>
        <p style={{ color: "red" }}>{error}</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="expense" />

        {/* Main Content */}
        <div className="flex-1 p-6 ml-72 bg-[#FEF6EA]">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold text-[#18864F]">Expense Management</h1>
            <button
              onClick={() => navigate("/addexpense")}
              className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-md hover:bg-yellow-500 transition duration-300"
            >
              Add Expense
            </button>
          </div>

          {/* Fixed Header */}
          <div className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-t-md mb-2">
            <div className="grid grid-cols-5"> {/* Updated to 5 columns */}
              <div>Date</div>
              <div>Subject</div> {/* Added Subject column */}
              <div>Category</div>
              <div className="text-right">Amount</div>
              <div className="text-center">Actions</div>
            </div>
          </div>

          {/* Expense Data Container */}
          <div className="bg-white rounded-b-md shadow-md" style={{ height: "500px" }}>
            {currentExpenses.length === 0 ? (
              <p className="text-center text-gray-500 py-4">No expense records found.</p>
            ) : (
              currentExpenses.map((expense) => (
                <div
                  key={expense.id}
                  className="grid grid-cols-5 py-2 px-4 border-b last:border-none"
                >
                  <div>{expense.date}</div>
                  <div>{expense.subject}</div> {/* Displaying Subject */}
                  <div>{expense.category}</div>
                  <div className="text-right">
                    {new Intl.NumberFormat("en-US", {
                      style: "currency",
                      currency: expense.currency || "USD",
                    }).format(expense.amount)}
                  </div>
                  <div className="text-center">
                    <button
                      onClick={() => navigate(`/editexpense/${expense.id}`)}
                      className="hover:opacity-80"
                    >
                      <img
                        src={editIcon}
                        alt="Edit"
                        className="h-5 w-5 inline-block"
                      />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Pagination */}
          <div className="flex justify-end items-center mt-4">
            <button
              onClick={handlePreviousPage}
              disabled={currentPage === 1}
              className={`bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-l-md ${
                currentPage === 1 ? "opacity-50 cursor-not-allowed" : "hover:bg-yellow-500"
              }`}
            >
              &lt;
            </button>
            <div className="bg-[#FFC107] text-[#18864F] font-bold py-2 px-4">
              {currentPage} out of {totalPages}
            </div>
            <button
              onClick={handleNextPage}
              disabled={currentPage === totalPages}
              className={`bg-[#FFC107] text-[#18864F] font-bold py-2 px-4 rounded-r-md ${
                currentPage === totalPages ? "opacity-50 cursor-not-allowed" : "hover:bg-yellow-500"
              }`}
            >
              &gt;
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Expense;