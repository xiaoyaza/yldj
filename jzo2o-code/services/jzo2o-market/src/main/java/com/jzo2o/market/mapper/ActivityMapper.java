package com.jzo2o.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jzo2o.market.model.domain.Activity;
import com.jzo2o.market.model.dto.request.ActivityQueryForPageReqDTO;
import com.jzo2o.market.model.dto.response.ActivityInfoResDTO;
import com.jzo2o.market.model.dto.response.SeizeCouponInfoResDTO;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 * @since 2023-09-16
 */
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 查询活动列表
     * @param activityQueryForPageReqDTO
     * @return
     */
    List<ActivityInfoResDTO> queryActivityList(ActivityQueryForPageReqDTO activityQueryForPageReqDTO);

    /**
     * 根据id查询活动信息
     * @param id
     * @return
     */
    ActivityInfoResDTO queryActivityById(Long id);

    /**
     * 小程序抢券活动列表
     * tabType: 1 抢券中, 2 即将开始
     * @param tabType
     * @return
     */
    List<SeizeCouponInfoResDTO> listForConsumer(Integer tabType);
}
