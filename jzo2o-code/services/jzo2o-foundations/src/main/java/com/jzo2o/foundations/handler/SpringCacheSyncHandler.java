package com.jzo2o.foundations.handler;

import com.jzo2o.api.foundations.dto.response.RegionSimpleResDTO;
import com.jzo2o.foundations.constants.RedisConstants;
import com.jzo2o.foundations.service.HomeService;
import com.jzo2o.foundations.service.IRegionService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Spring Cache 缓存同步任务
 */
@Slf4j
@Component
public class SpringCacheSyncHandler {

    private static final String ACTIVE_REGIONS_KEY = RedisConstants.CacheName.JZ_CACHE + "::ACTIVE_REGIONS";

    @Resource
    private IRegionService regionService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private HomeService homeService;

    /**
     * 已启用区域缓存更新。
     * 每日凌晨 1 点执行，由 XXL-JOB 调度。
     */
    @XxlJob("activeRegionCacheSync")
    public void activeRegionCacheSync() {
        log.info("active region cache sync start");

        // 删除已启用区域列表缓存，随后通过查询方法重新写入缓存。
        Boolean activeRegionsDeleted = redisTemplate.delete(ACTIVE_REGIONS_KEY);
        log.info("delete active region cache key: {}, result: {}", ACTIVE_REGIONS_KEY, activeRegionsDeleted);

        List<RegionSimpleResDTO> activeRegions = regionService.queryActiveRegionList();

        // 遍历已启用区域，刷新每个区域首页服务图标分类缓存。
        activeRegions.forEach(region -> {
            Long regionId = region.getId();
            String serveIconKey = RedisConstants.CacheName.SERVE_ICON + "::" + regionId;

            redisTemplate.delete(serveIconKey);
            homeService.queryServeIconCategoryByRegionIdCache(regionId);
        });

        log.info("active region cache sync end, region count: {}", activeRegions.size());
    }
}
