package com.jzo2o.orders.manager.strategy.impl;

import com.jzo2o.orders.base.enums.OrderStatusChangeEventEnum;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.strategy.OrderCancelStrategy;
import org.springframework.stereotype.Component;

/**
 * 系统取消派单中订单。
 */
@Component("0:DISPATCHING")
public class SystemDispatchingOrderCancelStrategy extends AbstractCloseOrderCancelStrategy
        implements OrderCancelStrategy {

    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        closeOrder(orderCancelDTO, OrderStatusChangeEventEnum.CLOSE_DISPATCHING_ORDER, false, true);
    }
}
