package com.finding.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finding.user.dto.UserSettingsDTO;
import com.finding.user.entity.UserSettings;
import com.finding.user.mapper.UserSettingsMapper;
import com.finding.user.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserSettingsMapper settingsMapper;

    @Override
    public UserSettings getSettings(Long userId) {
        UserSettings s = settingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, userId));
        if (s == null) {
            s = new UserSettings();
            s.setUserId(userId);
            s.setChatMuted(0);
            s.setFriendAddMode(1);
            s.setProfileVisible(1);
            s.setSearchable(1);
        }
        return s;
    }

    @Override
    public void updateSettings(Long userId, UserSettingsDTO dto) {
        // 显式 set 写列:updateById 默认忽略 null 字段,chatBg 清空需显式置 null
        LambdaUpdateWrapper<UserSettings> wrapper = new LambdaUpdateWrapper<UserSettings>()
                .eq(UserSettings::getUserId, userId);
        if (dto.getChatBg() != null) wrapper.set(UserSettings::getChatBg, dto.getChatBg().isEmpty() ? null : dto.getChatBg());
        if (dto.getChatMuted() != null) wrapper.set(UserSettings::getChatMuted, dto.getChatMuted());
        if (dto.getFriendAddMode() != null) wrapper.set(UserSettings::getFriendAddMode, dto.getFriendAddMode());
        if (dto.getProfileVisible() != null) wrapper.set(UserSettings::getProfileVisible, dto.getProfileVisible());
        if (dto.getSearchable() != null) wrapper.set(UserSettings::getSearchable, dto.getSearchable());

        if (settingsMapper.update(null, wrapper) == 0) {
            // 无行 → 插入(默认值兜底)
            UserSettings s = new UserSettings();
            s.setUserId(userId);
            s.setChatBg(dto.getChatBg() != null && !dto.getChatBg().isEmpty() ? dto.getChatBg() : null);
            s.setChatMuted(dto.getChatMuted() != null ? dto.getChatMuted() : 0);
            s.setFriendAddMode(dto.getFriendAddMode() != null ? dto.getFriendAddMode() : 1);
            s.setProfileVisible(dto.getProfileVisible() != null ? dto.getProfileVisible() : 1);
            s.setSearchable(dto.getSearchable() != null ? dto.getSearchable() : 1);
            settingsMapper.insert(s);
        }
    }
}
