package com.jzo2o.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.api.market.dto.request.CouponUseBackReqDTO;
import com.jzo2o.api.market.dto.request.CouponUseReqDTO;
import com.jzo2o.api.market.dto.response.AvailableCouponsResDTO;
import com.jzo2o.api.market.dto.response.CouponUseResDTO;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.market.model.domain.Coupon;
import com.jzo2o.market.model.dto.request.CouponLimitReqDTO;
import com.jzo2o.market.model.dto.request.CouponOperationPageQueryReqDTO;
import com.jzo2o.market.model.dto.response.CouponInfoResDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author itcast
 * @since 2023-09-16
 */
public interface ICouponService extends IService<Coupon> {


    /**
     * 分页查询领取列表
     */
    PageResult<CouponInfoResDTO> pageList(CouponOperationPageQueryReqDTO couponOperationPageQueryReqDTO);

    /**
     * 优惠券失效
     */
    void expireCoupon(Long id);

    /**
     * 我的优惠券列表
     */
    List<CouponInfoResDTO> myCoupons(CouponLimitReqDTO couponLimitReqDTO);

    /**
     * 用户优惠券失效
     */
    void userExprireCoupon();

    /**
     * 获取可用优惠券列表，并按优惠金额从大到小排序
     * @param userId      用户id
     * @param totalAmount 订单总金额（单位：分）
     * @return 可用优惠券列表
     */
    List<AvailableCouponsResDTO> getAvailable(Long userId, BigDecimal totalAmount);

    /**
     * 使用优惠券
     * @param couponUseReqDTO
     */
    CouponUseResDTO use(CouponUseReqDTO couponUseReqDTO);

    /**
     * 优惠券回退
     * @param couponUseBackReqDTO
     */
    void useBack(CouponUseBackReqDTO couponUseBackReqDTO);
}
