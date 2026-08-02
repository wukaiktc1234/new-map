package com.foodtraceability.config;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * HTTPS配置类
 * 配置HTTP到HTTPS的重定向
 */
@Configuration
public class HttpsConfig implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    /**
     * 配置HTTP到HTTPS的重定向
     */
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        // 暂时注释掉HTTP连接器配置，避免端口冲突
        // if (factory instanceof TomcatServletWebServerFactory tomcatFactory) {
        //     // 创建HTTP连接器
        //     Connector httpConnector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        //     // 设置HTTP端口
        //     httpConnector.setPort(9999);
        //     // 设置重定向到HTTPS端口
        //     httpConnector.setRedirectPort(8443);
        //     // 添加HTTP连接器
        //     tomcatFactory.addAdditionalTomcatConnectors(httpConnector);
        // }
    }
}
