import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import myAlkansyaLogo from "../assets/myAlkansyaLogo.png";
import myAlkansyaTextLogo from "../assets/MyAlkansyaTextLogo.png";

const LandingPage = () => {
  const navigate = useNavigate();

  useEffect(() => {
    // Only handle OAuth tokens from URL, don't auto-redirect logged in users
    const urlParams = new URLSearchParams(window.location.search);
    const oauthToken = urlParams.get("token");

    if (oauthToken) {
      // Store the token and redirect to homepage
      localStorage.setItem("authToken", oauthToken);
      navigate("/home");
    }
    // Removed the "else if" that was redirecting logged-in users
  }, [navigate]);

  return (
    <div className="min-h-screen bg-[#FEF6EA] flex flex-col">
      {/* Navigation Bar */}
      <nav className="bg-[#18864F] p-4 w-full">
        <div className="flex justify-between items-center">
          <div className="flex items-center">
            <img src={myAlkansyaLogo} alt="MyAlkansya Logo" className="h-10 w-10 mr-2" />
            <img src={myAlkansyaTextLogo} alt="MyAlkansya Text Logo" className="h-6" />
          </div>
          <div className="space-x-2">
            <button 
              className="bg-[#FFC107] text-white font-bold py-2 px-4 rounded"
              onClick={() => navigate("/login")}
            >
              SIGN IN
            </button>
            <button
              className="bg-[#FFC107] text-white font-bold py-2 px-4 rounded"
              onClick={() => navigate("/register")}
            >
              SIGN UP
            </button>
          </div>
        </div>
      </nav>

      {/* Main Content with Larger Container */}
      <div className="flex-1 flex items-center justify-center p-6">
        <div className="bg-white/70 p-12 rounded-xl shadow-lg w-full max-w-7xl mx-auto text-center">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-center">
            {/* Left side: Welcome Text and Logo */}
            <div className="flex flex-col items-center md:items-start">
              <h1 className="text-6xl font-bold mb-4 text-center md:text-left">Welcome to</h1>
              <div className="text-6xl font-bold mb-8">
                <span className="text-[#FFC107]">My</span>
                <span className="text-[#18864F]">Alkansya</span>
              </div>
              
              <p className="text-lg mb-8 text-gray-700 text-center md:text-left">
                Your personal finance manager that helps you track income, expenses, 
                budget effectively and achieve your savings goals.
              </p>
              
              <button 
                onClick={() => navigate("/register")}
                className="bg-[#FFC107] hover:bg-[#e5ac00] text-[#18864F] text-xl font-bold py-4 px-8 rounded-lg flex items-center transition duration-300 shadow-md"
              >
                Get Started
                <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 ml-2" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M10.293 5.293a1 1 0 011.414 0l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414-1.414L12.586 11H5a1 1 0 110-2h7.586l-2.293-2.293a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </button>
            </div>
            
            {/* Right side: Decorative elements */}
            <div className="flex justify-center items-center">
              <div className="relative">
                {/* Main logo in green background */}
                <div className="bg-[#18864F] p-10 rounded-full shadow-xl">
                  <img 
                    src={myAlkansyaLogo} 
                    alt="MyAlkansya Logo" 
                    className="h-48 w-48"
                  />
                </div>
                
                {/* Decorative circles */}
                <div className="absolute -top-6 -left-6 bg-[#FFC107]/30 w-16 h-16 rounded-full"></div>
                <div className="absolute -bottom-4 -right-4 bg-[#FFC107]/60 w-24 h-24 rounded-full"></div>
              </div>
            </div>
          </div>
          
          {/* Features Section */}
          <div className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="p-6 bg-white rounded-lg shadow-md hover:shadow-lg hover:bg-[#FAFDF7] transform hover:scale-105 transition-all duration-300 cursor-pointer">
              <div className="bg-[#EDFBE9] p-3 rounded-full w-14 h-14 flex items-center justify-center mx-auto mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-[#18864F]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h3 className="text-xl font-bold text-[#18864F] mb-2">Track Finances</h3>
              <p className="text-gray-600">Monitor your income and expenses in one place</p>
            </div>
            
            <div className="p-6 bg-white rounded-lg shadow-md hover:shadow-lg hover:bg-[#FAFDF7] transform hover:scale-105 transition-all duration-300 cursor-pointer">
              <div className="bg-[#EDFBE9] p-3 rounded-full w-14 h-14 flex items-center justify-center mx-auto mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-[#18864F]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <h3 className="text-xl font-bold text-[#18864F] mb-2">Budget Planning</h3>
              <p className="text-gray-600">Create and manage budgets easily</p>
            </div>
            
            <div className="p-6 bg-white rounded-lg shadow-md hover:shadow-lg hover:bg-[#FAFDF7] transform hover:scale-105 transition-all duration-300 cursor-pointer">
              <div className="bg-[#EDFBE9] p-3 rounded-full w-14 h-14 flex items-center justify-center mx-auto mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-[#18864F]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                </svg>
              </div>
              <h3 className="text-xl font-bold text-[#18864F] mb-2">Savings Goals</h3>
              <p className="text-gray-600">Set and achieve your financial goals</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LandingPage;