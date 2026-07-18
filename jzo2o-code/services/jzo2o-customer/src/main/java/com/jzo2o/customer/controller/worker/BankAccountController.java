package com.jzo2o.customer.controller.worker;

import com.jzo2o.common.constants.UserType;
import com.jzo2o.customer.model.dto.request.BankAccountUpsertReqDTO;
import com.jzo2o.customer.model.dto.response.BankAccountResDTO;
import com.jzo2o.customer.service.IBankAccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author itcast
 */
@RestController("workerBankAccountController")
@RequestMapping("/worker/bank-account")
@Api(tags = "服务人员端 - 银行账户相关接口")
public class BankAccountController {
    @Resource
    private IBankAccountService bankAccountService;

    @PostMapping
    @ApiOperation("新增或更新银行账号信息")
    public void upsert(@RequestBody @Validated BankAccountUpsertReqDTO bankAccountUpsertReqDTO) {
        bankAccountService.upsertCurrentUserBankAccount(bankAccountUpsertReqDTO, UserType.WORKER);
    }

    @GetMapping("/currentUserBankAccount")
    @ApiOperation("获取当前用户银行账号")
    public BankAccountResDTO currentUserBankAccount() {
        return bankAccountService.findCurrentUserBankAccount();
    }
}
