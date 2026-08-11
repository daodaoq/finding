package com.finding.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageSendDTO {

    /** 会话 ID(=room_id)；接收者由服务端从房间成员关系推导，客户端不可指定任意用户 */
    @NotNull(message = "会话ID不能为空")
    private Long roomId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private String messageType = "text";
}
