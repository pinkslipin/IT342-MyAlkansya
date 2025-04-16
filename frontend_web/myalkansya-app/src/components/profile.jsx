import React, { useState, useEffect } from "react";
import axios from "axios";
import Sidebar from "./sidebar";
import TopBar from "./topbar";

const Profile = () => {
  const [user, setUser] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    firstname: "",
    lastname: "",
    email: "",
    currency: "",
  });
  const [profileImage, setProfileImage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchUser = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You are not logged in.");
        return;
      }

      const response = await axios.get("http://localhost:8080/api/users/me", {
        headers: { Authorization: `Bearer ${authToken}` },
      });

      setUser(response.data);
      setFormData({
        firstname: response.data.firstname,
        lastname: response.data.lastname,
        email: response.data.email,
        currency: response.data.currency,
      });
      setProfileImage(response.data.profilePicture || "/default-profile.png");
      setLoading(false);
    } catch (err) {
      console.error("Error fetching user data:", err);
      setError("Failed to fetch user data.");
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    try {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setError("You are not logged in.");
        return;
      }

      await axios.put(
        "http://localhost:8080/api/users/update",
        { ...formData },
        {
          headers: { Authorization: `Bearer ${authToken}` },
        }
      );

      setUser((prev) => ({ ...prev, ...formData }));
      setIsEditing(false);
    } catch (err) {
      console.error("Error updating user data:", err);
      setError("Failed to update user data.");
    }
  };

  const handleProfilePictureChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("profilePicture", file);

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

      setProfileImage(response.data.profilePicture);
    } catch (err) {
      console.error("Error uploading profile picture:", err);
      setError("Failed to upload profile picture.");
    }
  };

  useEffect(() => {
    fetchUser();
  }, []);

  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div className="text-red-500">{error}</div>;
  }

  return (
    <div className="flex">
      <Sidebar activePage="profile" />
      <div className="flex-1">
        <TopBar />
        <div className="flex justify-center mt-10">
          <div className="w-full max-w-4xl bg-white shadow-md rounded-md p-8">
            <h1 className="text-2xl font-bold text-[#18864F] mb-6">Profile</h1>
            <div className="flex gap-8">
              {/* Profile Picture Section */}
              <div className="flex flex-col items-center">
                <img
                  src={profileImage}
                  alt="Profile"
                  className="w-32 h-32 rounded-full object-cover border"
                />
                <label
                  htmlFor="profilePicture"
                  className="mt-4 bg-[#FFC107] text-[#18864F] px-4 py-2 rounded-md cursor-pointer font-bold"
                >
                  Change Picture
                </label>
                <input
                  type="file"
                  id="profilePicture"
                  accept="image/*"
                  className="hidden"
                  onChange={handleProfilePictureChange}
                />
              </div>

              {/* Profile Details Section */}
              <div className="flex-1">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-gray-600 font-medium">
                      First Name
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        name="firstname"
                        value={formData.firstname}
                        onChange={handleInputChange}
                        className="w-full p-2 border rounded-md"
                      />
                    ) : (
                      <p className="text-gray-800">{user.firstname}</p>
                    )}
                  </div>
                  <div>
                    <label className="block text-gray-600 font-medium">
                      Last Name
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        name="lastname"
                        value={formData.lastname}
                        onChange={handleInputChange}
                        className="w-full p-2 border rounded-md"
                      />
                    ) : (
                      <p className="text-gray-800">{user.lastname}</p>
                    )}
                  </div>
                  <div>
                    <label className="block text-gray-600 font-medium">
                      Email
                    </label>
                    {isEditing ? (
                      <input
                        type="email"
                        name="email"
                        value={formData.email}
                        onChange={handleInputChange}
                        className="w-full p-2 border rounded-md"
                      />
                    ) : (
                      <p className="text-gray-800">{user.email}</p>
                    )}
                  </div>
                  <div>
                    <label className="block text-gray-600 font-medium">
                      Currency
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        name="currency"
                        value={formData.currency}
                        onChange={handleInputChange}
                        className="w-full p-2 border rounded-md"
                      />
                    ) : (
                      <p className="text-gray-800">{user.currency}</p>
                    )}
                  </div>
                </div>
                <div className="mt-6 flex gap-4">
                  {isEditing ? (
                    <>
                      <button
                        onClick={handleSave}
                        className="bg-[#18864F] text-white px-4 py-2 rounded-md font-bold"
                      >
                        Save
                      </button>
                      <button
                        onClick={() => setIsEditing(false)}
                        className="bg-gray-300 text-gray-800 px-4 py-2 rounded-md font-bold"
                      >
                        Cancel
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => setIsEditing(true)}
                      className="bg-[#FFC107] text-[#18864F] px-4 py-2 rounded-md font-bold"
                    >
                      Edit Profile
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Profile;