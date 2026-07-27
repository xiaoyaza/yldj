package com.jzo2o.orders.manager.service;

/**
 * 抢单池、派单池清理服务。
 */
public interface ISeizeDispatchService {

    /**
     * 清理已取消订单的抢派单数据。
     *
     * @param cityCode 城市编码
     * @param ordersId 订单id
     */
    void clearSeizeDispatchPool(String cityCode, Long ordersId);
}
