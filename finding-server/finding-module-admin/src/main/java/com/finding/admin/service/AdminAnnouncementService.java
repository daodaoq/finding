package com.finding.admin.service;

import com.finding.common.PageVO;
import com.finding.common.content.SystemAnnouncement;

import java.util.Map;

/**
 * 管理员 - 系统公告管理。
 *
 * <p>公告分两类:普通公告(type=1)发布后 WS 弹窗;永久展示公告(type=2)不弹窗,
 * 用户端顶部悬浮横条展示,变更(新增/下架/上架/编辑)时广播 permanent_announcement_changed
 * 让在线用户刷新横条。</p>
 *
 * <p><b>写入字段白名单</b>:请求体仍是 {@link SystemAnnouncement},但只有
 * title/content/type 会被落库;{@code id/status/createdAt} 一律由服务端决定
 * (status=1 展示中、id 由 DB 生成、createdAt 由 INSERT 填充),避免客户端越权改状态。</p>
 */
public interface AdminAnnouncementService {

    /** 公告列表(创建时间倒序) */
    PageVO<SystemAnnouncement> listAnnouncements(int page, int size);

    /** 新建公告:type 缺省为 1(弹窗公告),status 固定展示中;普通公告推送弹窗、永久公告广播刷新 */
    SystemAnnouncement createAnnouncement(SystemAnnouncement announcement);

    /** 编辑公告:仅 title/content/type 生效;永久公告变更后广播刷新横条 */
    void updateAnnouncement(Long id, SystemAnnouncement announcement);

    /** 下架(0)/上架(1)公告，永久公告变更后广播 */
    void updateAnnouncementStatus(Long id, Map<String, Integer> body);

    /** 删除公告(不存在时报错),永久公告删除后广播 */
    void deleteAnnouncement(Long id);
}
