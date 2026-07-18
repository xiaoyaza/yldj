package com.jzo2o.customer.model.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 机构认证申请
 *
 * @author itcast
 * @create 2023/9/6 09:30
 **/
@Data
@ApiModel("机构认证申请")
public class AgencyCertificationAuditAddReqDTO {
    @ApiModelProperty(value = "机构id", required = false)
    private Long serveProviderId;

    @ApiModelProperty(value = "企业名称", required = true)
    @Size(max = 50, message = "企业名称长度不能超过50")
    private String name;

    @ApiModelProperty(value = "统一社会信用代码", required = true)
    @Size(max = 50, message = "统一社会信用代码长度不能超过50")
    private String idNumber;

    @ApiModelProperty(value = "法人姓名", required = true)
    @Size(max = 50, message = "法人姓名长度不能超过50")
    private String legalPersonName;

    @ApiModelProperty(value = "法人身份证号", required = true)
    @Size(max = 50, message = "法人身份证号长度不能超过50")
    private String legalPersonIdCardNo;

    @ApiModelProperty(value = "营业执照", required = true)
    @Size(max = 255, message = "营业执照地址长度不能超过255")
    private String businessLicense;
}
