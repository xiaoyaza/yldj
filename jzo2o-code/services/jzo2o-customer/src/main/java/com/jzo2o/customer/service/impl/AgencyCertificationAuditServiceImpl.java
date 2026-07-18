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
import com.jzo2o.customer.mapper.AgencyCertificationAuditMapper;
import com.jzo2o.customer.mapper.AgencyCertificationMapper;
import com.jzo2o.customer.model.domain.AgencyCertification;
import com.jzo2o.customer.model.domain.AgencyCertificationAudit;
import com.jzo2o.customer.model.dto.request.AgencyCertificationAuditPageQueryReqDTO;
import com.jzo2o.customer.model.dto.request.CertificationAuditReqDTO;
import com.jzo2o.customer.model.dto.response.AgencyCertificationAuditResDTO;
import com.jzo2o.customer.service.IAgencyCertificationAuditService;
import com.jzo2o.mysql.utils.PageUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 机构认证审核表 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
@Service
public class AgencyCertificationAuditServiceImpl extends ServiceImpl<AgencyCertificationAuditMapper, AgencyCertificationAudit> implements IAgencyCertificationAuditService {
    @Resource
    private AgencyCertificationMapper agencyCertificationMapper;

    /**
     * 分页查询机构认证审核记录
     *
     * @param agencyCertificationAuditPageQueryReqDTO 分页查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<AgencyCertificationAuditResDTO> pageQuery(AgencyCertificationAuditPageQueryReqDTO agencyCertificationAuditPageQueryReqDTO) {
        Page<AgencyCertificationAudit> page = PageUtils.parsePageQuery(agencyCertificationAuditPageQueryReqDTO, AgencyCertificationAudit.class);
        LambdaQueryWrapper<AgencyCertificationAudit> queryWrapper = Wrappers.<AgencyCertificationAudit>lambdaQuery()
                .like(ObjectUtil.isNotEmpty(agencyCertificationAuditPageQueryReqDTO.getName()), AgencyCertificationAudit::getName, agencyCertificationAuditPageQueryReqDTO.getName())
                .eq(ObjectUtil.isNotEmpty(agencyCertificationAuditPageQueryReqDTO.getLegalPersonName()), AgencyCertificationAudit::getLegalPersonName, agencyCertificationAuditPageQueryReqDTO.getLegalPersonName())
                .eq(ObjectUtil.isNotEmpty(agencyCertificationAuditPageQueryReqDTO.getAuditStatus()), AgencyCertificationAudit::getAuditStatus, agencyCertificationAuditPageQueryReqDTO.getAuditStatus())
                .eq(ObjectUtil.isNotEmpty(agencyCertificationAuditPageQueryReqDTO.getCertificationStatus()), AgencyCertificationAudit::getCertificationStatus, agencyCertificationAuditPageQueryReqDTO.getCertificationStatus());
        Page<AgencyCertificationAudit> agencyCertificationAuditPage = baseMapper.selectPage(page, queryWrapper);
        return PageUtils.toPage(agencyCertificationAuditPage, AgencyCertificationAuditResDTO.class);
    }

    /**
     * 审核机构认证申请
     *
     * @param id 认证申请id
     * @param certificationAuditReqDTO 审核请求
     */
    @Override
    @Transactional
    public void audit(Long id, CertificationAuditReqDTO certificationAuditReqDTO) {
        Integer certificationStatus = certificationAuditReqDTO.getCertificationStatus();
        checkCertificationAuditStatus(certificationStatus, certificationAuditReqDTO.getRejectReason());

        AgencyCertificationAudit existAudit = baseMapper.selectById(id);
        if (existAudit == null) {
            throw new BadRequestException("认证申请不存在");
        }
        if (ObjectUtil.equal(existAudit.getAuditStatus(), 1)) {
            throw new BadRequestException("认证申请已审核，请勿重复审核");
        }

        LocalDateTime auditTime = LocalDateTime.now();
        CurrentUserInfo currentUser = UserContext.currentUser();

        AgencyCertificationAudit audit = new AgencyCertificationAudit();
        audit.setId(id);
        audit.setAuditStatus(1);
        audit.setAuditorId(currentUser.getId());
        audit.setAuditorName(currentUser.getName());
        audit.setAuditTime(auditTime);
        audit.setCertificationStatus(certificationStatus);
        audit.setRejectReason(ObjectUtil.equal(CertificationStatusEnum.FAIL.getStatus(), certificationStatus) ? certificationAuditReqDTO.getRejectReason() : null);
        baseMapper.updateById(audit);

        AgencyCertification agencyCertification = new AgencyCertification();
        agencyCertification.setId(existAudit.getServeProviderId());
        agencyCertification.setCertificationStatus(certificationStatus);
        if (ObjectUtil.equal(CertificationStatusEnum.SUCCESS.getStatus(), certificationStatus)) {
            agencyCertification.setName(existAudit.getName());
            agencyCertification.setIdNumber(existAudit.getIdNumber());
            agencyCertification.setLegalPersonName(existAudit.getLegalPersonName());
            agencyCertification.setLegalPersonIdCardNo(existAudit.getLegalPersonIdCardNo());
            agencyCertification.setBusinessLicense(existAudit.getBusinessLicense());
            agencyCertification.setCertificationTime(auditTime);
        }

        if (agencyCertificationMapper.selectById(existAudit.getServeProviderId()) == null) {
            agencyCertificationMapper.insert(agencyCertification);
        } else {
            agencyCertificationMapper.updateById(agencyCertification);
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
