package com.jzo2o.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.common.expcetions.CommonException;
import com.jzo2o.common.expcetions.DBException;
import com.jzo2o.common.utils.DateUtils;
import com.jzo2o.common.utils.IdUtils;
import com.jzo2o.common.utils.StringUtils;
import com.jzo2o.market.enums.CouponStatusEnum;
import com.jzo2o.market.mapper.CouponIssueMapper;
import com.jzo2o.market.model.domain.Activity;
import com.jzo2o.market.model.domain.Coupon;
import com.jzo2o.market.model.domain.CouponIssue;
import com.jzo2o.market.model.dto.request.CouponIssueReqDTO;
import com.jzo2o.market.service.IActivityService;
import com.jzo2o.market.service.ICouponIssueService;
import com.jzo2o.market.service.ICouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mr.M
 * @version 1.0
 * @description 发放优惠券服务类
 * @date 2024/9/23 16:33
 */
@Service
@Slf4j
public class CouponIssueServiceImpl extends ServiceImpl<CouponIssueMapper, CouponIssue> implements ICouponIssueService {

    /** 批量处理记录数 */
    private static final int BATCH_SIZE = 1000;

    @Resource
    private ICouponService couponService;

    @Resource
    private IActivityService activityService;

    /**
     * 自我注入，用于在 autoIssue 中通过 owner.issue() 调用触发 @Transactional 代理
     */
    @Resource
    private CouponIssueServiceImpl owner;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CouponIssue> save(CouponIssueReqDTO couponIssueReqDTO) {
        if (couponIssueReqDTO == null) {
            log.info("待发放优惠券数据为空，无需处理");
            throw new CommonException("待发放优惠券数据为空，无需处理");
        }
        // 1.校验活动id
        if (couponIssueReqDTO.getActivityId() == null) {
            throw new CommonException("活动id不能为空");
        }
        // 2.查询活动
        Activity activity = activityService.getById(couponIssueReqDTO.getActivityId());
        if (activity == null) {
            log.info("优惠券活动不存在，id:{}", couponIssueReqDTO.getActivityId());
            throw new CommonException("优惠券活动不存在");
        }
        // 3.校验活动是否过期
        if (activity.getDistributeEndTime().isBefore(LocalDateTime.now())) {
            throw new CommonException("活动已结束");
        }
        // 4.校验用户ids
        if (StringUtils.isBlank(couponIssueReqDTO.getUserIds())) {
            throw new CommonException("用户id不能为空");
        }

        // 5.解析userIds
        List<Long> userIds = Arrays.stream(couponIssueReqDTO.getUserIds().split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(Long::parseLong)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userIds)) {
            throw new CommonException("用户id不能为空");
        }

