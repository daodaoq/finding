package com.finding.admin.controller;

import com.finding.admin.service.AdminAnnouncementService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.content.SystemAnnouncement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员 - 系统公告管理(业务逻辑见 {@link AdminAnnouncementService})。
 * <p>公告分两类:普通公告(type=1)发布后 WS 弹窗;永久展示公告(type=2)不弹窗,
 * 用户端顶部悬浮横条展示,变更(新增/下架/上架/撤回/编辑)时广播 permanent_announcement_changed 让在线用户刷新横条。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AdminAnnouncementService adminAnnouncementService;

    @GetMapping("/announcements")
    public Result<PageVO<SystemAnnouncement>> listAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminAnnouncementService.listAnnouncements(page, size));
    }

    @PostMapping("/announcements")
    public Result<SystemAnnouncement> createAnnouncement(@RequestBody SystemAnnouncement announcement) {
        return Result.ok(adminAnnouncementService.createAnnouncement(announcement));
    }

    @PutMapping("/announcements/{id}")
    public Result<Void> updateAnnouncement(@PathVariable Long id, @RequestBody SystemAnnouncement announcement) {
        adminAnnouncementService.updateAnnouncement(id, announcement);
        return Result.ok();
    }

    /** 下架(0)/上架(1)公告 */
    @PutMapping("/announcements/{id}/status")
    public Result<Void> updateAnnouncementStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminAnnouncementService.updateAnnouncementStatus(id, body);
        return Result.ok();
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        adminAnnouncementService.deleteAnnouncement(id);
        return Result.ok();
    }
}
