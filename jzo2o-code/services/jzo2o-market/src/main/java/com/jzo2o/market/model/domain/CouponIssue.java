package com.jzo2o.market.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 待发放优惠券记录表
 * </p>
 *
 * @author itcast
 * @since 2024-09-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class CouponIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.NONE)
    private Long id;

    /**
     * 优惠券活动id
     */
    private Long activityId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 发放状态，0：待发放，1：已发放
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    private Integer isDeleted;

}
