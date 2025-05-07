import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useUser } from "./UserContext";

const TopBar = () => {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const navigate = useNavigate();
  const { user, profileImage } = useUser();

  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  const handleProfileClick = () => setDropdownOpen(!dropdownOpen);
  const handleNavigateToProfile = () => { navigate("/profile"); setDropdownOpen(false); };
  const handleLogout = () => { localStorage.removeItem("authToken"); navigate("/login"); setDropdownOpen(false); };

  return (
    <div className="bg-[#18864F] flex items-center justify-between px-6 py-3 shadow-md fixed top-0 left-0 w-full z-50">
      <div className="flex items-center cursor-pointer" onClick={() => navigate("/home")}>
        <img src="/assets/MyAlkansyaTextLogo.png" alt="MyAlkansya Logo" className="h-10" />
      </div>
      <div className="relative">
        <img
          src={profileImage || defaultProfilePic}
          alt="Profile"
          className="w-10 h-10 rounded-full cursor-pointer object-cover"
          onClick={handleProfileClick}
          onError={(e) => { e.target.src = defaultProfilePic; }}
        />
        {dropdownOpen && (
          <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg z-10">
            <div className="px-4 py-2 text-sm text-gray-700 border-b border-gray-200">
              {user ? `${user.firstname} ${user.lastname}` : 'Loading...'}
            </div>
            <button onClick={handleNavigateToProfile} className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
              Profile
            </button>
            <button onClick={handleLogout} className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
              Logout
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default TopBar;