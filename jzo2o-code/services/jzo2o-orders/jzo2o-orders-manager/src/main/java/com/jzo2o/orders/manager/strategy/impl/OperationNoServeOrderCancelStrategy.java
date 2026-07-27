package com.jzo2o.orders.manager.strategy.impl;

import com.jzo2o.orders.base.enums.OrderStatusChangeEventEnum;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.strategy.OrderCancelStrategy;
import org.springframework.stereotype.Component;

/**
 * 运营人员取消待服务订单。
 */
@Component("4:NO_SERVE")
public class OperationNoServeOrderCancelStrategy extends AbstractCloseOrderCancelStrategy
        implements OrderCancelStrategy {

    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        closeOrder(orderCancelDTO, OrderStatusChangeEventEnum.CLOSE_NO_SERVE_ORDER, true, false);
    }
}
