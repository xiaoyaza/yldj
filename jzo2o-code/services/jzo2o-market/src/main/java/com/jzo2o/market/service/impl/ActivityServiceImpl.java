package com.jzo2o.market.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.utils.BeanUtils;
import com.jzo2o.common.utils.DateUtils;
import com.jzo2o.common.utils.ObjectUtils;
import com.jzo2o.market.enums.ActivityStatusEnum;
import com.jzo2o.market.mapper.ActivityMapper;
import com.jzo2o.market.model.domain.Activity;
import com.jzo2o.market.model.dto.request.ActivityQueryForPageReqDTO;
import com.jzo2o.market.model.dto.request.ActivitySaveReqDTO;
import com.jzo2o.market.model.dto.response.ActivityInfoResDTO;
import com.jzo2o.market.service.IActivityService;
import com.jzo2o.market.service.ICouponService;
import com.jzo2o.market.service.ICouponWriteOffService;
import com.jzo2o.mysql.utils.PageHelperUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-09-16
 */
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements IActivityService {
    private static final int MILLION = 1000000;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private ICouponService couponService;

    @Resource
    private ICouponWriteOffService couponWriteOffService;


    /**
     * 添加活动
     *
     * @param activitySaveReqDTO
     */
    @Override
    public void addActivity(ActivitySaveReqDTO activitySaveReqDTO) {
        // 将dto 装为po 前端接收参数
        Activity activity = BeanUtils.toBean(activitySaveReqDTO, Activity.class);
        activity.setTotalNum(activitySaveReqDTO.getTotalNum());
        activity.setStatus(ActivityStatusEnum.NO_DISTRIBUTE.getStatus());

        if (ObjectUtils.isNotEmpty(activitySaveReqDTO.getId())){
            // 添加进去
            baseMapper.insert(activity);
        }else {
            // 修改
            baseMapper.updateById(activity);
        }
    }

    /**
     * 分页查询活动列表
     *
     * @param activityQueryForPageReqDTO
     * @return
     */
    @Override
    public PageResult<ActivityInfoResDTO> pageList(ActivityQueryForPageReqDTO activityQueryForPageReqDTO) {
        // 使用 pagehepler 封装分页
        return PageHelperUtils.selectPage(activityQueryForPageReqDTO, () -> baseMapper.queryActivityList(activityQueryForPageReqDTO));
    }

    /**
     * 获取活动详情
     *
     * @param id
     * @return
     */
    @Override
    public ActivityInfoResDTO getDetail(Long id) {
        return baseMapper.queryActivityById(id);
    }

    /**
     * 撤销活动
     *
     * @param id
     */
    @Override
    public void revokeActivity(Long id) {
        boolean update = lambdaUpdate()
                .eq(Activity::getId, id)
                .in(Activity::getStatus, ActivityStatusEnum.NO_DISTRIBUTE.getStatus(), ActivityStatusEnum.DISTRIBUTING.getStatus())
                .set(Activity::getStatus, ActivityStatusEnum.VOIDED.getStatus())
                .update();

        // 将所有抢到本优惠卷的状态为待生效，进行中，都改为作废
        if (update){
            couponService.expireCoupon(id);
        }
    }

    /**
     * 活动状态包括：1：NO_DISTRIBUTE待生效，2：DISTRIBUTING进行中，3：LOSE_EFFICACY已失效 4: VOIDED作废'
     * 对于待生效的活动：到达发放开始时间状态改为“进行中”。
     * 对于待生效及进行中的活动：到达发放结束时间状态改为“已失效”
     * 使用xxl-job定义定时任务，每分钟执行一次。
     */
    @Override
    public void updateActivityStatus() {
        // 获取当前时间
        LocalDateTime now = DateUtils.now();

        // 更新已经进行中的状态
        lambdaUpdate()
                .set(Activity::getStatus, ActivityStatusEnum.DISTRIBUTING.getStatus()) // 更新活动状态为进行中
                .eq(Activity::getStatus, ActivityStatusEnum.NO_DISTRIBUTE.getStatus()) // 检索待生效的活动
                .le(Activity::getDistributeStartTime, now) // 活动待开始时间小于等于当前时间
                .ge(Activity::getDistributeEndTime, now) // 活动结束时间大于当前时间
                .update();

        // 更新已经结束的活动状态
        lambdaUpdate()
                .set(Activity::getStatus, ActivityStatusEnum.LOSE_EFFICACY.getStatus()) // 更新活动为已失效
                .in(Activity::getStatus, Arrays.asList(
                        ActivityStatusEnum.NO_DISTRIBUTE.getStatus(),
                        ActivityStatusEnum.DISTRIBUTING.getStatus())) // 检索待生效和进行中的活动
                .lt(Activity::getDistributeEndTime, now) // 活动结束时间小于当前时间
                .update();
    }
}
