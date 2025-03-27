import React, { useEffect, useState } from "react";
import axios from "axios";
import { Link } from "react-router-dom";

const Home = () => {
  const [user, setUser] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const response = await axios.get("http://localhost:8080/user-info", {
          withCredentials: true, // Include cookies for authentication
        });
        setUser(response.data);
      } catch (err) {
        setError("You are not logged in. Please log in to access your account.");
      }
    };

    fetchUserInfo();
  }, []);

  if (error) {
    return (
      <div>
        <h1>Welcome to MyAlkansya</h1>
        <p>{error}</p>
        <p>
          <Link to="/login">Login</Link> or <Link to="/register">Register</Link>
        </p>
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
        <Link to="/login">Logout</Link>
      </p>
    </div>
  );
};

export default Home;