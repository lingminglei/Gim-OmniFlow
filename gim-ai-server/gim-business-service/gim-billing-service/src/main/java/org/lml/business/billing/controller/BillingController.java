package org.lml.business.billing.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 积分服务健康检查接口。
 */
@RestController
@RequestMapping("/billing")
public class BillingController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Collections.singletonMap("status", "UP");
    }
}
