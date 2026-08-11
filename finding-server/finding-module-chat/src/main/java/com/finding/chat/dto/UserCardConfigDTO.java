package com.finding.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import com.finding.chat.entity.UserCardConfig;

/**
 * 相识卡片展示项配置更新请求体。
 * 每个开关可空(空=保持默认开启),仅校验 0/1。
 */
@Data
public class UserCardConfigDTO {

    @Min(value = 0, message = "开关仅允许 0/1")
    @Max(value = 1, message = "开关仅允许 0/1")
    private Integer showPhoto;
    @Min(value = 0, message = "开关仅允许 0/1")
    @Max(value = 1, message = "开关仅允许 0/1")
    private Integer showNickname;
    @Min(value = 0, message = "开关仅允许 0/1")
    @Max(value = 1, message = "开关仅允许 0/1")
    private Integer showGender;
    @Min(value = 0, message = "开关仅允许 0/1")
    @Max(value = 1, message = "开关仅允许 0/1")
    private Integer showSchool;
    @Min(value = 0, message = "开关仅允许 0/1")
    @Max(value = 1, message = "开关仅允许 0/1")
    private Integer showCity;
    @Min(value = 0, message = "开关仅允许 0/1")
    @Max(value = 1, message = "开关仅允许 0/1")
    private Integer showDistance;
    @Min(value = 0, message = "开关仅允许 0/1")
    @Max(value = 1, message = "开关仅允许 0/1")
    private Integer showSignature;
    @Min(value = 0, message = "开关仅允许 0/1")
    @Max(value = 1, message = "开关仅允许 0/1")
    private Integer showMatchReasons;
    @Min(value = 0, message = "开关仅允许 0/1")
    @Max(value = 1, message = "开关仅允许 0/1")
    private Integer showLastOnline;

    public UserCardConfig toEntity() {
        UserCardConfig c = new UserCardConfig();
        BeanUtils.copyProperties(this, c);
        return c;
    }
}
