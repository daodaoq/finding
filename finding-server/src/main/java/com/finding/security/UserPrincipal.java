package com.finding.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.io.Serializable;

/**
 * 最小化的用户主体，携带用户 ID。
 * 挂载到 UsernamePasswordAuthenticationToken 上，
 * 控制器通过 SecurityContextHolder 获取当前用户 ID。
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements Serializable {

    private Long id;
    private String username;

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
