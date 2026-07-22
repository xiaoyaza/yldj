package com.jzo2o.market.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.market.dto.request.CouponUseBackReqDTO;
import com.jzo2o.api.market.dto.request.CouponUseReqDTO;
import com.jzo2o.api.market.dto.response.AvailableCouponsResDTO;
import com.jzo2o.api.market.dto.response.CouponUseResDTO;
import com.jzo2o.common.expcetions.BadRequestException;
import com.jzo2o.common.expcetions.DBException;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.utils.DateUtils;
import com.jzo2o.common.utils.IdUtils;
import com.jzo2o.common.utils.UserContext;
import com.jzo2o.market.enums.ActivityStatusEnum;
import com.jzo2o.market.enums.CouponStatusEnum;
import com.jzo2o.market.mapper.CouponMapper;
import com.jzo2o.market.model.domain.Activity;
import com.jzo2o.market.model.domain.Coupon;
import com.jzo2o.market.model.domain.CouponWriteOff;
import com.jzo2o.market.model.dto.request.CouponLimitReqDTO;
import com.jzo2o.market.model.dto.request.CouponOperationPageQueryReqDTO;
import com.jzo2o.market.model.dto.response.CouponInfoResDTO;
import com.jzo2o.market.service.IActivityService;
import com.jzo2o.market.service.ICouponService;
import com.jzo2o.market.service.ICouponUseBackService;
import com.jzo2o.market.service.ICouponWriteOffService;
import com.jzo2o.market.utils.CouponUtils;
import com.jzo2o.mysql.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-09-16
 */
