import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const HomePage = () => {
  const [user, setUser] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [profileImage, setProfileImage] = useState(null);
  const navigate = useNavigate();

  // Default profile image as fallback
  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        // Get the auth token
        const authToken = localStorage.getItem("authToken");
        
        if (!authToken) {
          // Check if we're returning from OAuth (URL parameters)
          const urlParams = new URLSearchParams(window.location.search);
          const oauthToken = urlParams.get('token');
          
          if (oauthToken) {
            // We have a token from OAuth redirect
            localStorage.setItem("authToken", oauthToken);
          } else {
            // No authentication, redirect to login
            throw new Error("No authentication token found");
          }
        }
        
        // Configure request with both bearer token and cookies
        const config = {
          withCredentials: true, // For OAuth cookies
          headers: {
            Authorization: `Bearer ${authToken || localStorage.getItem("authToken")}`
          }
        };
        
        // Try both endpoints based on your auth implementation
        let response;
        try {
          response = await axios.get("http://localhost:8080/api/users/me", config);
        } catch (innerErr) {
          // Fallback to user-info endpoint if the first one fails
          response = await axios.get("http://localhost:8080/user-info", config);
        }
        
        console.log("User data fetched:", response.data);
        setUser(response.data);
        
        // Look for profile picture in various possible locations
        const userData = response.data;
        if (userData) {
          // Try all possible profile picture fields
          const possiblePictureFields = [
            'picture', 'profilePicture', 'profile_picture', 
            'avatar', 'photo', 'image', 'imageUrl'
          ];
          
          for (const field of possiblePictureFields) {
            if (userData[field] && typeof userData[field] === 'string') {
              console.log(`Found picture in field: ${field}`, userData[field]);
              setProfileImage(userData[field]);
              break;
            }
          }
          
          // If user has a Google provider ID but no picture, try to construct a Google URL
          if (!profileImage && userData.authProvider === 'GOOGLE' && userData.providerId) {
            const googleId = userData.providerId;
            setProfileImage(`https://lh3.googleusercontent.com/a/${googleId}`);
          }
        }
        
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
    localStorage.removeItem("authToken");
    navigate("/login");
  };

  const goToIncomePage = () => {
    navigate("/income");
  };
  
  const goToExpensePage = () => {
    navigate("/expense");
  };
  
  const goToBudgetPage = () => {
    navigate("/budget");
  };

  if (loading) {
    return <div style={{ textAlign: 'center', marginTop: '50px' }}>
      <h2>Loading your profile...</h2>
      <div style={{ marginTop: '20px' }}>Please wait while we retrieve your information</div>
    </div>;
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
  
  // Get total savings and currency
  const totalSavings = user.totalSavings !== undefined ? user.totalSavings : 0;
  const currency = user.currency || 'USD';
  
  // Determine which profile image to show
  const imageToDisplay = profileImage || user.picture || user.profilePicture || defaultProfilePic;

  // Format the savings amount with the currency
  const formattedSavings = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(totalSavings);

  return (
    <div style={{ textAlign: 'center', maxWidth: '600px', margin: '0 auto', padding: '20px' }}>
      <h1 style={{ marginBottom: '10px' }}>Welcome, {displayName}!</h1>
      <p style={{ marginBottom: '5px' }}>Email: {emailToDisplay}</p>
      
      {/* Display total savings */}
      <div style={{ 
        backgroundColor: '#f0f8ff', 
        padding: '15px', 
        borderRadius: '8px', 
        margin: '20px auto',
        maxWidth: '300px',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
      }}>
        <h3 style={{ margin: '0 0 10px 0', color: '#28a745' }}>Total Savings</h3>
        <p style={{ 
          fontSize: '24px', 
          fontWeight: 'bold', 
          margin: '0',
          color: totalSavings >= 0 ? '#28a745' : '#dc3545'
        }}>
          {formattedSavings}
        </p>
      </div>
      
      {/* Display profile image with error handling */}
      <div>
        <img
          src={imageToDisplay}
          alt="Profile"
          style={{ 
            borderRadius: "50%", 
            width: "150px", 
            height: "150px",
            border: "2px solid #ccc",
            objectFit: "cover",
            boxShadow: "0 4px 8px rgba(0,0,0,0.1)"
          }}
          onError={(e) => {
            console.log("Image failed to load:", e.target.src);
            if (e.target.src !== defaultProfilePic) {
              e.target.src = defaultProfilePic;
            }
            e.target.onerror = null;
          }}
        />
      </div>
      
      <div style={{ marginTop: '30px', display: 'flex', flexDirection: 'column', gap: '10px', maxWidth: '300px', margin: '0 auto' }}>
        <button
          onClick={goToIncomePage}
          style={{
            backgroundColor: "#28a745", // Green for income
            color: "white",
            padding: "12px 20px",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
            fontWeight: "bold",
            fontSize: "16px"
          }}
        >
          Income Management
        </button>
        
        <button
          onClick={goToExpensePage}
          style={{
            backgroundColor: "#dc3545", // Red for expenses
            color: "white",
            padding: "12px 20px",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
            fontWeight: "bold",
            fontSize: "16px"
          }}
        >
          Expense Management
        </button>
        
        <button
          onClick={goToBudgetPage}
          style={{
            backgroundColor: "#007bff", // Blue for budget
            color: "white",
            padding: "12px 20px",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
            fontWeight: "bold",
            fontSize: "16px"
          }}
        >
          Budget Management
        </button>
        
        <button
          onClick={handleLogout}
          style={{
            backgroundColor: "#6c757d", // Gray for logout
            color: "white",
            padding: "12px 20px",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
            fontWeight: "bold",
            fontSize: "16px",
            marginTop: "10px"
          }}
        >
          Logout
        </button>
      </div>
    </div>  
  );
};

export default HomePage;