package com.finding.user.service;

import com.finding.common.BusinessException;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.impl.ProfileCompletenessServiceImpl;
import com.finding.user.vo.ProfileCompletenessVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileCompletenessServiceImplTest {

    @Mock
    private UserMapper userMapper;

    private ProfileCompletenessServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfileCompletenessServiceImpl(userMapper);
    }

    @Test
    void fullProfile_score10_noMissing() {
        User u = new User();
        u.setAvatar("a.jpg");
        u.setSchool("山东理工大学");
        u.setCity("淄博");
        u.setGender(1);
        u.setSignature("你好");
        u.setBirthday(LocalDate.of(2002, 1, 1));
        when(userMapper.selectById(1L)).thenReturn(u);

        ProfileCompletenessVO vo = service.completeness(1L);
        assertEquals(10, vo.getScore());
        assertEquals(6, vo.getFilled());
        assertEquals(6, vo.getTotal());
        assertTrue(vo.getMissing().isEmpty());
    }

    @Test
    void emptyProfile_score0_missingAll() {
        when(userMapper.selectById(1L)).thenReturn(new User());

        ProfileCompletenessVO vo = service.completeness(1L);
        assertEquals(0, vo.getScore());
        assertEquals(0, vo.getFilled());
        assertEquals(List.of("头像", "学校", "城市", "性别", "个性签名", "生日"), vo.getMissing());
    }

    @Test
    void partialProfile_missingOnlyUnfilled() {
        User u = new User();
        u.setAvatar("a.jpg");
        u.setSchool("山东理工大学");
        // 缺 city/gender/signature/birthday
        when(userMapper.selectById(1L)).thenReturn(u);

        ProfileCompletenessVO vo = service.completeness(1L);
        assertEquals(2, vo.getFilled());
        assertEquals(List.of("城市", "性别", "个性签名", "生日"), vo.getMissing());
    }

    @Test
    void userNotFound_throws() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.completeness(99L));
    }
}
