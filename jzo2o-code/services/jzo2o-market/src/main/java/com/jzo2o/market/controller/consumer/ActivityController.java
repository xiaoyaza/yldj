package com.jzo2o.market.controller.consumer;

import com.jzo2o.market.model.dto.response.SeizeCouponInfoResDTO;
import com.jzo2o.market.service.IActivityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController("consumerActivityController")
@RequestMapping("/consumer/activity")
public class ActivityController {

    @Resource
    private IActivityService activityService;

    /**
     * 小程序抢券活动列表
     * tabType: 1 抢券中, 2 即将开始
     */
    @GetMapping("/list")
    public List<SeizeCouponInfoResDTO> list(@RequestParam("tabType") Integer tabType) {
        return activityService.listForConsumer(tabType);
    }
}
