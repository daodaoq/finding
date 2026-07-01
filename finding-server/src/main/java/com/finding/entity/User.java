package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String nickname;
    private String avatar;
    private Integer gender;         // 0=unknown, 1=male, 2=female
    private LocalDate birthday;
    private String school;
    private String studentId;
    private String signature;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String role;            // user / admin
    private Integer status;         // 0=banned, 1=active, 2=frozen
    private Integer realNameVerified; // 0=no, 1=pending, 2=approved, 3=rejected
    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
