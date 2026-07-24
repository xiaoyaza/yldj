package com.jzo2o.orders.manager.service.impl;

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
import com.jzo2o.common.expcetions.BadRequestException;
import com.jzo2o.common.utils.DateUtils;
import com.jzo2o.common.utils.NumberUtils;
import com.jzo2o.common.utils.ObjectUtils;
import com.jzo2o.common.utils.UserContext;
import com.jzo2o.orders.base.enums.OrderPayStatusEnum;
import com.jzo2o.orders.base.enums.OrderStatusEnum;
import com.jzo2o.orders.base.mapper.OrdersMapper;
import com.jzo2o.orders.base.model.domain.Orders;
import com.jzo2o.orders.manager.model.dto.request.PlaceOrderReqDTO;
import com.jzo2o.orders.manager.model.dto.response.PlaceOrderResDTO;
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
        if (ObjectUtils.isEmpty(userId)){
            throw new BadRequestException("用户信息异常，无法下单!");
        }

        // 获取服务
        ServeAggregationResDTO serveResDTO = serveApi.findById(serveId);
        if (ObjectUtils.isEmpty(serveResDTO)){
            throw new BadRequestException("服务异常，无法下单!");
        }

        // 计算总金额
        BigDecimal totalAmount = serveResDTO.getPrice().multiply(new BigDecimal(purNum));

        // 获取可用优惠卷列表返回
        return couponApi.getAvailable(userId,totalAmount);
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
    @Lock(formatter = "ORDERS:CREATE:LOCK:#{userId}:#{placeOrderReqDTO.serveId}", time = 30, waitTime = 1,unlock=false)
    public PlaceOrderResDTO placeOrder(Long userId, PlaceOrderReqDTO placeOrderReqDTO) {
        //下单人信息，获取地址簿，调用jzo2o-customer服务获取
        AddressBookResDTO detail = addressBookApi.detail(placeOrderReqDTO.getAddressBookId());
        if (ObjectUtils.isEmpty(detail)){
            throw new BadRequestException("预约地址异常，无法下单!");
        }

        //服务相关信息,调用jzo2o-foundations获取
        ServeAggregationResDTO serveResDTO = serveApi.findById(placeOrderReqDTO.getServeId());
        // 服务不存在 不能下单
        if (ObjectUtils.isEmpty(serveResDTO)){
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



    public Long generateOrderId(){
        // 通过redis自增生成序列单号
        Long id = redisTemplate.opsForValue().increment(ORDERS_SHARD_KEY_ID_GENERATOR, 1);

        // 调用内部封装方法生成订单
        long orderId = DateUtils.getFormatDate(LocalDateTime.now(), "yyMMdd") * 10000000000L + id;
        return orderId;
    }
}
