package com.finding.framework.service;

import java.util.List;

/**
 * 消息推送服务 —— 封装 WebSocket 推送逻辑。
 */
public interface PushService {

    /** 推送给所有在线用户 */
    void sendPushMsg(Object message);

    /** 推送给指定用户列表 */
    void sendPushMsg(Object message, List<Long> uidList);

    /** 推送给单个用户 */
    void sendPushMsg(Object message, Long uid);
}
