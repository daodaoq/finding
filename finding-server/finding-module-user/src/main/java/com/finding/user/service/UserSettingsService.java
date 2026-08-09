package com.finding.user.service;

import com.finding.user.dto.UserSettingsDTO;
import com.finding.user.entity.UserSettings;

public interface UserSettingsService {

    /** 获取当前用户全局设置(无行时返回带默认值的对象) */
    UserSettings getSettings(Long userId);

    /** 更新全局设置(不存在则插入) */
    void updateSettings(Long userId, UserSettingsDTO dto);
}