@Service
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    @Resource(name = "seizeCouponScript")
    private DefaultRedisScript<String> seizeCouponScript;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private IActivityService activityService;

    @Resource
    private ICouponUseBackService couponUseBackService;

    @Resource
    private ICouponWriteOffService couponWriteOffService;


    /**
     * 分页查询领取列表
     *
     * @param couponOperationPageQueryReqDTO
     */
    @Override
    public PageResult<CouponInfoResDTO> pageList(CouponOperationPageQueryReqDTO couponOperationPageQueryReqDTO) {
        // 使用pageHelper分页
        // 分页配置
        Page<Coupon> couponPage = new Page<Coupon>(couponOperationPageQueryReqDTO.getPageNo(), couponOperationPageQueryReqDTO.getPageSize());
        // 分页查询
        Page<Coupon> page = page(couponPage, Wrappers.<Coupon>lambdaQuery().eq(Coupon::getActivityId, couponOperationPageQueryReqDTO.getActivityId()));
        // 结果封装
        return PageUtils.toPage(page, CouponInfoResDTO.class);
    }

    /**
     * 优惠券失效
     *
     * @param id
     */
    @Override
    public void expireCoupon(Long id) {
        boolean update = lambdaUpdate()
                .eq(Coupon::getActivityId, id)
                .eq(Coupon::getStatus, CouponStatusEnum.NO_USE.getStatus())
                .set(Coupon::getStatus, CouponStatusEnum.INVALID.getStatus())
                .update();
    }

    /**
     * 我的优惠券列表
     *
     * @param couponLimitReqDTO
     */
    @Override
    public List<CouponInfoResDTO> myCoupons(CouponLimitReqDTO couponLimitReqDTO) {
        List<Coupon> list = lambdaQuery()
                .eq(Coupon::getId, couponLimitReqDTO.getUserId())
                .eq(Coupon::getStatus, CouponStatusEnum.NO_USE.getStatus())
                .lt(ObjectUtils.isNotEmpty(couponLimitReqDTO.getLastId()), Coupon::getId, couponLimitReqDTO.getLastId())
                .orderByDesc(Coupon::getId)
                .last("limit 10")
                .list();
        return BeanUtil.copyToList(list, CouponInfoResDTO.class);
    }

    /**
     * 用户优惠券失效
     */
    @Override
    public void userExprireCoupon() {
        // 获取当前时间
        LocalDateTime now = DateUtils.now();
        lambdaUpdate()
                .eq(Coupon::getStatus, CouponStatusEnum.NO_USE.getStatus())
                .lt(Coupon::getValidityTime, now) // 失效时间小于当前时间
                .set(Coupon::getStatus, CouponStatusEnum.INVALID.getStatus())
                .update();
    }

    /**
     * 获取可用优惠券列表，并按优惠金额从大到小排序
     *
     * @param userId      用户id
     * @param totalAmount 订单总金额（单位：分）
     * @return 可用优惠券列表
     */
    @Override
    public List<AvailableCouponsResDTO> getAvailable(Long userId, BigDecimal totalAmount) {
        if (userId == null || totalAmount == null) {
            return List.of();
        }

        // 1. 查询用户未使用且未过期的优惠券
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getUserId, userId)
                .eq(Coupon::getStatus, CouponStatusEnum.NO_USE.getStatus())
                .ge(Coupon::getValidityTime, LocalDateTime.now())
                .list();
        if (ObjectUtils.isEmpty(coupons)) {
            return List.of();
        }

        // 2. 过滤门槛 + 按优惠金额降序 + 转 DTO
        return coupons.stream()
                .filter(c -> isMeetCondition(c, totalAmount))
                .sorted((a, b) -> CouponUtils.calDiscountAmount(b, totalAmount)
                        .compareTo(CouponUtils.calDiscountAmount(a, totalAmount)))
                .map(c -> BeanUtil.copyProperties(c, AvailableCouponsResDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * 使用优惠券
     *
     * @param couponUseReqDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponUseResDTO use(CouponUseReqDTO couponUseReqDTO) {
        //判空
        if (ObjectUtils.isNull(couponUseReqDTO.getOrdersId()) ||
                ObjectUtils.isNull(couponUseReqDTO.getTotalAmount()))
        {
            throw new BadRequestException("优惠券核销的订单信息为空");
        }
        //用户id
        Long userId = UserContext.currentUserId();
        //查询优惠券信息
        Coupon coupon = baseMapper.selectById(couponUseReqDTO.getId());
        // 优惠券判空
        if (coupon == null ) {
            throw new BadRequestException("优惠券不存在");
        }
        if ( !Objects.equals(coupon.getUserId(), userId)) {
            throw new BadRequestException("只允许核销自己的优惠券");
        }
        //更新优惠券表的状态
        boolean update = lambdaUpdate()
                .eq(Coupon::getId, couponUseReqDTO.getId())
                .eq(Coupon::getStatus, CouponStatusEnum.NO_USE.getStatus())
                .gt(Coupon::getValidityTime, DateUtils.now())
                .le(Coupon::getAmountCondition, couponUseReqDTO.getTotalAmount())
                .set(Coupon::getOrdersId, couponUseReqDTO.getOrdersId())
                .set(Coupon::getStatus, CouponStatusEnum.USED.getStatus())
                .set(Coupon::getUseTime, DateUtils.now())
                .update();
        if (!update) {
            throw new DBException("优惠券核销失败");
        }

        //添加核销记录
        CouponWriteOff couponWriteOff = CouponWriteOff.builder()
                .id(IdUtils.getSnowflakeNextId())
                .couponId(couponUseReqDTO.getId())
                .userId(userId)
                .ordersId(couponUseReqDTO.getOrdersId())
                .activityId(coupon.getActivityId())
                .writeOffTime(DateUtils.now())
                .writeOffManName(coupon.getUserName())
                .writeOffManPhone(coupon.getUserPhone())
                .build();
        if(!couponWriteOffService.save(couponWriteOff)){
            throw new DBException("优惠券核销失败");
        }

        // 计算优惠金额
        BigDecimal discountAmount = CouponUtils.calDiscountAmount(coupon, couponUseReqDTO.getTotalAmount());
        CouponUseResDTO couponUseResDTO = new CouponUseResDTO();
        couponUseResDTO.setDiscountAmount(discountAmount);
        return couponUseResDTO;

    }

    /**
     * 优惠券回退
     *
     * @param couponUseBackReqDTO 回退请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useBack(CouponUseBackReqDTO couponUseBackReqDTO) {
        // 1.校验是否可以回退
        CouponWriteOff couponWriteOff = couponWriteOffService.queryByUserIdIdAndOrdersId(
                couponUseBackReqDTO.getUserId(), couponUseBackReqDTO.getOrdersId());
        // 未查询到无需回滚
        if (couponWriteOff == null) {
            return;
        }
        Coupon coupon = baseMapper.selectById(couponWriteOff.getCouponId());
        if (coupon == null) {
            return;
        }

        Activity activity = activityService.getById(couponWriteOff.getActivityId());
        // 2.回退记录
        couponUseBackService.add(couponWriteOff.getCouponId(),
                couponUseBackReqDTO.getUserId(),
                couponWriteOff.getWriteOffTime());

        // 3.回滚优惠券
        CouponStatusEnum couponStatusEnum = coupon.getValidityTime().isAfter(DateUtils.now())
                ? CouponStatusEnum.NO_USE : CouponStatusEnum.INVALID;
        if (ActivityStatusEnum.VOIDED.equals(activity.getStatus())) {
            // 活动作废
            couponStatusEnum = CouponStatusEnum.VOIDED;
        }
        boolean update = lambdaUpdate()
                .set(Coupon::getStatus, couponStatusEnum.getStatus())
                .set(Coupon::getOrdersId, null)
                .set(Coupon::getUseTime, null)
                .eq(Coupon::getId, coupon.getId())
                .update();
        if (!update) {
            throw new RuntimeException("优惠券回退失败");
        }
        // 4.删除核销记录
        couponWriteOffService.removeById(couponWriteOff.getId());
    }

    /**
     * 是否满足使用门槛
     * amountCondition = 0 表示无门槛
     */
    private boolean isMeetCondition(Coupon coupon, BigDecimal totalAmount) {
        BigDecimal condition = coupon.getAmountCondition();
        if (condition == null || condition.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        return condition.compareTo(totalAmount) <= 0;
    }
}
