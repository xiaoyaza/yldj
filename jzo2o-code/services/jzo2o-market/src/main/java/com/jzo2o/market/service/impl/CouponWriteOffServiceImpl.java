package com.jzo2o.market.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.market.mapper.CouponWriteOffMapper;
import com.jzo2o.market.model.domain.CouponWriteOff;
import com.jzo2o.market.service.ICouponWriteOffService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 优惠券核销表 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-09-22
 */
@Service
public class CouponWriteOffServiceImpl extends ServiceImpl<CouponWriteOffMapper, CouponWriteOff> implements ICouponWriteOffService {

    @Override
    public CouponWriteOff queryByUserIdIdAndOrdersId(Long userId, Long ordersId) {
        return getBaseMapper().selectOne(
                Wrappers.<CouponWriteOff>lambdaQuery()
                        .eq(CouponWriteOff::getUserId, userId)
                        .eq(CouponWriteOff::getOrdersId, ordersId));
    }
}
