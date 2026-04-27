package org.lml.config;

import org.springframework.stereotype.Component;

/**
 * 本地用户线程
 */
@Component
public class UserContext {
    /**
     * 存放用户Id
     */
    private static final ThreadLocal<String> userThreadLocal = new ThreadLocal<>();
    public static void setUserId(String userId) {
        userThreadLocal.set(userId);
    }
    public static String  getUserId() {
        return userThreadLocal.get();
    }
    public static void removeUser() {
        userThreadLocal.remove();
    }
}
