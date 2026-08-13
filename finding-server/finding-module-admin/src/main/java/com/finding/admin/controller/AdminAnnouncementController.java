package com.finding.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.content.SystemAnnouncement;
import com.finding.common.content.SystemAnnouncementMapper;
import com.finding.common.PageVO;
import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员 - 系统公告管理。
 * <p>公告分两类:普通公告(type=1)发布后 WS 弹窗;永久展示公告(type=2)不弹窗,
 * 用户端顶部悬浮横条展示,变更(新增/下架/上架/撤回/编辑)时广播 permanent_announcement_changed 让在线用户刷新横条。
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
        if (announcement.getType() == null) announcement.setType(1);
        announcement.setStatus(1); // 新建默认展示中
        announcementMapper.insert(announcement);
        if (announcement.getType() == 2) {
            // 永久展示公告:不弹窗,通知在线用户刷新顶部横条
            broadcastPermanentChanged(announcement.getId());
        } else {
            // 主动推送系统公告给所有在线用户 → 用户端弹出公告面板
            WsMessage ws = new WsMessage();
            ws.setType("system_announcement");
            ws.setTitle(announcement.getTitle());
            ws.setContent(announcement.getContent());
            ws.setMessageId(announcement.getId());
            ws.setTimestamp(System.currentTimeMillis()); // 发布时刻,供用户端展示时间
            webSocketServer.sendToAllOnline(ws);
        }
        return Result.ok(announcement);
    }

    @PutMapping("/announcements/{id}")
    public Result<Void> updateAnnouncement(@PathVariable Long id, @RequestBody SystemAnnouncement announcement) {
        SystemAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.PARAM_ERROR, "公告不存在");
        if (announcement.getType() == null) announcement.setType(existing.getType());
        announcement.setId(id);
        announcementMapper.updateById(announcement);
        // 永久公告变更后通知在线用户刷新横条
        if (announcement.getType() == 2) {
            broadcastPermanentChanged(id);
        }
        return Result.ok();
    }

    /** 下架(0)/上架(1)公告 */
    @PutMapping("/announcements/{id}/status")
    public Result<Void> updateAnnouncementStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        SystemAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.PARAM_ERROR, "公告不存在");
        Integer status = body.get("status");
        if (status == null || (status != 1 && status != 0)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态参数不合法");
        }
        existing.setStatus(status);
        announcementMapper.updateById(existing);
        if (existing.getType() != null && existing.getType() == 2) {
            broadcastPermanentChanged(id);
        }
        return Result.ok();
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        SystemAnnouncement existing = announcementMapper.selectById(id);
        boolean wasPermanent = existing != null && existing.getType() != null && existing.getType() == 2;
        announcementMapper.deleteById(id);
        if (wasPermanent) {
            broadcastPermanentChanged(id);
        }
        return Result.ok();
    }

    /** 广播「永久公告变更」→ 在线用户刷新顶部横条 */
    private void broadcastPermanentChanged(Long announcementId) {
        WsMessage ws = new WsMessage();
        ws.setType("permanent_announcement_changed");
        ws.setMessageId(announcementId);
        ws.setTimestamp(System.currentTimeMillis());
        webSocketServer.sendToAllOnline(ws);
    }
}
