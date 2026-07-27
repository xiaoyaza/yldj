package com.jzo2o.orders.manager.strategy.impl;

import com.jzo2o.orders.base.enums.OrderStatusChangeEventEnum;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.strategy.OrderCancelStrategy;
import org.springframework.stereotype.Component;

/**
 * 运营人员取消派单中订单。
 */
@Component("4:DISPATCHING")
public class OperationDispatchingOrderCancelStrategy extends AbstractCloseOrderCancelStrategy
        implements OrderCancelStrategy {

    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        closeOrder(orderCancelDTO, OrderStatusChangeEventEnum.CLOSE_DISPATCHING_ORDER, false, true);
    }
}
