package com.finding.user.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.finding.common.BusinessException;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.user.entity.User;
import com.finding.user.entity.UserRemark;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserRemarkMapper;
import com.finding.user.service.impl.UserRemarkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRemarkServiceImplTest {

    @Mock
    private UserRemarkMapper userRemarkMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;
    @Mock
    private UserWriteGuard userWriteGuard;
    @Mock
    private RemarkCache remarkCache;

    private UserRemarkServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserRemarkServiceImpl(userRemarkMapper, userMapper, sensitiveWordFilter, userWriteGuard, remarkCache);
    }

    private void targetExists() {
        User target = new User();
        target.setId(2L);
        when(userMapper.selectById(2L)).thenReturn(target);
    }

    @Test
    void remarkSelf_throws() {
        assertThrows(BusinessException.class, () -> service.setRemark(1L, 1L, "小明"));
        verify(userRemarkMapper, never()).upsert(anyLong(), anyLong(), anyString());
    }

    @Test
    void targetNotFound_throws() {
        when(userMapper.selectById(2L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.setRemark(1L, 2L, "小明"));
    }

    @Test
    void blankRemark_throws() {
        targetExists();
        assertThrows(BusinessException.class, () -> service.setRemark(1L, 2L, "   "));
        verify(userRemarkMapper, never()).upsert(anyLong(), anyLong(), anyString());
    }

    @Test
    void tooLongRemark_throws() {
        targetExists();
        String tooLong = "备".repeat(21);
        assertThrows(BusinessException.class, () -> service.setRemark(1L, 2L, tooLong));
        verify(userRemarkMapper, never()).upsert(anyLong(), anyLong(), anyString());
    }

    @Test
    void forbiddenWord_propagates() {
        targetExists();
        doThrow(new BusinessException(com.finding.common.ResultCode.CONTENT_BLOCKED))
                .when(sensitiveWordFilter).assertClean(anyString());
        assertThrows(BusinessException.class, () -> service.setRemark(1L, 2L, "违禁词"));
        verify(userRemarkMapper, never()).upsert(anyLong(), anyLong(), anyString());
    }

    /** 落库的必须是 XSS 清洗后的值(不要重蹈 AuthServiceImpl 先赋值后清洗的覆辙) */
    @Test
    void writesCleanedValue() {
        targetExists();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        service.setRemark(1L, 2L, "  <script>x</script>小明  ");

        verify(userRemarkMapper).upsert(eq(1L), eq(2L), captor.capture());
        String written = captor.getValue();
        assertEquals(written.contains("<script") , false, "落库值不应包含未清洗的标签");
        assertEquals(written, written.trim(), "落库值应已去除首尾空白");
        verify(remarkCache).evict(1L, 2L);
    }

    @Test
    void clearRemovesRowAndEvicts() {
        service.clearRemark(1L, 2L);
        verify(userRemarkMapper).delete(any(Wrapper.class));
        verify(remarkCache).evict(1L, 2L);
    }

    @Test
    void getRemark_returnsStoredValue() {
        UserRemark row = new UserRemark();
        row.setRemark("小明");
        when(userRemarkMapper.selectOne(any(Wrapper.class))).thenReturn(row);
        assertEquals("小明", service.getRemark(1L, 2L));
    }

    @Test
    void getRemark_absentReturnsNull() {
        when(userRemarkMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        assertEquals(null, service.getRemark(1L, 2L));
    }
}
