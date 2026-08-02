package com.foodtraceability.service.impl;

import com.foodtraceability.dto.CaptchaResponse;
import com.foodtraceability.service.CacheService;
import com.foodtraceability.service.CaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaServiceImpl.class);

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String CAPTCHA_FAIL_PREFIX = "captcha:fail:";
    private static final int MAX_CAPTCHA_FAIL = 5;
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;


    public CaptchaServiceImpl(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    private final CacheService cacheService;

    @Override
    public CaptchaResponse generateCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String captchaText = generateRandomString(4);
        String captchaImage = generateCaptchaImage(captchaText);

        cacheService.put(CAPTCHA_PREFIX + captchaId, captchaText, CAPTCHA_EXPIRE_MINUTES);

        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaId(captchaId);
        response.setCaptchaImage(captchaImage);

        logger.info("验证码已生成: captchaId={}", captchaId);
        return response;
    }

    @Override
    public boolean validateCaptcha(String captchaId, String captcha) {
        if (captchaId == null || captcha == null) {
            return false;
        }

        String key = CAPTCHA_PREFIX + captchaId;
        String failKey = CAPTCHA_FAIL_PREFIX + captchaId;

        String failCountStr = cacheService.get(failKey);
        int failCount = 0;
        if (failCountStr != null) {
            try {
                failCount = Integer.parseInt(failCountStr);
            } catch (NumberFormatException e) {
                failCount = 0;
            }
        }
        if (failCount >= MAX_CAPTCHA_FAIL) {
            cacheService.delete(key);
            cacheService.delete(failKey);
            logger.warn("验证码验证失败次数超过限制: captchaId={}", captchaId);
            return false;
        }

        String storedCaptcha = cacheService.get(key);

        if (storedCaptcha == null) {
            return false;
        }

        boolean valid = storedCaptcha.equalsIgnoreCase(captcha);
        if (valid) {
            cacheService.delete(key);
            cacheService.delete(failKey);
        } else {
            failCount++;
            cacheService.put(failKey, String.valueOf(failCount), 5);
            logger.warn("验证码验证失败: captchaId={}, failCount={}", captchaId, failCount);
        }

        return valid;
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateCaptchaImage(String captchaText) {
        System.setProperty("java.awt.headless", "true");
        int width = 120;
        int height = 40;

        Graphics2D g = null;
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            g = image.createGraphics();

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            SecureRandom random = new SecureRandom();
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i < 5; i++) {
                int x1 = random.nextInt(width);
                int y1 = random.nextInt(height);
                int x2 = random.nextInt(width);
                int y2 = random.nextInt(height);
                g.drawLine(x1, y1, x2, y2);
            }

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            int x = 20;
            for (char c : captchaText.toCharArray()) {
                int y = 28 + random.nextInt(8) - 4;
                g.drawString(String.valueOf(c), x, y);
                x += 22;
            }

            for (int i = 0; i < 30; i++) {
                int x1 = random.nextInt(width);
                int y1 = random.nextInt(height);
                g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                g.drawRect(x1, y1, 1, 1);
            }

            g.dispose();
            g = null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            logger.error("生成验证码图片失败: {}", e.getMessage(), e);
            return "";
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
    }
}
