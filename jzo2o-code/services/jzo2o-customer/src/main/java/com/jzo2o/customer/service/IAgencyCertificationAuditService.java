package com.jzo2o.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.customer.model.domain.AgencyCertificationAudit;
import com.jzo2o.customer.model.dto.request.AgencyCertificationAuditPageQueryReqDTO;
import com.jzo2o.customer.model.dto.request.CertificationAuditReqDTO;
import com.jzo2o.customer.model.dto.response.AgencyCertificationAuditResDTO;

/**
 * <p>
 * 机构认证审核表 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
public interface IAgencyCertificationAuditService extends IService<AgencyCertificationAudit> {
    /**
     * 分页查询机构认证审核记录
     *
     * @param agencyCertificationAuditPageQueryReqDTO 分页查询条件
     * @return 分页结果
     */
    PageResult<AgencyCertificationAuditResDTO> pageQuery(AgencyCertificationAuditPageQueryReqDTO agencyCertificationAuditPageQueryReqDTO);

    /**
     * 审核机构认证申请
     *
     * @param id 认证申请id
     * @param certificationAuditReqDTO 审核请求
     */
    void audit(Long id, CertificationAuditReqDTO certificationAuditReqDTO);
}
