package com.foodtraceability.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * XSS防护过滤器
 * 用于过滤请求中的XSS攻击脚本
 */
public class XssFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(XssFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("XSS过滤器初始化");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // 检查是否是 JSON 请求
        String contentType = httpRequest.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            // JSON 请求不进行 XSS 过滤
            chain.doFilter(request, response);
        } else {
            // 非 JSON 请求进行 XSS 过滤
            XssHttpServletRequestWrapper wrappedRequest = new XssHttpServletRequestWrapper(httpRequest);
            chain.doFilter(wrappedRequest, response);
        }
    }

    @Override
    public void destroy() {
        logger.info("XSS过滤器销毁");
    }
}
