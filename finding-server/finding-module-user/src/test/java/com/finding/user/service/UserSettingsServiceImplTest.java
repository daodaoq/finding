package com.finding.user.service;

import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.user.dto.UserSettingsDTO;
import com.finding.user.mapper.UserSettingsMapper;
import com.finding.user.service.impl.UserSettingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 设置枚举校验单测 —— 非法枚举必须抛参数校验错误,且不落库。
 */
class UserSettingsServiceImplTest {

    @Mock
    private UserSettingsMapper settingsMapper;

    private UserSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserSettingsServiceImpl(settingsMapper);
    }

    @Test
    void friendAddMode_outOfRange_rejected() {
        UserSettingsDTO dto = new UserSettingsDTO();
        dto.setFriendAddMode(5);
        assertRejected(dto);
    }

    @Test
    void profileVisible_outOfRange_rejected() {
        UserSettingsDTO dto = new UserSettingsDTO();
        dto.setProfileVisible(3);
        assertRejected(dto);
    }

    @Test
    void searchable_outOfRange_rejected() {
        UserSettingsDTO dto = new UserSettingsDTO();
        dto.setSearchable(2);
        assertRejected(dto);
    }

    @Test
    void chatMuted_outOfRange_rejected() {
        UserSettingsDTO dto = new UserSettingsDTO();
        dto.setChatMuted(9);
        assertRejected(dto);
    }

    private void assertRejected(UserSettingsDTO dto) {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateSettings(1L, dto));
        assertEquals(ResultCode.PARAM_VALIDATION_FAILED.getCode(), ex.getCode());
        verify(settingsMapper, never()).update(any(), any());
        verify(settingsMapper, never()).insert(any());
    }
}
