package com.finding.user.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 滑块拼图验证码生成器 —— 生成一张带随机形状的背景图 + 一块圆片拼图,
 * 用户把拼图拖到缺口处即通过。纯 AWT 实现,无第三方依赖,headless 可用。
 * 校验的 x 为拼图块左边缘坐标(相对 300×150 原图)。
 */
public final class PuzzleCaptchaGenerator {

    public static final int BG_W = 300;
    public static final int BG_H = 150;
    /** 拼图块边长(圆) */
    public static final int PIECE = 50;
    /** 目标位置取值范围,保证拼图块落在背景图内且给左侧滑块留空间 */
    public static final int MIN_X = 70;
    public static final int MAX_X = BG_W - PIECE - 20; // 230
    public static final int MIN_Y = 20;
    public static final int MAX_Y = BG_H - PIECE - 20; // 80

    private static final SecureRandom RANDOM = new SecureRandom();

    private PuzzleCaptchaGenerator() {}

    /** 生成结果:背景/拼图块 base64 PNG + 目标 X/Y(均为拼图块左上角坐标) */
    public record Result(String bgImage, String pieceImage, int targetX, int targetY) {}

    public static Result generate() throws IOException {
        int targetX = MIN_X + RANDOM.nextInt(MAX_X - MIN_X + 1);
        int targetY = MIN_Y + RANDOM.nextInt(MAX_Y - MIN_Y + 1);

        BufferedImage bg = new BufferedImage(BG_W, BG_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 随机暖色渐变背景
        Color c1 = new Color(195 + RANDOM.nextInt(56), 165 + RANDOM.nextInt(60), 140 + RANDOM.nextInt(60));
        Color c2 = new Color(228 + RANDOM.nextInt(27), 208 + RANDOM.nextInt(47), 185 + RANDOM.nextInt(47));
        g.setPaint(new GradientPaint(0, 0, c1, BG_W, BG_H, c2));
        g.fillRect(0, 0, BG_W, BG_H);

        // 随机形状干扰
        for (int i = 0; i < 12; i++) {
            g.setColor(new Color(255, 255, 255, 35 + RANDOM.nextInt(60)));
            int r = 8 + RANDOM.nextInt(42);
            g.fillOval(RANDOM.nextInt(BG_W), RANDOM.nextInt(BG_H), r, r);
        }
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(90 + RANDOM.nextInt(90), 80 + RANDOM.nextInt(90), 70 + RANDOM.nextInt(80), 28));
            g.drawArc(RANDOM.nextInt(BG_W), RANDOM.nextInt(BG_H), 40 + RANDOM.nextInt(70), 40 + RANDOM.nextInt(70), 0, 360);
        }

        // 先裁拼图块(不含缺口描边),再在背景上画缺口提示
        BufferedImage piece = new BufferedImage(PIECE, PIECE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = piece.createGraphics();
        pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pg.setClip(new Ellipse2D.Float(0, 0, PIECE, PIECE));
        pg.drawImage(bg, -targetX, -targetY, null);
        // 拼图块描边,便于在背景上辨识
        pg.setClip(null);
        pg.setColor(new Color(0, 0, 0, 80));
        pg.setStroke(new BasicStroke(2));
        pg.draw(new Ellipse2D.Float(0, 0, PIECE, PIECE));
        pg.dispose();

        // 缺口:半透明虚线圆提示
        g.setColor(new Color(0, 0, 0, 105));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{7, 4}, 0));
        g.draw(new Ellipse2D.Float(targetX, targetY, PIECE, PIECE));
        g.dispose();

        return new Result(toBase64(bg, "png"), toBase64(piece, "png"), targetX, targetY);
    }

    private static String toBase64(BufferedImage img, String fmt) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, fmt, baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
