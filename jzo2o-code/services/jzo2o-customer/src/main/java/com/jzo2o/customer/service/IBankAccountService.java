package com.jzo2o.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.customer.model.domain.BankAccount;
import com.jzo2o.customer.model.dto.request.BankAccountUpsertReqDTO;
import com.jzo2o.customer.model.dto.response.BankAccountResDTO;

/**
 * <p>
 * 银行账户 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
public interface IBankAccountService extends IService<BankAccount> {
    /**
     * 新增或更新当前用户银行账户
     *
     * @param bankAccountUpsertReqDTO 银行账户新增或更新请求
     * @param userType 用户类型
     */
    void upsertCurrentUserBankAccount(BankAccountUpsertReqDTO bankAccountUpsertReqDTO, Integer userType);

    /**
     * 查询当前用户银行账户
     *
     * @return 银行账户信息
     */
    BankAccountResDTO findCurrentUserBankAccount();

    /**
     * 根据服务人员/机构id查询银行账户
     *
     * @param id 服务人员/机构id
     * @return 银行账户信息
     */
    BankAccountResDTO findByServeProviderId(Long id);
}
