package com.foodtraceability.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

/**
 * XSS防护工具类
 * 提供输入过滤和输出转义功能，防止XSS攻击
 *
 * 使用说明：
 * 1. 所有用户输入在存储前应调用 sanitize() 方法进行过滤
 * 2. 所有用户内容在HTML渲染前应调用 escapeHtml() 方法进行转义
 * 3. 避免使用 v-html 渲染用户内容，优先使用文本插值 {{ }}
 */
public class XssProtectionUtil {

    private static final Logger logger = LoggerFactory.getLogger(XssProtectionUtil.class);

    /**
     * 危险的HTML标签模式
     */
    private static final Pattern[] DANGEROUS_HTML_PATTERNS = {
        // 脚本标签
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        // 事件处理器（onclick, onerror等）
        Pattern.compile("\\s*on\\w+\\s*=\\s*[\"']?[^\"]*[\"']?", Pattern.CASE_INSENSITIVE),
        // JavaScript协议
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        // 表达式
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        // iframe/embed/object等危险标签
        Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // form标签
        Pattern.compile("<form[^>]*>.*?</form>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        // input标签（防止表单注入）
        Pattern.compile("<input[^>]*>", Pattern.CASE_INSENSITIVE),
        // style标签中的表达式
        Pattern.compile("style\\s*=\\s*[\"'][^\"']*expression", Pattern.CASE_INSENSITIVE),
        // import语句
        Pattern.compile("@import", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 需要转义的HTML特殊字符
     */
    private static final char[][] HTML_ESCAPE_CHARS = {
        {'&', '\0'},  // &amp; - 使用占位符，实际转义在下面处理
        {'<', '\0'},
        {'>', '\0'},
        {'"', '\0'},
        {'\'', '\0'},
        {'/', '\0'}
    };

    private static final String[] HTML_ESCAPE_VALUES = {
        "&amp;",
        "&lt;",
        "&gt;",
        "&quot;",
        "&#39;",
        "&#x2F;"
    };

    /**
     * 清理用户输入，移除潜在的XSS攻击代码
     *
     * @param input 用户输入字符串
     * @return 清理后的安全字符串
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // 移除危险的HTML模式和JavaScript代码
        for (Pattern pattern : DANGEROUS_HTML_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }

        // 记录清理操作（仅在检测到危险内容时）
        if (!result.equals(input)) {
            logger.warn("检测到并清理了潜在的XSS攻击代码");
            logger.debug("原始输入长度: {}, 清理后长度: {}", input.length(), result.length());
        }

        return result.trim();
    }

    /**
     * 转义HTML特殊字符，用于安全地显示用户内容
     *
     * @param input 包含可能HTML字符的字符串
     * @return 转义后的安全字符串
     */
    public static String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder escaped = new StringBuilder(input.length() * 2);

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            boolean escapedChar = false;

            for (int j = 0; j < HTML_ESCAPE_CHARS.length; j++) {
                if (c == HTML_ESCAPE_CHARS[j][0]) {
                    escaped.append(HTML_ESCAPE_VALUES[j]);
                    escapedChar = true;
                    break;
                }
            }

            if (!escapedChar) {
                escaped.append(c);
            }
        }

        return escaped.toString();
    }

    /**
     * 验证URL是否安全（不包含javascript:等危险协议）
     *
     * @param url 待验证的URL
     * @return true如果URL安全，false如果不安全
     */
    public static boolean isSafeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase().trim();

        // 检查危险协议
        String[] dangerousProtocols = {"javascript:", "vbscript:", "data:text/html"};

        for (String protocol : dangerousProtocols) {
            if (lowerUrl.startsWith(protocol)) {
                logger.warn("检测到危险URL协议: {}", protocol);
                return false;
            }
        }

        // 只允许http和https协议
        return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://") ||
               lowerUrl.startsWith("/") || lowerUrl.startsWith("#");
    }

    /**
     * 清理文件名，移除路径遍历字符和危险字符
     *
     * @param filename 文件名
     * @return 安全的文件名
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed_file";
        }

        // 移除路径遍历字符
        String sanitized = filename.replaceAll("..", "")
                                  .replaceAll("/", "_")
                                  .replaceAll("\\\\", "_")
                                  .replaceAll("%00", "")  // Null字节
                                  .replaceAll("%0a", "")  // 换行符
                                  .replaceAll("%0d", "");  // 回车符

        // 移除控制字符
        sanitized = sanitized.replaceAll("[\\p{Cntrl}]", "");

        // 限制文件名长度
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        // 如果清理后为空，返回默认名称
        if (sanitized.trim().isEmpty()) {
            return "unnamed_file";
        }

        return sanitized.trim();
    }

    /**
     * 检查字符串是否包含潜在的XSS攻击代码
     *
     * @param input 待检查的字符串
     * @return true如果包含可疑内容，false如果安全
     */
    public static boolean containsSuspiciousContent(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase();

        // 检查常见XSS攻击特征
        String[] suspiciousPatterns = {
            "<script",
            "javascript:",
            "onerror=",
            "onload=",
            "onclick=",
            "alert(",
            "document.cookie",
            "eval(",
            "expression("
        };

        for (String pattern : suspiciousPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }

        return false;
    }
}
