package com.jzo2o.orders.manager.service.impl;

import com.jzo2o.es.core.ElasticSearchTemplate;
import com.jzo2o.orders.base.constants.EsIndexConstants;
import com.jzo2o.orders.base.mapper.OrdersDispatchMapper;
import com.jzo2o.orders.base.mapper.OrdersSeizeMapper;
import com.jzo2o.orders.base.utils.RedisUtils;
import com.jzo2o.orders.manager.service.ISeizeDispatchService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

import static com.jzo2o.orders.base.constants.RedisConstants.RedisKey.ORDERS_RESOURCE_STOCK;

/**
 * 抢派单数据清理服务实现。
 */
@Service
public class SeizeDispatchServiceImpl implements ISeizeDispatchService {

    @Resource
    private OrdersSeizeMapper ordersSeizeMapper;

    @Resource
    private OrdersDispatchMapper ordersDispatchMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ElasticSearchTemplate elasticSearchTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearSeizeDispatchPool(String cityCode, Long ordersId) {
        ordersDispatchMapper.deleteById(ordersId);
        ordersSeizeMapper.deleteById(ordersId);

        if (cityCode != null) {
            String resourceStockKey = String.format(ORDERS_RESOURCE_STOCK, RedisUtils.getCityIndex(cityCode));
            redisTemplate.opsForHash().delete(resourceStockKey, ordersId);
        }
        elasticSearchTemplate.opsForDoc()
                .batchDelete(EsIndexConstants.ORDERS_SEIZE, Collections.singletonList(ordersId));
    }
}
