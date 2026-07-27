package com.jzo2o.orders.manager.strategy.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.orders.base.enums.OrderStatusChangeEventEnum;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.strategy.OrderCancelStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 普通用户取消待服务订单。
 */
@Component("1:NO_SERVE")
public class CommonUserNoServeOrderCancelStrategy extends AbstractCloseOrderCancelStrategy
        implements OrderCancelStrategy {

    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        if (ObjectUtil.notEqual(orderCancelDTO.getUserId(), orderCancelDTO.getCurrentUserId())) {
            throw new ForbiddenOperationException("非本人操作");
        }
        long minutes = LocalDateTimeUtil.between(LocalDateTime.now(),
                orderCancelDTO.getServeStartTime(), ChronoUnit.MINUTES);
        if (minutes < 120) {
            throw new ForbiddenOperationException("离预约时间不足120分钟，请联系客服取消");
        }
        closeOrder(orderCancelDTO, OrderStatusChangeEventEnum.CLOSE_NO_SERVE_ORDER, true, false);
    }
}
