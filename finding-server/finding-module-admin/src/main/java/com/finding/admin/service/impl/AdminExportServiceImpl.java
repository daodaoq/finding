package com.finding.admin.service.impl;

import com.finding.admin.service.AdminExportService;
import com.finding.post.entity.Post;
import com.finding.post.mapper.PostMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminExportServiceImpl implements AdminExportService {

    private final UserMapper userMapper;
    private final PostMapper postMapper;

    @Override
    public CsvFile exportUsers() {
        List<User> users = userMapper.selectList(null);
        return writeCsv("users.csv",
                List.of("ID", "昵称", "手机号", "学校", "性别", "状态", "注册时间"),
                users.stream().map(u -> List.of(
                        str(u.getId()), u.getNickname(), u.getPhone(), u.getSchool(),
                        genderDesc(u.getGender()), userStatusDesc(u.getStatus()), str(u.getCreatedAt())
                )).toList());
    }

    @Override
    public CsvFile exportPosts() {
        List<Post> posts = postMapper.selectList(null);
        return writeCsv("posts.csv",
                List.of("ID", "用户ID", "分类", "内容", "状态", "审核状态", "发布时间"),
                posts.stream().map(p -> List.of(
                        str(p.getId()), str(p.getUserId()),
                        p.getCategory() != null ? p.getCategory() : "",
                        p.getContent(),
                        postStatusDesc(p.getStatus()), reviewDesc(p.getReviewStatus()), str(p.getCreatedAt())
                )).toList());
    }

    /** 组装 CSV:UTF-8 BOM + 表头 + 数据行(行分隔符沿用运行环境默认值) */
    private CsvFile writeCsv(String filename, List<String> header, List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // UTF-8 BOM,便于 Excel 正确识别中文
        out.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        PrintWriter w = new PrintWriter(out, false, StandardCharsets.UTF_8);
        w.println(String.join(",", header.stream().map(AdminExportServiceImpl::csv).toList()));
        for (List<String> row : rows) {
            w.println(String.join(",", row.stream().map(AdminExportServiceImpl::csv).toList()));
        }
        w.flush();
        return new CsvFile(filename, out.toByteArray());
    }

    /** CSV 字段转义:含逗号/引号/换行时用双引号包裹并转义内部引号 */
    static String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    static String genderDesc(Integer g) {
        if (g == null) return "";
        return g == 1 ? "男" : g == 2 ? "女" : "未知";
    }

    static String userStatusDesc(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 0 -> "封禁";
            case 1 -> "正常";
            case 2 -> "冻结";
            default -> String.valueOf(s);
        };
    }

    static String postStatusDesc(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 0 -> "已删除";
            case 1 -> "正常";
            case 2 -> "已隐藏";
            default -> String.valueOf(s);
        };
    }

    static String reviewDesc(Integer r) {
        if (r == null || r == 0) return "已发布";
        return r == 1 ? "待审" : "拒绝";
    }
}
