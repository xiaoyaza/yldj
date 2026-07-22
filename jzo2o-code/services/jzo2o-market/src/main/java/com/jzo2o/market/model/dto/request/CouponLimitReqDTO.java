package com.jzo2o.market.model.dto.request;

import lombok.Data;

@Data
public class CouponLimitReqDTO {
    //状态
    private Integer status;
    //上一次查询的最后一张优惠券ID
    private Long lastId;
    //用户ID
    private Long userId;
}