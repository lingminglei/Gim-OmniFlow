package org.lml.utils;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class PasswordUtils {

    // 使用 BCrypt 加密密码
    public static String encryptPassword(String password) {
        // 加密密码并返回加密后的密码
        String salt = BCrypt.gensalt(12);  // 12 是加密的强度
        return BCrypt.hashpw(password, salt);
    }

    // 校验密码
    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

    public static void main(String[] args) {
        String password = "mySecretPassword";
        String encryptedPassword = encryptPassword(password);
        System.out.println("Encrypted password: " + encryptedPassword);
        
        // 校验密码
        boolean isMatch = checkPassword(password, encryptedPassword);
        System.out.println("Password match: " + isMatch);
    }
}