package com.foodtraceability.controller.hardware;

import com.foodtraceability.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

/**
 * TTS 语音播报控制器
 *
 * <p>提供多平台 TTS 语音合成接口（百度/Azure 已实现，讯飞/阿里云/腾讯云返回 NOT_IMPLEMENTED）。
 * 包含真实的第三方 TTS API 调用逻辑（RestTemplate 调用百度/Azure 云服务）。</p>
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             TTS 语音播报能力应封装为独立的 DeviceDriver 实现类（如 TtsDriver），
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理。
 *             旧控制器的 TTS API 调用逻辑应下沉为 DeviceDriver 实现类，被新系统调用。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/tts")
@Tag(name = "TTS语音播报", description = "多平台TTS语音合成接口（百度/讯飞/阿里云/腾讯/Azure）")
public class TTSController {

    private final RestTemplate restTemplate = new RestTemplate();

    // 百度 TTS API 凭据从配置注入（禁止前端通过请求体传入，避免密钥泄露）
    @Value("${baidu.tts.api-key:}")
    private String baiduTtsApiKey;

    @Value("${baidu.tts.api-secret:}")
    private String baiduTtsApiSecret;

    @PostMapping("/baidu")
    @Operation(summary = "百度TTS语音合成")
    @PreAuthorize("hasAuthority('device:tts:manage') or hasAuthority('*')")
    public ResponseEntity<byte[]> baiduTTS(@RequestBody Map<String, Object> request) {
        try {
            // 安全校验：API 凭据必须由后端配置注入，不再从请求体读取
            if (baiduTtsApiKey == null || baiduTtsApiKey.isEmpty()
                    || baiduTtsApiSecret == null || baiduTtsApiSecret.isEmpty()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("百度 TTS 服务未配置，请联系管理员设置 BAIDU_TTS_API_KEY/BAIDU_TTS_API_SECRET 环境变量"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            String text = (String) request.get("text");
            String voice = request.getOrDefault("voice", "0").toString();
            int speed = request.get("speed") != null ? ((Number) request.get("speed")).intValue() : 5;
            int volume = request.get("volume") != null ? ((Number) request.get("volume")).intValue() : 5;

            String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials"
                    + "&client_id=" + baiduTtsApiKey
                    + "&client_secret=" + baiduTtsApiSecret;

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restTemplate.getForObject(tokenUrl, Map.class);
            String accessToken = (String) tokenResponse.get("access_token");

            String ttsUrl = "https://tsn.baidu.com/text2audio?tex=" + java.net.URLEncoder.encode(text, "UTF-8")
                    + "&tok=" + accessToken
                    + "&cuid=restaurant_pos"
                    + "&ctp=1"
                    + "&lan=zh"
                    + "&per=" + voice
                    + "&spd=" + speed
                    + "&vol=" + volume;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    ttsUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class
            );

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentLength(response.getBody().length);

            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/xunfei")
    @Operation(summary = "讯飞TTS语音合成")
    @PreAuthorize("hasAuthority('device:tts:manage') or hasAuthority('*')")
    public ResponseEntity<byte[]> xunfeiTTS(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("讯飞TTS需要WebSocket实现，请联系开发".getBytes());
    }

    @PostMapping("/aliyun")
    @Operation(summary = "阿里云TTS语音合成")
    @PreAuthorize("hasAuthority('device:tts:manage') or hasAuthority('*')")
    public ResponseEntity<byte[]> aliyunTTS(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("阿里云TTS需要SDK集成，请联系开发".getBytes());
    }

    @PostMapping("/tencent")
    @Operation(summary = "腾讯云TTS语音合成")
    @PreAuthorize("hasAuthority('device:tts:manage') or hasAuthority('*')")
    public ResponseEntity<byte[]> tencentTTS(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("腾讯云TTS需要SDK集成，请联系开发".getBytes());
    }

    @PostMapping("/azure")
    @Operation(summary = "Azure TTS语音合成")
    @PreAuthorize("hasAuthority('device:tts:manage') or hasAuthority('*')")
    public ResponseEntity<byte[]> azureTTS(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            String subscriptionKey = (String) request.get("subscriptionKey");
            String region = request.getOrDefault("region", "eastasia").toString();
            String voice = request.getOrDefault("voice", "zh-CN-XiaoxiaoNeural").toString();
            String rate = request.getOrDefault("rate", "1").toString();

            String url = "https://" + region + ".tts.speech.microsoft.com/cognitiveservices/v1";

            String ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='zh-CN'>"
                    + "<voice name='" + voice + "'>"
                    + "<prosody rate='" + rate + "'>" + text + "</prosody>"
                    + "</voice></speak>";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
            headers.set("X-Microsoft-OutputFormat", "audio-16khz-128kbitrate-mono-mp3");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

            HttpEntity<String> entity = new HttpEntity<>(ssml, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentLength(response.getBody().length);

            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/providers")
    @Operation(summary = "获取TTS服务提供商列表")
    @PreAuthorize("hasAuthority('device:tts:view') or hasAuthority('*')")
    public ApiResponse<List<Map<String, Object>>> getProviders() {
        List<Map<String, Object>> providers = Arrays.asList(
                createProvider("browser", "浏览器内置", "使用浏览器自带的语音合成", "无限制", false),
                createProvider("baidu", "百度语音合成", "百度智能云语音合成服务", "每日50,000次", true),
                createProvider("xunfei", "讯飞语音", "科大讯飞在线语音合成", "每日500次", true),
                createProvider("aliyun", "阿里云TTS", "阿里云智能语音服务", "每月100万字符", true),
                createProvider("tencent", "腾讯云TTS", "腾讯云语音合成服务", "每月100万字符", true),
                createProvider("azure", "Azure Speech", "微软Azure认知服务", "每月50万字符", true)
        );
        return ApiResponse.success(providers);
    }

    private Map<String, Object> createProvider(String id, String name, String description, String freeQuota, boolean requiresApi) {
        Map<String, Object> provider = new HashMap<>();
        provider.put("id", id);
        provider.put("name", name);
        provider.put("description", description);
        provider.put("freeQuota", freeQuota);
        provider.put("requiresApi", requiresApi);
        return provider;
    }
}
