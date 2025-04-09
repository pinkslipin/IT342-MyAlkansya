import React, { useState } from "react";
import Sidebar from "./sidebar";
import TopBar from "./topbar";

const CurrencyConverter = () => {
  const [amount, setAmount] = useState("");
  const [fromCurrency, setFromCurrency] = useState("USD");
  const [toCurrency, setToCurrency] = useState("PHP");
  const [conversionRate, setConversionRate] = useState(-0.12); // Example rate
  const [conversionResult, setConversionResult] = useState("");

  const handleConvert = () => {
    // Example conversion logic
    const convertedAmount = (amount * 56.5).toFixed(2); // Example rate: 1 USD = 56.5 PHP
    setConversionResult(`${convertedAmount} ${toCurrency}`);
  };

  return (
    <div className="flex flex-col min-h-screen">
      {/* TopBar */}
      <TopBar />

      <div className="flex flex-1 mt-16">
        {/* Sidebar */}
        <Sidebar activePage="currencyconverter" />

        {/* Main Content */}
        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA]">
          <h1 className="text-3xl font-bold text-[#18864F] mb-6">Currency Converter</h1>

          {/* Converter Section */}
          <div className="bg-white p-6 rounded-lg shadow-md mb-6">
            <div className="grid grid-cols-3 gap-4 items-center">
              {/* Enter Amount */}
              <div>
                <label className="block text-[#18864F] font-bold mb-2">Enter Amount</label>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="w-full p-3 border rounded-md bg-[#FEF6EA] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  placeholder="Enter amount"
                />
              </div>

              {/* From Currency */}
              <div>
                <label className="block text-[#18864F] font-bold mb-2">From</label>
                <div className="flex items-center gap-2">
                  <span className="text-2xl">💵</span>
                  <select
                    value={fromCurrency}
                    onChange={(e) => setFromCurrency(e.target.value)}
                    className="w-full p-3 border rounded-md bg-[#FFC107] text-[#18864F] font-bold focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  >
                    <option value="USD">USD - US Dollar</option>
                    <option value="EUR">EUR - Euro</option>
                    <option value="JPY">JPY - Japanese Yen</option>
                  </select>
                </div>
              </div>

              {/* To Currency */}
              <div>
                <label className="block text-[#18864F] font-bold mb-2">To</label>
                <div className="flex items-center gap-2">
                  <span className="text-2xl">💱</span>
                  <select
                    value={toCurrency}
                    onChange={(e) => setToCurrency(e.target.value)}
                    className="w-full p-3 border rounded-md bg-[#18864F] text-white font-bold focus:outline-none focus:ring-2 focus:ring-[#FFC107]"
                  >
                    <option value="PHP">PHP - Philippine Peso</option>
                    <option value="USD">USD - US Dollar</option>
                    <option value="EUR">EUR - Euro</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Convert Button */}
            <div className="flex justify-center mt-6">
              <button
                onClick={handleConvert}
                className="bg-[#18864F] text-white font-bold py-2 px-6 rounded-md hover:bg-green-700 transition duration-300"
              >
                Convert
              </button>
            </div>
          </div>

          {/* Conversion Result */}
          <div className="bg-white p-6 rounded-lg shadow-md">
            <h2 className="text-2xl font-bold text-[#18864F] mb-4">
              {fromCurrency} to {toCurrency}
            </h2>
            <p className="text-lg text-[#18864F] font-bold">
              {conversionResult || "Enter an amount to see the result"}
            </p>
            <p className="text-sm text-gray-500 mt-2">
              {fromCurrency} to {toCurrency}: {conversionRate > 0 ? "+" : ""}
              {conversionRate}% (1w)
            </p>
            <p className="text-sm text-gray-500">
              {fromCurrency} to {toCurrency} conversion rate is based on the latest market data.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CurrencyConverter;