        // 6.根据活动id和用户ids查询待发放表中已存在的记录，去重
        List<CouponIssue> existList = lambdaQuery()
                .eq(CouponIssue::getActivityId, couponIssueReqDTO.getActivityId())
                .in(CouponIssue::getUserId, userIds)
                .list();
        List<Long> existUserIds = existList.stream()
                .map(CouponIssue::getUserId)
                .collect(Collectors.toList());
        List<Long> newUserIds = userIds.stream()
                .filter(uid -> !existUserIds.contains(uid))
                .collect(Collectors.toList());
        if (newUserIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 7.扣减库存（带库存充足条件）
        int size = newUserIds.size();
        boolean b = activityService.lambdaUpdate()
                .setSql("stock_num = stock_num - " + size)
                .eq(Activity::getId, activity.getId())
                .ge(Activity::getStockNum, size)
                .update();
        if (!b) {
            throw new CommonException("优惠券活动库存不足");
        }

        // 8.构造待发放记录
        List<CouponIssue> couponIssueListNew = new ArrayList<>(size);
        for (Long userId : newUserIds) {
            CouponIssue couponIssue = new CouponIssue();
            couponIssue.setId(IdUtils.getSnowflakeNextId());
            couponIssue.setActivityId(couponIssueReqDTO.getActivityId());
            couponIssue.setUserId(userId);
            couponIssue.setStatus(0);
            couponIssueListNew.add(couponIssue);
        }

        // 9.批量插入待发放表
        boolean b1 = saveBatch(couponIssueListNew);
        if (!b1) {
            throw new CommonException("提交待发放优惠券失败");
        }
        return couponIssueListNew;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CouponIssue> issue(CouponIssueReqDTO couponIssueReqDTO) {
        // 1.活动id
        Long activityId = couponIssueReqDTO.getActivityId();
        // 2.查询活动信息
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new CommonException("活动不存在");
        }
        // 3.校验活动是否结束
        if (activity.getDistributeEndTime().isBefore(LocalDateTime.now())) {
            throw new CommonException("活动已结束");
        }
        // 4.先插入到待发放表（去重 + 扣库存）
        save(couponIssueReqDTO);

        // 5.解析userIds
        List<Long> userIds = Arrays.stream(couponIssueReqDTO.getUserIds().split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 6.查询刚插入的待发放记录（status=0）
        List<CouponIssue> couponIssues = lambdaQuery()
                .eq(CouponIssue::getActivityId, activityId)
                .in(CouponIssue::getUserId, userIds)
                .eq(CouponIssue::getStatus, 0)
                .list();
        if (CollectionUtils.isEmpty(couponIssues)) {
            return couponIssues;
        }

        // 7.更新待发放记录状态为 1（已发放）
        couponIssues.forEach(couponIssue -> couponIssue.setStatus(1));
        boolean updateBatchById = updateBatchById(couponIssues);
        if (!updateBatchById) {
            throw new DBException("优惠券发放失败");
        }

        // 8.构造 Coupon 列表，写入优惠券表
        LocalDateTime now = DateUtils.now();
        List<Coupon> couponList = couponIssues.stream().map(couponIssue -> {
            Coupon coupon = new Coupon();
            coupon.setId(couponIssue.getId());
            coupon.setUserId(couponIssue.getUserId());
            coupon.setActivityId(couponIssue.getActivityId());
            coupon.setName(activity.getName());
            coupon.setType(activity.getType());
            coupon.setDiscountAmount(activity.getDiscountAmount());
            coupon.setDiscountRate(activity.getDiscountRate());
            coupon.setAmountCondition(activity.getAmountCondition());
            coupon.setValidityTime(now.plusDays(activity.getValidityDays()));
            coupon.setStatus(CouponStatusEnum.NO_USE.getStatus());
            coupon.setCreateTime(now);
            coupon.setUpdateTime(now);
            return coupon;
        }).collect(Collectors.toList());

        // 9.批量保存到优惠券表
        boolean b1 = couponService.saveBatch(couponList);
        if (!b1) {
            throw new CommonException("优惠券批量发放失败");
        }
        return couponIssues;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoIssue(Long activityId) {
        while (true) {
            // 1.分批取待发放记录（status=0）
            List<CouponIssue> couponIssueList = lambdaQuery()
                    .eq(CouponIssue::getActivityId, activityId)
                    .eq(CouponIssue::getStatus, 0)
                    .last("limit " + BATCH_SIZE)
                    .list();
            if (CollectionUtils.isEmpty(couponIssueList)) {
                break;
            }
            log.info("待发放记录：{}", couponIssueList);

            // 2.构造入参
            CouponIssueReqDTO reqDTO = new CouponIssueReqDTO();
            reqDTO.setActivityId(activityId);
            List<Long> userIds = couponIssueList.stream()
                    .map(CouponIssue::getUserId)
                    .collect(Collectors.toList());
            reqDTO.setUserIds(StringUtils.join(",", userIds));
            log.info("准备发放优惠券：{}", reqDTO);

            // 3.通过 self-injection 调用 issue，触发 @Transactional 代理
            try {
                owner.issue(reqDTO);
            } catch (Exception e) {
                log.info("发放优惠券：{} 异常", reqDTO, e);
                throw e;
            }

            // 4.休眠 1 秒，避免对 DB 压力过大
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("autoIssue 线程被中断", e);
            }
        }
    }
}
