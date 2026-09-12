package com.finding.admin.controller;

import com.finding.admin.service.AdminGroupService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 群聊管理(业务逻辑见 {@link AdminGroupService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminGroupController {

    private final AdminGroupService adminGroupService;

    @GetMapping("/groups")
    public Result<PageVO<Map<String, Object>>> listGroups(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminGroupService.listGroups(page, size, keyword));
    }

    /** 编辑群信息(管理员可改一切) */
    @PutMapping("/groups/{id}")
    public Result<Void> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminGroupService.updateGroup(id, body);
        return Result.ok();
    }

    /** 解散群：级联删除成员关系、群消息、群本身 */
    @DeleteMapping("/groups/{id}")
    public Result<Void> disbandGroup(@PathVariable Long id) {
        adminGroupService.disbandGroup(id);
        return Result.ok();
    }
}
