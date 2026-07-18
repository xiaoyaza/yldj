package com.jzo2o.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.common.constants.UserType;
import com.jzo2o.common.expcetions.BadRequestException;
import com.jzo2o.common.utils.UserContext;
import com.jzo2o.customer.enums.CertificationStatusEnum;
import com.jzo2o.customer.mapper.WorkerCertificationMapper;
import com.jzo2o.customer.model.domain.ServeProvider;
import com.jzo2o.customer.model.domain.WorkerCertification;
import com.jzo2o.customer.model.domain.WorkerCertificationAudit;
import com.jzo2o.customer.model.dto.WorkerCertificationUpdateDTO;
import com.jzo2o.customer.model.dto.request.WorkerCertificationAuditAddReqDTO;
import com.jzo2o.customer.model.dto.response.RejectReasonResDTO;
import com.jzo2o.customer.service.IServeProviderService;
import com.jzo2o.customer.service.IWorkerCertificationAuditService;
import com.jzo2o.customer.service.IWorkerCertificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * <p>
 * 服务人员认证信息表 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
@Service
public class WorkerCertificationServiceImpl extends ServiceImpl<WorkerCertificationMapper, WorkerCertification> implements IWorkerCertificationService {


    @Resource
    private IWorkerCertificationAuditService workerCertificationAuditService;

    @Resource
    private IServeProviderService serveProviderService;

    /**
     * 申请认证
     *
     * @param workerCertificationAuditAddReqDTO 申请认证模型
     */
    @Override
    @Transactional
    public void applyCertification(WorkerCertificationAuditAddReqDTO workerCertificationAuditAddReqDTO) {
        // 校验当前服务人员是否存在
        Long serveProviderId = workerCertificationAuditAddReqDTO.getServeProviderId();
        if (serveProviderId == null) {
            throw new BadRequestException("服务人员id不能为空");
        }

        // 当前申请必须是服务人员
        ServeProvider serveProvider = serveProviderService.getById(serveProviderId);
        if (serveProvider == null) {
            throw new BadRequestException("账号不存在");
        }
        if (!ObjectUtil.equal(UserType.WORKER, serveProvider.getType())) {
            throw new BadRequestException("当前用户不是服务人员");
        }

        // 查询当前服务人员申请认证状态
        WorkerCertification existCertification = this.getById(serveProviderId);
        if (existCertification != null) {
            Integer certificationStatus = existCertification.getCertificationStatus();

            if (ObjectUtil.equal(certificationStatus, CertificationStatusEnum.PROGRESSING.getStatus())) {
                throw new BadRequestException("当前服务人员正在认证中,请勿重复提交");
            }

            if (ObjectUtil.equal(certificationStatus, CertificationStatusEnum.SUCCESS.getStatus())) {
                throw new BadRequestException("当前服务人员已经认证成功,请勿重复提交");
            }
        }

        // 新增一条服务人员提交认证记录
        WorkerCertificationAudit workerCertificationAudit = BeanUtil.toBean(workerCertificationAuditAddReqDTO, WorkerCertificationAudit.class);
        workerCertificationAudit.setId(null);
        workerCertificationAudit.setServeProviderId(serveProviderId);
        workerCertificationAudit.setAuditStatus(0);
        workerCertificationAudit.setCertificationStatus(CertificationStatusEnum.PROGRESSING.getStatus());
        workerCertificationAuditService.save(workerCertificationAudit);

        // 更新服务人员认证信息为认证中
        WorkerCertification workerCertification = new WorkerCertification();
        workerCertification.setId(serveProviderId);
        workerCertification.setCertificationStatus(CertificationStatusEnum.PROGRESSING.getStatus());

        this.saveOrUpdate(workerCertification);
    }

    /**
     * 查询当前用户最近一次被拒绝的原因
     *
     * @return 拒绝原因
     */
    @Override
    public RejectReasonResDTO queryCurrentUserLastRejectReason() {
        LambdaQueryWrapper<WorkerCertificationAudit> queryWrapper =
                Wrappers.<WorkerCertificationAudit>lambdaQuery()
                        .eq(WorkerCertificationAudit::getServeProviderId, UserContext.currentUserId())
                        .eq(WorkerCertificationAudit::getCertificationStatus, CertificationStatusEnum.FAIL.getStatus())
                        .orderByDesc(WorkerCertificationAudit::getCreateTime)
                        .last("limit 1");

        WorkerCertificationAudit workerCertificationAudit = workerCertificationAuditService.getOne(queryWrapper);
        if (workerCertificationAudit == null) {
            return new RejectReasonResDTO(null);
        }

        return new RejectReasonResDTO(workerCertificationAudit.getRejectReason());
    }

    /**
     * 根据服务人员id更新
     *
     * @param workerCertificationUpdateDTO 服务人员认证更新模型
     */
    @Override
    public void updateById(WorkerCertificationUpdateDTO workerCertificationUpdateDTO) {
        LambdaUpdateWrapper<WorkerCertification> updateWrapper = Wrappers.<WorkerCertification>lambdaUpdate()
                .eq(WorkerCertification::getId, workerCertificationUpdateDTO.getId())
                .set(WorkerCertification::getCertificationStatus, workerCertificationUpdateDTO.getCertificationStatus())
                .set(ObjectUtil.isNotEmpty(workerCertificationUpdateDTO.getName()), WorkerCertification::getName, workerCertificationUpdateDTO.getName())
                .set(ObjectUtil.isNotEmpty(workerCertificationUpdateDTO.getIdCardNo()), WorkerCertification::getIdCardNo, workerCertificationUpdateDTO.getIdCardNo())
                .set(ObjectUtil.isNotEmpty(workerCertificationUpdateDTO.getFrontImg()), WorkerCertification::getFrontImg, workerCertificationUpdateDTO.getFrontImg())
                .set(ObjectUtil.isNotEmpty(workerCertificationUpdateDTO.getBackImg()), WorkerCertification::getBackImg, workerCertificationUpdateDTO.getBackImg())
                .set(ObjectUtil.isNotEmpty(workerCertificationUpdateDTO.getCertificationMaterial()), WorkerCertification::getCertificationMaterial, workerCertificationUpdateDTO.getCertificationMaterial())
                .set(ObjectUtil.isNotEmpty(workerCertificationUpdateDTO.getCertificationTime()), WorkerCertification::getCertificationTime, workerCertificationUpdateDTO.getCertificationTime());
        super.update(updateWrapper);
    }


}
