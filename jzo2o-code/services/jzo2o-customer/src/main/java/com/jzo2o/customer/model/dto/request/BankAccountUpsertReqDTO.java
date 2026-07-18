package com.jzo2o.customer.model.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * <p>
 * 银行账户新增或更新请求体
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
@Data
@ApiModel("银行账户新增或更新请求体")
public class BankAccountUpsertReqDTO implements Serializable {

    /**
     * 服务人员/机构id
     */
    @ApiModelProperty(value = "服务人员/机构id", required = false)
    private Long id;

    /**
     * 类型，2：服务人员，3：服务机构
     */
    @ApiModelProperty(value = "类型，2：服务人员，3：服务机构", required = false)
    private Integer type;

    /**
     * 户名
     */
    @ApiModelProperty(value = "户名", required = true)
    @NotBlank(message = "户名不能为空")
    @Size(max = 50, message = "户名长度不能超过50")
    private String name;

    /**
     * 银行名称
     */
    @ApiModelProperty(value = "银行名称", required = true)
    @NotBlank(message = "银行名称不能为空")
    @Size(max = 50, message = "银行名称长度不能超过50")
    private String bankName;

    /**
     * 省
     */
    @ApiModelProperty(value = "省", required = true)
    @NotBlank(message = "省不能为空")
    @Size(max = 50, message = "省长度不能超过50")
    private String province;

    /**
     * 市
     */
    @ApiModelProperty(value = "市", required = true)
    @NotBlank(message = "市不能为空")
    @Size(max = 50, message = "市长度不能超过50")
    private String city;

    /**
     * 区
     */
    @ApiModelProperty(value = "区", required = true)
    @NotBlank(message = "区不能为空")
    @Size(max = 50, message = "区长度不能超过50")
    private String district;

    /**
     * 网点
     */
    @ApiModelProperty(value = "网点", required = true)
    @NotBlank(message = "网点不能为空")
    @Size(max = 50, message = "网点长度不能超过50")
    private String branch;

    /**
     * 银行账号
     */
    @ApiModelProperty(value = "银行账号", required = true)
    @NotBlank(message = "银行账号不能为空")
    @Size(max = 50, message = "银行账号长度不能超过50")
    private String account;

    /**
     * 开户证明
     */
    @ApiModelProperty(value = "开户证明", required = true)
    @NotBlank(message = "开户证明不能为空")
    @Size(max = 100, message = "开户证明长度不能超过100")
    private String accountCertification;
}
