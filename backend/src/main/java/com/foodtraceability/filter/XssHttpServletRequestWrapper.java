package com.foodtraceability.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * XSS防护的HttpServletRequest包装器
 * 用于过滤请求参数中的XSS攻击脚本
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private static final Logger logger = LoggerFactory.getLogger(XssHttpServletRequestWrapper.class);

    private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe>(.*?)</iframe>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<object>(.*?)</object>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<embed>(.*?)</embed>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<link>(.*?)</link>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<style>(.*?)</style>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onerror=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onclick=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onfocus=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onblur=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onchange=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onmouseover=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onmouseout=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onkeydown=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onkeyup=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onkeypress=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onsubmit=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onreset=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onselect=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onresize=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onabort=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onunload=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onbeforeunload=(.*?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expression", Pattern.CASE_INSENSITIVE)
    };

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                values[i] = cleanXss(values[i]);
            }
        }
        return values;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> map = super.getParameterMap();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String[] values = entry.getValue();
            for (int i = 0; i < values.length; i++) {
                values[i] = cleanXss(values[i]);
            }
        }
        return map;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanXssHeader(value);
    }

    private String cleanXss(String value) {
        if (value == null) {
            return null;
        }
        
        String cleanValue = value;
        for (Pattern pattern : XSS_PATTERNS) {
            cleanValue = pattern.matcher(cleanValue).replaceAll("");
        }
        
        return cleanValue;
    }
    
    private String cleanXssHeader(String value) {
        if (value == null) {
            return null;
        }
        
        String cleanValue = value;
        for (Pattern pattern : XSS_PATTERNS) {
            cleanValue = pattern.matcher(cleanValue).replaceAll("");
        }
        
        cleanValue = cleanValue.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#39;");
        
        return cleanValue;
    }
}
