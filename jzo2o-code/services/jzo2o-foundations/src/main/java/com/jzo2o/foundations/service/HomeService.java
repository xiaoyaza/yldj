package com.jzo2o.foundations.service;

import com.jzo2o.foundations.model.dto.response.ServeAggregationSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeAggregationTypeSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeCategoryResDTO;

import java.util.List;

public interface HomeService{

    /**
     * 查询服务图标分类
     *
     * @param regionId 区域id
     */
    List<ServeCategoryResDTO> queryServeIconCategoryByRegionIdCache(Long regionId);

    /**
     * 查询首页热门服务列表
     *
     * @param regionId 区域id
     */
    List<ServeAggregationSimpleResDTO> queryHotServeListByRegionId(Long regionId);

    /**
     * 根据id查询服务
     *
     * @param id 服务id
     */
    ServeAggregationSimpleResDTO queryServeById(Long id);

    List<ServeAggregationSimpleResDTO> queryServeListByCityCodeAndServeTypeId(String cityCode, Long serveTypeId);

    /**
     * 根据区域查询服务列表
     * @param regionId
     * @return
     */
    List<ServeAggregationTypeSimpleResDTO> queryServeCategoryListByRegionId(Long regionId);
}
