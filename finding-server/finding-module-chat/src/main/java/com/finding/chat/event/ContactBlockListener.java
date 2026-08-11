package com.finding.chat.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finding.chat.entity.Contact;
import com.finding.chat.entity.RoomFriend;
import com.finding.chat.mapper.ContactMapper;
import com.finding.chat.mapper.RoomFriendMapper;
import com.finding.common.event.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 拉黑联动:任一方拉黑后,隐藏双方之间已建立的单聊会话(contact.hidden=1),
 * 使其从会话列表消失。消息发送已被 sendMessage 的拉黑拦截,故不会因新消息自动恢复。
 */
@Component
@RequiredArgsConstructor
public class ContactBlockListener {

    private final RoomFriendMapper roomFriendMapper;
    private final ContactMapper contactMapper;

    @EventListener
    public void onUserBlocked(UserBlockedEvent event) {
        long uid1 = Math.min(event.getUserId(), event.getBlockedUserId());
        long uid2 = Math.max(event.getUserId(), event.getBlockedUserId());
        RoomFriend rf = roomFriendMapper.selectOne(
                new LambdaQueryWrapper<RoomFriend>().eq(RoomFriend::getRoomKey, uid1 + "_" + uid2));
        if (rf == null) {
            return;
        }
        contactMapper.update(null, new LambdaUpdateWrapper<Contact>()
                .eq(Contact::getRoomId, rf.getRoomId())
                .in(Contact::getUid, List.of(event.getUserId(), event.getBlockedUserId()))
                .set(Contact::getHidden, 1));
    }
}
