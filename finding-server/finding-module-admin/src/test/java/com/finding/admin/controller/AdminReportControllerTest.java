package com.finding.admin.controller;

import com.finding.chat.entity.Report;
import com.finding.chat.mapper.ReportMapper;
import com.finding.common.BusinessException;
import com.finding.common.audit.OperationAuditService;
import com.finding.message.service.MessageService;
import com.finding.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 举报处理单测 —— 处理/驳回时写处理记录并通知投诉人。
 */
@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

    @Mock private ReportMapper reportMapper;
    @Mock private UserMapper userMapper;
    @Mock private MessageService messageService;
    @Mock private OperationAuditService operationAuditService;

    @InjectMocks
    private AdminReportController controller;

    @Test
    void handleReport_setsFieldsAndNotifiesReporter() {
        Report report = pendingReport(200L, 300L);
        when(reportMapper.selectById(1L)).thenReturn(report);

        controller.updateReportStatus(1L, Map.of("status", 1, "note", "已封禁处理"));

        ArgumentCaptor<Report> cap = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).updateById(cap.capture());
        Report saved = cap.getValue();
        assertEquals(1, saved.getStatus());
        assertEquals("已封禁处理", saved.getHandleNote());
        assertNotNull(saved.getHandleTime());
        verify(messageService).notify(any(), eq(200L), eq("report_handled"), contains("已处理"), eq(1L));
    }

    @Test
    void rejectReport_notifiesRejected() {
        Report report = pendingReport(200L, 300L);
        when(reportMapper.selectById(1L)).thenReturn(report);

        controller.updateReportStatus(1L, Map.of("status", 2));

        ArgumentCaptor<Report> cap = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).updateById(cap.capture());
        assertEquals(2, cap.getValue().getStatus());
        verify(messageService).notify(any(), eq(200L), eq("report_rejected"), any(), eq(1L));
    }

    @Test
    void invalidStatus_rejected() {
        when(reportMapper.selectById(1L)).thenReturn(new Report());
        assertThrows(BusinessException.class, () -> controller.updateReportStatus(1L, Map.of("status", 3)));
    }

    @Test
    void reportNotFound_rejected() {
        when(reportMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> controller.updateReportStatus(999L, Map.of("status", 1)));
    }

    private Report pendingReport(Long from, Long target) {
        Report r = new Report();
        r.setId(1L);
        r.setFromUserId(from);
        r.setTargetUserId(target);
        r.setStatus(0);
        return r;
    }
}
