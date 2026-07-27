package com.jzo2o.orders.manager.strategy.impl;

import cn.hutool.core.bean.BeanUtil;
import com.jzo2o.orders.base.config.OrderStateMachine;
import com.jzo2o.orders.base.enums.OrderRefundStatusEnum;
import com.jzo2o.orders.base.enums.OrderStatusChangeEventEnum;
import com.jzo2o.orders.base.model.domain.OrdersCanceled;
import com.jzo2o.orders.base.model.domain.OrdersRefund;
import com.jzo2o.orders.base.model.dto.OrderSnapshotDTO;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.service.IOrdersCanceledService;
import com.jzo2o.orders.manager.service.IOrdersRefundService;
import com.jzo2o.orders.manager.service.IOrdersServeManagerService;
import com.jzo2o.orders.manager.service.ISeizeDispatchService;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 关闭已支付订单策略的公共处理逻辑。
 */
abstract class AbstractCloseOrderCancelStrategy {

    @Resource
    private OrderStateMachine orderStateMachine;

    @Resource
    private IOrdersCanceledService ordersCanceledService;

    @Resource
    private IOrdersRefundService ordersRefundService;

    @Resource
    private IOrdersServeManagerService ordersServeManagerService;

    @Resource
    private ISeizeDispatchService seizeDispatchService;

    protected void closeOrder(OrderCancelDTO orderCancelDTO,
                              OrderStatusChangeEventEnum event,
                              boolean cancelServe,
                              boolean clearSeizeDispatchPool) {
        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
                .refundStatus(OrderRefundStatusEnum.REFUNDING.getStatus())
                .cancellerId(orderCancelDTO.getCurrentUserId())
                .cancelerName(orderCancelDTO.getCurrentUserName())
                .cancellerType(orderCancelDTO.getCurrentUserType())
                .cancelReason(orderCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        OrdersCanceled ordersCanceled = BeanUtil.toBean(orderSnapshotDTO, OrdersCanceled.class);
        ordersCanceled.setId(orderCancelDTO.getId());
        ordersCanceledService.save(ordersCanceled);

        orderStateMachine.changeStatus(orderCancelDTO.getUserId(),
                orderCancelDTO.getId().toString(), event, orderSnapshotDTO);

        if (cancelServe) {
            ordersServeManagerService.cancelByUserAndOperation(orderCancelDTO.getId());
        }

        OrdersRefund ordersRefund = new OrdersRefund();
        ordersRefund.setId(orderCancelDTO.getId());
        ordersRefund.setTradingOrderNo(orderCancelDTO.getTradingOrderNo());
        ordersRefund.setRealPayAmount(orderCancelDTO.getRealPayAmount());
        ordersRefundService.save(ordersRefund);

        if (clearSeizeDispatchPool) {
            seizeDispatchService.clearSeizeDispatchPool(orderCancelDTO.getCityCode(), orderCancelDTO.getId());
        }
    }
}
