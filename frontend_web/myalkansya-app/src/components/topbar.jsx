import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

const TopBar = () => {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [user, setUser] = useState(null);
  const [profileImage, setProfileImage] = useState(null);
  const navigate = useNavigate();

  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  // Fetch user data and profile picture
  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const authToken = localStorage.getItem("authToken");
        if (!authToken) {
          navigate("/login");
          return;
        }

        const response = await axios.get("http://localhost:8080/api/users/me", {
          headers: { Authorization: `Bearer ${authToken}` },
        });
        
        setUser(response.data);

        // Universal profile picture handling - works with both uploaded and OAuth pictures
        if (response.data.profilePicture) {
          // If it's already a full URL (like from Google/Facebook)
          if (response.data.profilePicture.startsWith('http')) {
            console.log("TopBar: Using external provider profile picture");
            setProfileImage(response.data.profilePicture);
          } 
          // If it's a path to our own API (uploaded pictures)
          else {
            const baseUrl = "http://localhost:8080";
            const path = response.data.profilePicture.startsWith('/') 
              ? response.data.profilePicture 
              : `/${response.data.profilePicture}`;
            
            const finalUrl = `${baseUrl}${path}?t=${new Date().getTime()}`;
            console.log("TopBar: Using uploaded profile picture");
            setProfileImage(finalUrl);
          }
        } else {
          setProfileImage(defaultProfilePic);
        }

      } catch (err) {
        console.error("Error fetching user data for topbar:", err);
        navigate("/login");
      }
    };

    fetchUserInfo();
  }, [navigate]);

  const handleProfileClick = () => {
    setDropdownOpen(!dropdownOpen);
  };

  const handleNavigateToProfile = () => {
    navigate("/profile");
    setDropdownOpen(false);
  };

  const handleLogout = () => {
    localStorage.removeItem("authToken");
    navigate("/login");
    setDropdownOpen(false);
  };

  // Handle image loading errors
  const handleImageError = () => {
    console.log("Error loading profile image in topbar, using default");
    setProfileImage(defaultProfilePic);
  };

  return (
    <div className="bg-[#18864F] flex items-center justify-between px-6 py-3 shadow-md fixed top-0 left-0 w-full z-50">
      {/* Logo */}
      <div
        className="flex items-center cursor-pointer"
        onClick={() => navigate("/home")}
      >
        <img
          src="/src/assets/MyAlkansyaTextLogo.png"
          alt="MyAlkansya Logo"
          className="h-10"
        />
      </div>

      {/* Profile Section */}
      <div className="relative">
        <img
          src={profileImage || defaultProfilePic}
          alt="Profile"
          className="w-10 h-10 rounded-full cursor-pointer object-cover"
          onClick={handleProfileClick}
          onError={handleImageError}
        />
        {dropdownOpen && (
          <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg z-10">
            <div className="px-4 py-2 text-sm text-gray-700 border-b border-gray-200">
              {user ? `${user.firstname} ${user.lastname}` : 'Loading...'}
            </div>
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