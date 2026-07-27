package com.jzo2o.orders.manager.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jzo2o.api.trade.RefundRecordApi;
import com.jzo2o.api.trade.dto.response.ExecutionResultResDTO;
import com.jzo2o.api.trade.enums.RefundStatusEnum;
import com.jzo2o.common.constants.UserType;
import com.jzo2o.orders.base.enums.OrderRefundStatusEnum;
import com.jzo2o.orders.base.mapper.OrdersMapper;
import com.jzo2o.orders.base.model.domain.Orders;
import com.jzo2o.orders.base.model.domain.OrdersRefund;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.service.IOrdersCreateService;
import com.jzo2o.orders.manager.service.IOrdersManagerService;
import com.jzo2o.orders.manager.service.IOrdersRefundService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class OrdersHandler {
    
    //注入ordersCreateService
    @Resource
    private IOrdersCreateService ordersCreateService;
    
    //注入ordersManagerService
    @Resource
    private IOrdersManagerService ordersManagerService;

    @Resource
    private RefundRecordApi refundRecordApi;

    //通过Spring代理调用同类事务方法
    @Resource
    private OrdersHandler ordersHandler;

    @Resource
    private IOrdersRefundService ordersRefundService;

    @Resource
    private OrdersMapper ordersMapper;
    
    /**
     * 支付超时取消订单
     * 每分钟执行一次
     */
    @XxlJob(value = "cancelOverTimePayOrder")
    public void cancelOverTimePayOrder() {
    
        //查询支付超时状态订单
        List<Orders> ordersList = ordersCreateService.queryOverTimePayOrdersListByCount(100);
        if (CollUtil.isEmpty(ordersList)) {
            XxlJobHelper.log("查询超时订单列表为空！");
            return;
        }
    
        for (Orders orders : ordersList) {
            //取消订单
            OrderCancelDTO orderCancelDTO = BeanUtil.toBean(orders, OrderCancelDTO.class);
            orderCancelDTO.setCurrentUserType(UserType.SYSTEM);
            orderCancelDTO.setCancelReason("订单超时支付，自动取消");
            ordersManagerService.cancel(orderCancelDTO);
        }
    }

    /**
     * 订单退款异步任务。
     */
    @XxlJob(value = "handleRefundOrders")
    public void handleRefundOrders() {
        List<OrdersRefund> ordersRefundList = ordersRefundService.queryRefundOrderListByCount(100);
        if (CollUtil.isEmpty(ordersRefundList)) {
            XxlJobHelper.log("待退款订单列表为空");
            return;
        }

        for (OrdersRefund ordersRefund : ordersRefundList) {
            try {
                ExecutionResultResDTO executionResult = refundRecordApi.refundTrading(
                        ordersRefund.getTradingOrderNo(),
                        ordersRefund.getId(),
                        ordersRefund.getRealPayAmount());
                if (ObjectUtil.isNotNull(executionResult)) {
                    ordersHandler.refundOrder(ordersRefund, executionResult);
                }
            } catch (Exception e) {
                log.error("订单退款请求失败，ordersId={}", ordersRefund.getId(), e);
            }
        }
    }

    /**
     * 根据支付服务响应更新订单退款状态。
     *
     * @param ordersRefund 退款任务
     * @param executionResult 支付服务退款结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(OrdersRefund ordersRefund, ExecutionResultResDTO executionResult) {
        int refundStatus = OrderRefundStatusEnum.REFUNDING.getStatus();
        if (ObjectUtil.equal(RefundStatusEnum.SUCCESS.getCode(), executionResult.getRefundStatus())) {
            refundStatus = OrderRefundStatusEnum.REFUND_SUCCESS.getStatus();
        } else if (ObjectUtil.equal(RefundStatusEnum.FAIL.getCode(), executionResult.getRefundStatus())) {
            refundStatus = OrderRefundStatusEnum.REFUND_FAIL.getStatus();
        }

        //支付服务仍在退款中时保留任务，等待下一次扫描
        if (ObjectUtil.equal(refundStatus, OrderRefundStatusEnum.REFUNDING.getStatus())) {
            return;
        }

        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, ordersRefund.getId())
                .eq(Orders::getRefundStatus, OrderRefundStatusEnum.REFUNDING.getStatus())
                .set(Orders::getRefundStatus, refundStatus)
                .set(ObjectUtil.isNotEmpty(executionResult.getRefundId()),
                        Orders::getRefundId, executionResult.getRefundId())
                .set(ObjectUtil.isNotEmpty(executionResult.getRefundNo()),
                        Orders::getRefundNo, executionResult.getRefundNo());
        int updated = ordersMapper.update(null, updateWrapper);
        if (updated > 0) {
            //退款结束后删除任务，避免定时任务重复扫描
            ordersRefundService.removeById(ordersRefund.getId());
        }
    }
    
}
