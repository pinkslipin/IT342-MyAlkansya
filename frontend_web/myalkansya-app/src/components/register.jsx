import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import myAlkansyaLogo from "../assets/myAlkansyaLogo.png";
import myAlkansyaTextLogo from "../assets/MyAlkansyaTextLogo.png";

const Register = () => {
  const [firstname, setFirstname] = useState("");
  const [lastname, setLastname] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [currency, setCurrency] = useState("PHP");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get("token");

    if (token) {
      localStorage.setItem("authToken", token);
      navigate("/");
    }
  }, [navigate]);

  const handleRegister = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    try {
      const response = await axios.post("http://localhost:8080/api/users/register", {
        firstname,
        lastname,
        email,
        password,
        currency,
      });

      setSuccess("Registration successful! Redirecting to login...");
      setTimeout(() => {
        navigate("/login");
      }, 2000);
    } catch (err) {
      console.error("Error during registration:", err);
      if (err.response && err.response.data) {
        setError(err.response.data.message || "An error occurred. Please try again.");
      } else {
        setError("An error occurred. Please try again.");
      }
    }
  };

  const handleGoogleSignUp = () => {
    localStorage.setItem("frontendRedirectUrl", window.location.origin);
    window.location.href = `http://localhost:8080/oauth2/authorization/google?redirect_uri=${encodeURIComponent(
      window.location.origin
    )}`;
  };

  const handleFacebookSignUp = () => {
    localStorage.setItem("frontendRedirectUrl", window.location.origin);
    window.location.href = `http://localhost:8080/oauth2/authorization/facebook?redirect_uri=${encodeURIComponent(
      window.location.origin
    )}`;
  };

  return (
    <div className="min-h-screen bg-[#FEF6EA] flex flex-col">
      {/* Navigation Bar */}
      <nav className="bg-[#18864F] p-4 w-full">
        <div className="flex justify-between items-center">
          <div className="flex items-center">
            <img src={myAlkansyaLogo} alt="MyAlkansya Logo" className="h-10 w-10 mr-2" />
            <img src={myAlkansyaTextLogo} alt="MyAlkansya Text Logo" className="h-6" />
          </div>
          <div className="space-x-2">
          <button
            className="bg-[#FFC107] text-[#FFFFFF] font-bold py-2 px-4 rounded hover:bg-yellow-500 transition duration-300"
            onClick={() => navigate("/login")}
          >
            SIGN IN
          </button>
          <button
            className="bg-[#FFC107] text-[#FFFFFF] font-bold py-2 px-4 rounded hover:bg-yellow-500 transition duration-300"
            onClick={() => navigate("/register")}
          >
            SIGN UP
          </button>
        </div>
        </div>
      </nav>

      {/* Main Content */}
      <div className="flex flex-1 flex-col md:flex-row items-center justify-center">
        {/* Register Form */}
        <div className="bg-white p-6 rounded-lg shadow-md w-full max-w-md mx-4">
          <h2 className="text-2xl font-bold mb-4">Register</h2>

          <form onSubmit={handleRegister}>
            <div className="mb-3">
              <div className="relative">
                <input
                  type="text"
                  placeholder="First Name"
                  className="w-full p-2 text-sm border rounded-md pl-8 focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  value={firstname}
                  onChange={(e) => setFirstname(e.target.value)}
                  required
                />
                <div className="absolute top-2.5 left-2 text-gray-400">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4c1.657 0 3 1.343 3 3s-1.343 3-3 3-3-1.343-3-3 1.343-3 3-3zm0 10c-3.866 0-7 3.134-7 7h14c0-3.866-3.134-7-7-7z" />
                  </svg>
                </div>
              </div>
            </div>

            <div className="mb-3">
              <div className="relative">
                <input
                  type="text"
                  placeholder="Last Name"
                  className="w-full p-2 text-sm border rounded-md pl-8 focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  value={lastname}
                  onChange={(e) => setLastname(e.target.value)}
                  required
                />
                <div className="absolute top-2.5 left-2 text-gray-400">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4c1.657 0 3 1.343 3 3s-1.343 3-3 3-3-1.343-3-3 1.343-3 3-3zm0 10c-3.866 0-7 3.134-7 7h14c0-3.866-3.134-7-7-7z" />
                  </svg>
                </div>
              </div>
            </div>

            <div className="mb-3">
              <div className="relative">
                <input
                  type="email"
                  placeholder="Email"
                  className="w-full p-2 text-sm border rounded-md pl-8 focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
                <div className="absolute top-2.5 left-2 text-gray-400">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                  </svg>
                </div>
              </div>
            </div>

            <div className="mb-3">
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  placeholder="Password"
                  className="w-full p-2 text-sm border rounded-md pl-8 focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <div className="absolute top-2.5 left-2 text-gray-400">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </div>
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute top-2.5 right-2 text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? (
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-5.523 0-10-4.477-10-10 0-1.875.525-3.625 1.425-5.125M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3.98 8.223A10.05 10.05 0 012 9c0 5.523 4.477 10 10 10 1.875 0 3.625-.525 5.125-1.425M9 12a3 3 0 116 0 3 3 0 01-6 0z" />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <div className="mb-3">
              <div className="relative">
                <input
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder="Confirm Password"
                  className="w-full p-2 text-sm border rounded-md pl-8 focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
                <div className="absolute top-2.5 left-2 text-gray-400">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </div>
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute top-2.5 right-2 text-gray-400 hover:text-gray-600"
                >
                  {showConfirmPassword ? (
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-5.523 0-10-4.477-10-10 0-1.875.525-3.625 1.425-5.125M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3.98 8.223A10.05 10.05 0 012 9c0 5.523 4.477 10 10 10 1.875 0 3.625-.525 5.125-1.425M9 12a3 3 0 116 0 3 3 0 01-6 0z" />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <div className="mb-4">
              <div className="relative">
                <select
                  className="w-full p-2 text-sm border rounded-md pl-8 focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value)}
                  required
                >
                  <option value="" disabled>
                    Select Currency
                  </option>
                  <option value="PHP">PHP</option>
                  <option value="USD">USD</option>
                  <option value="EUR">EUR</option>
                </select>
                <div className="absolute top-2.5 left-2 text-gray-400">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                  </svg>
                </div>
              </div>
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}
            {success && <p className="text-green-500 text-sm">{success}</p>}

            <button
              type="submit"
              className="w-full bg-[#18864F] text-white font-bold py-2 px-4 rounded-md hover:bg-green-700 transition duration-300"
            >
              REGISTER
            </button>
          </form>

          <div className="flex items-center my-4">
            <div className="flex-grow border-t border-gray-300"></div>
            <span className="px-4 text-gray-500 text-sm">OR</span>
            <div className="flex-grow border-t border-gray-300"></div>
          </div>

          <div className="mb-4">
            <p className="text-center mb-4">Sign In With</p>
            <button
              type="button"
              onClick={handleGoogleSignUp}
              className="w-full mb-3 flex items-center justify-center bg-white border border-gray-300 rounded-md p-2 hover:bg-gray-50 transition duration-300"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 48 48">
                <path fill="#FFC107" d="M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z"></path>
                <path fill="#FF3D00" d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z"></path>
                <path fill="#4CAF50" d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z"></path>
                <path fill="#1976D2" d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z"></path>
              </svg>
              Sign up with Google
            </button>
            <button
              type="button"
              onClick={handleFacebookSignUp}
              className="w-full flex items-center justify-center bg-white border border-gray-300 rounded-md p-2 hover:bg-gray-50 transition duration-300"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2 text-blue-600" viewBox="0 0 48 48">
                <path fill="#3F51B5" d="M42,37c0,2.762-2.238,5-5,5H11c-2.761,0-5-2.238-5-5V11c0-2.762,2.239-5,5-5h26c2.762,0,5,2.238,5,5V37z"></path>
                <path fill="#FFF" d="M34.368,25H31v13h-5V25h-3v-4h3v-2.41c0.002-3.508,1.459-5.59,5.592-5.59H35v4h-2.287C31.104,17,31,17.6,31,18.723V21h4L34.368,25z"></path>
              </svg>
              Sign up with Facebook
            </button>
          </div>
          <div className="text-center text-sm text-gray-600">
                      <span>Already have an account?</span>
                      <a href="#" onClick={() => navigate("/login")} className="text-[#18864F] font-medium hover:underline ml-1">Sign In</a>
                    </div>
        </div>

        {/* Welcome Side */}
        <div className="hidden md:flex md:w-1/2 justify-center items-center p-10 min-h-[60px]">
          <div className="bg-white/50 p-8 rounded-lg shadow-md w-full max-w-md mx-4 text-center min-h-[560px] min-w-[600px] flex flex-col justify-center">
            <h1 className="text-5xl font-bold mb-4">Get Started with</h1>
            <div className="text-5xl font-bold">
              <span className="text-[#FFC107]">My</span>
              <span className="text-[#18864F]">Alkansya</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;