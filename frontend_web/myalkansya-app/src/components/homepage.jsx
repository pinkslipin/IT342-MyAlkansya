import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const HomePage = () => {
  const [user, setUser] = useState(null);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const response = await axios.get("http://localhost:8080/user-info", {
          withCredentials: true, // Include cookies for authentication
        });
        setUser(response.data);
      } catch (err) {
        setError("Failed to fetch user information. Please log in again.");
        setTimeout(() => {
          navigate("/login"); // Redirect to login if not authenticated
        }, 3000);
      }
    };

    fetchUserInfo();
  }, [navigate]);

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
    return <p>Loading...</p>;
  }

  return (
    <div>
      <h1>Welcome, {user.name}!</h1>
      <p>Email: {user.email}</p>
      <img
        src={user.picture}
        alt="Profile"
        style={{ borderRadius: "50%", width: "150px", height: "150px" }}
      />
      <p>
        <button
          onClick={() => {
            localStorage.removeItem("authToken"); // Clear token
            navigate("/login"); // Redirect to login
          }}
        >
          Logout
        </button>
      </p>
    </div>
  );
};

export default HomePage;