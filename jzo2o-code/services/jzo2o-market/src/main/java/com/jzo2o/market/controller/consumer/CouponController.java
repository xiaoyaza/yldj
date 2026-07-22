package com.jzo2o.market.controller.consumer;

import com.jzo2o.common.model.CurrentUserInfo;
import com.jzo2o.common.utils.UserContext;
import com.jzo2o.market.model.dto.request.CouponLimitReqDTO;
import com.jzo2o.market.model.dto.response.CouponInfoResDTO;
import com.jzo2o.market.service.ICouponService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController("consumerCouponController")
@RequestMapping("/consumer/coupon")
public class CouponController {

    @Resource
    private ICouponService couponService;

    /**
     * 我的优惠券列表
     */
    @GetMapping("/my")
    public List<CouponInfoResDTO> myCoupons(CouponLimitReqDTO couponLimitReqDTO){
        CurrentUserInfo currentUserInfo = UserContext.currentUser();
        couponLimitReqDTO.setUserId(currentUserInfo.getId());
        return couponService.myCoupons(couponLimitReqDTO);
    }
}