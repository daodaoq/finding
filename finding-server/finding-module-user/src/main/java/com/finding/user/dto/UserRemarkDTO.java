package com.finding.user.dto;

import lombok.Data;

/** 设置备注请求体 */
@Data
public class UserRemarkDTO {

    /** 备注名(应用层最长 20 字);清除备注请走 DELETE 接口 */
    private String remark;
}
