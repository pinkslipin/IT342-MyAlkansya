import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

const TopBar = ({ user, profileImage, onLogout }) => {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const navigate = useNavigate();

  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  const handleProfileClick = () => {
    setDropdownOpen(!dropdownOpen);
  };

  const handleNavigateToProfile = () => {
    navigate("/profile"); // Adjust this route to your profile page
    setDropdownOpen(false);
  };

  const handleLogout = () => {
    onLogout();
    setDropdownOpen(false);
  };

  return (
    <div className="bg-[#18864F] flex items-center justify-between px-6 py-3 shadow-md fixed top-0 left-0 w-full z-50">
      {/* Logo */}
      <div
        className="flex items-center cursor-pointer"
        onClick={() => navigate("/")}
      >
        <img
          src="/src/assets/MyAlkansyaTextLogo.png" // Replace with your logo path
          alt="MyAlkansya Logo"
          className="h-10"
        />
      </div>

      {/* Profile Section */}
      <div className="relative">
        <img
          src={profileImage || defaultProfilePic}
          alt="Profile"
          className="w-10 h-10 rounded-full cursor-pointer"
          onClick={handleProfileClick}
        />
        {dropdownOpen && (
          <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg z-10">
            <button
              onClick={handleNavigateToProfile}
              className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
            >
              Profile
            </button>
            <button
              onClick={handleLogout}
              className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
            >
              Logout
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default TopBar;