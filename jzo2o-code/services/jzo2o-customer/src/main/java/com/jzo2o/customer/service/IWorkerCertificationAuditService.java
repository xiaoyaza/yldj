package com.jzo2o.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.customer.model.domain.WorkerCertificationAudit;
import com.jzo2o.customer.model.dto.request.CertificationAuditReqDTO;
import com.jzo2o.customer.model.dto.request.WorkerCertificationAuditPageQueryReqDTO;
import com.jzo2o.customer.model.dto.response.WorkerCertificationAuditResDTO;

/**
 * <p>
 * 服务人员认证审核表 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
public interface IWorkerCertificationAuditService extends IService<WorkerCertificationAudit> {
    /**
     * 分页查询服务人员认证审核记录
     *
     * @param workerCertificationAuditPageQueryReqDTO 分页查询条件
     * @return 分页结果
     */
    PageResult<WorkerCertificationAuditResDTO> pageQuery(WorkerCertificationAuditPageQueryReqDTO workerCertificationAuditPageQueryReqDTO);

    /**
     * 审核服务人员认证申请
     *
     * @param id 认证申请id
     * @param certificationAuditReqDTO 审核请求
     */
    void audit(Long id, CertificationAuditReqDTO certificationAuditReqDTO);
}
