import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    try {
      const response = await axios.post("http://localhost:8080/api/users/login", {
        email,
        password,
      });

      // Assuming the response contains a token and user details
      const { token, user } = response.data;

      // Save the token to localStorage (or cookies)
      localStorage.setItem("authToken", token);

      setSuccess(`Welcome, ${user.firstname} ${user.lastname}!`);

      // Redirect to the homepage
      setTimeout(() => {
        navigate("/");
      }, 2000);
    } catch (err) {
      if (err.response && err.response.status === 401) {
        setError("Invalid email or password.");
      } else {
        setError("An error occurred. Please try again later.");
      }
    }
  };

  const handleGoogleLogin = () => {
    // Store the frontend URL for redirection after OAuth
    localStorage.setItem("frontendRedirectUrl", window.location.origin);
    
    // Redirect to the Google OAuth2 login endpoint with the redirect parameter
    window.location.href = `http://localhost:8080/oauth2/authorization/google?redirect_uri=${encodeURIComponent(window.location.origin)}`;
  };

  const handleFacebookLogin = () => {
    // Store the frontend URL for redirection after OAuth
    localStorage.setItem("frontendRedirectUrl", window.location.origin);
    
    // Redirect to the Facebook OAuth2 login endpoint with the redirect parameter
    window.location.href = `http://localhost:8080/oauth2/authorization/facebook?redirect_uri=${encodeURIComponent(window.location.origin)}`;
  };

  return (
    <div>
      <h2>Login</h2>
      <form onSubmit={handleLogin}>
        <div>
          <label>Email:</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div>
          <label>Password:</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit">Login</button>
      </form>
      {error && <p style={{ color: "red" }}>{error}</p>}
      {success && <p style={{ color: "green" }}>{success}</p>}
      <hr />
      <button
        onClick={handleGoogleLogin}
        style={{
          backgroundColor: "#4285F4",
          color: "white",
          padding: "10px",
          border: "none",
          cursor: "pointer",
          marginBottom: "10px",
          width: "100%",
          display: "block"
        }}
      >
        Login with Google
      </button>
      
      <button
        onClick={handleFacebookLogin}
        style={{
          backgroundColor: "#1877F2", // Facebook blue
          color: "white",
          padding: "10px",
          border: "none",
          cursor: "pointer",
          width: "100%",
          display: "block"
        }}
      >
        Login with Facebook
      </button>
      
      <div style={{ marginTop: "15px" }}>
        <p>Don't have an account?</p>
        <button
          onClick={() => navigate("/register")}
          style={{
            backgroundColor: "#28a745",
            color: "white",
            padding: "10px",
            border: "none",
            cursor: "pointer",
          }}
        >
          Register
        </button>
      </div>
    </div>
  );
};

export default Login;