package org.openjfx.BibleReadingTrackerFX;

import java.io.*;
import java.util.Properties;

public class UserManager {
    private static final String USER_DIR = "users/";

    public UserManager() {
        File dir = new File(USER_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return false;
        }

        File userFile = new File(USER_DIR + username + ".properties");
        if (userFile.exists()) {
            return false; // User already exists
        }

        Properties props = new Properties();
        props.setProperty("password", password);

        try (OutputStream out = new FileOutputStream(userFile)) {
            props.store(out, "User: " + username);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean validateUser(String username, String password) {
        File userFile = new File(USER_DIR + username + ".properties");
        if (!userFile.exists()) {
            return false;
        }

        Properties props = new Properties();
        try (InputStream in = new FileInputStream(userFile)) {
            props.load(in);
            String storedPassword = props.getProperty("password");
            return storedPassword != null && storedPassword.equals(password);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean resetPassword(String username, String newPassword) {
        File userFile = new File(USER_DIR + username + ".properties");
        if (!userFile.exists()) return false;

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(userFile)) {
            props.load(in);
            props.setProperty("password", newPassword);
            
            try (FileOutputStream out = new FileOutputStream(userFile)) {
                props.store(out, "User: " + username);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}