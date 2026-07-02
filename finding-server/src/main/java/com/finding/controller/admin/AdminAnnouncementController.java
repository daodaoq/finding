package com.finding.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.entity.SystemAnnouncement;
import com.finding.mapper.SystemAnnouncementMapper;
import com.finding.vo.PageVO;
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
