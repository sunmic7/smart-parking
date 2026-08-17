package com.parking.smart_parking.controller;

import com.parking.smart_parking.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 【验证码控制器】CaptchaController.java —— 登录验证码生成与校验支持
 *
 * 提供两个接口：
 *   1. GET /api/captcha        生成新的验证码，返回验证码 key 和图片地址
 *   2. GET /api/captcha/image/{key}  根据 key 返回 PNG 验证码图片
 *
 * 验证码文本存储在 Redis 中，有效期 5 分钟，过期自动失效。
 * 相比原来的 JVM 内存缓存（ConcurrentHashMap），Redis 版本支持集群部署和到期自动清理。
 */
@RestController
@RequestMapping
public class CaptchaController {

    /** Redis 中验证码 key 的前缀 */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /** 验证码有效期：5 分钟 */
    private static final long EXPIRE_MINUTES = 5;

    /** 验证码字符池：去除容易混淆的 0、O、1、I、L */
    private static final String CHAR_POOL = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 【生成验证码】GET /api/captcha
     *
     * 生成 4 位随机验证码并写入 Redis，设置 5 分钟过期时间。
     */
    @GetMapping("/api/captcha")
    public Result<?> generateCaptcha() {
        String code = generateCode(4);
        String key  = UUID.randomUUID().toString().replace("-", "");

        stringRedisTemplate.opsForValue()
                .set(CAPTCHA_KEY_PREFIX + key, code.toLowerCase(), EXPIRE_MINUTES, TimeUnit.MINUTES);

        Map<String, String> data = new HashMap<>();
        data.put("captchaKey", key);
        data.put("captchaImage", "/api/captcha/image/" + key);

        return Result.success("验证码生成成功", data);
    }

    /**
     * 【获取验证码图片】GET /api/captcha/image/{key}
     *
     * 根据 key 从 Redis 读取验证码并绘制 PNG 图片返回。
     * 如果 key 不存在或已过期，返回空白占位图。
     */
    @GetMapping("/api/captcha/image/{key}")
    public void captchaImage(@PathVariable String key, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        String code = stringRedisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + key);
        if (code == null) {
            code = "    ";
        }

        BufferedImage image = drawCaptchaImage(code);

        try (OutputStream out = response.getOutputStream()) {
            ImageIO.write(image, "png", out);
            out.flush();
        }
    }

    /**
     * 校验验证码（供 LoginController 调用）。
     *
     * @param key  前端传来的验证码 key
     * @param code 前端用户输入的验证码
     * @return true=校验通过；false=错误或已过期
     */
    public boolean validate(String key, String code) {
        if (key == null || code == null) return false;

        String redisKey = CAPTCHA_KEY_PREFIX + key;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) return false;

        boolean ok = storedCode.equalsIgnoreCase(code.trim());
        if (ok) {
            // 校验成功后立即删除，防止重复利用
            stringRedisTemplate.delete(redisKey);
        }
        return ok;
    }

    /** 生成指定长度的随机验证码字符串 */
    private String generateCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    /** 绘制验证码图片：120×40，带干扰线、随机字体颜色 */
    private BufferedImage drawCaptchaImage(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        Random random = new Random();

        for (int i = 0; i < 12; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(20 + random.nextInt(100), 20 + random.nextInt(100), 20 + random.nextInt(100)));
            int x = 18 + i * 24;
            int y = 28 + random.nextInt(6) - 3;
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        g.dispose();
        return image;
    }
}
