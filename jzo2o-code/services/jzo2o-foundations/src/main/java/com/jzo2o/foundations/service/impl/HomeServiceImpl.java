package com.jzo2o.foundations.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.jzo2o.common.utils.ObjectUtils;
import com.jzo2o.foundations.constants.RedisConstants;
import com.jzo2o.foundations.enums.FoundationStatusEnum;
import com.jzo2o.foundations.mapper.RegionMapper;
import com.jzo2o.foundations.mapper.ServeMapper;
import com.jzo2o.foundations.model.domain.Region;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.domain.ServeItem;
import com.jzo2o.foundations.model.dto.response.ServeAggregationSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeAggregationTypeSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeCategoryResDTO;
import com.jzo2o.foundations.model.dto.response.ServeSimpleResDTO;
import com.jzo2o.foundations.service.HomeService;
import com.jzo2o.foundations.service.IServeItemService;
import com.jzo2o.foundations.service.IServeService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
public class HomeServiceImpl implements HomeService{

    @Resource
    private ServeMapper serveMapper;

    @Resource
    private RegionMapper regionMapper;

    @Resource
    private IServeService serveService;

    @Resource
    private IServeItemService serveItemService;

    /**
     * 查询服务图标分类
     *
     * @param regionId 区域id
     */
    @Caching(
            cacheable = {
                    //result为null时,属于缓存穿透情况，缓存时间30分钟
                    @Cacheable(value = RedisConstants.CacheName.SERVE_ICON, key = "#regionId", unless = "#result.size() != 0", cacheManager = RedisConstants.CacheManager.THIRTY_MINUTES),
                    //result不为null时,永久缓存
                    @Cacheable(value = RedisConstants.CacheName.SERVE_ICON, key = "#regionId", unless = "#result.size() == 0", cacheManager = RedisConstants.CacheManager.FOREVER)
            }
    )
    @Override
    public List<ServeCategoryResDTO> queryServeIconCategoryByRegionIdCache(Long regionId) {
        // 查询区域，是否存在 有没有启用
        Region region = regionMapper.selectById(regionId);
        if (!ObjectUtils.isNotEmpty(region) || !region.getActiveStatus().equals(FoundationStatusEnum.ENABLE.getStatus())){
            return Collections.emptyList();
        }

        // 查询区域服务列表
        List<ServeCategoryResDTO> serveCategoryResDTOS = serveMapper.findServeIconCategoryByRegionId(regionId);

        // 处理数据
        // 分类只保留 2个， end：集合的长度和 2，谁小 获取谁
        serveCategoryResDTOS = CollUtil.sub(serveCategoryResDTOS, 0, Math.min(2, serveCategoryResDTOS.size()));

        // 每个分类的服务列表只保留4个
        for (ServeCategoryResDTO serveCategoryResDTO : serveCategoryResDTOS){
            List<ServeSimpleResDTO> serveSimpleResDTOList = serveCategoryResDTO.getServeResDTOList();
            serveCategoryResDTO.setServeResDTOList(CollUtil.sub(serveSimpleResDTOList, 0, Math.min(4, serveSimpleResDTOList.size())));
        }
        return serveCategoryResDTOS;
    }

    /**
     * 查询首页热门服务列表
     *
     * @param regionId 区域id
     */
    @Caching(
            cacheable = {
                    @Cacheable(value = RedisConstants.CacheName.HOT_SERVE, key = "#regionId", unless = "#result.size() != 0", cacheManager = RedisConstants.CacheManager.THIRTY_MINUTES),
                    @Cacheable(value = RedisConstants.CacheName.HOT_SERVE, key = "#regionId", unless = "#result.size() == 0", cacheManager = RedisConstants.CacheManager.FOREVER)
            }
    )
    @Override
    public List<ServeAggregationSimpleResDTO> queryHotServeListByRegionId(Long regionId) {
        Region region = regionMapper.selectById(regionId);
        if (!ObjectUtils.isNotEmpty(region) || !region.getActiveStatus().equals(FoundationStatusEnum.ENABLE.getStatus())) {
            return Collections.emptyList();
        }
        return serveMapper.findHotServeListByRegionId(regionId);
    }

    /**
     * 根据id查询服务
     *
     * @param id 服务id
     */
    @Override
    public ServeAggregationSimpleResDTO queryServeById(Long id) {
        // 拿到服务项 看该服务项是否真的存在
        Serve serve = serveService.queryServeByIdCache(id);
        if (!ObjectUtils.isNotEmpty(serve)
                || !serve.getSaleStatus().equals(FoundationStatusEnum.ENABLE.getStatus())) {
            return null;
        }

        ServeItem serveItem = serveItemService.queryServeItemByIdCache(serve.getServeItemId());
        if (!ObjectUtils.isNotEmpty(serveItem)
                || !serveItem.getActiveStatus().equals(FoundationStatusEnum.ENABLE.getStatus())) {
            return null;
        }

        ServeAggregationSimpleResDTO serveAggregationSimpleResDTO = BeanUtil.toBean(serve, ServeAggregationSimpleResDTO.class);
        serveAggregationSimpleResDTO.setServeItemName(serveItem.getName());
        serveAggregationSimpleResDTO.setServeItemImg(serveItem.getImg());
        serveAggregationSimpleResDTO.setDetailImg(serveItem.getDetailImg());
        serveAggregationSimpleResDTO.setUnit(serveItem.getUnit());
        return serveAggregationSimpleResDTO;
    }

    /**
     * 根据城市和服务类型查询服务列表
     *
     * @param cityCode
     * @param serveTypeId
     * @return
     */
    @Caching(
            cacheable = {
                    @Cacheable(value = RedisConstants.CacheName.SERVE_LIST, key = "#cityCode + ':' + #serveTypeId", unless = "#result.size() != 0", cacheManager = RedisConstants.CacheManager.THIRTY_MINUTES),
                    @Cacheable(value = RedisConstants.CacheName.SERVE_LIST, key = "#cityCode + ':' + #serveTypeId", unless = "#result.size() == 0", cacheManager = RedisConstants.CacheManager.FOREVER)
            }
    )
    @Override
    public List<ServeAggregationSimpleResDTO> queryServeListByCityCodeAndServeTypeId(String cityCode, Long serveTypeId) {
        return serveMapper.findServeListByCityCodeAndServeTypeId(cityCode, serveTypeId);
    }

    /**
     * 根据区域查询服务列表
     *
     * @param regionId
     * @return
     */
    @Override
    @Caching(
            cacheable = {
                    @Cacheable(value = RedisConstants.CacheName.SERVE_TYPE, key = "#regionId", unless = "#result.size() != 0", cacheManager = RedisConstants.CacheManager.THIRTY_MINUTES),
                    @Cacheable(value = RedisConstants.CacheName.SERVE_TYPE, key = "#regionId", unless = "#result.size() == 0", cacheManager = RedisConstants.CacheManager.FOREVER)
            }
    )
    public List<ServeAggregationTypeSimpleResDTO> queryServeCategoryListByRegionId(Long regionId) {
        // 判断区域是否存在
        Region region = regionMapper.selectById(regionId);
        if (!ObjectUtils.isNotEmpty(region) || !region.getActiveStatus().equals(FoundationStatusEnum.ENABLE.getStatus())){
            return Collections.emptyList();
        }
        return serveMapper.findServeCategoryListByRegionId(regionId);
    }
}
