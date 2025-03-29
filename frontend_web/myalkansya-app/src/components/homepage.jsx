import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const HomePage = () => {
  const [user, setUser] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        // Get the auth token for local users
        const authToken = localStorage.getItem("authToken");
        
        if (!authToken) {
          // Check if we're returning from Google OAuth (check URL parameters)
          const urlParams = new URLSearchParams(window.location.search);
          const oauthToken = urlParams.get('token');
          
          if (oauthToken) {
            // We have a token from OAuth redirect
            localStorage.setItem("authToken", oauthToken);
          } else {
            // No authentication at all, redirect to login
            throw new Error("No authentication token found");
          }
        }
        
        // Configure request headers with proper authentication
        const config = {
          headers: {
            Authorization: `Bearer ${authToken || localStorage.getItem("authToken")}`
          }
        };
        
        const response = await axios.get("http://localhost:8080/api/users/me", config);
        setUser(response.data);
        setLoading(false);
      } catch (err) {
        console.error("Error fetching user info:", err);
        setError("Failed to fetch user information. Please log in again.");
        setLoading(false);
        setTimeout(() => {
          navigate("/login");
        }, 2000);
      }
    };

    fetchUserInfo();
  }, [navigate]);

  const handleLogout = () => {
    // Clear local storage
    localStorage.removeItem("authToken");
    // For Google OAuth users, it's simpler to just redirect to login
    navigate("/login");
  };

  const goToIncomePage = () => {
    navigate("/income");
  };

  if (loading) {
    return <p>Loading...</p>;
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

  if (!user) {
    return <p>No user data available. Please <button onClick={() => navigate("/login")}>Login</button></p>;
  }

  // Handle both Google OAuth and Local login user formats
  const displayName = user.name ? 
    user.name : 
    `${user.firstname || ''} ${user.lastname || ''}`.trim();
  
  const emailToDisplay = user.email || '';
  
  // Default profile image if none provided
  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";
  const profilePic = user.picture || user.profilePicture || defaultProfilePic;

  return (
    <div>
      <h1>Welcome, {displayName}!</h1>
      <p>Email: {emailToDisplay}</p>
      <img
        src={profilePic}
        alt="Profile"
        style={{ borderRadius: "50%", width: "150px", height: "150px" }}
      />
      <p>
        <button
          onClick={handleLogout}
          style={{
            backgroundColor: "#dc3545",
            color: "white",
            padding: "10px",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
            margin: "5px"
          }}
        >
          Logout
        </button>
      </p>
      <p>
        <button
          onClick={goToIncomePage}
          style={{
            backgroundColor: "#007bff",
            color: "white",
            padding: "10px",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
            margin: "5px"
          }}
        >
          Go to Income Page
        </button>
      </p>
    </div>
  );
};

export default HomePage;