package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 系统网络工具接口
 * 提供本机局域网 IP（用于生成手机扫码访问的手持收货链接）
 */
@RestController
@RequestMapping("/v1/system/net")
@Tag(name = "系统网络工具")
public class SystemNetController {

    /**
     * 获取本机局域网 IPv4 地址列表（排除回环与链路本地地址）
     * 前后端同机部署场景下，用于生成手机可访问的二维码链接
     */
    @GetMapping("/local-ip")
    @Operation(summary = "获取本机局域网 IP")
    @PreAuthorize("isAuthenticated()")
    public Result<List<String>> getLocalIp() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            // 网络接口枚举失败时返回空列表，前端回退到当前 hostname
        }
        return Result.success(ips);
    }
}
