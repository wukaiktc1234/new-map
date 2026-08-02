package com.foodtraceability.service;

import com.foodtraceability.dto.CaptchaResponse;

public interface CaptchaService {

    CaptchaResponse generateCaptcha();

    boolean validateCaptcha(String captchaId, String captcha);
}
