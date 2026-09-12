package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminAnnouncementService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.content.SystemAnnouncement;
import com.finding.common.content.SystemAnnouncementMapper;
import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAnnouncementServiceImpl implements AdminAnnouncementService {

    /** 普通公告(用户端弹窗) */
    private static final int TYPE_NORMAL = 1;
    /** 永久展示公告(用户端顶部横条) */
    private static final int TYPE_PERMANENT = 2;

    private final SystemAnnouncementMapper announcementMapper;
    private final WebSocketServer webSocketServer;

    @Override
    public PageVO<SystemAnnouncement> listAnnouncements(int page, int size) {
        size = AdminPaging.clampSize(size);
        Page<SystemAnnouncement> result = announcementMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SystemAnnouncement>().orderByDesc(SystemAnnouncement::getCreatedAt));
        return PageVO.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public SystemAnnouncement createAnnouncement(SystemAnnouncement announcement) {
        SystemAnnouncement entity = new SystemAnnouncement();
        entity.setTitle(announcement.getTitle());
        entity.setContent(announcement.getContent());
        entity.setType(announcement.getType() != null ? announcement.getType() : TYPE_NORMAL);
        entity.setStatus(1); // 新建默认展示中
        announcementMapper.insert(entity);
        if (entity.getType() == TYPE_PERMANENT) {
            // 永久展示公告:不弹窗,通知在线用户刷新顶部横条
            broadcastPermanentChanged(entity.getId());
        } else {
            // 主动推送系统公告给所有在线用户 → 用户端弹出公告面板
            WsMessage ws = new WsMessage();
            ws.setType("system_announcement");
            ws.setTitle(entity.getTitle());
            ws.setContent(entity.getContent());
            ws.setMessageId(entity.getId());
            ws.setTimestamp(System.currentTimeMillis()); // 发布时刻,供用户端展示时间
            webSocketServer.sendToAllOnline(ws);
        }
        return entity;
    }

    @Override
    public void updateAnnouncement(Long id, SystemAnnouncement announcement) {
        SystemAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.PARAM_ERROR, "公告不存在");

        // 只落白名单字段:title/content/type 由请求体决定,其余保持库中原值
        SystemAnnouncement entity = new SystemAnnouncement();
        entity.setId(id);
        entity.setTitle(announcement.getTitle());
        entity.setContent(announcement.getContent());
        Integer type = announcement.getType() != null ? announcement.getType() : existing.getType();
        entity.setType(type);
        announcementMapper.updateById(entity);

        // 永久公告变更后通知在线用户刷新横条
        if (type != null && type == TYPE_PERMANENT) {
            broadcastPermanentChanged(id);
        }
    }

    @Override
    public void updateAnnouncementStatus(Long id, Map<String, Integer> body) {
        SystemAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.PARAM_ERROR, "公告不存在");
        Integer status = body.get("status");
        if (status == null || (status != 1 && status != 0)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态参数不合法");
        }
        existing.setStatus(status);
        announcementMapper.updateById(existing);
        if (existing.getType() != null && existing.getType() == TYPE_PERMANENT) {
            broadcastPermanentChanged(id);
        }
    }

    @Override
    public void deleteAnnouncement(Long id) {
        SystemAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.PARAM_ERROR, "公告不存在");
        boolean wasPermanent = existing.getType() != null && existing.getType() == TYPE_PERMANENT;
        announcementMapper.deleteById(id);
        if (wasPermanent) {
            broadcastPermanentChanged(id);
        }
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
