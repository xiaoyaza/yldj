package com.jzo2o.orders.manager.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.DbRuntimeException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.customer.AddressBookApi;
import com.jzo2o.api.customer.CouponApi;
import com.jzo2o.api.customer.dto.response.AddressBookResDTO;
import com.jzo2o.api.foundations.ServeApi;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.api.market.dto.request.CouponUseReqDTO;
import com.jzo2o.api.market.dto.response.AvailableCouponsResDTO;
import com.jzo2o.api.market.dto.response.CouponUseResDTO;
import com.jzo2o.api.trade.NativePayApi;
import com.jzo2o.api.trade.TradingApi;
import com.jzo2o.api.trade.dto.request.NativePayReqDTO;
import com.jzo2o.api.trade.dto.response.NativePayResDTO;
import com.jzo2o.api.trade.dto.response.TradingResDTO;
import com.jzo2o.api.trade.enums.PayChannelEnum;
import com.jzo2o.api.trade.enums.TradingStateEnum;
import com.jzo2o.common.expcetions.BadRequestException;
import com.jzo2o.common.expcetions.CommonException;
import com.jzo2o.common.model.msg.TradeStatusMsg;
import com.jzo2o.common.utils.*;
import com.jzo2o.orders.base.enums.OrderPayStatusEnum;
import com.jzo2o.orders.base.enums.OrderStatusEnum;
import com.jzo2o.orders.base.mapper.OrdersMapper;
import com.jzo2o.orders.base.model.domain.Orders;
import com.jzo2o.orders.manager.model.dto.request.OrdersPayReqDTO;
import com.jzo2o.orders.manager.model.dto.request.PlaceOrderReqDTO;
import com.jzo2o.orders.manager.model.dto.response.OrdersPayResDTO;
import com.jzo2o.orders.manager.model.dto.response.PlaceOrderResDTO;
import com.jzo2o.orders.manager.porperties.TradeProperties;
import com.jzo2o.orders.manager.service.IOrdersCreateService;
import com.jzo2o.redis.annotations.Lock;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.jzo2o.common.constants.ErrorInfo.Code.TRADE_FAILED;
import static com.jzo2o.orders.base.constants.RedisConstants.Lock.ORDERS_SHARD_KEY_ID_GENERATOR;

/**
 * <p>
 * 下单服务类
 * </p>
 *
 * @author itcast
 * @since 2023-07-10
 */
@Slf4j
@Service
public class OrdersCreateServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersCreateService {

    @Resource
    private RedisTemplate<String, Long> redisTemplate;

    @Resource
    private AddressBookApi addressBookApi;

    @Resource
    private ServeApi serveApi;

    @Resource
    private OrdersCreateServiceImpl owner;

    @Resource
    private CouponApi couponApi;

    @Resource
    private TradeProperties tradeProperties;

    @Resource
    private NativePayApi nativePayApi;

    @Resource
    private TradingApi tradingApi;


    /**
     * 获取订单支付结果
     *
     * @param id 订单id
     * @return 订单支付结果
     */
    @Override
    public OrdersPayResDTO getPayResultFromTradServer(Long id) {
        //查询订单表
        Orders orders = baseMapper.selectById(id);
        if (ObjectUtil.isNull(orders)) {
            throw new CommonException(TRADE_FAILED, "订单不存在");
        }
        //支付结果
        Integer payStatus = orders.getPayStatus();
        //未支付且已存在支付服务的交易单号此时远程调用支付服务查询支付结果
        if (OrderPayStatusEnum.NO_PAY.getStatus() == payStatus
                && ObjectUtil.isNotEmpty(orders.getTradingOrderNo())) {
            //远程调用支付服务查询支付结果
            TradingResDTO tradingResDTO = tradingApi.findTradResultByTradingOrderNo(orders.getTradingOrderNo());
            //如果支付成功这里更新订单状态
            if (ObjectUtil.isNotNull(tradingResDTO)
                    && ObjectUtil.equals(tradingResDTO.getTradingState(), TradingStateEnum.YJS)) {
                //设置订单的支付状态成功
                TradeStatusMsg msg = TradeStatusMsg.builder()
                        .productOrderNo(String.valueOf(orders.getId()))
                        .tradingChannel(tradingResDTO.getTradingChannel())
                        .statusCode(TradingStateEnum.YJS.getCode())
                        .tradingOrderNo(tradingResDTO.getTradingOrderNo())
                        .transactionId(tradingResDTO.getTransactionId())
                        .build();
                owner.paySuccess(msg);
                //构造返回数据
                OrdersPayResDTO ordersPayResDTO = BeanUtils.toBean(msg , OrdersPayResDTO.class);
                ordersPayResDTO.setPayStatus(OrderPayStatusEnum.PAY_SUCCESS.getStatus());
                return ordersPayResDTO;
            }
        }
        OrdersPayResDTO ordersPayResDTO = new OrdersPayResDTO();
        ordersPayResDTO.setPayStatus(payStatus);
        ordersPayResDTO.setProductOrderNo(orders.getId());
        ordersPayResDTO.setTradingOrderNo(orders.getTradingOrderNo());
        ordersPayResDTO.setTradingChannel(orders.getTradingChannel());
        return ordersPayResDTO;
    }

