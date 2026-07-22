package com.jzo2o.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.market.model.domain.CouponIssue;
import com.jzo2o.market.model.dto.request.CouponIssueReqDTO;

import java.util.List;

/**
 * <p>
 * 待发放优惠券记录表 服务类
 * </p>
 *
 * @author itcast
 * @since 2024-09-23
 */
public interface ICouponIssueService extends IService<CouponIssue> {

    /**
     * 提交待发放优惠券
     *
     * @param couponIssueReqDTO 提交参数
     * @return 提交成功的待发放记录
     */
    List<CouponIssue> save(CouponIssueReqDTO couponIssueReqDTO);

    /**
     * 立即发放优惠券
     *
     * @param couponIssueReqDTO 发放参数
     * @return 发放成功的待发放记录
     */
    List<CouponIssue> issue(CouponIssueReqDTO couponIssueReqDTO);

    /**
     * 自动发放优惠券（按活动id分批）
     *
     * @param activityId 活动id
     */
    void autoIssue(Long activityId);
}
