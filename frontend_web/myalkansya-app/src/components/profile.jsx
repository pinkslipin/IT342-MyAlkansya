import React, { useState, useEffect } from "react";
import axios from "axios";
import Sidebar from "./sidebar";
import TopBar from "./topbar";
import { useNavigate } from "react-router-dom";
import { getCurrencyName, fetchAvailableCurrencies } from "./getCurrency";

const Profile = () => {
  const [user, setUser] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [showPasswordChange, setShowPasswordChange] = useState(false);
  const [formData, setFormData] = useState({
    firstname: "",
    lastname: "",
    email: "",
    currency: "",
  });
  const [passwordData, setPasswordData] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [profileImage, setProfileImage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();
  
  // Add these state variables right after your other useState declarations
  const [showProfileUpdateConfirm, setShowProfileUpdateConfirm] = useState(false);
  const [showPictureUploadConfirm, setShowPictureUploadConfirm] = useState(false);
  const [showPasswordChangeConfirm, setShowPasswordChangeConfirm] = useState(false);
  const [tempProfilePicture, setTempProfilePicture] = useState(null);
  const [availableCurrencies, setAvailableCurrencies] = useState([]);

  // Clean up the isOAuthUser function to be more focused
  const isOAuthUser = () => {
    return user && (
      user.authProvider === "GOOGLE" || 
      user.authProvider === "FACEBOOK" ||
      user.providerId !== null
    );
  };

  // Simplify the isLocalUser function
  const isLocalUser = () => {
    return user && user.authProvider === "LOCAL";
  };

  // First, let's modify the debugging function to help identify what's happening:
  useEffect(() => {
    if (user) {
      console.log("User auth provider:", user.authProvider);
      console.log("Is LOCAL user?:", user.authProvider === "LOCAL");
      console.log("Is OAuth user by our check:", isOAuthUser());
      console.log("Full user object:", user);
    }
  }, [user]);

  // Add this useEffect to forcibly reset email to original value whenever formData changes for OAuth users
  useEffect(() => {
    if (isOAuthUser() && user && formData.email !== user.email) {
      // Force reset the email field to prevent any changes
      setFormData(prevData => ({
        ...prevData,
        email: user.email
      }));
    }
  }, [formData, user]); 

  // Clean up the fetchUser function by removing excess logging
  const fetchUser = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You are not logged in.");
        setLoading(false);
        setTimeout(() => navigate("/login"), 2000);
        return;
      }

      const response = await axios.get("http://localhost:8080/api/users/me", {
        headers: { Authorization: `Bearer ${authToken}` },
      });
      
      // Create user object with consistent auth provider
      const userData = {
        ...response.data,
        authProvider: response.data.authProvider || 
                     (response.data.providerId ? "OAUTH" : "LOCAL")
      };
      
      setUser(userData);
      
      // Initialize form data with user data
      setFormData({
        firstname: userData.firstname || "",
        lastname: userData.lastname || "",
        email: userData.email || "",
        currency: userData.currency || "PHP",
      });
      
      // Set profile image if available
      if (userData.profilePicture) {
        if (userData.profilePicture.startsWith('http')) {
          setProfileImage(userData.profilePicture);
        } else {
          const baseUrl = "http://localhost:8080";
          const path = userData.profilePicture.startsWith('/') 
            ? userData.profilePicture 
            : `/${userData.profilePicture}`;
          
          const finalUrl = `${baseUrl}${path}?t=${new Date().getTime()}`;
          setProfileImage(finalUrl);
        }
      }
      
      setLoading(false);
    } catch (err) {
      console.error("Error fetching user data:", err);
      setError("Failed to load your profile. Please try again later.");
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };
  
  const handlePasswordInputChange = (e) => {
    const { name, value } = e.target;
    setPasswordData((prev) => ({ ...prev, [name]: value }));
  };

  // Modify handleSave to be even more strict about email updates for OAuth users
  const handleSave = () => {
    setError("");
    
    // Validate form data
    if (!formData.firstname.trim() || !formData.lastname.trim()) {
      setError("Name fields are required");
      return;
    }
  
    if (!isOAuthUser() && !formData.email.trim()) {
      setError("Email is required");
      return;
    } else if (isOAuthUser() && formData.email !== user.email) {
      setError("Email cannot be changed for accounts linked to Google or Facebook");
      return;
    }
    
    // Show confirmation dialog
    setShowProfileUpdateConfirm(true);
  };
  
  // Add the actual update function that will be called after confirmation
  const confirmProfileUpdate = async () => {
    setShowProfileUpdateConfirm(false);
    setSaving(true);
    
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You are not logged in.");
        setSaving(false);
        setTimeout(() => navigate("/login"), 2000);
        return;
      }

      // Check if currency has changed
      const currencyChanged = user.currency !== formData.currency;
      const oldCurrency = user.currency;
      
      // Create data to update
      const updatedData = {
        firstname: formData.firstname,
        lastname: formData.lastname,
        currency: formData.currency
      };
      
      if (!isOAuthUser()) {
        updatedData.email = formData.email;
      }

      // If currency has changed, add a flag to tell the backend to convert data
      if (currencyChanged) {
        updatedData.convertFromCurrency = oldCurrency;
        updatedData.convertToCurrency = formData.currency;
      }

      const response = await axios.put(
        "http://localhost:8080/api/users/update",
        updatedData,
        {
          headers: { 
            Authorization: `Bearer ${authToken}`,
            'Content-Type': 'application/json'
          },
        }
      );

      const updatedUserData = {
        ...response.data,
        authProvider: response.data.authProvider || user.authProvider || 
                     (response.data.providerId ? "OAUTH" : "LOCAL")
      };
      
      setUser(updatedUserData);
      
      if (currencyChanged) {
        setSuccess(`Profile updated successfully! All your financial data has been converted from ${oldCurrency} to ${formData.currency}.`);
      } else {
        setSuccess("Profile updated successfully!");
      }
      
      setIsEditing(false);
      setTimeout(() => setSuccess(""), 5000); // Show success message longer for currency conversions
    } catch (err) {
      console.error("Error updating user data:", err);
      if (err.response && err.response.status === 409) {
        setError("Email already in use by another account.");
      } else if (err.response && err.response.data && err.response.data.error) {
        setError(err.response.data.error);
      } else {
        setError("Failed to update profile. Please try again later.");
      }
    } finally {
      setSaving(false);
    }
  };

  const handleChangePassword = (e) => {
    e.preventDefault();
    setError("");
    
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setError("New passwords do not match");
      return;
    }
    
    if (passwordData.newPassword.length < 6) {
      setError("Password must be at least 6 characters long");
      return;
    }
    
    // Show confirmation dialog
    setShowPasswordChangeConfirm(true);
  };
  
  // Add the actual password change function that will be called after confirmation
  const confirmPasswordChange = async () => {
    setShowPasswordChangeConfirm(false);
    setSaving(true);
    
    try {
      const authToken = localStorage.getItem("authToken");
      
      await axios.put("http://localhost:8080/api/users/change-password", 
        {
          currentPassword: passwordData.currentPassword,
          newPassword: passwordData.newPassword
        },
        {
          headers: { Authorization: `Bearer ${authToken}` }
        }
      );
      
      setSuccess("Password updated successfully!");
      setPasswordData({
        currentPassword: "",
        newPassword: "",
        confirmPassword: ""
      });
      setShowPasswordChange(false);
      setTimeout(() => setSuccess(""), 3000);
    } catch (err) {
      console.error("Error changing password:", err);
      if (err.response && err.response.status === 401) {
        setError("Current password is incorrect");
      } else {
        setError("Failed to update password. Please try again later.");
      }
    } finally {
      setSaving(false);
    }
  };

  const handleProfilePictureChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
  
    // Validate file is an image
    if (!file.type.startsWith('image/')) {
      setError("Please select an image file");
      return;
    }
    
    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setError("Image size should be less than 5MB");
      return;
    }
  
    // Store the file temporarily and show confirmation
    setTempProfilePicture(file);
    setShowPictureUploadConfirm(true);
  };
  
  // Add the actual upload function that will be called after confirmation
  const confirmProfilePictureUpload = async () => {
    setShowPictureUploadConfirm(false);
    setError("");
    setUploading(true);
    
    const formData = new FormData();
    formData.append("profilePicture", tempProfilePicture);
  
    try {
      const authToken = localStorage.getItem("authToken");
      const response = await axios.post(
        "http://localhost:8080/api/users/uploadProfilePicture",
        formData,
        {
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "multipart/form-data",
          },
        }
      );
      
      if (response.data.profilePicture) {
        if (response.data.profilePicture.startsWith('http')) {
          setProfileImage(response.data.profilePicture);
        } else {
          const baseUrl = "http://localhost:8080";
          const path = response.data.profilePicture.startsWith('/') 
            ? response.data.profilePicture 
            : `/${response.data.profilePicture}`;
          
          const finalUrl = `${baseUrl}${path}?t=${new Date().getTime()}`;
          setProfileImage(finalUrl);
        }
        
        setUser(prev => ({...prev, profilePicture: response.data.profilePicture}));
      }
      
      setSuccess("Profile picture uploaded successfully!");
      setTimeout(() => setSuccess(""), 3000);
    } catch (err) {
      console.error("Error uploading profile picture:", err);
      setError("Failed to upload profile picture. Please try again later.");
    } finally {
      setUploading(false);
      setTempProfilePicture(null);
    }
  };

  const handleImageError = () => {
    console.log("Error loading profile image, falling back to default");
    setProfileImage("/default-profile.png");
  };

  useEffect(() => {
    const initializeData = async () => {
      await fetchUser();
      // Use the imported fetchAvailableCurrencies function
      await fetchAvailableCurrencies(setAvailableCurrencies);
    };
    
    initializeData();
  }, []);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-t-2 border-b-2 border-[#18864F]"></div>
        <p className="mt-4 text-[#18864F] font-semibold">Loading profile...</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-screen">
      <TopBar />
      
      <div className="flex flex-1 mt-16">
        <Sidebar activePage="profile" />
        
        <div className="flex-1 p-8 ml-72 bg-[#FEF6EA]">
          {/* Header with background */}
          <div className="bg-[#18864F] p-6 rounded-t-lg shadow-md">
            <h1 className="text-2xl font-bold text-white">My Profile</h1>
            <p className="text-white/80">Manage and protect your account</p>
          </div>
          
          {error && (
            <div className="mb-6 p-4 bg-red-100 border-l-4 border-red-500 text-red-700 rounded">
              <p>{error}</p>
            </div>
          )}
          
          {success && (
            <div className="mb-6 p-4 bg-green-100 border-l-4 border-green-500 text-green-700 rounded">
              <p>{success}</p>
            </div>
          )}
          
          {/* OAuth Account Banner - Only show when NOT editing */}
          {isOAuthUser() && !isEditing && (
            <div className="mb-4 p-4 bg-blue-50 border-l-4 border-blue-500 text-blue-700 rounded">
              <div className="flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                </svg>
                <p>You're signed in with <strong>{user.authProvider ? (user.authProvider.charAt(0) + user.authProvider.slice(1).toLowerCase()) : 'OAuth provider'}</strong>.</p>
              </div>
            </div>
          )}
          
          {isEditing && (
            <div className="mb-6 p-4 bg-yellow-100 border-l-4 border-yellow-500 text-yellow-800 rounded flex items-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
              </svg>
              <p className="font-medium">You are currently editing your profile. Click "Update Profile" to save changes.</p>
            </div>
          )}

          <div className="bg-white p-8 rounded-b-lg shadow-md">
            <div className="flex flex-col md:flex-row gap-10">
              {/* Profile Picture Section - Left Side */}
              <div className="flex flex-col items-center">
                <div className="relative">
                  <div className="w-36 h-36 rounded-full border-4 border-[#FFC107] overflow-hidden">
                    <img
                      src={profileImage}
                      alt="Profile"
                      className="w-full h-full object-cover"
                      onError={handleImageError}
                    />
                  </div>
                  <label
                    htmlFor="profilePicture"
                    className={`absolute bottom-0 right-0 bg-[#18864F] text-white p-2 rounded-full cursor-pointer ${uploading ? 'opacity-50 pointer-events-none' : ''}`}
                  >
                    {uploading ? (
                      <div className="h-5 w-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    ) : (
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    )}
                  </label>
                  <input
                    type="file"
                    id="profilePicture"
                    accept="image/*"
                    className="hidden"
                    onChange={handleProfilePictureChange}
                    disabled={uploading}
                  />
                </div>
              </div>
            
              {/* Form Sections - Right Side */}
              <div className="flex-1">
                {/* Account Information Section */}
                <div className="mb-8">
                  <h2 className="text-xl font-bold text-[#18864F] mb-4 pb-2 border-b-2 border-[#FFC107]">
                    Account Information
                  </h2>
                  
                  <div className="space-y-4">
                    {/* Email Field - Show restrictions only when editing */}
                    <div>
                      <label className="block text-[#18864F] font-medium mb-2">
                        Email:
                        {isEditing && user && user.authProvider !== "LOCAL" && (
                          <span className="text-xs ml-2 bg-red-100 text-red-800 px-2 py-1 rounded-full">
                            Cannot be changed (OAuth account)
                          </span>
                        )}
                      </label>
                      
                      {/* Modified condition to ensure LOCAL users can edit their email */}
                      {isEditing && user.authProvider === "LOCAL" ? (
                        <input
                          type="email"
                          name="email"
                          value={formData.email}
                          onChange={handleInputChange}
                          className="w-full p-3 bg-yellow-50 border-2 border-[#18864F] rounded-md focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                          required
                        />
                      ) : isEditing && user.authProvider !== "LOCAL" ? (
                        <div className="w-full p-3 bg-gray-100 border-l-4 border-l-red-300 rounded-md">
                          {user.email}
                          <span className="ml-2 text-sm text-red-500 font-bold">
                            (Managed by external provider)
                          </span>
                        </div>
                      ) : (
                        <div className="w-full p-3 bg-gray-100 rounded-md">
                          {user.email}
                        </div>
                      )}
                    </div>
                    
                    {/* Password Field - Only show for LOCAL auth_provider users */}
                    {user && (user.authProvider === "LOCAL") && (
                      <div>
                        <div className="flex justify-between items-center mb-2">
                          <label className="block text-[#18864F] font-medium">
                            Password:
                            {/* Optional: Show what auth type was detected */}
                            <span className="text-xs ml-2 text-gray-500">
                            </span>
                          </label>
                          <button 
                            type="button" 
                            className="text-sm text-[#18864F] font-medium hover:text-[#FFC107] transition duration-300"
                            onClick={() => setShowPasswordChange(!showPasswordChange)}
                          >
                            Change Password
                          </button>
                        </div>
                        <div className="w-full p-3 bg-gray-100 rounded-md">
                          ••••••••••••
                        </div>
                        
                        {/* Rest of password change form */}
                        {showPasswordChange && (
                          <div className="mt-4 p-4 bg-gray-50 rounded-md border border-gray-200">
                            <h3 className="text-lg font-medium text-[#18864F] mb-3">Change Password</h3>
                            <form onSubmit={handleChangePassword} className="space-y-4">
                              {/* Current Password */}
                              <div className="relative">
                                <label className="block text-gray-700 text-sm font-medium mb-1">Current Password:</label>
                                <input
                                  type="password"
                                  name="currentPassword"
                                  value={passwordData.currentPassword}
                                  onChange={handlePasswordInputChange}
                                  className="w-full p-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                                  required
                                />
                              </div>
                              
                              {/* New Password */}
                              <div className="relative">
                                <label className="block text-gray-700 text-sm font-medium mb-1">New Password:</label>
                                <input
                                  type="password"
                                  name="newPassword"
                                  value={passwordData.newPassword}
                                  onChange={handlePasswordInputChange}
                                  className="w-full p-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                                  required
                                />
                              </div>
                              
                              {/* Confirm New Password */}
                              <div className="relative">
                                <label className="block text-gray-700 text-sm font-medium mb-1">Confirm New Password:</label>
                                <input
                                  type="password"
                                  name="confirmPassword"
                                  value={passwordData.confirmPassword}
                                  onChange={handlePasswordInputChange}
                                  className="w-full p-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                                  required
                                />
                              </div>
                              
                              {/* Submit Button */}
                              <button
                                type="submit"
                                disabled={saving}
                                className={`w-full bg-[#18864F] text-white py-2 rounded-md hover:bg-[#146c3c] transition duration-300 ${saving ? 'opacity-70' : ''}`}
                              >
                                {saving ? "Updating..." : "Change Password"}
                              </button>
                            </form>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
                
                {/* Personal Information Section */}
                <div>
                  <h2 className="text-xl font-bold text-[#18864F] mb-4 pb-2 border-b-2 border-[#FFC107]">
                    Personal Information
                  </h2>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-[#18864F] font-medium mb-2">
                        First Name:
                        {isEditing && <span className="text-xs ml-2 bg-green-100 text-green-800 px-2 py-1 rounded-full">Editing</span>}
                      </label>
                      {isEditing ? (
                        <input
                          type="text"
                          name="firstname"
                          value={formData.firstname}
                          onChange={handleInputChange}
                          className="w-full p-3 bg-yellow-50 border-2 border-[#18864F] rounded-md focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                          required
                        />
                      ) : (
                        <div className="w-full p-3 bg-gray-100 rounded-md">{user.firstname}</div>
                      )}
                    </div>

                    <div>
                      <label className="block text-[#18864F] font-medium mb-2">
                        Last Name:
                        {isEditing && <span className="text-xs ml-2 bg-green-100 text-green-800 px-2 py-1 rounded-full">Editing</span>}
                      </label>
                      {isEditing ? (
                        <input
                          type="text"
                          name="lastname"
                          value={formData.lastname}
                          onChange={handleInputChange}
                          className="w-full p-3 bg-yellow-50 border-2 border-[#18864F] rounded-md focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                          required
                        />
                      ) : (
                        <div className="w-full p-3 bg-gray-100 rounded-md">{user.lastname}</div>
                      )}
                    </div>

                    <div className="md:col-span-2">
                      <label className="block text-[#18864F] font-medium mb-2">
                        Preferred Currency:
                        {isEditing && <span className="text-xs ml-2 bg-green-100 text-green-800 px-2 py-1 rounded-full">Editing</span>}
                      </label>
                      {isEditing ? (
                        <select
                          name="currency"
                          value={formData.currency}
                          onChange={handleInputChange}
                          className="w-full p-3 bg-yellow-50 border-2 border-[#18864F] rounded-md focus:outline-none focus:ring-2 focus:ring-[#18864F]"
                          required
                        >
                          {availableCurrencies.length > 0 ? (
                            availableCurrencies.map(currency => (
                              <option key={currency.code} value={currency.code}>
                                {currency.code} - {currency.name}
                              </option>
                            ))
                          ) : (
                            // Fallback options if API fails
                            <>
                              <option value="PHP">PHP - Philippine Peso</option>
                              <option value="USD">USD - US Dollar</option>
                              <option value="EUR">EUR - Euro</option>
                              <option value="JPY">JPY - Japanese Yen</option>
                              <option value="GBP">GBP - British Pound</option>
                            </>
                          )}
                        </select>
                      ) : (
                        <div className="w-full p-3 bg-gray-100 rounded-md">{user.currency || "PHP"}</div>
                      )}
                    </div>
                  </div>
                </div>
                
                {/* Update Profile Button */}
                <div className="mt-8 flex justify-center">
                  {isEditing ? (
                    <div className="flex gap-4">
                      <button
                        type="button"
                        onClick={handleSave}
                        disabled={saving}
                        className={`bg-[#18864F] text-white px-6 py-3 rounded-md font-bold hover:bg-[#146c3c] transition duration-300 ${saving ? 'opacity-70' : ''}`}
                      >
                        {saving ? "Updating..." : "Update Profile"}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setIsEditing(false);
                          setError("");
                          // Reset form data to original values
                          setFormData({
                            firstname: user.firstname || "",
                            lastname: user.lastname || "",
                            email: user.email || "",
                            currency: user.currency || "PHP",
                          });
                        }}
                        className="bg-gray-300 text-gray-700 px-6 py-3 rounded-md font-bold hover:bg-gray-400 transition duration-300"
                        disabled={saving}
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setIsEditing(true)}
                      className="bg-[#FFC107] text-[#18864F] px-6 py-3 rounded-md font-bold hover:bg-[#e5ac00] transition duration-300"
                    >
                      Update Profile
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <ConfirmationModal
        isOpen={showProfileUpdateConfirm}
        title="Confirm Profile Update"
        message="Are you sure you want to update your profile?"
        onConfirm={confirmProfileUpdate}
        onCancel={() => setShowProfileUpdateConfirm(false)}
      />
      <ConfirmationModal
        isOpen={showPictureUploadConfirm}
        title="Confirm Picture Upload"
        message="Are you sure you want to upload this picture?"
        onConfirm={confirmProfilePictureUpload}
        onCancel={() => setShowPictureUploadConfirm(false)}
      />
      <ConfirmationModal
        isOpen={showPasswordChangeConfirm}
        title="Confirm Password Change"
        message="Are you sure you want to change your password?"
        onConfirm={confirmPasswordChange}
        onCancel={() => setShowPasswordChangeConfirm(false)}
      />
    </div>
  );
};

// Updated ConfirmationModal that doesn't use a full black overlay
const ConfirmationModal = ({ isOpen, title, message, onConfirm, onCancel }) => {
  if (!isOpen) return null;
  
  return (
    <div className="fixed inset-0 flex items-center justify-center z-50 bg-transparent backdrop-blur-sm">
      <div className="bg-white rounded-lg p-6 max-w-md w-full shadow-2xl border border-gray-200">
        <h3 className="text-xl font-bold text-[#18864F] mb-3">{title}</h3>
        <p className="text-gray-700 mb-6">{message}</p>
        <div className="flex justify-end space-x-3">
          <button 
            onClick={onCancel}
            className="px-4 py-2 bg-gray-300 text-gray-800 rounded-md hover:bg-gray-400 transition"
          >
            Cancel
          </button>
          <button 
            onClick={onConfirm}
            className="px-4 py-2 bg-[#18864F] text-white rounded-md hover:bg-[#146c3c] transition"
          >
            Confirm
          </button>
        </div>
      </div>
    </div>
  );
};

export default Profile;