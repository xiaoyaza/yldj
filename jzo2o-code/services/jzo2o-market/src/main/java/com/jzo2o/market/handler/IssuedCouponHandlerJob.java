package com.jzo2o.market.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jzo2o.market.model.domain.Activity;
import com.jzo2o.market.service.IActivityService;
import com.jzo2o.market.service.ICouponIssueService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 自动发放优惠券任务
 * 按活动维度派发线程执行。
 *
 * @author Mr.M
 */
@Slf4j
@Component
public class IssuedCouponHandlerJob {

    private static ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private ICouponIssueService couponIssueService;

    @Resource
    private IActivityService activityService;

    @Resource
    private RedissonClient redissonClient;

    static {
        threadPoolExecutor = new ThreadPoolExecutor(
                0, 20, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
    }

    /**
     * 启动任务
     * 查询所有未结束的活动，为每个活动启动一个发放线程。
     */
    public void start() {
        log.info("自动发放优惠券任务开始");
        // 查询未结束的活动：1=待生效，2=进行中
        List<Activity> activityList = activityService.list(
                new LambdaQueryWrapper<Activity>()
                        .in(Activity::getStatus, Arrays.asList(1, 2)));
        log.info("本次扫描活动数：{}", activityList.size());

        activityList.forEach(activity -> {
            IssuedCouponHandler handler = new IssuedCouponHandler(
                    activity.getId(), couponIssueService, redissonClient);
            threadPoolExecutor.execute(handler);
        });
    }
}
