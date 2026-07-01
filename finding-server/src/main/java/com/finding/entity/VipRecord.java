package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("vip_record")
public class VipRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer level;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amount;
    private String paymentMethod;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
