import React, { createContext, useContext, useState, useEffect } from "react";
import axios from "axios";

const UserContext = createContext();

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [profileImage, setProfileImage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(() => localStorage.getItem("authToken"));

  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  useEffect(() => {
    const fetchUserInfo = async () => {
      if (!token) {
        setLoading(false);
        setUser(null);
        setProfileImage(defaultProfilePic);
        return;
      }

      try {
        const response = await axios.get("https://myalkansya-sia.as.r.appspot.com/api/users/me", {
          headers: { Authorization: `Bearer ${token}` },
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
  }, [token]);

  // Listen for token changes in localStorage (cross-tab)
  useEffect(() => {
    const handleStorage = (e) => {
      if (e.key === "authToken") {
        setToken(e.newValue);
      }
    };
    window.addEventListener("storage", handleStorage);
    return () => window.removeEventListener("storage", handleStorage);
  }, []);

  // Also update token state when it changes in this tab (after login)
  useEffect(() => {
    const origSetItem = localStorage.setItem;
    localStorage.setItem = function (key, value) {
      origSetItem.apply(this, arguments);
      if (key === "authToken") {
        setToken(value);
      }
    };
    return () => {
      localStorage.setItem = origSetItem;
    };
  }, []);

  return (
    <UserContext.Provider value={{ user, profileImage, loading }}>
      {children}
    </UserContext.Provider>
  );
};

export const useUser = () => useContext(UserContext);