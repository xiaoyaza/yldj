package com.jzo2o.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.market.model.domain.CouponUseBack;

import java.time.LocalDateTime;

/**
 * <p>
 * 优惠券使用回退记录 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-09-18
 */
public interface ICouponUseBackService extends IService<CouponUseBack> {

    /**
     * 新增回退记录
     *
     * @param couponId     优惠券id
     * @param userId       用户id
     * @param writeOffTime 核销时间
     */
    void add(Long couponId, Long userId, LocalDateTime writeOffTime);

}
