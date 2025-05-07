import React, { createContext, useContext, useState, useEffect } from "react";
import axios from "axios";

const UserContext = createContext();

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [profileImage, setProfileImage] = useState(null);
  const [loading, setLoading] = useState(true);

  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  useEffect(() => {
    const fetchUserInfo = async () => {
      const authToken = localStorage.getItem("authToken");
      if (!authToken) {
        setLoading(false);
        return;
      }

      try {
        const response = await axios.get("https://myalkansya-sia.as.r.appspot.com/api/users/me", {
          headers: { Authorization: `Bearer ${authToken}` },
        });
        setUser(response.data);

        if (response.data.profilePicture) {
          if (response.data.profilePicture.startsWith('data:')) {
            setProfileImage(response.data.profilePicture);
          } else if (response.data.profilePicture.startsWith('http')) {
            setProfileImage(response.data.profilePicture);
          } else {
            const baseUrl = "https://myalkansya-sia.as.r.appspot.com";
            const path = response.data.profilePicture.startsWith('/')
              ? response.data.profilePicture
              : `/${response.data.profilePicture}`;
            setProfileImage(`${baseUrl}${path}?t=${new Date().getTime()}`);
          }
        } else {
          setProfileImage(defaultProfilePic);
        }
      } catch (err) {
        setUser(null);
        setProfileImage(defaultProfilePic);
      } finally {
        setLoading(false);
      }
    };

    fetchUserInfo();

    // Listen for token changes (e.g., after login)
    window.addEventListener("storage", fetchUserInfo);
    return () => window.removeEventListener("storage", fetchUserInfo);
  }, [localStorage.getItem("authToken")]);

  return (
    <UserContext.Provider value={{ user, profileImage, loading }}>
      {children}
    </UserContext.Provider>
  );
};

export const useUser = () => useContext(UserContext);