package com.jzo2o.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.market.model.domain.Activity;
import com.jzo2o.market.model.dto.request.ActivityQueryForPageReqDTO;
import com.jzo2o.market.model.dto.request.ActivitySaveReqDTO;
import com.jzo2o.market.model.dto.response.ActivityInfoResDTO;
import com.jzo2o.market.model.dto.response.SeizeCouponInfoResDTO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author itcast
 * @since 2023-09-16
 */
public interface IActivityService extends IService<Activity> {


    /**
     * 添加活动
     * @param activitySaveReqDTO
     */
    void addActivity(ActivitySaveReqDTO activitySaveReqDTO);

    /**
     * 分页查询活动列表
     * @param activityQueryForPageReqDTO
     * @return
     */
    PageResult<ActivityInfoResDTO> pageList(ActivityQueryForPageReqDTO activityQueryForPageReqDTO);

    /**
     * 获取活动详情
     * @param id
     * @return
     */
    ActivityInfoResDTO getDetail(Long id);

    /**
     * 撤销活动
     * @param id
     */
    void revokeActivity(Long id);

    /**
     * 活动状态包括：1：待生效，2：进行中，3：已失效 4: 作废'
     * 对于待生效的活动：到达发放开始时间状态改为“进行中”。
     * 对于待生效及进行中的活动：到达发放结束时间状态改为“已失效”
     * 使用xxl-job定义定时任务，每分钟执行一次。
     */
    void updateActivityStatus();

    /**
     * 小程序抢券活动列表
     * tabType: 1 抢券中, 2 即将开始
     * @param tabType
     * @return
     */
    List<SeizeCouponInfoResDTO> listForConsumer(Integer tabType);
}
