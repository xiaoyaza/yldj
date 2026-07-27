package com.jzo2o.orders.manager.strategy.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.DbRuntimeException;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.orders.base.enums.OrderStatusEnum;
import com.jzo2o.orders.base.model.domain.OrdersCanceled;
import com.jzo2o.orders.base.model.dto.OrderUpdateStatusDTO;
import com.jzo2o.orders.base.service.IOrdersCommonService;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.service.IOrdersCanceledService;
import com.jzo2o.orders.manager.strategy.OrderCancelStrategy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 普通用户取消待支付订单。
 */
@Component("1:NO_PAY")
public class CommonUserNoPayOrderCancelStrategy implements OrderCancelStrategy {

    @Resource
    private IOrdersCanceledService ordersCanceledService;

    @Resource
    private IOrdersCommonService ordersCommonService;

    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        if (ObjectUtil.notEqual(orderCancelDTO.getUserId(), orderCancelDTO.getCurrentUserId())) {
            throw new ForbiddenOperationException("非本人操作");
        }

        OrdersCanceled ordersCanceled = BeanUtil.toBean(orderCancelDTO, OrdersCanceled.class);
        ordersCanceled.setCancellerId(orderCancelDTO.getCurrentUserId());
        ordersCanceled.setCancelerName(orderCancelDTO.getCurrentUserName());
        ordersCanceled.setCancellerType(orderCancelDTO.getCurrentUserType());
        ordersCanceled.setCancelTime(LocalDateTime.now());
        ordersCanceledService.save(ordersCanceled);

        OrderUpdateStatusDTO orderUpdateStatusDTO = OrderUpdateStatusDTO.builder()
                .id(orderCancelDTO.getId())
                .originStatus(OrderStatusEnum.NO_PAY.getStatus())
                .targetStatus(OrderStatusEnum.CANCELED.getStatus())
                .build();
        int result = ordersCommonService.updateStatus(orderUpdateStatusDTO);
        if (result <= 0) {
            throw new DbRuntimeException("订单取消事件处理失败");
        }
    }
}
