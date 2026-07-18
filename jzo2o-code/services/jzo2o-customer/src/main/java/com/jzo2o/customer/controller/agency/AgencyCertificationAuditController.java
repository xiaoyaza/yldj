package com.jzo2o.customer.controller.agency;


import com.jzo2o.common.utils.UserContext;
import com.jzo2o.customer.model.dto.AgencyCertificationUpdateDTO;
import com.jzo2o.customer.model.dto.request.AgencyCertificationAuditAddReqDTO;
import com.jzo2o.customer.model.dto.response.RejectReasonResDTO;
import com.jzo2o.customer.service.IAgencyCertificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("agencyCertificationAuditController")
@RequestMapping("/agency/agency-certification-audit")
@Api(tags = "机构端-机构认证审核接口")
public class AgencyCertificationAuditController {

    @Resource
    private IAgencyCertificationService agencyCertificationService;

    @PostMapping
    @ApiOperation("提交机构认证审核")
    public void audit(@RequestBody AgencyCertificationAuditAddReqDTO agencyCertificationAuditAddReqDTO) {
        agencyCertificationAuditAddReqDTO.setServeProviderId(UserContext.currentUserId());
        agencyCertificationService.applyCertification(agencyCertificationAuditAddReqDTO);
    }

    @GetMapping("/rejectReason")
    @ApiOperation("查询最新驳回原因")
    public RejectReasonResDTO queryCurrentUserLastRejectReason(){
        return agencyCertificationService.queryCurrentUserLastRejectReason();
    }
}
