package com.jzo2o.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.customer.model.domain.WorkerCertification;
import com.jzo2o.customer.model.dto.WorkerCertificationUpdateDTO;
import com.jzo2o.customer.model.dto.request.WorkerCertificationAuditAddReqDTO;
import com.jzo2o.customer.model.dto.response.RejectReasonResDTO;

/**
 * <p>
 * 服务人员认证信息表 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
public interface IWorkerCertificationService extends IService<WorkerCertification> {



    /**
     * 根据服务人员id更新
     *
     * @param workerCertificationUpdateDTO 服务人员认证更新模型
     */
    void updateById(WorkerCertificationUpdateDTO workerCertificationUpdateDTO);

    /**
     * 申请认证
     *
     * @param workerCertificationAuditAddReqDTO 申请认证模型
     */
    void applyCertification(WorkerCertificationAuditAddReqDTO workerCertificationAuditAddReqDTO);

    /**
     * 查询当前用户最近一次被拒绝的原因
     *
     * @return 拒绝原因
     */
    RejectReasonResDTO queryCurrentUserLastRejectReason();
}
