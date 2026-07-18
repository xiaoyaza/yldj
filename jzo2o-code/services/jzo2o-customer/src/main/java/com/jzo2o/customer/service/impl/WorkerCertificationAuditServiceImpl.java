package com.jzo2o.customer.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.common.expcetions.BadRequestException;
import com.jzo2o.common.model.CurrentUserInfo;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.utils.UserContext;
import com.jzo2o.customer.enums.CertificationStatusEnum;
import com.jzo2o.customer.mapper.WorkerCertificationMapper;
import com.jzo2o.customer.mapper.WorkerCertificationAuditMapper;
import com.jzo2o.customer.model.domain.WorkerCertification;
import com.jzo2o.customer.model.domain.WorkerCertificationAudit;
import com.jzo2o.customer.model.dto.request.CertificationAuditReqDTO;
import com.jzo2o.customer.model.dto.request.WorkerCertificationAuditPageQueryReqDTO;
import com.jzo2o.customer.model.dto.response.WorkerCertificationAuditResDTO;
import com.jzo2o.customer.service.IWorkerCertificationAuditService;
import com.jzo2o.mysql.utils.PageUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务人员认证审核表 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
@Service
public class WorkerCertificationAuditServiceImpl extends ServiceImpl<WorkerCertificationAuditMapper, WorkerCertificationAudit> implements IWorkerCertificationAuditService {
    @Resource
    private WorkerCertificationMapper workerCertificationMapper;

    /**
     * 分页查询服务人员认证审核记录
     *
     * @param workerCertificationAuditPageQueryReqDTO 分页查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<WorkerCertificationAuditResDTO> pageQuery(WorkerCertificationAuditPageQueryReqDTO workerCertificationAuditPageQueryReqDTO) {
        Page<WorkerCertificationAudit> page = PageUtils.parsePageQuery(workerCertificationAuditPageQueryReqDTO, WorkerCertificationAudit.class);
        LambdaQueryWrapper<WorkerCertificationAudit> queryWrapper = Wrappers.<WorkerCertificationAudit>lambdaQuery()
                .like(ObjectUtil.isNotEmpty(workerCertificationAuditPageQueryReqDTO.getName()), WorkerCertificationAudit::getName, workerCertificationAuditPageQueryReqDTO.getName())
                .eq(ObjectUtil.isNotEmpty(workerCertificationAuditPageQueryReqDTO.getIdCardNo()), WorkerCertificationAudit::getIdCardNo, workerCertificationAuditPageQueryReqDTO.getIdCardNo())
                .eq(ObjectUtil.isNotEmpty(workerCertificationAuditPageQueryReqDTO.getAuditStatus()), WorkerCertificationAudit::getAuditStatus, workerCertificationAuditPageQueryReqDTO.getAuditStatus())
                .eq(ObjectUtil.isNotEmpty(workerCertificationAuditPageQueryReqDTO.getCertificationStatus()), WorkerCertificationAudit::getCertificationStatus, workerCertificationAuditPageQueryReqDTO.getCertificationStatus());
        Page<WorkerCertificationAudit> workerCertificationAuditPage = baseMapper.selectPage(page, queryWrapper);
        return PageUtils.toPage(workerCertificationAuditPage, WorkerCertificationAuditResDTO.class);
    }

    /**
     * 审核服务人员认证申请
     *
     * @param id 认证申请id
     * @param certificationAuditReqDTO 审核请求
     */
    @Override
    @Transactional
    public void audit(Long id, CertificationAuditReqDTO certificationAuditReqDTO) {
        Integer certificationStatus = certificationAuditReqDTO.getCertificationStatus();
        checkCertificationAuditStatus(certificationStatus, certificationAuditReqDTO.getRejectReason());

        WorkerCertificationAudit existAudit = baseMapper.selectById(id);
        if (existAudit == null) {
            throw new BadRequestException("认证申请不存在");
        }
        if (ObjectUtil.equal(existAudit.getAuditStatus(), 1)) {
            throw new BadRequestException("认证申请已审核，请勿重复审核");
        }

        LocalDateTime auditTime = LocalDateTime.now();
        CurrentUserInfo currentUser = UserContext.currentUser();

        WorkerCertificationAudit audit = new WorkerCertificationAudit();
        audit.setId(id);
        audit.setAuditStatus(1);
        audit.setAuditorId(currentUser.getId());
        audit.setAuditorName(currentUser.getName());
        audit.setAuditTime(auditTime);
        audit.setCertificationStatus(certificationStatus);
        audit.setRejectReason(ObjectUtil.equal(CertificationStatusEnum.FAIL.getStatus(), certificationStatus) ? certificationAuditReqDTO.getRejectReason() : null);
        baseMapper.updateById(audit);

        WorkerCertification workerCertification = new WorkerCertification();
        workerCertification.setId(existAudit.getServeProviderId());
        workerCertification.setCertificationStatus(certificationStatus);
        if (ObjectUtil.equal(CertificationStatusEnum.SUCCESS.getStatus(), certificationStatus)) {
            workerCertification.setName(existAudit.getName());
            workerCertification.setIdCardNo(existAudit.getIdCardNo());
            workerCertification.setFrontImg(existAudit.getFrontImg());
            workerCertification.setBackImg(existAudit.getBackImg());
            workerCertification.setCertificationMaterial(existAudit.getCertificationMaterial());
            workerCertification.setCertificationTime(auditTime);
        }

        if (workerCertificationMapper.selectById(existAudit.getServeProviderId()) == null) {
            workerCertificationMapper.insert(workerCertification);
        } else {
            workerCertificationMapper.updateById(workerCertification);
        }
    }

    private void checkCertificationAuditStatus(Integer certificationStatus, String rejectReason) {
        if (!ObjectUtil.equal(CertificationStatusEnum.SUCCESS.getStatus(), certificationStatus)
                && !ObjectUtil.equal(CertificationStatusEnum.FAIL.getStatus(), certificationStatus)) {
            throw new BadRequestException("认证状态错误");
        }
        if (ObjectUtil.equal(CertificationStatusEnum.FAIL.getStatus(), certificationStatus)
                && ObjectUtil.isEmpty(rejectReason)) {
            throw new BadRequestException("驳回原因不能为空");
        }
    }
}
