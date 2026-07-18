package com.jzo2o.customer.model.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 服务人员申请资质认证
 *
 * @author itcast
 * @create 2023/9/6 10:24
 **/
@Data
@ApiModel("服务人员认证申请请求体")
public class WorkerCertificationAuditAddReqDTO {
    @ApiModelProperty(value = "服务人员id", required = false)
    private Long serveProviderId;

    @ApiModelProperty(value = "姓名", required = true)
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50")
    private String name;

    @ApiModelProperty(value = "身份证号", required = true)
    @NotBlank(message = "身份证号不能为空")
    @Size(max = 50, message = "身份证号长度不能超过50")
    private String idCardNo;

    @ApiModelProperty(value = "身份证正面", required = true)
    @NotBlank(message = "身份证正面不能为空")
    @Size(max = 100, message = "身份证正面地址长度不能超过100")
    private String frontImg;

    @ApiModelProperty(value = "身份证反面", required = true)
    @NotBlank(message = "身份证反面不能为空")
    @Size(max = 100, message = "身份证反面地址长度不能超过100")
    private String backImg;

    @ApiModelProperty(value = "证明资料", required = true)
    @NotBlank(message = "证明资料不能为空")
    @Size(max = 100, message = "证明资料地址长度不能超过100")
    private String certificationMaterial;
}
