package com.finding.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageSendDTO {

    /** 会话 ID(=room_id)；接收者由服务端从房间成员关系推导，客户端不可指定任意用户 */
    @NotNull(message = "会话ID不能为空")
    private Long roomId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字")
    private String content;

    /** text / image */
    private String messageType = "text";

    /** 客户端生成的幂等 ID(同一 senderId+clientMessageId 弱网重试不重复落库) */
    @Size(max = 64, message = "clientMessageId 过长")
    private String clientMessageId;
}
