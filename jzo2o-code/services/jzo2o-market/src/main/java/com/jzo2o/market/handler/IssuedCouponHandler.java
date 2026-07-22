package com.jzo2o.market.handler;

import com.jzo2o.market.service.ICouponIssueService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 自动发放优惠券处理器
 * 根据活动id从待发放记录表查找该活动的待发放记录，然后批量进行发放。
 *
 * @author Mr.M
 */
@Slf4j
public class IssuedCouponHandler implements Runnable {

    private final ICouponIssueService couponIssueService;
    private final RedissonClient redissonClient;
    private final Long activityId;

    public IssuedCouponHandler(Long activityId,
                               ICouponIssueService couponIssueService,
                               RedissonClient redissonClient) {
        this.activityId = activityId;
        this.couponIssueService = couponIssueService;
        this.redissonClient = redissonClient;
    }

    @Override
    public void run() {
        String lockKey = "activity:issued:lock:" + activityId;
        log.info("尝试获取锁：{}", lockKey);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // waitTime=1s, leaseTime=-1 启用看门狗自动续期
            boolean tryLock = lock.tryLock(1, -1, TimeUnit.SECONDS);
            if (!tryLock) {
                log.info("获取锁失败：{}", lockKey);
                return;
            }
            try {
                log.info("开始发放优惠券：activityId={}", activityId);
                couponIssueService.autoIssue(activityId);
            } catch (Exception e) {
                log.error("发放优惠券失败：activityId={}, msg={}", activityId, e.getMessage(), e);
            } finally {
                // 仅当前线程持有时才释放
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("释放锁：{}", lockKey);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁被中断：{}", lockKey, e);
        }
    }
}
