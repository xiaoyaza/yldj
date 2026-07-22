package com.jzo2o.market.controller.operation;

import com.jzo2o.common.model.PageResult;
import com.jzo2o.market.model.dto.request.ActivityQueryForPageReqDTO;
import com.jzo2o.market.model.dto.request.ActivitySaveReqDTO;
import com.jzo2o.market.model.dto.response.ActivityInfoResDTO;
import com.jzo2o.market.service.IActivityService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("operationActivityController")
@RequestMapping("/operation/activity")
public class ActivityController {

    @Resource
    private IActivityService activityService;

    /***
     * 新增优惠券
     */
    @PostMapping("/save")
    public void save(@RequestBody ActivitySaveReqDTO activitySaveReqDTO){
        activityService.addActivity(activitySaveReqDTO);
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public PageResult<ActivityInfoResDTO> page(ActivityQueryForPageReqDTO activityQueryForPageReqDTO){
        return activityService.pageList(activityQueryForPageReqDTO);
    }


    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public ActivityInfoResDTO getDetail(@PathVariable("id")Long id){
        return activityService.getDetail(id);
    }

    /**
     * 撤销活动
     */
    @PostMapping("/revoke/{id}")
    public void revokeActivity(@PathVariable("id")Long id){
        activityService.revokeActivity(id);
    }

}
