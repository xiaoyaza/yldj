package com.jzo2o.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jzo2o.customer.model.domain.WorkerCertificationAudit;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 服务人员认证审核表 Mapper 接口
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
@Mapper
public interface WorkerCertificationAuditMapper extends BaseMapper<WorkerCertificationAudit> {
}
