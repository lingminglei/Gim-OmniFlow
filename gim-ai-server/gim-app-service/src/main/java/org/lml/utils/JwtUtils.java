package org.lml.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
public class JwtUtils {

    private String secret;

    private long expire;

    private String header;

    private long checkRefreshExpire;

    private final static String pre = "sxuasnqwako=as0xasasqxscadaeas";
    /**
     * 生成jwt token
     */
    public static String generateToken(String userId) {

        Map<String, Object> map = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("userId", userId);

                put("expire_time", System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7);

            }
        };

        String token  = JWTUtil.createToken(map, pre.getBytes());

        return token;
    }

    public void cleanToken(String uid) {

    }

    public boolean hasToken(String uid) {
       return true;
    }

    /**
     * token是否过期
     *
     * @return true：过期
     */
    public boolean isTokenExpired(String token) {

        return JWTUtil.verify(token, pre.getBytes());

    }

    /**
     * 解析token
     */
    public String parseToken(String token) {
        // 验证 Token 是否有效
        boolean isValid = JWTUtil.verify(token, pre.getBytes());

        if (!isValid) {
            throw new RuntimeException("Invalid token");
        }

        // 解析 Token
        JWT jwt = JWTUtil.parseToken(token);

        // 获取 payload 中的数据
        String userId = jwt.getPayload("userId").toString();

        return userId;
    }


}
