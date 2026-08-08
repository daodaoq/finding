package com.finding.user.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 图片验证码生成器 —— 生成带干扰线/干扰点的 4 位字符 PNG,返回 base64。
 * 纯 AWT 实现,无第三方依赖,headless 环境下可用。
 */
public final class CaptchaGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** 去掉易混淆字符 O/0/I/1 */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private CaptchaGenerator() {}

    public static String randomCode(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /** 生成验证码 PNG 的 base64 字符串 */
    public static String drawImage(String code) throws IOException {
        int width = 130, height = 42;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 背景
        g.setColor(new Color(245, 246, 250));
        g.fillRect(0, 0, width, height);

        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(160 + RANDOM.nextInt(90), 160 + RANDOM.nextInt(90), 160 + RANDOM.nextInt(90)));
            g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height), RANDOM.nextInt(width), RANDOM.nextInt(height));
        }

        // 干扰点
        for (int i = 0; i < 40; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.fillRect(RANDOM.nextInt(width), RANDOM.nextInt(height), 1, 1);
        }

        // 字符(随机角度、颜色、字号)
        for (int i = 0; i < code.length(); i++) {
            g.setFont(new Font("Arial", Font.BOLD, 26 + RANDOM.nextInt(5)));
            g.setColor(new Color(30 + RANDOM.nextInt(120), 30 + RANDOM.nextInt(120), 30 + RANDOM.nextInt(120)));
            double angle = (RANDOM.nextInt(36) - 18) * Math.PI / 180;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.rotate(angle, 16 + i * 28, height / 2);
            g2.drawString(String.valueOf(code.charAt(i)), 12 + i * 28, height - 10);
            g2.dispose();
        }
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
