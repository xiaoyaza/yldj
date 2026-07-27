package com.jzo2o.orders.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.api.market.dto.response.AvailableCouponsResDTO;
import com.jzo2o.common.model.msg.TradeStatusMsg;
import com.jzo2o.orders.base.model.domain.Orders;
import com.jzo2o.orders.manager.model.dto.request.OrdersPayReqDTO;
import com.jzo2o.orders.manager.model.dto.request.PlaceOrderReqDTO;
import com.jzo2o.orders.manager.model.dto.response.OrdersPayResDTO;
import com.jzo2o.orders.manager.model.dto.response.PlaceOrderResDTO;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(rollbackFor = Exception.class)
    void paySuccess(TradeStatusMsg tradeStatusMsg);

    /**
     * 订单支付
     *
     * @param id              订单id
     * @param ordersPayReqDTO 订单支付请求体
     * @return 订单支付响应体
     */
    OrdersPayResDTO pay(Long id, OrdersPayReqDTO ordersPayReqDTO);

    /**
     * 获取订单支付结果
     *
     * @param id 订单id
     * @return 订单支付结果
     */
    OrdersPayResDTO getPayResultFromTradServer(Long id);
}
