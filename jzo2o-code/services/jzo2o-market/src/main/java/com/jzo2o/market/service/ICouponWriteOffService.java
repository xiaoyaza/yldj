package com.jzo2o.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.market.model.domain.CouponWriteOff;

/**
 * <p>
 * 优惠券核销表 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-09-22
 */
public interface ICouponWriteOffService extends IService<CouponWriteOff> {

    /**
     * 根据用户id和订单id查询核销记录
     *
     * @param userId   用户id
     * @param ordersId 订单id
     * @return 核销记录
     */
    CouponWriteOff queryByUserIdIdAndOrdersId(Long userId, Long ordersId);

}
