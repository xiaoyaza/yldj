package com.jzo2o.orders.manager.strategy.impl;

import com.jzo2o.orders.base.enums.OrderStatusChangeEventEnum;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.strategy.OrderCancelStrategy;
import org.springframework.stereotype.Component;

/**
 * 运营人员取消服务中订单。
 */
@Component("4:SERVING")
public class OperationServingOrderCancelStrategy extends AbstractCloseOrderCancelStrategy
        implements OrderCancelStrategy {

    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        closeOrder(orderCancelDTO, OrderStatusChangeEventEnum.CLOSE_SERVING_ORDER, true, false);
    }
}
