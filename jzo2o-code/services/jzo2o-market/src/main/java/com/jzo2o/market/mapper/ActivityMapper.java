package com.jzo2o.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jzo2o.market.model.domain.Activity;
import com.jzo2o.market.model.dto.request.ActivityQueryForPageReqDTO;
import com.jzo2o.market.model.dto.response.ActivityInfoResDTO;

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
}
