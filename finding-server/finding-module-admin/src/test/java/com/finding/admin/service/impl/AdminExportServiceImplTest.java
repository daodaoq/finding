package com.finding.admin.service.impl;

import com.finding.admin.service.AdminExportService;
import com.finding.post.mapper.PostMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminExportServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private PostMapper postMapper;
    @InjectMocks private AdminExportServiceImpl service;

    @Test
    void exportUsers_bomHeaderAndEscaping() {
        User u = new User();
        u.setId(1L);
        u.setNickname("张三,同学"); // 含逗号 → 需双引号包裹
        u.setPhone("13800000000");
        u.setSchool("山东理工大学");
        u.setGender(1);
        u.setStatus(1);
        when(userMapper.selectList(null)).thenReturn(List.of(u));

        AdminExportService.CsvFile file = service.exportUsers();
        String content = new String(file.bytes(), StandardCharsets.UTF_8);

        assertEquals("users.csv", file.filename());
        assertTrue(content.startsWith("﻿"), "应带 UTF-8 BOM 供 Excel 识别");
        assertTrue(content.contains("ID,昵称,手机号"), "应含中文表头");
        assertTrue(content.contains("\"张三,同学\""), "含逗号字段应被双引号包裹");
    }
}
