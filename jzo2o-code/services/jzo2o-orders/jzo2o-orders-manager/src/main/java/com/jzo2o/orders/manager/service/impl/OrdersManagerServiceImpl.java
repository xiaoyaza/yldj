package com.jzo2o.orders.manager.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.customer.CouponApi;
import com.jzo2o.api.market.dto.request.CouponUseBackReqDTO;
import com.jzo2o.api.orders.dto.response.OrderResDTO;
import com.jzo2o.api.orders.dto.response.OrderSimpleResDTO;
import com.jzo2o.common.constants.UserType;
import com.jzo2o.common.enums.EnableStatusEnum;
import com.jzo2o.common.expcetions.CommonException;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.common.utils.ObjectUtils;
import com.jzo2o.orders.base.enums.OrderStatusEnum;
import com.jzo2o.orders.base.mapper.OrdersMapper;
import com.jzo2o.orders.base.model.domain.Orders;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.service.IOrdersManagerService;
import com.jzo2o.orders.manager.strategy.OrderCancelStrategyManager;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.jzo2o.orders.base.constants.FieldConstants.SORT_BY;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-07-10
 */
@Slf4j
@Service
public class OrdersManagerServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersManagerService {

    //注入自己
    @Resource
    private OrdersManagerServiceImpl owner;

    @Resource
    private CouponApi couponApi;

    @Resource
    private OrderCancelStrategyManager orderCancelStrategyManager;


    /**
     * 取消订单
     *
     * @param orderCancelDTO 取消订单模型
     */
    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        Long id = orderCancelDTO.getId();
        Orders orders = queryById(id);
        if (ObjectUtils.isNull(orders)) {
            throw new CommonException("订单不存在");
        }
        if (ObjectUtil.equal(UserType.C_USER, orderCancelDTO.getCurrentUserType())
                && ObjectUtil.notEqual(orders.getUserId(), orderCancelDTO.getCurrentUserId())) {
            throw new ForbiddenOperationException("非本人操作");
        }

        //优惠券属于下单用户，不能使用运营人员等取消操作者的id退券
        orderCancelDTO.setUserId(orders.getUserId());
        orderCancelDTO.setTradingOrderNo(orders.getTradingOrderNo());
        orderCancelDTO.setRealPayAmount(orders.getRealPayAmount());

