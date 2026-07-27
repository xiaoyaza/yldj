package com.jzo2o.orders.manager.strategy.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jzo2o.common.expcetions.CommonException;
import com.jzo2o.orders.base.enums.OrderStatusChangeEventEnum;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.strategy.OrderCancelStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 运营人员取消完成后15日内的订单。
 */
@Component("4:FINISHED")
public class OperationFinishedOrderCancelStrategy extends AbstractCloseOrderCancelStrategy
        implements OrderCancelStrategy {

    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        if (ObjectUtil.isNull(orderCancelDTO.getRealServeEndTime())) {
            throw new CommonException("订单缺少实际服务完成时间");
        }
        long days = LocalDateTimeUtil.between(orderCancelDTO.getRealServeEndTime(),
                LocalDateTime.now(), ChronoUnit.DAYS);
        if (days >= 15) {
            throw new CommonException("服务完成超过15日，不可退款");
        }
        closeOrder(orderCancelDTO, OrderStatusChangeEventEnum.CLOSE_FINISHED_ORDER, true, false);
    }
}
