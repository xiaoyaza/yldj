package com.jzo2o.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.common.expcetions.BadRequestException;
import com.jzo2o.common.utils.UserContext;
import com.jzo2o.customer.mapper.BankAccountMapper;
import com.jzo2o.customer.model.domain.BankAccount;
import com.jzo2o.customer.model.domain.ServeProvider;
import com.jzo2o.customer.model.dto.request.BankAccountUpsertReqDTO;
import com.jzo2o.customer.model.dto.response.BankAccountResDTO;
import com.jzo2o.customer.service.IBankAccountService;
import com.jzo2o.customer.service.IServeProviderService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 银行账户 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
@Service
public class BankAccountServiceImpl extends ServiceImpl<BankAccountMapper, BankAccount> implements IBankAccountService {
    @Resource
    private IServeProviderService serveProviderService;

    /**
     * 新增或更新当前用户银行账户
     *
     * @param bankAccountUpsertReqDTO 银行账户新增或更新请求
     * @param userType 用户类型
     */
    @Override
    public void upsertCurrentUserBankAccount(BankAccountUpsertReqDTO bankAccountUpsertReqDTO, Integer userType) {
        Long currentUserId = UserContext.currentUserId();
        ServeProvider serveProvider = serveProviderService.getById(currentUserId);
        if (serveProvider == null) {
            throw new BadRequestException("账号不存在");
        }
        if (!ObjectUtil.equal(userType, serveProvider.getType())) {
            throw new BadRequestException("当前用户类型不允许设置该银行账户");
        }

        BankAccount bankAccount = BeanUtil.toBean(bankAccountUpsertReqDTO, BankAccount.class);
        bankAccount.setId(currentUserId);
        bankAccount.setType(userType);
        this.saveOrUpdate(bankAccount);
    }

    /**
     * 查询当前用户银行账户
     *
     * @return 银行账户信息
     */
    @Override
    public BankAccountResDTO findCurrentUserBankAccount() {
        return findByServeProviderId(UserContext.currentUserId());
    }

    /**
     * 根据服务人员/机构id查询银行账户
     *
     * @param id 服务人员/机构id
     * @return 银行账户信息
     */
    @Override
    public BankAccountResDTO findByServeProviderId(Long id) {
        BankAccount bankAccount = this.getById(id);
        return BeanUtil.toBean(bankAccount, BankAccountResDTO.class);
    }
}
