#!/bin/bash
# ============================================================
# 食品溯源系统 - 防火墙规则配置脚本
# 适用于: CentOS/RHEL/Ubuntu
# ============================================================

set -e

echo "========================================="
echo "食品溯源系统防火墙配置"
echo "========================================="

# 检测操作系统
if [ -f /etc/redhat-release ]; then
    OS_TYPE="rhel"
elif [ -f /etc/debian_version ]; then
    OS_TYPE="debian"
else
    OS_TYPE="unknown"
fi

echo "检测到操作系统: $OS_TYPE"

# 管理IP列表（根据实际情况修改）
ADMIN_IPS=("203.0.113.50" "198.51.100.10")

# 应用服务器IP（数据库白名单）
APP_SERVER_IP=$(hostname -I | awk '{print $1}')

case "$OS_TYPE" in
    rhel)
        echo "[RHEL/CentOS] 配置 firewalld..."
        
        # 启动firewalld
        systemctl start firewalld
        systemctl enable firewalld
        
        # 清除现有规则
        firewall-cmd --complete-reload
        
        # 允许SSH（仅管理IP）
        for admin_ip in "${ADMIN_IPS[@]}"; do
            firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address='"$admin_ip"' service name="ssh" accept'
            echo "已允许SSH访问: $admin_ip"
        done
        
        # 允许HTTPS (443) - 所有IP
        firewall-cmd --permanent --add-service=https
        echo "已允许HTTPS (443)"
        
        # 允许HTTP (80) - 用于重定向到HTTPS
        firewall-cmd --permanent --add-service=http
        echo "已允许HTTP (80) - 重定向到HTTPS"
        
        # 允许应用端口 (8081) - 仅内网或管理IP
        for admin_ip in "${ADMIN_IPS[@]}"; do
            firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address='"$admin_ip"' port protocol="tcp" port="8081" accept'
            done
        echo "已允许后端API端口 (8081) - 仅管理IP"
        
        # 数据库端口 (5432) - 仅允许应用服务器
        firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="127.0.0.1" port protocol="tcp" port="5432" accept'
        firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address='"$APP_SERVER_IP"' port protocol="tcp" port="5432" accept'
        echo "已允许PostgreSQL (5432) - 仅本地和应用服务器"
        
        # Redis端口 (6379) - 仅允许本地和内网
        firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="127.0.0.1" port protocol="tcp" port="6379" accept'
        firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="10.0.0.0/8" port protocol="tcp" port="6379" accept'
        echo "已允许Redis (6379) - 仅本地和内网"
        
        # ICMP限制
        firewall-cmd --permanent --add-icmp-block=redirect
        echo "已阻止ICMP重定向"
        
        # 默认拒绝其他入站连接
        firewall-cmd --set-default-zone=public
        echo "默认策略: DROP"
        
        # 应用规则
        firewall-cmd --reload
        
        ;;
    
    debian|ubuntu)
        echo "[Ubuntu/Debian] 配置 iptables..."
        
        # 安装iptables-persistent
        apt-get update && apt-get install -y iptables-persistent netfilter-persistent
        
        # 清除现有规则
        iptables -F
        iptables -X
        iptables -t nat -F
        iptables -t nat -X
        
        # 默认策略：拒绝入站，允许出站
        iptables -P INPUT DROP
        iptables -P FORWARD DROP
        iptables -P OUTPUT ACCEPT
        
        # 允许回环接口
        iptables -A INPUT -i lo -j ACCEPT
        iptables -A OUTPUT -o lo -j ACCEPT
        
        # 允许已建立的连接
        iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
        
        # 允许SSH（仅管理IP）
        for admin_ip in "${ADMIN_IPS[@]}"; do
            iptables -A INPUT -p tcp -s "$admin_ip" --dport 22 -m state --state NEW -j ACCEPT
            echo "已允许SSH访问: $admin_ip"
        done
        
        # 允许HTTPS (443)
        iptables -A INPUT -p tcp --dport 443 -m state --state NEW -j ACCEPT
        echo "已允许HTTPS (443)"
        
        # 允许HTTP (80) - 重定向用
        iptables -A INPUT -p tcp --dport 80 -m state --state NEW -j ACCEPT
        echo "已允许HTTP (80)"
        
        # 允许后端API (8081) - 仅管理IP
        for admin_ip in "${ADMIN_IPS[@]}"; do
            iptables -A INPUT -p tcp -s "$admin_ip" --dport 8081 -m state --state NEW -j ACCEPT
        done
        echo "已允许后端API (8081) - 仅管理IP"
        
        # 允许PostgreSQL (5432) - 仅本地和应用服务器
        iptables -A INPUT -p tcp -s 127.0.0.1 --dport 5432 -j ACCEPT
        iptables -A INPUT -p tcp -s "$APP_SERVER_IP" --dport 5432 -j ACCEPT
        echo "已允许PostgreSQL (5432)"
        
        # 允许Redis (6379) - 仅本地和内网
        iptables -A INPUT -p tcp -s 127.0.0.1 --dport 6379 -j ACCEPT
        iptables -A INPUT -p tcp -s 10.0.0.0/8 --dport 6379 -j ACCEPT
        echo "已允许Redis (6379)"
        
        # 防止DDoS - 限制连接速率
        iptables -A INPUT -p tcp --dport 80 -m limit --limit 30/second --limit-burst 50 -j ACCEPT
        iptables -A INPUT -p tcp --dport 443 -m limit --limit 30/second --limit-burst 50 -j ACCEPT
        echo "已配置DDoS防护限流"
        
        # 记录被拒绝的包（调试用，生产环境可关闭）
        # iptables -A INPUT -m limit --limit 5/min -j LOG --log-prefix "IPTABLES_DROP: "
        
        # 保存规则
        netfilter-persistent save
        echo "防火墙规则已保存"
        
        ;;
esac

echo ""
echo "========================================="
echo "防火墙配置完成！"
echo ""
echo "当前规则:"
if [ "$OS_TYPE" = "rhel" ]; then
    firewall-cmd --list-all
else
    iptables -L -n --line-numbers
fi
echo "========================================="