    /**
     * 支付成功， 其他信息暂且不填
     *
     * @param tradeStatusMsg 交易状态消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(TradeStatusMsg tradeStatusMsg) {
        //查询订单
        Orders orders = baseMapper.selectById(Long.parseLong(tradeStatusMsg.getProductOrderNo()));
        if (ObjectUtil.isNull(orders)) {
            throw new CommonException(TRADE_FAILED, "订单不存在");
        }
        //校验支付状态如果不是待支付状态则不作处理
        if (ObjectUtil.notEqual(OrderPayStatusEnum.NO_PAY.getStatus(), orders.getPayStatus())) {
            log.info("更新订单支付成功，当前订单:{}支付状态不是待支付状态", orders.getId());
            return;
        }
        //校验订单状态如果不是待支付状态则不作处理
        if (ObjectUtils.notEqual(OrderStatusEnum.NO_PAY.getStatus(),orders.getOrdersStatus())) {
            log.info("更新订单支付成功，当前订单:{}状态不是待支付状态", orders.getId());
            return;
        }

        //第三方支付单号校验
        if (ObjectUtil.isEmpty(tradeStatusMsg.getTransactionId())) {
            log.error("支付成功事件处理失败，缺少第三方支付单号，订单号：{}", orders.getId());
            return;
        }
        //更新订单的支付状态及第三方交易单号等信息
        boolean update = lambdaUpdate()
                .eq(Orders::getId, orders.getId())//订单号
                .eq(Orders::getPayStatus, OrderPayStatusEnum.NO_PAY.getStatus())//原支付状态
                .eq(Orders::getOrdersStatus, OrderStatusEnum.NO_PAY.getStatus())//原订单状态
                .set(Orders::getPayTime, LocalDateTime.now())//支付时间
                .set(Orders::getTradingOrderNo, tradeStatusMsg.getTradingOrderNo())//交易单号
                .set(Orders::getTradingChannel, tradeStatusMsg.getTradingChannel())//支付渠道
                .set(Orders::getTransactionId, tradeStatusMsg.getTransactionId())//第三方支付交易号
                .set(Orders::getPayStatus, OrderPayStatusEnum.PAY_SUCCESS.getStatus())//支付状态
                .set(Orders::getOrdersStatus, OrderStatusEnum.DISPATCHING.getStatus())//订单状态更新为派单中
                .update();
        if(!update){
            log.info("更新订单:{}支付成功失败", orders.getId());
            throw new CommonException("更新订单"+orders.getId()+"支付成功失败");
        }


    }


    /**
     * 订单支付
     *
     * @param id              订单id
     * @param ordersPayReqDTO 订单支付请求体
     * @return 订单支付响应体
     */
    @Override
    public OrdersPayResDTO pay(Long id, OrdersPayReqDTO ordersPayReqDTO) {
        // 查看该订单是否存在
        Orders orders = baseMapper.selectById(id);
        if (ObjectUtils.isEmpty(orders)) {
            throw new BadRequestException("订单不存在");
        }

        // 订单状态为成功直接返回
        if (orders.getOrdersStatus() == OrderPayStatusEnum.PAY_SUCCESS.getStatus() && ObjectUtils.isNotEmpty(orders.getTradingOrderNo())) {
            OrdersPayResDTO ordersPayResDTO = new OrdersPayResDTO();
            ordersPayResDTO.setPayStatus(orders.getPayStatus());
            ordersPayResDTO.setTradingOrderNo(orders.getTradingOrderNo());
            ordersPayResDTO.setTradingChannel(orders.getTradingChannel());
            ordersPayResDTO.setProductOrderNo(orders.getId());
            return ordersPayResDTO;
        } else {
            //生成二维码
            NativePayResDTO nativePayResDTO = generateQrCode(orders, ordersPayReqDTO.getTradingChannel());
            OrdersPayResDTO ordersPayResDTO = BeanUtil.toBean(nativePayResDTO, OrdersPayResDTO.class);
            return ordersPayResDTO;
        }
    }


