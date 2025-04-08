import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import axios from "axios";

const EditIncome = () => {
  const [formData, setFormData] = useState({
    source: "",
    date: "",
    amount: "",
    currency: "PHP",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { incomeId } = useParams();

  useEffect(() => {
    const fetchIncome = async () => {
      try {
        const authToken = localStorage.getItem("authToken");
        if (!authToken) {
          setError("You must be logged in to edit income.");
          navigate("/login");
          return;
        }

        const config = {
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        };

        const response = await axios.get(`http://localhost:8080/api/incomes/getIncome/${incomeId}`, config);
        setFormData(response.data);
        setLoading(false);
      } catch (err) {
        console.error("Error fetching income:", err);
        setError("Failed to fetch income details. Please try again later.");
        setLoading(false);
      }
    };

    fetchIncome();
  }, [incomeId, navigate]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleEditIncome = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You must be logged in to edit income.");
        return;
      }

      const config = {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      };

      await axios.put(`http://localhost:8080/api/incomes/putIncome/${incomeId}`, formData, config);
      navigate("/income"); // Redirect to the Income page after successful update
    } catch (err) {
      console.error("Error editing income:", err);
      setError("Failed to edit income. Please try again later.");
    }
  };

  if (loading) {
    return <p>Loading income details...</p>;
  }

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="income" />

        {/* Main Content */}
        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA] flex justify-center items-center">
          {/* White Container */}
          <div className="bg-white p-12 rounded-lg shadow-md w-full max-w-7xl flex" style={{ height: "650px" }}>
            {/* Left Section: Form */}
            <div className="w-2/3 pr-8">
              <div className="flex items-center mb-6">
                <button
                  onClick={() => navigate("/income")}
                  className="text-[#18864F] hover:text-green-700 mr-4"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-6 w-6"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M15 19l-7-7 7-7"
                    />
                  </svg>
                </button>
                <h1 className="text-2xl font-bold text-[#18864F]">Edit Income</h1>
              </div>

              <form onSubmit={handleEditIncome}>
                <div className="mb-4">
                  <label className="block text-[#18864F] font-bold mb-2">Source</label>
                  <input
                    type="text"
                    name="source"
                    value={formData.source}
                    onChange={handleInputChange}
                    className="w-full max-w-lg p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                    placeholder="Enter income source"
                    required
                  />
                </div>

                <div className="mb-4">
                  <label className="block text-[#18864F] font-bold mb-2">Date</label>
                  <input
                    type="date"
                    name="date"
                    value={formData.date}
                    onChange={handleInputChange}
                    className="w-full max-w-lg p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                    required
                  />
                </div>

                <div className="mb-4">
                  <label className="block text-[#18864F] font-bold mb-2">Amount</label>
                  <div className="flex max-w-lg">
                    <input
                      type="number"
                      name="amount"
                      value={formData.amount}
                      onChange={handleInputChange}
                      className="w-full p-3 border rounded-l-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                      placeholder="Enter amount"
                      required
                    />
                    <select
                      name="currency"
                      value={formData.currency}
                      onChange={handleInputChange}
                      className="p-3 border rounded-r-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                    >
                      <option value="PHP">PHP</option>
                      <option value="USD">USD</option>
                      <option value="EUR">EUR</option>
                    </select>
                  </div>
                </div>

                {error && <p className="text-red-500 text-sm mb-4">{error}</p>}

                <div className="flex justify-between max-w-lg">
                  <button
                    type="submit"
                    className="bg-[#18864F] text-white font-bold py-2 px-4 rounded-md hover:bg-green-700 transition duration-300"
                  >
                    Save Changes
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate("/income")}
                    className="bg-[#FEF6EA] text-[#18864F] font-bold py-2 px-4 rounded-md border border-[#18864F] hover:bg-[#EDFBE9] transition duration-300"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>

            {/* Right Section: Empty for Future Use */}
            <div className="w-1/3 bg-[#F9F9F9] rounded-lg p-4 flex items-center justify-center">
              <p className="text-gray-500">Future content goes here</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditIncome;