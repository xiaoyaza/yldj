package com.jzo2o.customer.controller.operation;

import com.jzo2o.common.model.PageResult;
import com.jzo2o.customer.model.dto.request.AgencyCertificationAuditPageQueryReqDTO;
import com.jzo2o.customer.model.dto.request.CertificationAuditReqDTO;
import com.jzo2o.customer.model.dto.request.WorkerCertificationAuditPageQueryReqDTO;
import com.jzo2o.customer.model.dto.response.AgencyCertificationAuditResDTO;
import com.jzo2o.customer.model.dto.response.WorkerCertificationAuditResDTO;
import com.jzo2o.customer.service.IAgencyCertificationAuditService;
import com.jzo2o.customer.service.IWorkerCertificationAuditService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/operation")
@Api(tags = "运营端 - 认证审核相关接口")
public class OperationAllController {

    @Resource
    private IWorkerCertificationAuditService workerCertificationAuditService;

    @Resource
    private IAgencyCertificationAuditService agencyCertificationAuditService;

    @GetMapping("/worker-certification-audit/page")
    @ApiOperation("分页查询服务人员认证审核记录")
    public PageResult<WorkerCertificationAuditResDTO> pageQueryWorkerCertificationAudit(WorkerCertificationAuditPageQueryReqDTO workerCertificationAuditPageQueryReqDTO) {
        return workerCertificationAuditService.pageQuery(workerCertificationAuditPageQueryReqDTO);
    }

    @PutMapping("/worker-certification-audit/audit/{id}")
    @ApiOperation("审核服务人员认证信息")
    public void auditWorkerCertification(@PathVariable("id") Long id, CertificationAuditReqDTO certificationAuditReqDTO) {
        workerCertificationAuditService.audit(id, certificationAuditReqDTO);
    }

    @GetMapping("/agency-certification-audit/page")
    @ApiOperation("分页查询机构认证审核记录")
    public PageResult<AgencyCertificationAuditResDTO> pageQueryAgencyCertificationAudit(AgencyCertificationAuditPageQueryReqDTO agencyCertificationAuditPageQueryReqDTO) {
        return agencyCertificationAuditService.pageQuery(agencyCertificationAuditPageQueryReqDTO);
    }

    @PutMapping("/agency-certification-audit/audit/{id}")
    @ApiOperation("审核机构认证信息")
    public void auditAgencyCertification(@PathVariable("id") Long id, CertificationAuditReqDTO certificationAuditReqDTO) {
        agencyCertificationAuditService.audit(id, certificationAuditReqDTO);
    }
}
