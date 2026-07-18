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
import com.jzo2o.customer.mapper.AgencyCertificationMapper;
import com.jzo2o.customer.model.domain.AgencyCertification;
import com.jzo2o.customer.model.domain.AgencyCertificationAudit;
import com.jzo2o.customer.model.domain.ServeProvider;
import com.jzo2o.customer.model.dto.AgencyCertificationUpdateDTO;
import com.jzo2o.customer.model.dto.request.AgencyCertificationAuditAddReqDTO;
import com.jzo2o.customer.model.dto.response.RejectReasonResDTO;
import com.jzo2o.customer.service.IAgencyCertificationAuditService;
import com.jzo2o.customer.service.IAgencyCertificationService;
import com.jzo2o.customer.service.IServeProviderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * <p>
 * 机构认证信息表 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
@Service
public class AgencyCertificationServiceImpl extends ServiceImpl<AgencyCertificationMapper, AgencyCertification> implements IAgencyCertificationService {

    @Resource
    private IAgencyCertificationAuditService agencyCertificationAuditService;

    @Resource
    private IServeProviderService serveProviderService;


    /**
     * 机构认证申请
     *
     * @param agencyCertificationAuditAddReqDTO 机构认证申请模型
     */
    @Override
    @Transactional
    public void applyCertification(AgencyCertificationAuditAddReqDTO agencyCertificationAuditAddReqDTO) {
        // 校验当前账号是否存在
        Long serveProviderId = agencyCertificationAuditAddReqDTO.getServeProviderId();
        // 拿到机构信息
        ServeProvider serveProvider = serveProviderService.getById(serveProviderId);

        if (ObjectUtil.isEmpty(serveProvider)) {
            throw new BadRequestException("当前账号不存在");
        }
        // 当前账号 必须是 机构
        if (!ObjectUtil.equal(UserType.INSTITUTION, serveProvider.getType())) {
            throw new BadRequestException("只有机构账号才能提交机构认证");
        }

        // 查询当前机构信息 认证状态
        AgencyCertification existCertification = this.getById(serveProviderId);
        if (existCertification != null) {
            Integer certificationStatus = existCertification.getCertificationStatus();

            if (ObjectUtil.equal(CertificationStatusEnum.PROGRESSING.getStatus(), certificationStatus)) {
                throw new BadRequestException("当前机构正在认证中,请勿重复提交");
            }

            if (ObjectUtil.equal(CertificationStatusEnum.SUCCESS.getStatus(), certificationStatus)) {
                throw new BadRequestException("当前机构已认证成功,请勿重复提交");
            }
        }

        // 新增一条审核记录
        AgencyCertificationAudit agencyCertificationAudit = BeanUtil.toBean(agencyCertificationAuditAddReqDTO, AgencyCertificationAudit.class);
        // 设置默认值
        agencyCertificationAudit.setId(null);
        agencyCertificationAudit.setServeProviderId(serveProviderId);
        agencyCertificationAudit.setAuditStatus(0);
        agencyCertificationAudit.setCertificationStatus(CertificationStatusEnum.PROGRESSING.getStatus());
        agencyCertificationAuditService.save(agencyCertificationAudit);

        // 更新机构认证信息
        AgencyCertification agencyCertification = new AgencyCertification();
        agencyCertification.setId(serveProviderId);
        agencyCertification.setCertificationStatus(CertificationStatusEnum.PROGRESSING.getStatus());

        this.saveOrUpdate(agencyCertification);

    }

    /**
     * 查询当前用户最近一次的拒绝原因
     *
     * @return 拒绝原因
     */
    /**
     * 查询当前用户最近一次被拒绝的原因
     *
     * @return 拒绝原因
     */
    @Override
    public RejectReasonResDTO queryCurrentUserLastRejectReason() {
        LambdaQueryWrapper<AgencyCertificationAudit> queryWrapper =
                Wrappers.<AgencyCertificationAudit>lambdaQuery()
                        .eq(AgencyCertificationAudit::getServeProviderId, UserContext.currentUserId())
                        .eq(AgencyCertificationAudit::getCertificationStatus, CertificationStatusEnum.FAIL.getStatus())
                        .eq(AgencyCertificationAudit::getAuditStatus, 1)
                        .orderByDesc(AgencyCertificationAudit::getCreateTime)
                        .last("limit 1");

        AgencyCertificationAudit agencyCertificationAudit = agencyCertificationAuditService.getOne(queryWrapper);
        if (agencyCertificationAudit == null) {
            return new RejectReasonResDTO(null);
        }

        return new RejectReasonResDTO(agencyCertificationAudit.getRejectReason());
    }

    /**
     * 根据机构id更新
     *
     * @param agencyCertificationUpdateDTO 机构认证更新模型
     */
    @Override
    public void updateByServeProviderId(AgencyCertificationUpdateDTO agencyCertificationUpdateDTO) {
        LambdaUpdateWrapper<AgencyCertification> updateWrapper = Wrappers.<AgencyCertification>lambdaUpdate()
                .eq(AgencyCertification::getId, agencyCertificationUpdateDTO.getId())
                .set(AgencyCertification::getCertificationStatus, agencyCertificationUpdateDTO.getCertificationStatus())
                .set(ObjectUtil.isNotEmpty(agencyCertificationUpdateDTO.getName()), AgencyCertification::getName, agencyCertificationUpdateDTO.getName())
                .set(ObjectUtil.isNotEmpty(agencyCertificationUpdateDTO.getIdNumber()), AgencyCertification::getIdNumber, agencyCertificationUpdateDTO.getIdNumber())
                .set(ObjectUtil.isNotEmpty(agencyCertificationUpdateDTO.getLegalPersonName()), AgencyCertification::getLegalPersonName, agencyCertificationUpdateDTO.getLegalPersonName())
                .set(ObjectUtil.isNotEmpty(agencyCertificationUpdateDTO.getLegalPersonIdCardNo()), AgencyCertification::getLegalPersonIdCardNo, agencyCertificationUpdateDTO.getLegalPersonIdCardNo())
                .set(ObjectUtil.isNotEmpty(agencyCertificationUpdateDTO.getBusinessLicense()), AgencyCertification::getBusinessLicense, agencyCertificationUpdateDTO.getBusinessLicense())
                .set(ObjectUtil.isNotEmpty(agencyCertificationUpdateDTO.getCertificationTime()), AgencyCertification::getCertificationTime, agencyCertificationUpdateDTO.getCertificationTime());
        super.update(updateWrapper);
    }


}
