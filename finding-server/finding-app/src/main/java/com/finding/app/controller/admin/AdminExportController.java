package com.finding.app.controller.admin;

import com.finding.post.entity.Post;
import com.finding.post.mapper.PostMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理员 - 数据导出(CSV)。
 * 供运营离线分析/归档,返回带 UTF-8 BOM 的 CSV(Excel 直接打开中文不乱码)。
 */
@RestController
@RequestMapping("/api/v1/admin/export")
@RequiredArgsConstructor
public class AdminExportController {

    private final UserMapper userMapper;
    private final PostMapper postMapper;

    @GetMapping("/users")
    public void exportUsers(HttpServletResponse response) throws IOException {
        List<User> users = userMapper.selectList(null);
        writeCsv(response, "users.csv",
                List.of("ID", "昵称", "手机号", "学校", "性别", "状态", "注册时间"),
                users.stream().map(u -> List.of(
                        str(u.getId()), u.getNickname(), u.getPhone(), u.getSchool(),
                        genderDesc(u.getGender()), userStatusDesc(u.getStatus()), str(u.getCreatedAt())
                )).toList());
    }

    @GetMapping("/posts")
    public void exportPosts(HttpServletResponse response) throws IOException {
        List<Post> posts = postMapper.selectList(null);
        writeCsv(response, "posts.csv",
                List.of("ID", "用户ID", "分类", "内容", "状态", "审核状态", "发布时间"),
                posts.stream().map(p -> List.of(
                        str(p.getId()), str(p.getUserId()),
                        p.getCategory() != null ? p.getCategory() : "",
                        p.getContent(),
                        postStatusDesc(p.getStatus()), reviewDesc(p.getReviewStatus()), str(p.getCreatedAt())
                )).toList());
    }

    private void writeCsv(HttpServletResponse response, String filename, List<String> header, List<List<String>> rows) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        // UTF-8 BOM,便于 Excel 正确识别中文
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        PrintWriter w = new PrintWriter(response.getOutputStream(), false, StandardCharsets.UTF_8);
        w.println(String.join(",", header.stream().map(this::csv).toList()));
        for (List<String> row : rows) {
            w.println(String.join(",", row.stream().map(this::csv).toList()));
        }
        w.flush();
    }

    /** CSV 字段转义:含逗号/引号/换行时用双引号包裹并转义内部引号 */
    private String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String genderDesc(Integer g) {
        if (g == null) return "";
        return g == 1 ? "男" : g == 2 ? "女" : "未知";
    }

    private String userStatusDesc(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 0 -> "封禁";
            case 1 -> "正常";
            case 2 -> "冻结";
            default -> String.valueOf(s);
        };
    }

    private String postStatusDesc(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 0 -> "已删除";
            case 1 -> "正常";
            case 2 -> "已隐藏";
            default -> String.valueOf(s);
        };
    }

    private String reviewDesc(Integer r) {
        if (r == null || r == 0) return "已发布";
        return r == 1 ? "待审" : "拒绝";
    }
}
