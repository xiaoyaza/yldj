package com.jzo2o.customer.controller.open;

import com.jzo2o.customer.model.dto.request.InstitutionRegisterReqDTO;
import com.jzo2o.customer.model.dto.request.InstitutionResetPasswordReqDTO;
import com.jzo2o.customer.service.IServeProviderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("openServeProviderController")
@RequestMapping("/open/serve-provider")
@Api(tags = "白名单接口 - 服务人员或机构相关接口")
public class ServeProviderContorller {

    @Resource
    private IServeProviderService serveProviderService;

    @PostMapping("/institution/register")
    @ApiOperation("机构端注册服务")
    public void InstitutionalRegister(@RequestBody InstitutionRegisterReqDTO institutionRegisterReqDTO) {
        serveProviderService.registerServeProvider(institutionRegisterReqDTO);
    }

    @PostMapping("/institution/resetPassword")
    @ApiOperation("机构端重置密码")
    public void InstitutionalResetPassword(@RequestBody @Validated InstitutionResetPasswordReqDTO institutionResetPasswordReqDTO) {
        serveProviderService.registerPassword(institutionResetPasswordReqDTO);
    }


}
