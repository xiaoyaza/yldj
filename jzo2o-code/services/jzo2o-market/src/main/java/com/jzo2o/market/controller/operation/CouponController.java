package com.jzo2o.market.controller.operation;

import com.jzo2o.common.model.PageResult;
import com.jzo2o.market.model.dto.request.CouponOperationPageQueryReqDTO;
import com.jzo2o.market.model.dto.response.CouponInfoResDTO;
import com.jzo2o.market.service.ICouponService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("operationCouponController")
@RequestMapping("/operation/coupon")
public class CouponController {

    @Resource
    private ICouponService couponService;

    /**
     * 分页查询领取列表
     */
    @GetMapping("/page")
    public PageResult<CouponInfoResDTO> page(CouponOperationPageQueryReqDTO couponOperationPageQueryReqDTO) {
        return couponService.pageList(couponOperationPageQueryReqDTO);
    }
}