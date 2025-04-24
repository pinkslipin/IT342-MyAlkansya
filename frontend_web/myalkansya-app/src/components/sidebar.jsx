import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

const Sidebar = ({ activePage }) => {
  const [user, setUser] = useState(null);
  const [profileImage, setProfileImage] = useState(null);
  const navigate = useNavigate();

  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const authToken = localStorage.getItem("authToken");
        if (!authToken) {
          navigate("/login");
          return;
        }

        const config = {
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        };

        const response = await axios.get("http://localhost:8080/api/users/me", config);
        setUser(response.data);

        // Universal profile picture handling - works with both uploaded and OAuth pictures
        if (response.data.profilePicture) {
          // If it's already a full URL (like from Google/Facebook)
          if (response.data.profilePicture.startsWith('http')) {
            console.log("Sidebar: Using external provider profile picture");
            setProfileImage(response.data.profilePicture);
          } 
          // If it's a path to our own API (uploaded pictures)
          else {
            const baseUrl = "http://localhost:8080";
            const path = response.data.profilePicture.startsWith('/') 
              ? response.data.profilePicture 
              : `/${response.data.profilePicture}`;
            
            const finalUrl = `${baseUrl}${path}?t=${new Date().getTime()}`;
            console.log("Sidebar: Using uploaded profile picture");
            setProfileImage(finalUrl);
          }
        } else {
          setProfileImage(defaultProfilePic);
        }
      } catch (err) {
        console.error("Error fetching user info:", err);
        navigate("/login");
      }
    };

    fetchUserInfo();
  }, [navigate]);

  return (
    <div className="fixed top-16 left-0 h-[calc(100%-4rem)] w-72 bg-white shadow-md flex flex-col z-40">
      {/* Logo */}
      <div className="p-4">
        <img
          src="/src/assets/myAlkansyaLogo.png" // Replace with your logo path
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
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 10h11M9 21V3m12 7h-3m0 0v11m0-11l-3 3m3-3l3 3" />
          </svg>
          Dashboard
        </button>

        <button
          onClick={() => navigate("/income")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "income" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-3.866 0-7 3.134-7 7h14c0-3.866-3.134-7-7-7z" />
          </svg>
          Income Management
        </button>

        <button
          onClick={() => navigate("/expense")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "expense" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-3.866 0-7 3.134-7 7h14c0-3.866-3.134-7-7-7z" />
          </svg>
          Expense Management
        </button>

        <button
          onClick={() => navigate("/budget")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "budget" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-3.866 0-7 3.134-7 7h14c0-3.866-3.134-7-7-7z" />
          </svg>
          Budgeting
        </button>

        <button
          onClick={() => navigate("/savingsgoal")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "savingsgoal" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-3.866 0-7 3.134-7 7h14c0-3.866-3.134-7-7-7z" />
          </svg>
          Savings Goal
        </button>

        {/* New Currency Converter Button */}
        <button
          onClick={() => navigate("/currencyconverter")}
          className={`flex items-center gap-3 font-bold p-2 rounded-md ${
            activePage === "currencyconverter" ? "bg-[#EDFBE9] text-[#18864F]" : "text-gray-600 hover:bg-[#EDFBE9]"
          }`}
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-3.866 0-7 3.134-7 7h14c0-3.866-3.134-7-7-7z" />
          </svg>
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