    //生成二维码
    private NativePayResDTO generateQrCode(Orders orders, PayChannelEnum tradingChannel) {
        //判断支付渠道
        Long enterpriseId = ObjectUtil.equal(PayChannelEnum.ALI_PAY, tradingChannel) ?
                tradeProperties.getAliEnterpriseId() : tradeProperties.getWechatEnterpriseId();

        //构建支付请求参数
        NativePayReqDTO nativePayReqDTO = new NativePayReqDTO();
        //商户号
        nativePayReqDTO.setEnterpriseId(enterpriseId);
        //业务系统标识
        nativePayReqDTO.setProductAppId("jzo2o.orders");
        //家政订单号
        nativePayReqDTO.setProductOrderNo(orders.getId());
        //支付渠道
        nativePayReqDTO.setTradingChannel(tradingChannel);
        //支付金额
        nativePayReqDTO.setTradingAmount(orders.getRealPayAmount());
        //备注信息
        nativePayReqDTO.setMemo(orders.getServeItemName());
        //判断是否切换支付渠道
        if (ObjectUtil.isNotEmpty(orders.getTradingChannel())
                && ObjectUtil.notEqual(orders.getTradingChannel(), tradingChannel.toString())) {
            nativePayReqDTO.setChangeChannel(true);
        }

        // 生成支付二维码
        NativePayResDTO downLineTrading = nativePayApi.createDownLineTrading(nativePayReqDTO);
        if (ObjectUtils.isNotNull(downLineTrading)) {
            log.info("订单:{}请求支付,生成二维码:{}", orders.getId(), downLineTrading.toString());
            // 将二维码更新到交易订单中
            boolean update = lambdaUpdate()
                    .eq(Orders::getId, downLineTrading.getProductOrderNo())
                    .set(Orders::getTradingOrderNo, downLineTrading.getTradingOrderNo())
                    .set(Orders::getTradingChannel, downLineTrading.getTradingChannel())
                    .update();
            if (!update) {
                throw new CommonException("订单:" + orders.getId() + "请求支付更新交易单号失败");
            }
        }
        return downLineTrading;
    }


    /**
     * 查询超时订单列表
     *
     * @param count
     */
    @Override
    public List<Orders> queryOverTimePayOrdersListByCount(Integer count) {
        //  查询订单状态为待支付，超时时间在当前时间之后的订单，查询count条，且只查询订单id和用户id字段
        // 查询待支付状态的订单
        List<Orders> list = lambdaQuery()
                .eq(Orders::getOrdersStatus, OrderStatusEnum.NO_PAY.getStatus())
                //超时时间小于当前时间
                .lt(Orders::getOverTime, LocalDateTime.now())
                .last("limit " + count)
                .select(Orders::getId, Orders::getUserId)
                .list();
        return list;
    }


    /**
     * 获取可用优惠券列表
     *
     * @param serveId
     * @param purNum
     */
    @Override
    public List<AvailableCouponsResDTO> getAvailableCoupons(Long serveId, Integer purNum) {
        // 获取当前用户
        Long userId = UserContext.currentUserId();
        if (ObjectUtils.isEmpty(userId)) {
            throw new BadRequestException("用户信息异常，无法下单!");
        }

        // 获取服务
        ServeAggregationResDTO serveResDTO = serveApi.findById(serveId);
        if (ObjectUtils.isEmpty(serveResDTO)) {
            throw new BadRequestException("服务异常，无法下单!");
        }

        // 计算总金额
        BigDecimal totalAmount = serveResDTO.getPrice().multiply(new BigDecimal(purNum));

        // 获取可用优惠卷列表返回
        return couponApi.getAvailable(userId, totalAmount);
    }


    /**
     * 使用优惠券下单
     *
     * @param orders   订单信息
     * @param couponId 优惠券id
     */
    @GlobalTransactional // 分布式事物
    @Override
    public void addWithCoupon(Orders orders, Long couponId) {
        CouponUseReqDTO couponUseReqDTO = new CouponUseReqDTO();
        couponUseReqDTO.setOrdersId(orders.getId());
        couponUseReqDTO.setId(couponId);
        couponUseReqDTO.setTotalAmount(orders.getTotalAmount());
        //优惠券核销
        CouponUseResDTO couponUseResDTO = couponApi.use(couponUseReqDTO);
        // 设置优惠金额
        orders.setDiscountAmount(couponUseResDTO.getDiscountAmount());
        // 计算实付金额
        BigDecimal realPayAmount = orders.getTotalAmount().subtract(orders.getDiscountAmount());
        orders.setRealPayAmount(realPayAmount);
        //保存订单
        owner.add(orders);
    }

    @Override
    public PlaceOrderResDTO placeOrder(PlaceOrderReqDTO placeOrderReqDTO) {
        return placeOrder(UserContext.currentUserId(), placeOrderReqDTO);
    }

