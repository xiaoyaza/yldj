package com.jzo2o.market.controller.inner;

import com.jzo2o.api.market.dto.request.CouponUseBackReqDTO;
import com.jzo2o.api.market.dto.request.CouponUseReqDTO;
import com.jzo2o.api.market.dto.response.AvailableCouponsResDTO;
import com.jzo2o.api.market.dto.response.CouponUseResDTO;
import com.jzo2o.market.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@RestController("innerCouponController")
@RequestMapping("/inner/coupon")
@Api(tags = "内部接口-优惠券相关接口")
public class CouponController {

    @Resource
    private ICouponService couponService;

    @GetMapping("/getAvailable")
    @ApiOperation("获取可用优惠券列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "totalAmount", value = "总金额，单位分", required = true, dataTypeClass = BigDecimal.class),
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataTypeClass = Long.class)
    })
    public List<AvailableCouponsResDTO> getAvailable(@RequestParam("userId") Long userId, @RequestParam("totalAmount") BigDecimal totalAmount) {
        return couponService.getAvailable(userId, totalAmount);
    }

    @PostMapping("/use")
    @ApiOperation("使用优惠券，并返回优惠金额")
    public CouponUseResDTO use(@RequestBody CouponUseReqDTO couponUseReqDTO) {
        CouponUseResDTO use = couponService.use(couponUseReqDTO);
        return use;
    }

    @PostMapping("/useBack")
    @ApiOperation("优惠券退回接口")
    public void useBack(@RequestBody CouponUseBackReqDTO couponUseBackReqDTO) {
        couponService.useBack(couponUseBackReqDTO);
    }
}
