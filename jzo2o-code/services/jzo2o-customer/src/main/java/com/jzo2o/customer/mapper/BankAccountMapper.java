package com.jzo2o.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jzo2o.customer.model.domain.BankAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 银行账户 Mapper 接口
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
@Mapper
public interface BankAccountMapper extends BaseMapper<BankAccount> {
}
