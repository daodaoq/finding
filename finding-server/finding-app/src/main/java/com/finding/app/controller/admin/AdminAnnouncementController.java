package com.finding.app.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.app.entity.SystemAnnouncement;
import com.finding.app.mapper.SystemAnnouncementMapper;
import com.finding.common.PageVO;
import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员 - 系统公告管理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final SystemAnnouncementMapper announcementMapper;
    private final WebSocketServer webSocketServer;

    @GetMapping("/announcements")
    public Result<PageVO<SystemAnnouncement>> listAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<SystemAnnouncement> result = announcementMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SystemAnnouncement>().orderByDesc(SystemAnnouncement::getCreatedAt));
        return Result.ok(PageVO.of(result.getRecords(), result.getTotal(), page, size));
    }

    @PostMapping("/announcements")
    public Result<SystemAnnouncement> createAnnouncement(@RequestBody SystemAnnouncement announcement) {
        announcementMapper.insert(announcement);
        // 主动推送系统公告给所有在线用户 → 用户端弹出公告面板
        WsMessage ws = new WsMessage();
        ws.setType("system_announcement");
        ws.setTitle(announcement.getTitle());
        ws.setContent(announcement.getContent());
        ws.setMessageId(announcement.getId());
        ws.setTimestamp(System.currentTimeMillis()); // 发布时刻,供用户端展示时间
        webSocketServer.sendToAllOnline(ws);
        return Result.ok(announcement);
    }

    @PutMapping("/announcements/{id}")
    public Result<Void> updateAnnouncement(@PathVariable Long id, @RequestBody SystemAnnouncement announcement) {
        SystemAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.PARAM_ERROR, "公告不存在");
        announcement.setId(id);
        announcementMapper.updateById(announcement);
        return Result.ok();
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementMapper.deleteById(id);
        return Result.ok();
    }
}
