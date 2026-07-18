package com.jzo2o.customer.model.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ApiModel("机构密码重置接口")
public class InstitutionResetPasswordReqDTO {
    @ApiModelProperty(value = "新密码",required = true)
    @NotBlank(message = "密码不能为空")
    @Size(max = 16, min = 8, message = "密码输入格式错误，请重新输入")
    private String password;

    @ApiModelProperty(value = "手机号",required = true)
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @ApiModelProperty(value = "短信验证码",required = true)
    @NotBlank(message = "验证码错误，请重新输入")
    private String verifyCode;

}
