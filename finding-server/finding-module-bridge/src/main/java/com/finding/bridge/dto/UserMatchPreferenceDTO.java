package com.finding.bridge.dto;

import com.finding.bridge.entity.UserMatchPreference;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * 相亲交友偏好更新请求体。
 *
 * <p>与实体分离:字段级 Bean Validation 兜底,服务层再做二次校验。
 * 所有字段可空(空=按默认 0/不限 处理),由服务层归一化。</p>
 */
@Data
public class UserMatchPreferenceDTO {

    /** 0=不限 1=男 2=女 */
    @Min(value = 0, message = "preferGender 仅允许 0/1/2")
    @Max(value = 2, message = "preferGender 仅允许 0/1/2")
    private Integer preferGender;

    /** 最小年龄,0=不限 */
    @Min(value = 0, message = "minAge 不能为负数")
    @Max(value = 100, message = "minAge 超出合理范围")
    private Integer minAge;

    /** 最大年龄,0=不限 */
    @Min(value = 0, message = "maxAge 不能为负数")
    @Max(value = 100, message = "maxAge 超出合理范围")
    private Integer maxAge;

    /** 最大距离km,0=不限 */
    @Min(value = 0, message = "maxDistanceKm 不能为负数")
    @Max(value = 10000, message = "maxDistanceKm 超出合理范围")
    private Integer maxDistanceKm;

    /** 只看已认证 0=否 1=是 */
    @Min(value = 0, message = "onlyVerified 仅允许 0/1")
    @Max(value = 1, message = "onlyVerified 仅允许 0/1")
    private Integer onlyVerified;

    /** 偏好城市,空=不限 */
    @Size(max = 50, message = "preferCity 长度不能超过 50")
    private String preferCity;

    /** 偏好目标 0=不限 1=找对象 2=交朋友 */
    @Min(value = 0, message = "preferTargetType 仅允许 0/1/2")
    @Max(value = 2, message = "preferTargetType 仅允许 0/1/2")
    private Integer preferTargetType;

    /** 资料完整度最低门槛 0-10,0=不限 */
    @Min(value = 0, message = "minCompleteness 仅允许 0-10")
    @Max(value = 10, message = "minCompleteness 仅允许 0-10")
    private Integer minCompleteness;

    /** 复制到业务实体(仅业务字段) */
    public UserMatchPreference toEntity() {
        UserMatchPreference pref = new UserMatchPreference();
        BeanUtils.copyProperties(this, pref);
        return pref;
    }
}
