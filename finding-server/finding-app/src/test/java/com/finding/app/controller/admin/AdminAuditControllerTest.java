package com.finding.app.controller.admin;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.audit.OperationLog;
import com.finding.common.audit.OperationLogMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditControllerTest {

    @Mock private OperationLogMapper operationLogMapper;
    @Mock private UserMapper userMapper;
    @InjectMocks private AdminAuditController controller;

    @BeforeEach
    void initMybatisLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OperationLog.class);
    }

    @Test
    void list_resolvesOperatorNickname() {
        OperationLog log = new OperationLog();
        log.setId(1L);
        log.setOperatorId(7L);
        log.setAction("post_review");
        log.setDetail("审核通过");
        Page<OperationLog> page = new Page<>(1, 20);
        page.setRecords(List.of(log));
        when(operationLogMapper.selectPage(any(), any())).thenReturn(page);

        User op = new User();
        op.setId(7L);
        op.setNickname("管理员小王");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(op));

        Result<PageVO<Map<String, Object>>> res = controller.list(1, 20, "post_review", null, null, null);

        Map<String, Object> first = res.getData().getRecords().get(0);
        assertEquals("管理员小王", first.get("operatorNickname"));
        assertEquals("post_review", first.get("action"));
    }
}
