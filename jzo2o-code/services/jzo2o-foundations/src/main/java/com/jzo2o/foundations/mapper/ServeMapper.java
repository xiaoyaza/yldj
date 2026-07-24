package com.jzo2o.foundations.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.dto.response.ServeAggregationSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeAggregationTypeSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeCategoryResDTO;
import com.jzo2o.foundations.model.dto.response.ServeResDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author itcast
 * @since 2023-07-03
 */
public interface ServeMapper extends BaseMapper<Serve> {

    /**
     * 根据区域查询服务列表
     * @param regionId
     * @return
     */
    List<ServeResDTO> queryServeListByRegionId(@Param("regionId") Long regionId);

    /**
     * 根据区域id查询服务图标
     *
     * @param regionId 区域id
     * @return 服务图标
     */
    List<ServeCategoryResDTO> findServeIconCategoryByRegionId(@Param("regionId")Long regionId);

    /**
     * 根据区域id查询首页热门服务
     *
     * @param regionId 区域id
     * @return 首页热门服务
     */
    List<ServeAggregationSimpleResDTO> findHotServeListByRegionId(@Param("regionId") Long regionId);

    /**
     * 根据id查询服务
     *
     * @param id 服务id
     * @return 服务
     */
    ServeAggregationSimpleResDTO findServeById(Long id);

    List<ServeAggregationSimpleResDTO> findServeListByCityCodeAndServeTypeId(@Param("cityCode") String cityCode,
                                                                             @Param("serveTypeId") Long serveTypeId);

    /**
     * 根据区域id查询服务分类
     *
     * @param regionId 区域id
     * @return 服务分类
     */
    List<ServeAggregationTypeSimpleResDTO> findServeCategoryListByRegionId(@Param("regionId") Long regionId);

    /**
     * 根据id查询服务聚合信息
     *
     * @param id 服务id
     * @return 服务聚合信息
     */
    ServeAggregationResDTO findById(Long id);
}
