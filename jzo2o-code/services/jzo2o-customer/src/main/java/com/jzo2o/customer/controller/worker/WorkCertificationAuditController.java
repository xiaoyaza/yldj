package com.jzo2o.customer.controller.worker;


import com.jzo2o.common.utils.UserContext;
import com.jzo2o.customer.model.dto.request.WorkerCertificationAuditAddReqDTO;
import com.jzo2o.customer.model.dto.response.RejectReasonResDTO;
import com.jzo2o.customer.service.IWorkerCertificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("workerCertificationAuditController")
@RequestMapping("/worker/worker-certification-audit")
@Api(tags = "服务端-服务认证审核接口")
public class WorkCertificationAuditController {

    @Resource
    private IWorkerCertificationService workerCertificationService;

    @PostMapping
    @ApiOperation("提交服务认证审核")
    public void ServiceCertification(@RequestBody @Validated WorkerCertificationAuditAddReqDTO workerCertificationAuditAddReqDTO){
        workerCertificationAuditAddReqDTO.setServeProviderId(UserContext.currentUserId());
        workerCertificationService.applyCertification(workerCertificationAuditAddReqDTO);
    }


    @GetMapping("/rejectReason")
    @ApiOperation("查询最新驳回原因")
    public RejectReasonResDTO queryCurrentUserLastRejectReason(){
        return workerCertificationService.queryCurrentUserLastRejectReason();
    }
}
