package com.jzo2o.orders.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.api.market.dto.response.AvailableCouponsResDTO;
import com.jzo2o.orders.base.model.domain.Orders;
import com.jzo2o.orders.manager.model.dto.request.PlaceOrderReqDTO;
import com.jzo2o.orders.manager.model.dto.response.PlaceOrderResDTO;

import java.util.List;

/**
 * <p>
 * 下单服务类
 * </p>
 *
 * @author itcast
 * @since 2023-07-10
 */
public interface IOrdersCreateService extends IService<Orders> {


    /**
     * 下单方法
     */
    PlaceOrderResDTO placeOrder(PlaceOrderReqDTO placeOrderReqDTO);

    void addWithCoupon(Orders orders, Long couponId);

    void add(Orders orders);

    /**
     * 获取可用优惠券列表
     */
    List<AvailableCouponsResDTO> getAvailableCoupons(Long serveId, Integer purNum);

    /**
     * 查询超时订单列表
     */
    List<Orders> queryOverTimePayOrdersListByCount(Integer cc);
}
