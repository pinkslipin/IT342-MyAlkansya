import React, { createContext, useContext, useState, useEffect } from "react";
import axios from "axios";

const UserContext = createContext();

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [profileImage, setProfileImage] = useState(null);
  const [loading, setLoading] = useState(true);

  const defaultProfilePic = "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y";

  // Always fetch the latest token from localStorage
  const getToken = () => localStorage.getItem("authToken");

  useEffect(() => {
    const fetchUserInfo = async () => {
      const token = getToken();
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

    // Listen for token changes in localStorage (cross-tab)
    const handleStorage = (e) => {
      if (e.key === "authToken") {
        fetchUserInfo();
      }
    };
    window.addEventListener("storage", handleStorage);

    // Listen for navigation changes (in case of redirect after OAuth)
    const handlePopState = () => {
      fetchUserInfo();
    };
    window.addEventListener("popstate", handlePopState);

    // Patch setItem for in-tab token changes
    const origSetItem = localStorage.setItem;
    localStorage.setItem = function (key, value) {
      origSetItem.apply(this, arguments);
      if (key === "authToken") {
        fetchUserInfo();
      }
    };

    return () => {
      window.removeEventListener("storage", handleStorage);
      window.removeEventListener("popstate", handlePopState);
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