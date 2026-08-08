package com.finding.user.vo;

import com.finding.user.entity.UserResume;
import lombok.Data;

/**
 * 查看他人情感简历的返回:未互换时仅返回锁定状态,不返回内容。
 */
@Data
public class ResumeViewVO {

    /** 是否已互换信息(可查看对方简历) */
    private Boolean infoShared;

    /** 互换状态:0=无 1=待处理 2=已互换 3=已拒绝 */
    private Integer shareStatus;

    /** 对方情感简历(null = 未解锁 或 对方未填写) */
    private UserResume resume;
}
