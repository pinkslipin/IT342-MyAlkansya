import React, { useState } from "react";
import axios from "axios";

const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

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

      setSuccess(`Welcome, ${user.name}!`);
    } catch (err) {
      if (err.response && err.response.status === 401) {
        setError("Invalid email or password.");
      } else {
        setError("An error occurred. Please try again later.");
      }
    }
  };

  const handleGoogleLogin = () => {
    // Redirect to the Google OAuth2 login endpoint
    window.location.href = "http://localhost:8080/oauth2/authorization/google";
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
      <button onClick={handleGoogleLogin} style={{ backgroundColor: "#4285F4", color: "white", padding: "10px", border: "none", cursor: "pointer" }}>
        Login with Google
      </button>
    </div>
  );
};

export default Login;