package com.jzo2o.foundations.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.common.expcetions.CommonException;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.foundations.constants.RedisConstants;
import com.jzo2o.foundations.enums.FoundationStatusEnum;
import com.jzo2o.foundations.mapper.RegionMapper;
import com.jzo2o.foundations.mapper.ServeItemMapper;
import com.jzo2o.foundations.mapper.ServeMapper;
import com.jzo2o.foundations.model.domain.Region;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.domain.ServeItem;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServeUpsertReqDTO;
import com.jzo2o.foundations.model.dto.response.ServeResDTO;
import com.jzo2o.foundations.service.IServeService;
import com.jzo2o.mysql.utils.PageHelperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ServeServiceImpl extends ServiceImpl<ServeMapper, Serve> implements IServeService {

    @Autowired
    private ServeItemMapper serveItemMapper;
    @Autowired
    private RegionMapper regionMapper;

    /**
     * 查询服务项详情缓存版
     *
     * @param id
     * @return
     */
    @Override
    @Cacheable(value = RedisConstants.CacheName.SERVE, key = "#id", unless = "#result == null", cacheManager = RedisConstants.CacheManager.ONE_DAY)
    public Serve queryServeByIdCache(Long id) {
        return getById(id);
    }

    /**
     * 查询区域服务信息
     *
     * @param id 对应serve表主键
     * @return 区域服务信息
     */
    @Override
    public ServeAggregationResDTO findById(Long id) {
        return getBaseMapper().findById(id);
    }

    /**
     * 分页查询服务列表
     *
     * @param servePageQueryReqDTO
     * @return
     */
    @Override
    public PageResult<ServeResDTO> page(ServePageQueryReqDTO servePageQueryReqDTO) {
        // 先使用baseMapper  再来调用queryServeListByRegionId 方法
        PageResult<ServeResDTO> serveResDTOPageResult = PageHelperUtils.selectPage(servePageQueryReqDTO, () -> baseMapper.queryServeListByRegionId(servePageQueryReqDTO.getRegionId()));

        return serveResDTOPageResult;
    }

    /**
     * 批量新增
     *
     * @param serveUpsertReqDTOList 批量新增数据
     */
    @Override
    @Transactional
    @CacheEvict(value = {
            RedisConstants.CacheName.SERVE_ICON,
            RedisConstants.CacheName.SERVE_TYPE,
            RedisConstants.CacheName.SERVE_LIST,
            RedisConstants.CacheName.HOT_SERVE
    }, allEntries = true, beforeInvocation = true)
    public void batchAdd(List<ServeUpsertReqDTO> serveUpsertReqDTOList) {
        for (ServeUpsertReqDTO serveUpsertReqDTO : serveUpsertReqDTOList) {
            // 校验服务项 是否是启用状态，不是启用状态不能新增
            ServeItem serveItem = serveItemMapper.selectById(serveUpsertReqDTO.getServeItemId());
            // 如果服务项信息不存在或者 未启用
            if (ObjectUtil.isNull(serveItem) || serveItem.getActiveStatus() != FoundationStatusEnum.ENABLE.getStatus()){
                throw new ForbiddenOperationException("该服务未启用无法添加到区域下使用");
            }

            // 校验是否重复新增
            Long count = lambdaQuery()
                    .eq(Serve::getRegionId, serveUpsertReqDTO.getRegionId())
                    .eq(Serve::getServeItemId, serveUpsertReqDTO.getServeItemId())
                    .count();
            if (count > 0){
                throw new ForbiddenOperationException(serveItem.getName() + "该服务已存在");
            }

            // 新增服务
            Serve serve = BeanUtil.toBean(serveUpsertReqDTO, Serve.class);
            // 默认为服务项的价格
            serve.setPrice(serveItem.getReferencePrice());
            Region region = regionMapper.selectById(serveUpsertReqDTO.getRegionId());
            serve.setCityCode(region.getCityCode());
            baseMapper.insert(serve);
        }
    }

    /**
     * 修改服务价格
     *
     * @param id
     * @param price
     */
    @Override
    @Caching(
            put = {
                    @CachePut(value = RedisConstants.CacheName.SERVE, key = "#id", cacheManager = RedisConstants.CacheManager.ONE_DAY)
            },
            evict = {
                    @CacheEvict(value = RedisConstants.CacheName.SERVE_LIST, allEntries = true, beforeInvocation = true),
                    @CacheEvict(value = RedisConstants.CacheName.HOT_SERVE, allEntries = true, beforeInvocation = true)
            }
    )
    public Serve update(Long id, BigDecimal price) {
        // 更新服务价格
        boolean update = lambdaUpdate()
                .eq(Serve::getId, id)
                .set(Serve::getPrice, price)
                .update();
        if (!update){
            throw new ForbiddenOperationException("修改服务价格失败!");
        }
        return baseMapper.selectById(id);
    }

    /**
     * 上架
     *
     * @param id
     */
    @Override
    @Caching(
            put = {
                    @CachePut(value = RedisConstants.CacheName.SERVE, key = "#id", cacheManager = RedisConstants.CacheManager.ONE_DAY)
            },
            evict = {
                    @CacheEvict(value = RedisConstants.CacheName.SERVE_ICON, allEntries = true, beforeInvocation = true),
                    @CacheEvict(value = RedisConstants.CacheName.SERVE_TYPE, allEntries = true, beforeInvocation = true),
                    @CacheEvict(value = RedisConstants.CacheName.SERVE_LIST, allEntries = true, beforeInvocation = true),
                    @CacheEvict(value = RedisConstants.CacheName.HOT_SERVE, allEntries = true, beforeInvocation = true)
            }
    )
    @Transactional
    public Serve onSale(Long id) {
        // 先获取要上架的id
        Serve serve = baseMapper.selectById(id);

        // 校验id是否为空
        if (ObjectUtil.isNull(serve)){
            throw new ForbiddenOperationException("区域服务不存在!");
        }
        // 上架状态
        Integer saleStatus = serve.getSaleStatus();
        // 状态要为草稿或者下架状态方可上架
        if (!(saleStatus == FoundationStatusEnum.INIT.getStatus() || saleStatus == FoundationStatusEnum.DISABLE.getStatus())){
            throw new ForbiddenOperationException("草稿和下架状态方可上架!");
        }
        // 服务项 ID
        Long serveItemId = serve.getServeItemId();
        // 拿出服务项
        ServeItem serveItem = serveItemMapper.selectById(serveItemId);
        if (ObjectUtil.isNull(serveItemId)){
            throw new ForbiddenOperationException("服务项不存在!");
        }

        // 服务项 状态
        Integer activeStatus = serveItem.getActiveStatus();
        if (!(activeStatus == FoundationStatusEnum.ENABLE.getStatus())){
            throw new ForbiddenOperationException("服务项为启用状态方可上架！");
        }

        // 更新上架状态
        boolean update = lambdaUpdate()
                .eq(Serve::getId, serve.getId())
                .set(Serve::getSaleStatus, FoundationStatusEnum.ENABLE.getStatus())
                .update();
        if (!update){
            throw new CommonException("启动服务失败！");
        }
        return baseMapper.selectById(id);
    }

    /**
     * 根据id删除区域服务
     *
     * @param id
     * @return
     */
    @Override
    public void deleteById(Long id) {

        int delete = getBaseMapper()
                .delete(Wrappers.<Serve>lambdaQuery()
                        .eq(Serve::getId, id)
                        .eq(Serve::getSaleStatus, FoundationStatusEnum.INIT.getStatus()));

        if (delete == 0){
            throw new ForbiddenOperationException("区域服务不为草稿状态删除失败!");
        }
    }

    /**
     * 下架
     *
     * @param id
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = RedisConstants.CacheName.SERVE, key = "#id", beforeInvocation = true),
            @CacheEvict(value = RedisConstants.CacheName.SERVE_ICON, allEntries = true, beforeInvocation = true),
            @CacheEvict(value = RedisConstants.CacheName.SERVE_TYPE, allEntries = true, beforeInvocation = true),
            @CacheEvict(value = RedisConstants.CacheName.SERVE_LIST, allEntries = true, beforeInvocation = true),
            @CacheEvict(value = RedisConstants.CacheName.HOT_SERVE, allEntries = true, beforeInvocation = true)
    })
    public void offsale(Long id) {
        // 服务状态要为非启用才能下架
        Serve serve = baseMapper.selectById(id);

        if (ObjectUtil.isNull(serve)){
            throw new ForbiddenOperationException("服务不存在!");
        }

        if (!(serve.getSaleStatus() == FoundationStatusEnum.ENABLE.getStatus())){
            throw new ForbiddenOperationException("服务状态启用中不能下架！");
        }

        // 更改服务状态为下架
        boolean update = lambdaUpdate()
                .eq(Serve::getId, id)
                .eq(Serve::getSaleStatus, FoundationStatusEnum.DISABLE.getStatus())
                .update();

        if (!update){
            throw new ForbiddenOperationException("服务下架失败!");
        }
        
    }

    /**
     * 区域服务设置热门
     *
     * @param id
     */
    @CacheEvict(value = RedisConstants.CacheName.HOT_SERVE, allEntries = true, beforeInvocation = true)
    @Override
    public void onHot(Long id) {
        boolean update = lambdaUpdate()
                .set(Serve::getIsHot, 1)
                .eq(Serve::getIsHot,0)
                .eq(Serve::getId, id)
                .update();
        if (!update){
            throw new ForbiddenOperationException("设置服务为热门失败! 该服务可能已为热门");
        }
    }

    /**
     * 区域服务取消热门
     *
     * @param id
     */
    @CacheEvict(value = RedisConstants.CacheName.HOT_SERVE, allEntries = true, beforeInvocation = true)
    @Override
    public void offHot(Long id) {
        boolean update = lambdaUpdate()
                .set(Serve::getIsHot, 0)
                .eq(Serve::getId, id)
                .update();
        if (!update){
            throw new ForbiddenOperationException("取消服务为热门失败! 该服务可能已取消热门");
        }
    }

    /**
     * 查询指定服务状态数量
     *
     * @param regionId
     * @param saleStatus
     */
    @Override
    public Long queryServeCountByRegionIdAndSaleStatus(Long regionId, Integer saleStatus) {
        return getBaseMapper().selectCount(Wrappers.<Serve>lambdaQuery()
                // 区域id
                .eq(Serve::getRegionId, regionId)
                // 状态
                .eq(Serve::getSaleStatus, saleStatus));
    }

    /**
     * 查询指定服务项状态数量
     *
     * @param serveItemId
     * @param saleStatus
     * @return
     */
    @Override
    public Long queryServeCountByServeItemIdAndSaleStatus(Long serveItemId, Integer saleStatus) {
        return getBaseMapper().selectCount(Wrappers.<Serve>lambdaQuery()
                .eq(Serve::getServeItemId, serveItemId)
                .eq(Serve::getSaleStatus, saleStatus));
    }

}
