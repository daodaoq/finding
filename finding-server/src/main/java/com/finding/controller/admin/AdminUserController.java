package com.finding.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.entity.User;
import com.finding.mapper.UserMapper;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员 - 用户管理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public Result<PageVO<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getNickname, keyword).or().like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = result.getRecords().stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("nickname", u.getNickname());
            map.put("phone", u.getPhone());
            map.put("school", u.getSchool());
            map.put("status", u.getStatus());
            map.put("realNameVerified", u.getRealNameVerified());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).toList();

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 新建用户 */
    @PostMapping("/users")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        User user = new User();
        user.setNickname((String) body.getOrDefault("nickname", "新用户"));
        user.setPhone((String) body.get("phone"));
        if (!StringUtils.hasText(user.getPhone())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "手机号不能为空");
        }
        // 检查手机号唯一
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, user.getPhone())) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "手机号已存在");
        }
        user.setUsername(user.getPhone());
        user.setPassword(passwordEncoder.encode(
                (String) body.getOrDefault("password", "123456")));
        user.setAvatar((String) body.get("avatar"));
        user.setSchool((String) body.get("school"));
        user.setGender(body.get("gender") != null ? (Integer) body.get("gender") : 0);
        user.setSignature((String) body.get("signature"));
        user.setCity((String) body.get("city"));
        user.setStatus(body.get("status") != null ? (Integer) body.get("status") : 1);
        user.setRole((String) body.getOrDefault("role", "user"));
        user.setRealNameVerified(0);
        userMapper.insert(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        return Result.ok(result);
    }

    /** 获取用户详情（含完整字段） */
    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("nickname", user.getNickname());
        map.put("phone", user.getPhone());
        map.put("avatar", user.getAvatar());
        map.put("school", user.getSchool());
        map.put("gender", user.getGender());
        map.put("signature", user.getSignature());
        map.put("city", user.getCity());
        map.put("status", user.getStatus());
        map.put("role", user.getRole());
        map.put("realNameVerified", user.getRealNameVerified());
        map.put("createdAt", user.getCreatedAt());
        return Result.ok(map);
    }

    /** 编辑用户信息 */
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");

        if (body.containsKey("nickname") && body.get("nickname") != null)
            user.setNickname((String) body.get("nickname"));
        if (body.containsKey("phone") && body.get("phone") != null)
            user.setPhone((String) body.get("phone"));
        if (body.containsKey("avatar") && body.get("avatar") != null)
            user.setAvatar((String) body.get("avatar"));
        if (body.containsKey("school") && body.get("school") != null)
            user.setSchool((String) body.get("school"));
        if (body.containsKey("gender") && body.get("gender") != null)
            user.setGender((Integer) body.get("gender"));
        if (body.containsKey("signature") && body.get("signature") != null)
            user.setSignature((String) body.get("signature"));
        if (body.containsKey("city") && body.get("city") != null)
            user.setCity((String) body.get("city"));
        if (body.containsKey("status") && body.get("status") != null)
            user.setStatus((Integer) body.get("status"));

        // 密码单独处理：非空才更新
        String password = (String) body.get("password");
        if (StringUtils.hasText(password)) {
            user.setPassword(passwordEncoder.encode(password));
        }

        userMapper.updateById(user);
        return Result.ok();
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        Integer newStatus = body.get("status");
        user.setStatus(newStatus != null ? newStatus : (user.getStatus() == 1 ? 0 : 1));
        userMapper.updateById(user);
        return Result.ok();
    }
}
