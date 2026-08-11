package com.finding.user.service;

import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.user.dto.UserResumeDTO;
import com.finding.user.entity.UserResume;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserResumeMapper;
import com.finding.user.service.impl.UserResumeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 情感简历保存单测 —— 生日推导年龄 / 相册校验 / 并发首存冲突兜底。
 */
class UserResumeServiceImplTest {

    @Mock private UserResumeMapper resumeMapper;
    @Mock private UserMapper userMapper;
    @Mock private InfoShareQuery infoShareQuery;
    @Mock private SensitiveWordFilter sensitiveWordFilter;

    private UserResumeServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserResumeServiceImpl(resumeMapper, userMapper, infoShareQuery, sensitiveWordFilter);
    }

    @Test
    void saveResume_birthdayDerivesAge() {
        UserResumeDTO dto = new UserResumeDTO();
        dto.setBirthday(LocalDate.of(2000, 1, 1));
        dto.setAge(0); // 客户端给的 age 应被生日覆盖
        when(resumeMapper.selectOne(any())).thenReturn(null);
        when(resumeMapper.insert(any())).thenReturn(1);

        service.saveResume(1L, dto);

        ArgumentCaptor<UserResume> captor = ArgumentCaptor.forClass(UserResume.class);
        verify(resumeMapper).insert(captor.capture());
        int expectedAge = LocalDate.now().getYear() - 2000; // 2000-01-01 生日
        assertEquals(expectedAge, captor.getValue().getAge());
    }

    @Test
    void saveResume_albumTooMany_rejected() {
        UserResumeDTO dto = new UserResumeDTO();
        dto.setPhotoAlbum(List.of("http://a.com/1.jpg", "http://a.com/2.jpg", "http://a.com/3.jpg",
                "http://a.com/4.jpg", "http://a.com/5.jpg", "http://a.com/6.jpg",
                "http://a.com/7.jpg", "http://a.com/8.jpg", "http://a.com/9.jpg", "http://a.com/10.jpg"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.saveResume(1L, dto));
        assertEquals(ResultCode.PARAM_VALIDATION_FAILED.getCode(), ex.getCode());
    }

    @Test
    void saveResume_albumBadUrl_rejected() {
        UserResumeDTO dto = new UserResumeDTO();
        dto.setPhotoAlbum(List.of("javascript:alert(1)"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.saveResume(1L, dto));
        assertEquals(ResultCode.PARAM_VALIDATION_FAILED.getCode(), ex.getCode());
    }

    @Test
    void saveResume_insertConflict_reQueriesAndUpdates() {
        UserResumeDTO dto = new UserResumeDTO();
        UserResume existing = new UserResume();
        existing.setId(5L);
        existing.setUserId(1L);
        // 首次 selectOne 返回 null(未创建),insert 触发唯一键冲突,再 selectOne 返回已存在记录
        when(resumeMapper.selectOne(any())).thenReturn(null).thenReturn(existing);
        when(resumeMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_user_id"));
        when(resumeMapper.updateById(any())).thenReturn(1);

        service.saveResume(1L, dto);

        verify(resumeMapper).insert(any());
        verify(resumeMapper).updateById(existing);
    }
}