    /**
     * 下单方法
     *
     * @param placeOrderReqDTO
     */
    @Lock(formatter = "ORDERS:CREATE:LOCK:#{userId}:#{placeOrderReqDTO.serveId}", time = 30, waitTime = 1, unlock = false)
    public PlaceOrderResDTO placeOrder(Long userId, PlaceOrderReqDTO placeOrderReqDTO) {
        //下单人信息，获取地址簿，调用jzo2o-customer服务获取
        AddressBookResDTO detail = addressBookApi.detail(placeOrderReqDTO.getAddressBookId());
        if (ObjectUtils.isEmpty(detail)) {
            throw new BadRequestException("预约地址异常，无法下单!");
        }

        //服务相关信息,调用jzo2o-foundations获取
        ServeAggregationResDTO serveResDTO = serveApi.findById(placeOrderReqDTO.getServeId());
        // 服务不存在 不能下单
        if (ObjectUtils.isEmpty(serveResDTO)) {
            throw new BadRequestException("服务异常，无法下单!");
        }

        // 下单前的数据准备
        Orders orders = new Orders();
        // 设置订单id
        orders.setId(generateOrderId());
        // 获取当前用户id 从threadLocal获取当前登录用户的id，通过UserContextInteceptor拦截进行设置

        orders.setUserId(userId);

        // 设置服务id
        orders.setServeId(serveResDTO.getId());
        // 服务项id
        orders.setServeItemId(serveResDTO.getServeItemId());
        // 服务项名
        orders.setServeItemName(serveResDTO.getServeItemName());
        // 服务项图片
        orders.setServeItemImg(serveResDTO.getServeItemImg());
        // 服务项单位
        orders.setUnit(serveResDTO.getUnit());

        // 服务类型信息
        orders.setServeTypeId(serveResDTO.getServeTypeId());
        // 服务类型名
        orders.setServeTypeName(serveResDTO.getServeTypeName());

        // 订单状态 待支付
        orders.setOrdersStatus(OrderStatusEnum.NO_PAY.getStatus());
        // 支付状态 先初始化一个
        orders.setPayStatus(OrderPayStatusEnum.NO_PAY.getStatus());

        // 服务时间
        orders.setServeStartTime(placeOrderReqDTO.getServeStartTime());
        //城市编码
        orders.setCityCode(serveResDTO.getCityCode());
        //地理位置
        orders.setLon(detail.getLon());
        orders.setLat(detail.getLat());
        // 拼装地址信息
        String serveAddress = new StringBuilder(detail.getProvince())
                .append(detail.getCity())
                .append(detail.getCounty())
                .append(detail.getAddress())
                .toString();
        // 重新设置完整地址信息
        orders.setServeAddress(serveAddress);
        // 联系人
        orders.setContactsName(detail.getName());
        // 联系人电话
        orders.setContactsPhone(detail.getPhone());

        // 价格
        orders.setPrice(serveResDTO.getPrice());
        // 购买数量
        orders.setPurNum(placeOrderReqDTO.getPurNum());

        // 订单总金额 价格 * 购买数量
        orders.setTotalAmount(orders.getPrice().multiply(new BigDecimal(orders.getPurNum())));

        // 优惠金额 当前默认0
        orders.setDiscountAmount(BigDecimal.ZERO);
        // 实付金额 订单总金额 - 优惠金额
        orders.setRealPayAmount(NumberUtils.sub(orders.getTotalAmount(), orders.getDiscountAmount()));
        //排序字段,根据服务开始时间转为毫秒时间戳+订单后5位
        long sortBy = DateUtils.toEpochMilli(orders.getServeStartTime()) + orders.getId() % 100000;
        orders.setSortBy(sortBy);
        //支付超时时间 定为30分钟后，极端情况下可能在到达超时用户进行支付，这里超时时间多出5分钟，定时任务根据此时间查询超时订单并进行取消
        orders.setOverTime(LocalDateTime.now().plusMinutes(35));
        // 使用优惠券下单
        if (ObjectUtils.isNotNull(placeOrderReqDTO.getCouponId())) {
            owner.addWithCoupon(orders, placeOrderReqDTO.getCouponId());
        } else {
            // 无优惠券下单，走本地事务
            owner.add(orders);
        }
        return new PlaceOrderResDTO(orders.getId());
    }


    @Transactional(rollbackFor = Exception.class)
    public void add(Orders orders) {
        boolean save = this.save(orders);
        if (!save) {
            throw new DbRuntimeException("下单失败");
        }
    }


    public Long generateOrderId() {
        // 通过redis自增生成序列单号
        Long id = redisTemplate.opsForValue().increment(ORDERS_SHARD_KEY_ID_GENERATOR, 1);

        // 调用内部封装方法生成订单
        long orderId = DateUtils.getFormatDate(LocalDateTime.now(), "yyMMdd") * 10000000000L + id;
        return orderId;
    }
}
