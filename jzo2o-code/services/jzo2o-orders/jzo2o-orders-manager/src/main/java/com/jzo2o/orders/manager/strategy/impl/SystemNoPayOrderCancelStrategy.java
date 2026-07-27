package com.jzo2o.orders.manager.strategy.impl;

import cn.hutool.core.bean.BeanUtil;
import com.jzo2o.common.constants.UserType;
import com.jzo2o.orders.base.config.OrderStateMachine;
import com.jzo2o.orders.base.enums.OrderStatusChangeEventEnum;
import com.jzo2o.orders.base.model.domain.OrdersCanceled;
import com.jzo2o.orders.base.model.dto.OrderSnapshotDTO;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.service.IOrdersCanceledService;
import com.jzo2o.orders.manager.strategy.OrderCancelStrategy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 系统取消支付超时订单。
 */
@Component("0:NO_PAY")
public class SystemNoPayOrderCancelStrategy implements OrderCancelStrategy {

    @Resource
    private OrderStateMachine orderStateMachine;

    @Resource
    private IOrdersCanceledService ordersCanceledService;

    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
                .cancelReason(orderCancelDTO.getCancelReason())
                .cancellerType(UserType.SYSTEM)
                .cancelTime(LocalDateTime.now())
                .build();

        OrdersCanceled ordersCanceled = BeanUtil.toBean(orderSnapshotDTO, OrdersCanceled.class);
        ordersCanceled.setId(orderCancelDTO.getId());
        ordersCanceledService.save(ordersCanceled);

        orderStateMachine.changeStatus(orderCancelDTO.getUserId(),
                orderCancelDTO.getId().toString(), OrderStatusChangeEventEnum.CANCEL, orderSnapshotDTO);
    }
}
