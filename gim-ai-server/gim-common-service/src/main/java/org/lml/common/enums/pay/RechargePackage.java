package org.lml.common.enums.pay;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Recharge package definitions.
 */
@Getter
@AllArgsConstructor
public enum RechargePackage {

    /**
     * 入门体验包：9.9元 / 100积分
     */
    ENTRY("PackageA", "入门体验包", new BigDecimal("9.9"), 100),

    /**
     * 初级进阶包：19.9元 / 250积分
     */
    JUNIOR("PackageB", "初级进阶包", new BigDecimal("19.9"), 250),

    /**
     * 中级超值包：99.9元 / 1500积分
     */
    MIDDLE("PackageC", "中级超值包", new BigDecimal("99.9"), 1500),

    /**
     * 高级尊享包：199.0元 / 3500积分
     */
    ADVANCED("PackageD", "高级尊享包", new BigDecimal("199.0"), 3500),

    /**
     * 专业至尊包：299.0元 / 6000积分
     */
    PRO("PackageE", "专业至尊包", new BigDecimal("299.0"), 6000),

    /**
     * 顶级王者包：499.0元 / 12000积分
     */
    ULTIMATE("PackageF", "顶级王者包", new BigDecimal("499.0"), 12000);

    /**
     * 套餐唯一编码
     */
    private final String code;

    /**
     * 套餐在前端显示的名称
     */
    private final String displayName;

    /**
     * 套餐价格 (人民币)
     */
    private final BigDecimal price;

    /**
     * 购买该套餐可获得的积分总额
     */
    private final int creditAmount;

    public BigDecimal getEfficiency() {
        return new BigDecimal(creditAmount).divide(price, 2, RoundingMode.HALF_UP);
    }

    public static RechargePackage fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported recharge package: " + code));
    }
}