        if (ObjectUtils.isNotNull(orders.getDiscountAmount())
                && orders.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            owner.cancelWithCoupon(orderCancelDTO);
        } else {
            owner.cancelWithoutCoupon(orderCancelDTO);
        }
    }

    /**
     * 隐藏订单
     *
     * @param id       订单id
     * @param userType 用户类型
     * @param userId      用户id
     */
    @Override
    public void hide(Long id, Integer userType, Long userId) {
        // 判断是否是管理员
//        if (userType == UserType.SYSTEM) {
//            throw new ForbiddenOperationException("无权限操作");
//        }
        // 校验是否是本人操作
        Orders orders = queryById(id);
        if (ObjectUtil.notEqual(userId, orders.getUserId())) {
            throw new ForbiddenOperationException("非本人不能操作");
        }

        // 校验订单状态 要为 待支付 或者 已取消 才能隐藏
        if (orders.getOrdersStatus() != OrderStatusEnum.NO_PAY.getStatus() && orders.getOrdersStatus() != OrderStatusEnum.CANCELED.getStatus()) {
            throw new CommonException("当前订单状态异常不能操作!");
        }

        // 更新订单状态 隐藏
        boolean update = lambdaUpdate()
                .eq(Orders::getId, id)
                .set(Orders::getDisplay, EnableStatusEnum.DISABLE.getStatus())
                .update();
    }

    @Override
    public Orders canalIfPayOvertime(Orders orders){
        // 订单到预期超时时间自动取消订单
        if (orders.getOrdersStatus() == OrderStatusEnum.NO_PAY.getStatus() && orders.getOverTime().isBefore(LocalDateTime.now())){
            //todo 查询最新支付状态，如果仍是未支付进行取消订单
            //取消订单
            OrderCancelDTO orderCancelDTO = new OrderCancelDTO();
            orderCancelDTO.setId(orders.getId());
            orderCancelDTO.setCurrentUserId(orders.getUserId());
            orderCancelDTO.setCurrentUserType(UserType.SYSTEM);
            orderCancelDTO.setCancelReason("订单超时支付，自动取消");
            cancel(orderCancelDTO);
            orders = getById(orders.getId());
        }
        return orders;
    }


    @GlobalTransactional
    public void cancelWithCoupon(OrderCancelDTO orderCancelDTO) {
        //退回优惠券
        CouponUseBackReqDTO couponUseBackReqDTO = new CouponUseBackReqDTO();
        couponUseBackReqDTO.setUserId(orderCancelDTO.getUserId());
        couponUseBackReqDTO.setOrdersId(orderCancelDTO.getId());
        couponApi.useBack(couponUseBackReqDTO);
        orderCancelStrategyManager.cancel(orderCancelDTO);
    }

    /**
     * 未使用优惠券的订单通过本地事务执行取消策略。
     *
     * @param orderCancelDTO 取消订单模型
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelWithoutCoupon(OrderCancelDTO orderCancelDTO) {
        orderCancelStrategyManager.cancel(orderCancelDTO);
    }



    @Override
    public List<Orders> batchQuery(List<Long> ids) {
        LambdaQueryWrapper<Orders> queryWrapper = Wrappers.<Orders>lambdaQuery().in(Orders::getId, ids).ge(Orders::getUserId, 0);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Orders queryById(Long id) {
        return baseMapper.selectById(id);
    }

    /**
     * 滚动分页查询
     *
     * @param currentUserId 当前用户id
     * @param ordersStatus  订单状态，0：待支付，100：派单中，200：待服务，300：服务中，400：待评价，500：订单完成，600：已取消，700：已关闭
     * @param sortBy        排序字段
     * @return 订单列表
     */
    @Override
    public List<OrderSimpleResDTO> consumerQueryList(Long currentUserId, Integer ordersStatus, Long sortBy) {
        //1.构件查询条件
        LambdaQueryWrapper<Orders> queryWrapper = Wrappers.<Orders>lambdaQuery()
                .eq(ObjectUtils.isNotNull(ordersStatus), Orders::getOrdersStatus, ordersStatus)
                .lt(ObjectUtils.isNotNull(sortBy), Orders::getSortBy, sortBy)
                .eq(Orders::getUserId, currentUserId)
                .eq(Orders::getDisplay, EnableStatusEnum.ENABLE.getStatus());
        Page<Orders> queryPage = new Page<>();
        queryPage.addOrder(OrderItem.desc(SORT_BY));
        queryPage.setSearchCount(false);

        //2.查询订单列表
        Page<Orders> ordersPage = baseMapper.selectPage(queryPage, queryWrapper);
        List<Orders> records = ordersPage.getRecords();
        List<OrderSimpleResDTO> orderSimpleResDTOS = BeanUtil.copyToList(records, OrderSimpleResDTO.class);
        return orderSimpleResDTOS;

    }
    /**
     * 根据订单id查询
     *
     * @param id 订单id
     * @return 订单详情
     */
    @Override
    public OrderResDTO getDetail(Long id) {
        Orders orders = queryById(id);
        OrderResDTO orderResDTO = BeanUtil.toBean(orders, OrderResDTO.class);
        return orderResDTO;
    }

    /**
     * 订单评价
     *
     * @param ordersId 订单id
     */
    @Override
    @Transactional
    public void evaluationOrder(Long ordersId) {
//        //查询订单详情
//        Orders orders = queryById(ordersId);
//
//        //构建订单快照
//        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
//                .evaluationTime(LocalDateTime.now())
//                .build();
//
//        //订单状态变更
//        orderStateMachine.changeStatus(orders.getUserId(), orders.getId().toString(), OrderStatusChangeEventEnum.EVALUATE, orderSnapshotDTO);
    }


}
