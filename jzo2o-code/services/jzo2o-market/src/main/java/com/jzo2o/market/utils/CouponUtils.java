package com.jzo2o.market.utils;

import com.jzo2o.market.model.domain.Coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 优惠券相关工具
 */
public class CouponUtils {

    /**
     * 优惠券类型：满减
     */
    public static final int TYPE_AMOUNT_DISCOUNT = 1;

    /**
     * 优惠券类型：折扣
     */
    public static final int TYPE_RATE_DISCOUNT = 2;

    /**
     * 计算优惠券的优惠金额
     *
     * @param coupon     优惠券
     * @param totalAmount 订单总金额（单位：分）
     * @return 优惠金额（单位：分）
     */
    public static BigDecimal calDiscountAmount(Coupon coupon, BigDecimal totalAmount) {
        if (coupon == null || totalAmount == null) {
            return BigDecimal.ZERO;
        }
        Integer type = coupon.getType();
        if (type == null) {
            return BigDecimal.ZERO;
        }
        // 满减券：直接返回配置的优惠金额
        if (TYPE_AMOUNT_DISCOUNT == type) {
            return coupon.getDiscountAmount() == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        }
        // 折扣券：totalAmount * (100 - discountRate) / 100
        if (TYPE_RATE_DISCOUNT == type && coupon.getDiscountRate() != null) {
            return totalAmount.multiply(BigDecimal.valueOf(100 - coupon.getDiscountRate()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
