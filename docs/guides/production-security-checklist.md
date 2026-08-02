# 食品溯源系统 - 生产环境安全部署检查清单

> **项目信息**
> - 项目名称：食品溯源系统 (Food Traceability System)
> - 技术栈：Spring Boot 3.2.0 + PostgreSQL 18 + Redis + RabbitMQ + Vue.js 3.3.8
> - 服务地址：后端 http://localhost:8081/api，前端 http://localhost:3000/
> - 文档版本：v1.0 | 更新日期：2026-04-02

---

## 目录

1. [操作系统级安全配置](#1-操作系统级安全配置)
2. [JDK/JVM安全配置](#2-jdkjvm安全配置)
3. [PostgreSQL数据库安全](#3-postgresql数据库安全)
4. [Redis安全配置](#4-redis安全配置)
5. [RabbitMQ安全配置](#5-rabbitmq安全配置)
6. [Spring Boot应用安全配置](#6-spring-boot应用安全配置)
7. [Nginx反向代理安全配置](#7-nginx反向代理安全配置)
8. [网络安全配置](#8-网络安全配置)
9. [日志和监控安全](#9-日志和监控安全)
10. [部署自动化安全](#10-部署自动化安全)
11. [应急响应预案](#11-应急响应预案)

---

## 1. 操作系统级安全配置

### 1.1 内核参数调优（sysctl.conf）

**文件位置**: `/etc/sysctl.conf`

```bash
# ============================================
# 网络安全参数配置
# ============================================

# 禁止IP转发（非路由器场景）
# 默认值: 0 | 建议值: 0
net.ipv4.ip_forward = 0

# 禁止发送/接受ICMP重定向消息
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv6.conf.all.accept_redirects = 0
net.ipv6.conf.default.accept_redirects = 0

# 启用SYN Cookie防护（防SYN Flood攻击）
net.ipv4.tcp_syncookies = 1

# SYN队列最大长度（默认512 -> 建议值4096）
net.ipv4.tcp_max_syn_backlog = 4096

# TCP连接重用和回收
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_tw_recycle = 1

# 保持连接超时时间（默认7200秒 -> 建议值600秒）
net.ipv4.tcp_keepalive_time = 600
net.ipv4.tcp_keepalive_intvl = 30
net.ipv4.tcp_keepalive_probes = 10

# 最大打开文件数（默认1048576 -> 建议值2097152）
fs.file-max = 2097152

# 禁止源路由包
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0

# 启用反向路径过滤（防止IP欺骗）
net.ipv4.conf.all.rp_filter = 1
net.ipv4.conf.default.rp_filter = 1

# 记录可疑数据包
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.log_martians = 1

# ICMP广播忽略
net.icmp_echo_ignore_broadcasts = 1

# 虚拟内存设置（防止OOM）
vm.swappiness = 10

# 核心转储限制（生产环境禁用核心转储）
fs.suid_dumpable = 0
```

**生效命令**: `sudo sysctl -p`

**检查清单**:
- [ ] 已配置sysctl.conf内核安全参数
- [ ] 已执行`sysctl -p`使配置生效
- [ ] 已验证SYN Cookie防护启用状态
- [ ] 已验证反向路径过滤启用状态
- [ ] 已验证禁止IP转发配置

---

### 1.2 文件描述符限制

**文件位置**: `/etc/security/limits.conf`

```bash
# 应用用户限制（以foodtrace用户为例）
foodtrace        soft    nofile          65536
foodtrace        hard    nofile          65536
foodtrace        soft    nproc           65536
foodtrace        hard    nproc           65536
foodtrace        soft    memlock         unlimited
foodtrace        hard    memlock         unlimited
foodtrace        soft    stack           8192
foodtrace        hard    stack           8192

# Root用户限制
root             soft    nofile          65536
root             hard    nofile          65536

# 全局默认限制
*                soft    nofile          65536
*                hard    nofile          65536
*                soft    nproc           65536
*                hard    nproc           65536
```

**验证命令**: `ulimit -n && ulimit -u`

**检查清单**:
- [ ] 已配置limits.conf文件描述符限制
- [ ] 应用运行用户已添加到limits配置
- [ ] 已验证文件描述符限制生效（>=65536）

---

### 1.3 用户权限隔离

**创建专用运行用户**:
```bash
# 创建食品溯源系统专用用户组
sudo groupadd foodtrace_group

# 创建专用运行用户（禁止登录、无家目录）
sudo useradd --system --no-create-home \
  --home-dir /opt/food-traceability \
  --shell /usr/sbin/nologin \
  --gid foodtrace_group foodtrace

# 设置用户密码锁定（防止被用于登录）
sudo passwd -l foodtrace

# 创建应用目录结构并设置权限
sudo mkdir -p /opt/food-traceability/{app,logs,config,uploads,backups}
sudo chown -R foodtrace:foodtrace_group /opt/food-traceability
sudo chmod -R 750 /opt/food-traceability/{app,config}
sudo chmod -R 770 /opt/food-traceability/{logs,uploads}
sudo chmod -R 700 /opt/food-traceability/backups

# 敏感配置文件权限（仅owner可读写）
sudo chmod 600 /opt/food-traceability/config/application-prod.yml
sudo chmod 600 /opt/food-traceability/config/*.jks
sudo chmod 600 /opt/food-traceability/config/*.pem
```

**检查清单**:
- [ ] 已创建专用运行用户（foodtrace）并设置nologin shell
- [ ] 已创建专用用户组（foodtrace_group）
- [ ] 已配置正确的目录权限（750/770/700/600）
- [ ] 敏感配置文件权限设置为600
- [ ] 已验证用户无法直接登录系统

---

### 1.4 SSH加固配置

**文件位置**: `/etc/ssh/sshd_config`

```bash
# 监听端口（建议修改为非标准端口）
Port 2222
ListenAddress 192.168.1.100

# 禁止root远程登录
PermitRootLogin no
PermitEmptyPasswords no
MaxAuthTries 3
LoginGraceTime 60

# 仅允许特定用户登录
AllowUsers deploy@192.168.1.0/24 admin@10.0.0.0/8
DenyUsers root nobody

# 强制公钥认证，禁用密码认证
PubkeyAuthentication yes
PasswordAuthentication no
Protocol 2

# 禁用X11和端口转发
X11Forwarding no
AllowTcpForwarding no
AllowAgentForwarding no

# 日志级别
LogLevel VERBOSE
MaxSessions 3
UseDNS no
StrictModes yes
```

**重启服务**: `sudo systemctl restart sshd`

**检查清单**:
- [ ] 已修改SSH监听端口为非22端口
- [ ] 已禁止root远程登录
- [ ] 已禁用密码认证（强制密钥登录）
- [ ] 已限制允许登录的用户列表
- [ ] 最大认证尝试次数<=3
- [ ] 已禁用X11和端口转发
- [ ] 登录超时时间<=60秒
- [ ] 已测试SSH密钥登录正常工作

---

### 1.5 防火墙规则配置

#### 方案A：firewalld（CentOS/RHEL/Rocky Linux）

```bash
# 启动并启用firewalld
sudo systemctl start firewalld && sudo systemctl enable firewalld

# 创建自定义区域
sudo firewall-cmd --permanent --new-zone=foodtrace_zone
sudo firewall-cmd --permanent --set-target=DROP --zone=foodtrace_zone

# SSH访问（修改后的端口，仅管理网段）
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=2222/tcp --source=192.168.1.0/24

# HTTP/HTTPS流量（通过Nginx）
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-service=http
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-service=https

# Spring Boot应用端口（仅限内部网络）
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=8081/tcp --source=127.0.0.1
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=8081/tcp --source=10.0.0.0/8

# PostgreSQL（仅限应用服务器）
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=5432/tcp --source=10.0.0.50
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=5432/tcp --source=10.0.0.51

# Redis（仅限应用服务器）
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=6379/tcp --source=10.0.0.50
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=6379/tcp --source=10.0.0.51

# RabbitMQ（应用服务器+管理网段）
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=5672/tcp --source=10.0.0.50
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=15672/tcp --source=192.168.1.0/24

# 监控端口（Prometheus/Grafana）
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=9090/tcp --source=192.168.1.0/24
sudo firewall-cmd --permanent --zone=foodtrace_zone --add-port=3000/tcp --source=192.168.1.0/24

# 绑定网卡并重载规则
sudo firewall-cmd --permanent --change-interface=eth0 --zone=foodtrace_zone
sudo firewall-cmd --reload
```

#### 方案B：iptables关键规则

```bash
# 清空现有规则并设置默认策略
sudo iptables -F && sudo iptables -t nat -F
sudo iptables -P INPUT DROP && sudo iptables -P FORWARD DROP && sudo iptables -P OUTPUT ACCEPT

# 允许回环接口和已建立连接
sudo iptables -A INPUT -i lo -j ACCEPT
sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# 允许必要的服务端口（示例）
sudo iptables -A INPUT -p tcp -s 192.168.1.0/24 --dport 2222 -m state --state NEW -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 443 -m state --state NEW -j ACCEPT
sudo iptables -A INPUT -p tcp -s 10.0.0.0/8 --dport 8081 -m state --state NEW -j ACCEPT
sudo iptables -A INPUT -p tcp -s 10.0.0.50 --dport 5432 -m state --state NEW -j ACCEPT
sudo iptables -A INPUT -p tcp -s 10.0.0.50 --dport 6379 -m state --state NEW -j ACCEPT

# 拒绝非法TCP标志组合（端口扫描检测）
sudo iptables -A INPUT -p tcp --tcp-flags ALL NONE -j DROP
sudo iptables -A INPUT -p tcp --tcp-flags ALL ALL -j DROP
sudo iptables -A INPUT -p tcp --tcp-flags ALL FIN,URG,PSH -j DROP
```

**检查清单**:
- [ ] 已安装并启动防火墙服务（firewalld/iptables）
- [ ] 已配置默认拒绝策略（DROP）
- [ ] 数据库端口已限制为仅应用服务器可访问
- [ ] Redis/RabbitMQ端口已限制为仅内网可访问
- [ ] 已配置防端口扫描规则
- [ ] 已保存防火墙规则并设置开机自启

---

### 1.6 SELinux/AppArmor配置

#### CentOS/RHEL - SELinux

```bash
# 强制执行模式
sudo setenforce 1
# 编辑/etc/selinux/config: SELINUX=enforcing

# 允许非标准端口
sudo semanage port -a -t http_port_t -p tcp 8081
sudo semanage port -a -t postgresql_port_t -p tcp 5432
sudo semanage port -a -t redis_port_t -p tcp 6379
sudo semanage port -a -t amqp_port_t -p tcp 5672
```

#### Ubuntu/Debian - AppArmor

```bash
# 检查状态: sudo aa-status
# 加载自定义配置后测试:
sudo aa-complain /opt/food-traceability/app/*.jar  # 测试模式
sudo aa-enforce /opt/food-traceability/app/*.jar   # 强制模式
```

**检查清单**:
- [ ] 已启用SELinux/AppArmor（Enforcing模式）
- [ ] 已配置正确的文件上下文/访问控制
- [ ] 已在Complain模式下完成测试
- [ ] 已切换到Enforce模式

---

### 1.7 审计日志启用（auditd）

```bash
# 安装: sudo yum install audit (CentOS) 或 sudo apt-get install auditd (Ubuntu)
# 启动: sudo systemctl enable --now auditd

# 配置审计规则 (/etc/audit/rules.d/food-trace.rules):
-w /opt/food-traceability/config/ -p wa -k food_trace_config
-w /etc/passwd -p wa -k identity_modification
-w /etc/shadow -p wa -k identity_modification
-w /etc/sudoers -p wa -k privilege_escalation
-w /etc/ssh/sshd_config -p wa -k ssh_config_change
-a always,exit -F arch=b64 -S execve -F euid=0 -F key=privileged_command
-a always,exit -F arch=b64 -S bind -F key=network_changes
-a always,exit -F arch=b64 -S connect -F key=network_connections
-f 2  # 规则加载失败时暂停系统

# 重载: sudo systemctl restart auditd
# 查看: sudo auditctl -l
# 搜索: ausearch -k food_trace_config
```

**检查清单**:
- [ ] 已安装并启动auditd服务
- [ ] 已配置关键文件监控规则
- [ ] 已配置特权命令监控和网络操作监控
- [ ] 已配置磁盘空间管理策略
- [ ] 已设置告警通知

---

### 1.8 自动更新策略

```bash
# CentOS/RHEL: yum install dnf-automatic
# 配置 /etc/dnf/automatic.conf:
download_updates = yes; apply_updates = yes
exclude = kernel* postgresql* redis*
email_to = ops-team@food-trace.com
random_sleep = 3600
# 启用: sudo systemctl enable --now dnf-automatic.timer

# Ubuntu/Debian: apt-get install unattended-upgrades
# 配置 /etc/apt/apt.conf.d/50unattended-upgrades:
Unattended-Upgrade::Allowed-Origins { "${distro_id}:${distro_codename}-security"; };
Unattended-Upgrade::Package-Blacklist { "postgresql*"; "redis-server"; };
Unattended-Upgrade::Automatic-Reboot "false";
Unattended-Upgrade::Mail "ops-team@food-trace.com";
```

**检查清单**:
- [ ] 已安装自动更新工具
- [ ] 已配置仅安装安全更新
- [ ] 已排除关键服务包（kernel/postgresql/redis）
- [ ] 已配置更新通知邮件
- [ ] 已启用定时任务

---

## 2. JDK/JVM安全配置

### 2.1 java.security配置修改

**文件位置**: `$JAVA_HOME/conf/security/java.security`

```properties
# 禁用弱SSL/TLS协议（保留TLSv1.2和TLSv1.3）
jdk.tls.disabledAlgorithms=SSLv3, TLSv1, TLSv1.1, RC4, DES, MD5withRSA, \
    DH keySize < 2048, EC keySize < 224, 3DES_EDE_CBC, AES128

# 启用的TLS协议
jdk.tls.client.protocols=TLSv1.3,TLSv1.2

# 禁用弱加密套件
jdk.tls.legacyAlgorithms=NULL, MD5, SHA1, DSA, RSA keySize < 2048, \
    EC keySize < 224, 3DES_EDE_CBC, AES128

# 证书指纹算法（禁用MD2/MD5）
jdk.certpath.disabledAlgorithms=MD2, MD5, RSA keySize < 1024
```

**验证命令**:
```bash
java -version
echo | openssl s_client -connect localhost:8081 -tls1_2 2>/dev/null | grep Protocol
echo | openssl s_client -connect localhost:8081 -ssl3 2>/dev/null || echo "SSLv3已正确禁用"
```

**检查清单**:
- [ ] 已禁用SSLv3、TLSv1.0、TLSv1.1等弱协议
- [ ] 已禁用RC4、DES、MD5等弱加密算法
- [ ] 已设置最小密钥长度要求（RSA >= 2048位）
- [ ] 已备份原始java.security文件

---

### 2.2 JVM启动安全参数（生产环境模板）

```bash
#!/bin/bash
# 生产环境JVM启动参数
JAVA_OPTS="\
-Xms2g -Xmx4g -Xmn1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/food-traceability/logs/heap-dump.hprof \
-Djava.rmi.server.disableURLCodes=true \
-Dcom.sun.management.jmxremote=false \
-Dnetworkaddress.cache.ttl=60 \
-Dnetworkaddress.cache.negative.ttl=10 \
-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai \
-Dspring.profiles.active=prod \
-Xlog:gc*:file=/opt/food-traceability/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=20m \
-XX:-OmitStackTraceInFastThrow \
-Djava.awt.headless=true \
-Djava.net.preferIPv4Stack=true"

export JAVA_OPTS
nohup java $JAVA_OPTS -jar /opt/food-traceability/app/food-traceability.jar \
  > /opt/food-traceability/logs/stdout.log 2>&1 &
echo $! > /opt/food-traceability/app.pid
```

**systemd服务单元文件** (`/etc/systemd/system/food-trace.service`):

```ini
[Unit]
Description=Food Traceability System
After=network.target postgresql.service redis-server.service rabbitmq-server.service

[Service]
Type=simple
User=foodtrace
Group=foodtrace_group
WorkingDirectory=/opt/food-traceability/app
Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk"
Environment="JAVA_OPTS=-Xms2g -Xmx4g -Xmn1g -XX:+UseG1GC \
  -Djava.rmi.server.disableURLCodes=true -Dcom.sun.management.jmxremote=false \
  -Dnetworkaddress.cache.ttl=60 -Dspring.profiles.active=prod \
  -XX:+HeapDumpOnOutOfMemoryError"
ExecStart=/usr/lib/jvm/java-17-openjdk/bin/java $JAVA_OPTS -jar food-traceability.jar
Restart=on-failure; RestartSec=10; StartLimitBurst=5
StandardOutput=append:/opt/food-traceability/logs/service.log
StandardError=append:/opt/food-traceability/logs/service-error.log
LimitNOFILE=65535; LimitNPROC=65535; MemoryMax=8G
PrivateTmp=yes
EnvironmentFile=-/opt/food-traceability/config/.env

[Install]
WantedBy=multi-user.target
```

**检查清单**:
- [ ] 已禁用RMI远程调用 (`disableURLCodes`)
- [ ] 已禁用JMX远程管理或配置严格访问控制
- [ ] 已配置DNS缓存TTL（<=60秒）
- [ ] 已启用G1垃圾收集器和堆转储配置
- [ ] 已配置GC日志（含时间戳和原因）
- [ ] 已创建systemd服务单元文件并配置资源限制
- [ ] 已配置自动重启策略

---

## 3. PostgreSQL数据库安全

### 3.1 pg_hba.conf客户端认证配置

**文件位置**: `/var/lib/pgsql/data/pg_hba.conf`

```bash
# 格式: TYPE  DATABASE  USER  ADDRESS  METHOD

# 本地Unix域套接字
local   all             postgres                                peer
local   all             food_trace_app                          scram-sha-256

# IPv4本地连接
host    all             all             127.0.0.1/32            scram-sha-256

# 应用服务器连接（内网IP段）
host    food_traceability   food_trace_app   10.0.0.50/32       scram-sha-256
host    food_traceability   food_trace_app   10.0.0.51/32       scram-sha-256

# 备份服务器连接
host    food_traceability   backup_user      10.0.0.100/32      scram-sha-256

# IPv6本地连接
host    all             all             ::1/128                 scram-sha-256

# 拒绝所有其他连接（必须放在最后！）
host    all             all             0.0.0.0/0               reject
host    all             all             ::/0                    reject
```

**检查清单**:
- [ ] 使用scram-sha-256作为认证方法（不使用MD5/trust）
- [ ] 应用服务器已限制为指定内网IP
- [ ] 最后一条规则拒绝所有其他连接
- [ ] 备份用户有独立的最小权限账户

---

### 3.2 postgresql.conf安全参数

**文件位置**: `/var/lib/pgsql/data/postgresql.conf`

```ini
# 连接和认证
listen_addresses = '10.0.0.1'      # 仅内网IP，不用 *
port = 5432
max_connections = 200
superuser_reserved_connections = 5

# 密码加密（强烈建议SCRAM-SHA-256）
password_encryption = scram-sha-256

# SSL/TLS加密（生产环境必须启用！）
ssl = on
ssl_cert_file = '/etc/postgresql/certs/server.crt'
ssl_key_file = '/etc/postgresql/certs/server.key'
ssl_ca_file = '/etc/postgresql/certs/ca.crt'
ssl_min_protocol_version = 'TLSv1.2'
ssl_ciphers = 'HIGH:!aNULL:!eNULL:!3DES:!DES:!MD5:!RC4:!SEED:!IDEA:!CAMELLIA'

# 日志和审计
logging_collector = on
log_directory = 'pg_log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_file_mode = 0600
log_statement = 'ddl'              # 记录DDL语句
log_min_duration_statement = 5000  # 慢查询阈值5秒
log_disconnections = on            # 记录断开事件
log_connections = on               # 记录连接事件
log_lock_waits = on                # 记录锁等待

# 性能和安全
shared_buffers = 1GB               # 物理内存25%左右
work_mem = 64MB
maintenance_work_mem = 512MB
temp_file_limit = 1073741824       # 临时文件大小限制1GB
statement_timeout = 300000         # 语句超时5分钟
lock_timeout = 30000               # 锁定超时30秒
idle_in_transaction_session_timeout = 600000  # 空闲事务超时10分钟

# 安全相关
copy_from_program_allowed = off    # 禁止COPY执行程序
```

**检查清单**:
- [ ] listen_addresses绑定到内网IP（不是 * 或 0.0.0.0）
- [ ] password_encryption设置为scram-sha-256
- [ ] SSL已启用且证书路径正确
- [ ] 最小TLS版本为TLSv1.2+
- [ ] 已配置慢查询日志和连接日志
- [ ] 已设置statement_timeout和lock_timeout
- [ ] 已禁止copy_from_program

---

### 3.3 用户权限最小化SQL命令

```sql
-- 以超级用户身份连接PostgreSQL执行以下操作：

-- 1. 创建应用程序专用用户（非超级用户！）
CREATE USER food_trace_app WITH PASSWORD 'Strong_Pass_Here_123!'
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT LOGIN CONNECTION LIMIT 50;

-- 2. 创建只读用户
CREATE USER food_trace_readonly WITH PASSWORD 'Readonly_Pass_456!'
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT LOGIN CONNECTION LIMIT 10;

-- 3. 授予数据库连接权限
GRANT CONNECT ON DATABASE food_traceability TO food_trace_app;
GRANT CONNECT ON DATABASE food_traceability TO food_trace_readonly;

-- 4. 切换到目标数据库并授予权限
\c food_traceability

-- 5. Schema权限
GRANT USAGE ON SCHEMA public TO food_trace_app;
GRANT CREATE ON SCHEMA public TO food_trace_app;
GRANT USAGE ON SCHEMA public TO food_trace_readonly;

-- 6. DEFAULT PRIVILEGES（自动应用于未来新建的表）
ALTER DEFAULT PRIVILEGES FOR ROLE food_trace_app IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO food_trace_app;
ALTER DEFAULT PRIVILEGES FOR ROLE food_trace_app IN SCHEMA public
  GRANT SELECT, USAGE ON SEQUENCES TO food_trace_app;
ALTER DEFAULT PRIVILEGES FOR ROLE food_trace_app IN SCHEMA public
  GRANT SELECT ON TABLES TO food_trace_readonly;

-- 7. 授予已有表的权限
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN SELECT tablename FROM pg_tables WHERE schemaname='public' LOOP
    EXECUTE format('GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE %I TO food_trace_app', r.tablename);
    EXECUTE format('GRANT SELECT ON TABLE %I TO food_trace_readonly', r.tablename);
  END LOOP;
END $$;

-- 8. 撤销PUBLIC角色的危险权限（重要！）
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE food_traceability FROM PUBLIC;

-- 9. RBAC角色层次结构
CREATE ROLE ft_developer;
CREATE ROLE ft_analyst;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ft_developer;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ft_analyst;
GRANT ft_developer TO food_trace_app;
GRANT ft_analyst TO food_trace_readonly;
```

**权限审计查询**:
```sql
-- 查找拥有SUPERUSER权限的用户（应尽可能少）
SELECT usename, usesuper FROM pg_user WHERE usesuper = true;

-- 检查PUBLIC角色权限（不应过多）
SELECT grantee, privilege_type FROM information_schema.table_privileges
WHERE grantee = 'PUBLIC' LIMIT 20;
```

**检查清单**:
- [ ] 应用程序用户不是超级用户
- [ ] 已设置强密码（>=16字符混合字符类型）
- [ ] 已限制用户连接数
- [ ] 已撤销PUBLIC角色的危险权限
- [ ] 已配置DEFAULT PRIVILEGES实现权限继承
- [ ] 已实施RBAC角色模型
- [ ] 已运行权限审计脚本确认配置正确

---

### 3.4 SSL/TLS加密配置

```bash
# 创建证书目录并生成证书
sudo mkdir -p /etc/postgresql/certs && sudo chmod 700 $_
sudo chown postgres:postgres /etc/postgresql/certs

# 生成CA证书（有效期10年）
sudo -u postgres openssl req -new -x509 -days 3650 \
  -keyout /etc/postgresql/certs/ca.key -out /etc/postgresql/certs/ca.crt \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=Food Trace/CN=PG CA"

# 生成服务器证书（有效期1年）
sudo -u postgres openssl req -newkey rsa:4096 -days 365 -nodes \
  -keyout /etc/postgresql/certs/server.key -out /etc/postgresql/certs/server.csr \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=Food Trace/CN=$(hostname)"

# CA签发证书
sudo -u postgres openssl x509 -req -days 365 \
  -in /etc/postgresql/certs/server.csr -CA /etc/postgresql/certs/ca.crt \
  -CAkey /etc/postgresql/certs/ca.key -CAcreateserial \
  -out /etc/postgresql/certs/server.crt

# 设置权限（私钥必须是600！）
sudo chmod 600 /etc/postgresql/certs/server.key
sudo chmod 644 /etc/postgresql/certs/server.crt /etc/postgresql/certs/ca.crt

# 重启PostgreSQL使SSL生效
sudo systemctl restart postgresql

# 验证SSL连接
psql "host=10.0.0.1 dbname=food_traceability user=food_trace_app sslmode=require"
psql -c "SELECT ssl, version, cipher FROM pg_stat_ssl WHERE pid = pg_backend_pid();"
```

**检查清单**:
- [ ] 已生成有效的SSL/TLS证书
- [ ] 私钥文件权限设置为600
- [ ] 已在postgresql.conf中启用SSL
- [ ] 最小TLS版本为TLSv1.2+
- [ ] 已验证SSL连接正常工作
- [ ] （生产环境）已配置Let's Encrypt证书自动续期

---

### 3.5 备份加密策略

```bash
#!/bin/bash
# PostgreSQL加密备份脚本 (/opt/food-traceability/scripts/backup-postgres.sh)
set -e
DB_HOST="10.0.0.1"; DB_NAME="food_traceability"; DB_USER="backup_user"
BACKUP_DIR="/opt/food-traceability/backups/postgresql"
ENCRYPTION_KEY="/opt/food-traceability/config/backup-key.bin"
DATE=$(date +%Y%m%d_%H%M%S)

# 使用pg_dump + GPG加密备份
BACKUP_FILE="${BACKUP_DIR}/daily/${DB_NAME}_${DATE}.dump.gpg"
PGPASSWORD="${BACKUP_PASSWORD}" pg_dump -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  --format=custom --compress=9 | gpg --batch --yes \
  --passphrase-file ${ENCRYPTION_KEY} --symmetric --cipher-algo AES256 -o ${BACKUP_FILE}

# 清理过期备份（日备30天/周备8周/月备12个月）
find ${BACKUP_DIR}/daily -name "*.gpg" -mtime +30 -delete
find ${BACKUP_DIR}/weekly -name "*.gpg" -mtime +56 -delete
find ${BACKUP_DIR}/monthly -name "*.gpg" -mtime +360 -delete

# Cron定时任务: 每天2:00执行
# 0 2 * * * /opt/food-traceability/scripts/backup-postgres.sh >> /var/log/backup.log 2>&1
```

**生成加密密钥**: `dd if=/dev/urandom of=/opt/food-traceability/config/backup-key.bin bs=32 count=1 && chmod 400 $_`

**检查清单**:
- [ ] 已编写加密备份脚本（AES256）
- [ ] 已生成加密密钥并妥善保管（权限400）
- [ ] 已配置每日/每周/每月备份策略
- [ ] 已配置备份保留期和清理任务
- [ ] 已配置cron定时执行
- [ ] 已实现远程备份存储（AWS S3/对象存储）

---

### 3.6 审计日志配置（pg_audit）

```bash
# 安装扩展: yum install postgresql18-contrib 或 apt-get install postgresql-18-pgaudit

# 在postgresql.conf中添加:
shared_preload_libraries = 'pg_audit'

# pg_audit配置
pg_audit.log = 'ddl, role, misc'       # 审计类别
pg_audit.log_catalog = off              # 不记录系统catalog操作
pg_audit.log_client = on                # 记录客户端信息（IP等）
pg_audit.log_level = log
pg_audit.log_relation = on              # 记录操作的表名
pg_audit.log_hostname = on              # 记录主机名

# 重启PostgreSQL后，在数据库中创建扩展:
CREATE EXTENSION IF NOT EXISTS pgaudit;
```

**检查清单**:
- [ ] 已安装pg_audit扩展并在preload加载
- [ ] 已配置审计类别（DDL/ROLE/MISC）
- [ ] 已启用客户端信息和关系名记录
- [ ] 已集成ELK或其他日志分析平台

---

## 4. Redis安全配置

### 4.1 redis.conf安全设置

**文件位置**: `/etc/redis/redis.conf`

```bash
# ============================================
# 网络绑定和访问控制
# ============================================
bind 10.0.0.1                    # 仅内网IP，不用0.0.0.0
protected-mode yes                # 必须启用保护模式
port 6379
tcp-keepalive 300
tcp-backlog 1024
timeout 300                       # 客户端空闲超时（秒）
maxclients 2000                   # 最大客户端连接数

# ============================================
# 认证配置（必须设置强密码！）
# ============================================
requirepass Your_Very_Strong_Redis_Password_Here_1234567890ABCDEF!

# ============================================
# 命令禁用和重命名（生产环境必须！）
# ============================================
rename-command FLUSHDB ""           # 禁用清空数据库
rename-command FLUSHALL ""         # 禁用清空所有数据库
rename-command CONFIG ""           # 禁用修改配置
rename-command DEBUG ""            # 禁用调试命令
rename-command SHUTDOWN ""         # 禁用关闭服务器
rename-command KEYS "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"  # 重命名敏感命令
rename-command INFO "YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY"

# ============================================
# TLS/SSL配置（生产环境强烈建议启用）
# ============================================
tls-port 6380
port 0                            # 禁用非TLS端口
tls-cert-file /etc/redis/certs/redis.crt
tls-key-file /etc/redis/certs/redis.key
tls-ca-cert-file /etc/redis/certs/ca.crt
tls-min-version TLSv1.2
tls-ciphers HIGH:!aNULL:!MD5:!RC4:!DES:!3DES

# ============================================
# 持久化和内存管理
# ============================================
save 900 1; save 300 10; save 60 10000
appendonly yes                     # 启用AOF持久化
appendfsync everysec
maxmemory 4gb                     # 最大内存（不超过物理内存70%）
maxmemory-policy allkeys-lru       # 内存淘汰策略
maxmemory-samples 10

# ============================================
# 日志配置
# ============================================
loglevel notice
logfile /var/log/redis/redis.log
slowlog-log-slower-than 10000     # 慢查询阈值10ms
slowlog-max-len 128
```

**生成Redis TLS证书**:
```bash
sudo mkdir -p /etc/redis/certs && sudo chmod 700 $_ && sudo chown redis:redis $_
sudo -u redis openssl req -new -x509 -days 3650 -keyout ca.key -out ca.crt -subj "/CN=Redis CA"
sudo -u redis openssl req -newkey rsa:4096 -days 365 -nodes \
  -keyout server.key -out server.csr -subj "/CN=$(hostname)"
sudo -u redis openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out server.crt
sudo chmod 600 /etc/redis/certs/server.key
```

**检查清单**:
- [ ] 绑定内网IP地址（不使用0.0.0.0）
- [ ] 已启用protected-mode保护模式
- [ ] 已设置强密码（32字符以上）
- [ ] 已禁用FLUSHALL/FLUSHDB/CONFIG/DEBUG/SHUTDOWN命令
- [ ] 已重命名KEYS/INFO等敏感命令
- [ ] 已启用TLS/SSL加密（生产环境）
- [ ] 已配置最大内存限制和淘汰策略
- [ ] 已启用AOF持久化
- [ ] 私钥文件权限设置为600

---

## 5. RabbitMQ安全配置

### 5.1 用户权限配置命令

```bash
# ============================================
# RabbitMQ用户和权限配置
# ============================================

# 1. 删除默认guest用户（生产环境必须删除！）
rabbitmqctl delete_user guest

# 2. 创建管理员用户
rabbitmqctl add_user admin Admin_Strong_Password_123!
rabbitmqctl set_user_tags admin administrator
rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"

# 3. 创建应用程序用户
rabbitmqctl add_user food_trace_app App_Password_Here_456!
rabbitmqctl set_user_tags food_trace_app monitoring

# 4. 创建虚拟主机（隔离不同环境）
rabbitmqctl add_vhost /food_trace_production
rabbitmqctl add_vhost /food_trace_staging

# 5. 设置最小权限原则（格式: configure write read）
rabbitmqctl set_permissions -p /food_trace_production food_trace_app \
  "^trace\." "^trace\." "^trace\."

# 6. 创建只读监控用户
rabbitmqctl add_user monitor Monitor_Password_789!
rabbitmqctl set_user_tags monitor monitoring
rabbitmqctl set_permissions -p /food_trace_production monitor "^$" "^$" ".*"

# 7. 查看配置结果
rabbitmqctl list_users
rabbitmqctl list_permissions -p /food_trace_production
```

**权限说明**:
- **configure**: 配置资源权限（创建/删除队列、交换机等）
- **write**: 写入权限（发布消息）
- **read**: 读取权限（消费消息）
- `"^trace\."`: 仅允许操作以"trace."开头的资源

**检查清单**:
- [ ] 已删除默认guest用户
- [ ] 已创建管理员和应用专用用户（强密码）
- [ ] 已创建独立的虚拟主机（按环境隔离）
- [ ] 已配置最小权限原则（configure/write/read）
- [ ] 已限制用户只能操作特定模式的资源
- [ ] 已创建监控专用用户

---

### 5.2 Vhost隔离策略和高可用策略

```bash
# Vhost级别的资源限制
rabbitmqctl set_vm_memory_high_watermark absolute 2GB  # 内存水印
rabbitmqctl set_disk_free_limit absolute 5GB            # 磁盘空间限制

# 高可用策略（镜像队列）
rabbitmqctl set_policy -p /production HA-all "^trace\." \
  '{"ha-mode":"exactly","ha-sync-mode":"automatic","ha-promote-on-shutdown":"when-synced"}' \
  --apply-to queues --priority 1

# TTL策略（消息过期24小时）
rabbitmqctl set_policy -p /production TTL-policy "^trace\..*" \
  '{"message-ttl":86400000}' --apply-to queues --priority 2

# 死信交换机策略
rabbitmqctl set_policy -p /production DLX-policy "^trace\..*\.dlx" \
  '{"dead-letter-routing-key":"dlx","dead-letter-exchange":"dlx.exchange"}' \
  --apply-to queues --priority 3
```

**检查清单**:
- [ ] 已配置内存和磁盘水印限制
- [ ] 已配置高可用镜像队列策略
- [ ] 已配置消息TTL策略
- [ ] 已配置死信交换机策略

---

### 5.3 TLS证书配置

```bash
# 创建证书目录
sudo mkdir -p /etc/rabbitmq/ssl && sudo chmod 700 $_ && sudo chown rabbitmq:rabbitmq $_

# 生成CA和服务器证书（类似PostgreSQL流程）
sudo -u rabbitmq openssl req -new -x509 -days 3650 -keyout ca.key -out ca.crt \
  -subj "/C=CN/ST=Beijing/O=Food Trace/CN=RabbitMQ CA"
sudo -u rabbitmq openssl req -newkey rsa:4096 -days 365 -nodes \
  -keyout server.key -out server.csr -subj "/CN=rabbitmq.food-trace.com"
sudo -u rabbitmq openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out server.crt

# 生成客户端证书（双向认证）
sudo -u rabbitmq openssl req -newkey rsa:2048 -days 365 -nodes \
  -keyout client.key -out client.csr -subj "/CN=food_trace_app"
sudo -u rabbitmq openssl x509 -req -days 365 -in client.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out client.crt
cat client.crt ca.crt > client_chain.pem

# 设置权限
sudo chmod 600 /etc/rabbitmq/ssl/*.key
sudo chmod 644 /etc/rabbitmq/ssl/*.crt /etc/rabbitmq/ssl/*.pem
```

**rabbitmq.conf TLS配置**:
```ini
listeners.ssl.default = 5671
ssl_options.cacertfile = /etc/rabbitmq/ssl/ca.crt
ssl_options.certfile = /etc/rabbitmq/ssl/server.crt
ssl_options.keyfile = /etc/rabbitmq/ssl/server.key
ssl_options.verify = verify_peer
ssl_options.honor_cipher_order = true
ssl_options.versions.1 = tlsv1.2
ssl_options.versions.2 = tlsv1.3
ssl_options.ciphers.1 = ECDHE-RSA-AES256-GCM-SHA384
ssl_options.ciphers.2 = ECDHE-RSA-AES128-GCM-SHA256
management.ssl.port = 15671
management.tcp.port = none  # 禁用HTTP管理界面
```

**Spring Boot配置连接** (`application-prod.yml`):
```yaml
spring.rabbitmq.port: 5671  # TLS端口
spring.rabbitmq.ssl.enabled: true
spring.rabbitmq.ssl.algorithm: TLSv1.2
spring.rabbitmq.ssl.trust-store: classpath:rabbitmq-truststore.jks
```

**检查清单**:
- [ ] 已生成CA、服务器和客户端证书
- [ ] 已配置SAN（Subject Alternative Name）
- [ ] 已在rabbitmq.conf中配置TLS选项
- [ ] 已选择强加密套件（ECDHE+AES-GCM）
- [ ] 已禁用HTTP管理界面（仅HTTPS）
- [ ] Spring Boot已配置SSL连接
- [ ] 私钥文件权限设置为600

---

### 5.4 管理界面访问控制

```bash
# 通过防火墙限制管理界面访问IP
sudo iptables -A INPUT -p tcp -s 192.168.1.0/24 --dport 15671 -m state --state NEW -j ACCEPT

# Nginx反向代理添加安全Header（见第7章详细配置）
```

**检查清单**:
- [ ] 已通过防火墙限制管理界面访问IP
- [ ] 已配置Nginx反向代理（推荐）
- [ ] 已添加安全响应头
- [ ] 已配置速率限制

---

## 6. Spring Boot应用安全配置

### 6.1 application-prod.yml完整模板（脱敏版）

基于项目的 [application-prod.yml](backend/src/main/resources/application-prod.yml)，以下是增强的生产环境安全配置：

```yaml
# 食品溯源系统 - 生产环境配置
# ⚠️ 所有敏感信息必须通过环境变量注入！

app:
  security:
    enabled: true
  cors:
    allowed-origins:
      - https://www.food-trace.com
      - https://admin.food-trace.com
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allow-credentials: true

spring:
  main:
    allow-circular-references: false  # 生产环境禁止循环依赖
  threads.virtual.enabled: true

  datasource:
    url: jdbc:postgresql://${DB_HOST:10.0.0.1}:${DB_PORT:5432}/${DB_NAME:food_traceability}?useUnicode=true&characterEncoding=utf8&ssl=true&socketTimeout=30&connectTimeout=10
    username: ${DB_USERNAME:food_trace_app}
    password: ${DB_PASSWORD:}  # 必须通过环境变量设置！
    driver-class-name: org.postgresql.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 15; min-idle: 10; max-active: 80; max-wait: 60000
      validation-query: SELECT 1; test-while-idle: true; test-on-borrow: true  # 生产环境启用
      filters: stat,wall,slf4j,config
      stat-view-servlet.enabled: false  # 生产环境必须禁用！
      web-stat-filter.enabled: false    # 生产环境必须禁用！
      wall.config.multi-statement-allow: false
      wall.config.deny-functions: |
        load_file,information_schema,into outfile,into dumpfile,
        sleep,benchmark,extractvalue,updatexml

  mybatis-plus:
    configuration.local-cache-scope: STATEMENT  # 禁用全局缓存
    global-config.db-config.logic-delete-field: deleted
    global-config.db-config.logic-delete-value: 1
    global-config.db-config.logic-not-delete-value: 0

  cache.redis.time-to-live: 3600000
  web.resources.add-mappings: false

  jackson.default-property-inclusion: non_null
  servlet.multipart.max-file-size: 10MB; max-request-size: 50MB

  redis:
    enabled: true; host: ${REDIS_HOST:10.0.0.1}; port: ${REDIS_PORT:6380}; ssl: true
    password: ${REDIS_PASSWORD:}
    lettuce.pool.max-active: 32; max-wait: 5000ms; max-idle: 16; min-idle: 8

  rabbitmq:
    enabled: true; host: ${RABBITMQ_HOST:10.0.0.1}; port: ${RABBITMQ_PORT:5671}
    username: ${RABBITMQ_USERNAME:food_trace_app}; password: ${RABBITMQ_PASSWORD:}
    virtual-host: ${RABBITMQ_VIRTUAL_HOST:/food_trace_production}
    ssl.enabled: true; ssl.algorithm: TLSv1.2
    listener.simple.acknowledge-mode: manual; prefetch: 1
    listener.simple.retry.max-attempts: 3; retry.initial-interval: 1000ms

jwt:
  secret: ${JWT_SECRET:}  # 必须64字节以上！
  access-token-expiration: 28800000  # 8小时
  refresh-token-expiration: 604800000  # 7天
  issuer: food-trace-system; audience: food-trace-client

logging:
  level.root: WARN; level.com.example.demo: INFO
  file.name: /opt/food-traceability/logs/food-traceability.log
  file.max-size: 100MB; file.max-history: 30; file.total-size-cap: 1GB
  pattern.file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{36}:%line] - %msg%n"

management:
  endpoints.web.exposure.include: health,info,metrics,prometheus
  endpoint.health.show-details: when-authorized
  endpoint.env.enabled: false       # 禁止暴露环境变量！
  endpoint.configprops.enabled: false
  endpoint.beans.enabled: false
  endpoint.mappings.enabled: false
  metrics.export.prometheus.enabled: true
  server.port: 8082                 # Actuator独立端口
  server.address: 127.0.0.1         # 仅本地访问

server:
  port: 8081
  address: 0.0.0.0
  servlet.context-path: /api
  servlet.session.cookie.secure: true
  servlet.session.cookie.http-only: true
  servlet.session.cookie.same-site: strict
  tomcat.reject-illegal-header: true
  tomcat.max-header-count: 200
  tomcat.max-http-header-size: 16384
  headers:
    X-Content-Type-Options: nosniff
    X-Frame-Options: DENY
    X-XSS-Protection: "1; mode=block"
    Strict-Transport-Security: "max-age=31536000; includeSubDomains; preload"
    Content-Security-Policy: "default-src 'self'; frame-ancestors 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:"
    Referrer-Policy: strict-origin-when-cross-origin
    Permissions-Policy: camera=(), microphone=(), geolocation=()
```

**环境变量文件** (`/opt/food-traceability/config/.env`) 权限: `chmod 600`

**检查清单**:
- [ ] 所有敏感信息通过环境变量注入（不硬编码）
- [ ] .env文件权限设置为600
- [ ] 已启用数据库/Redis/RabbitMQ SSL连接
- [ ] 已禁用Druid监控页面
- [ ] 已配置SQL注入防护（WallFilter deny-functions）
- [ ] 已配置JWT Issuer/Audience验证
- [ ] Session Cookie设置secure/http-only/same-site
- [ ] 已配置完整的安全响应头（CSP/HSTS/X-Frame-Options等）
- [ ] Actuator仅暴露必要端点且使用独立端口+本地访问
- [ ] 已禁用env/configprops/beans/mappings端点
- [ ] MyBatis Plus禁用全局缓存（STATEMENT级别）
- [ ] CORS严格限制为生产域名
- [ ] 生产环境禁止循环依赖

---

### 6.2 SecurityConfig关键配置点

基于项目的 [SecurityConfig.java](backend/src/main/java/com/example/demo/security/config/SecurityConfig.java)，生产环境应增强：

**关键安全要点**:

1. **CORS严格限制**: 仅允许生产域名，不允许通配符 `*`
2. **CSRF保护**: 如果使用Cookie-based session必须启用CSRF；如果纯JWT Bearer Token可禁用
3. **Session管理**: STATELESS模式，最大会话数=1，阻止并发登录
4. **公开端点最小化**: 仅保留必要的公开API（如/auth/**、/actuator/health）
5. **Security Headers**: CSP、HSTS、X-Frame-Options、Referrer-Policy、Permissions-Policy
6. **Rate Limiting**: 已有[RateLimitFilter](backend/src/main/java/com/example/demo/security/filter/RateLimitFilter.java)，确认生产环境启用
7. **JWT验证**: 必须验证Issuer、Audience、签名算法（拒绝None算法）、黑名单机制

**检查清单**:
- [ ] CORS已严格限制为生产域名列表
- [ ] CSRF策略符合实际认证方式
- [ ] Session设置为STATELESS
- [ ] 公开端点已最小化（移除Swagger等开发工具）
- [ ] 所有安全响应Header已配置
- [ ] RateLimitFilter已启用且参数合理
- [ ] JWT验证包含Issuer/Audience/黑名单检查
- [ ] 密码编码器强度足够（BCrypt >= 12 rounds）

---

### 6.3 JWT密钥管理策略

```bash
# 生成64字节以上的随机密钥（使用openssl）
openssl rand -base64 64

# 或使用Java KeyTool生成密钥库
keytool -genseckey -alias jwtsecret -keyalg HMACSHA256 -keysize 256 \
  -keystore /opt/food-traceability/config/jwt-secret.jks \
  -storetype JCEKS -storepass ${JASYPT_ENCRYPTOR_PASSWORD}

# 密钥存储要求：
# 1. 通过环境变量注入，不硬编码在代码或配置文件中
# 2. 使用密钥管理系统（HashiCorp Vault/AWS KMS）最佳
# 3. 定期轮换（建议每90天）
# 4. 不同环境使用不同的密钥
# 5. 密钥长度 >= 256 bits (HS512要求)
```

**检查清单**:
- [ ] JWT密钥长度 >= 64字节（512 bits）
- [ ] 密钥通过环境变量或KMS注入
- [ ] 已制定密钥轮换计划（每90天）
- [ ] 不同环境使用不同密钥
- [ ] 已配置签名算法白名单（仅允许HS512）

---

## 7. Nginx反向代理安全配置

### 7.1 nginx.conf安全配置段

```nginx
# ============================================
# Nginx主配置安全增强
# ============================================

# 隐藏版本号（安全最佳实践）
server_tokens off;

# worker进程安全
worker_processes auto;
worker_rlimit_nofile 65535;

events {
    worker_connections 4096;
    use epoll;
    multi_accept on;
}

http {
    # 基础安全设置
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    types_hash_max_size 2048;
    server_tokens off;

    # === Gzip配置（减少带宽但注意BREACH攻击风险）===
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    # 注意：不要对认证页面启用gzip（可能泄露token信息）

    # === 限速配置 ===
    limit_req_zone $binary_remote_addr zone=api:10m rate=100r/s;
    limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;
    limit_conn_zone $binary_remote_addr zone=connlimit:10m;

    # === 上游服务器定义 ===
    upstream spring_backend {
        server 127.0.0.1:8081 weight=1 max_fails=3 fail_timeout=30s;
        keepalive 32;  # 保持长连接
    }

    upstream vue_frontend {
        server 127.0.0.1:3000 weight=1;
    }

    # ============================================
    # HTTP -> HTTPS 强制跳转
    # ============================================
    server {
        listen 80;
        listen [::]:80;
        server_name www.food-trace.com admin.food-trace.com api.food-trace.com;
        return 301 https://$host$request_uri;
    }

    # ============================================
    # HTTPS 主服务器配置
    # ============================================
    server {
        listen 443 ssl http2;
        listen [::]:443 ssl http2;
        server_name www.food-trace.com;

        # === SSL/TLS 配置 ===
        ssl_certificate /etc/nginx/ssl/star.food-trace.com.crt;
        ssl_certificate_key /etc/nginx/ssl/star.food-trace.com.key;
        ssl_session_timeout 1d;
        ssl_session_cache shared:SSL:50m;
        ssl_session_tickets off;  # 禁用Session Tickets（前向保密）

        # 现代化密码套件（优先ECDHE + AES-GCM + CHACHA20_POLY1305）
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
        ssl_prefer_server_ciphers off;  # 让客户端选择最优套件

        # OCSP Stapling（加速证书验证）
        ssl_stapling on;
        ssl_stapling_verify on;
        ssl_trusted_certificate /etc/nginx/ssl/chain.crt;
        resolver 8.8.8.8 8.8.4.4 valid=300s;
        resolver_timeout 5s;

        # === 安全响应头 ===
        add_header X-Frame-Options "DENY" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Content-Security-Policy "default-src 'self'; frame-ancestors 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self'; connect-src 'self'; base-uri 'self'; form-action 'self'" always;
        add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
        add_header Cache-Control "no-transform" always;
        # 移除显示后端信息的头
        proxy_hide_header X-Powered-By;
        proxy_hide_header Server;

        # === 请求大小限制 ===
        client_max_body_size 10m;           # 请求体大小限制
        client_body_buffer_size 128k;
        client_body_timeout 30s;
        client_header_timeout 30s;
        keepalive_timeout 65;

        # === 反向代理到Spring Boot后端 ===
        location /api/ {
            proxy_pass http://spring_backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Port $server_port;
            proxy_set_header Connection "";

            # 超时设置
            proxy_connect_timeout 10s;
            proxy_send_timeout 120s;
            proxy_read_timeout 120s;

            # 缓冲区设置（防止缓冲区溢出攻击）
            proxy_buffer_size 16k;
            proxy_buffers 8 16k;
            proxy_busy_buffers_size 32k;

            # WebSocket支持（如果需要）
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";

            # 速率限制
            limit_req zone=api burst=50 nodelay;
            limit_conn connlimit 20;

            # 禁止通过代理访问敏感路径
            if ($request_uri ~* "\.(git|svn|env|conf|bak|backup|sql|log)$") {
                return 403;
            }
        }

        # === 登录接口更严格的限流 ===
        location ~ ^/api/(v1/)?auth/login {
            proxy_pass http://spring_backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            limit_req zone=login burst=3 nodelay;
            limit_req_status 429;
        }

        # === Actuator端点保护（仅允许内网访问）===
        location ~ ^/actuator/ {
            allow 127.0.0.1;
            allow 10.0.0.0/8;
            allow 192.168.1.0/24;
            deny all;
            proxy_pass http://spring_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # === 静态资源和前端 ===
        location / {
            proxy_pass http://vue_frontend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            # 静态资源缓存
            location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg|webp)$ {
                expires 30d;
                add_header Cache-Control "public, immutable";
                access_log off;
            }
        }

        # === 禁止访问隐藏文件和敏感文件 ===
        location ~ /\. {
            deny all;
            access_log off;
            log_not_found off;
        }

        location ~* \.(git|svn|env|conf|bak|backup|sql|log|sh|py|pl|cgi)$ {
            deny all;
            access_log off;
        }

        # === 访问日志（JSON格式便于分析）===
        access_log /var/log/nginx/access.log combined if=$loggable;
        error_log /var/log/nginx/error.log warn;

        # 定义哪些请求不记录日志（健康检查等）
        map $request_uri $loggable {
            default 1;
            ~^/actuator/health 0;
            ~*\.(png|jpg|css|js|gif|ico|woff|woff2)$ 0;
        }
    }
}
```

**SSL证书安全配置**:
```bash
# 生成DH参数（增强DHE密钥交换安全性）
openssl dhparam -out /etc/nginx/ssl/dhparam.pem 2048

# 在nginx.conf的http块中添加:
ssl_dhparam /etc/nginx/ssl/dhparam.pem;

# 证书文件权限
chmod 644 /etc/nginx/ssl/star.food-trace.com.crt
chmod 600 /etc/nginx/ssl/star.food-trace.com.key
chown root:root /etc/nginx/ssl/*
```

**检查清单**:
- [ ] 已隐藏Nginx版本号（server_tokens off）
- [ ] 已配置HTTP->HTTPS强制跳转
- [ ] 已使用TLSv1.2/TLSv1.3和现代化密码套件
- [ ] 已禁用SSL Session Tickets（前向保密）
- [ ] 已启用OCSP Stapling
- [ ] 已配置完整的安全响应头（CSP/HSTS/X-Frame-Options等）
- [ ] 已设置请求大小限制（client_max_body_size）
- [ ] 已配置API速率限制（通用100r/s，登录5r/m）
- [ ] 已隐藏后端信息头（X-Powered-By/Server）
- [ ] 已禁止访问隐藏文件和敏感文件
- [ ] Actuator端点已限制为内网访问
- [ ] 已配置WebSocket支持
- [ ] 已使用JSON格式访问日志
- [ ] DH参数文件已生成（2048位）
- [ ] 证书私钥权限为600

---

## 8. 网络安全配置

### 8.1 VPC/子网划分建议

```
VPC: 10.0.0.0/16 (CIDR)
├── Public Subnet A: 10.0.1.0/24 (Nginx/ALB)
├── Public Subnet B: 10.0.2.0/24 (备用)
├── Private Subnet A: 10.0.10.0/24 (Spring Boot App Server 1)
├── Private Subnet B: 10.0.11.0/24 (Spring Boot App Server 2)
├── DB Subnet A: 10.0.20.0/24 (PostgreSQL Primary)
├── DB Subnet B: 10.0.21.0/24 (PostgreSQL Replica)
├── Cache Subnet: 10.0.30.0/24 (Redis Cluster)
└── Mgmt Subnet: 10.0.100.0/24 (Monitoring/Bastion)
```

**安全组规则**:

| 安全组 | 入站规则 | 出站规则 |
|--------|---------|---------|
| **ALB/Nginx** | TCP 443 from 0.0.0.0/0<br>TCP 80 from 0.0.0.0/0 | TCP 8081 to App SG |
| **App Server** | TCP 8081 from ALB SG<br>TCP 22 from Bastion SG (10.0.100.0/24) | TCP 5432 to DB SG<br>TCP 6379 to Redis SG<br>TCP 5672 to RabbitMQ SG |
| **PostgreSQL** | TCP 5432 from App SG<br>TCP 22 from Bastion SG | All traffic denied (or minimal) |
| **Redis** | TCP 6379 from App SG<br>TCP 22 from Bastion SG | No outbound |
| **RabbitMQ** | TCP 5671/5672 from App SG<br>TCP 15671 from Bastion SG | No outbound |
| **Bastion** | TCP 22 from Office IP | TCP 22 to all internal SGs |

**检查清单**:
- [ ] VPC已划分为Public/Private/DB子网
- [ ] 应用服务器位于Private子网（无公网IP）
- [ ] 数据库位于独立的DB子网
- [ ] 每层使用独立的安全组
- [ ] 安全组规则遵循最小权限原则
- [ ] 仅ALB/Nginx暴露公网IP
- [ ] Bastion主机用于远程管理

---

### 8.2 DDoS防护和负载均衡SSL终止

```bash
# AWS ALB/CloudFront DDoS防护配置建议：
# 1. 启用AWS Shield Standard（免费基础防护）
# 2. 对于高级防护，订阅AWS Shield Advanced
# 3. CloudFront作为CDN+WAF，缓解DDoS攻击

# Nginx作为负载均衡器的DDoS防护配置：
# 在http块中添加：
limit_req_zone $binary_remote_addr zone=perip:10m rate=30r/s;
limit_req_zone $server_name zone=perserver:10m rate=300r/s;
limit_conn_zone $binary_remote_addr zone=conns:10m;

# 在server块中使用：
limit_req zone=perip burst=50 nodelay;
limit_req zone=perserver burst=1000 nodelay;
limit_conn conns 50;

# 连接超时设置
client_body_timeout 30s;
client_header_timeout 30s;
send_timeout 30s;
keepalive_timeout 65;

# 限制并发连接数
worker_connections 4096;
worker_rlimit_nofile 65535;
```

**WAF规则建议（ModSecurity/AWS WAF）**:
```
# OWASP Core Rule Set (CRS) 关键规则：
- SQL Injection Detection (942xxx)
- Cross Site Scripting (XSS) (93xxxx)
- Remote File Inclusion (RFI) (93xxx)
- Local File Inclusion (LFI) (93xxx)
- Command Injection (94xxxx)
- Session Fixation (95xxxx)

# 自定义业务规则：
- 阻止频繁失败的登录尝试 (>5次/分钟/IP)
- 阻止异常大的请求体 (>10MB)
- 阻止可疑的User-Agent（扫描器特征）
- 阻止访问敏感路径（/.git/, /.env/, /actuator/env等）
```

**检查清单**:
- [ ] 已启用云服务商DDoS防护（Shield/阿里云DDoS Basic）
- [ ] 已配置应用层速率限制
- [ ] 已配置WAF规则（OWASP CRS）
- [ ] 已配置自定义业务安全规则
- [ ] 负载均衡器已配置SSL终止
- [ ] CDN已启用并配置安全Headers

---

## 9. 日志和监控安全

### 9.1 日志收集配置

```yaml
# Filebeat配置 (/etc/filebeat/filebeat.yml)
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /opt/food-traceability/logs/*.log
    - /var/log/redis/redis.log
    - /var/lib/pgsql/data/pg_log/*.csv
  fields:
    service: food-trace
    env: production
  multiline.pattern: '^[0-9]{4}-[0-9]{2}-[0-9]{2}'
  multiline.negate: true
  multiline.match: after

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "food-trace-%{[fields.env]}-%{+yyyy.MM.dd}"
  protocol: https
  ssl.certificate_authorities: ["/etc/filebeat/ca.crt"]

setup.kibana:
  host: "kibana:5601"
  protocol: https
  ssl.certificate_authorities: ["/etc/filebeat/ca.crt"]
```

**Logback增强配置** (`logback-prod.xml`) - 基于项目的 [logback-spring.xml](backend/src/main/resources/logback-spring.xml):

```xml
<configuration>
    <!-- 敏感数据脱敏 -->
    <conversionRule conversionWord="mask" converterClass="com.example.demo.util.MaskingConverter"/>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/opt/food-traceability/logs/food-traceability.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/food-traceability.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{36}:%line] - %mask%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ERROR级别单独文件 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.LevelFilter"><level>ERROR</level><onMatch>ACCEPT</onMatch><onMismatch>DENY</onMismatch></filter>
        <file>logs/food-traceability-error.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/food-traceability-error.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder><pattern>%d{...} %-5level %mask%n</pattern></encoder>
    </appender>

    <!-- 生产环境降低日志级别 -->
    <logger name="com.example.demo" level="INFO" additivity="false">
        <appender-ref ref="FILE"/><appender-ref ref="ERROR_FILE"/>
    </logger>
    <root level="WARN"><appender-ref ref="FILE"/></root>
</configuration>
```

**检查清单**:
- [ ] 已部署Filebeat/Fluentd日志收集器
- [ ] 日志已发送到集中式日志平台（ELK/Loki）
- [ ] Logback已配置敏感数据脱敏（密码/token/手机号/邮箱）
- [ ] ERROR日志单独存储便于告警
- [ ] 日志格式统一且包含时间戳、线程、行号
- [ ] 日志轮转策略合理（100MB/文件，30天保留）

---

### 9.2 异常告警阈值

```yaml
# Prometheus告警规则 (/etc/prometheus/alertrules.yml)
groups:
- name: food-trace-security
  rules:
  # 认证失败率过高（暴力破解检测）
  - alert: HighAuthFailureRate
    expr: rate(http_requests_total{status=~"401|403", job="food-trace-api"}[5m]) > 10
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "认证失败率过高，可能存在暴力破解攻击"
      description: "5分钟内认证失败超过 {{ $value }} 次/秒"

  # 429 Too Many Requests频率过高（DDoS检测）
  - alert: HighRateLimitHits
    expr: rate(http_requests_total{status="429", job="food-trace-api"}[5m]) > 50
    for: 3m
    labels:
      severity: critical
    annotations:
      summary: "速率限制触发频繁，可能遭受DDoS攻击"

  # 5xx错误率过高
  - alert: HighErrorRate
    expr: |
      (
        sum(rate(http_requests_total{job="food-trace-api", status=~"5.."}[5m]))
        /
        sum(rate(http_requests_total{job="food-trace-api"}[5m]))
      ) * 100 > 5
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "5xx错误率超过5%，需要立即处理"

  # 数据库连接池耗尽
  - alert: DatabaseConnectionPoolExhausted
    expr: hikaricp_connections_active{pool="FoodTraceHikariPool"} / hikaricp_connections_max{pool="FoodTraceHikariPool"} > 0.9
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "数据库连接池使用率超过90%"

  # Redis连接失败
  - alert: RedisConnectionFailure
    expr: up{job="redis"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Redis服务不可达"

  # RabbitMQ队列积压
  - alert: RabbitMQQueueBacklog
    expr: rabbitmq_queue_messages{queue=~"trace\\..*"} > 10000
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "RabbitMQ队列 {{ $labels.queue }} 消息积压超过10000条"

  # 异常的CPU/内存使用
  - alert: HighMemoryUsage
    expr: process_resident_memory_bytes{job="food-trace-api"} / 1024 / 1024 / 1024 > 3.5  # 3.5GB/4GB
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "JVM堆内存使用超过87.5%"
```

**检查清单**:
- [ ] 已配置Prometheus/Grafana监控
- [ ] 已配置认证失败率告警（暴力破解检测）
- [ ] 已配置429速率限制告警（DDoS检测）
- [ ] 已配置5xx错误率告警
- [ ] 已配置数据库连接池/Redis/RabbitMQ健康检查
- [ ] 已配置资源使用率告警（CPU/内存/磁盘）
- [ ] 告警通知渠道已配置（邮件/钉钉/微信/短信）

---

### 9.3 SIEM集成建议

```bash
# 将安全日志发送到SIEM平台（如Splunk/Sentinel/Elastic SIEM）
# 1. 审计日志（auditd）
# 2. 应用访问日志（Nginx + Spring Boot）
# 3. 数据库审计日志（pg_audit）
# 4. Redis/RabbitMQ访问日志
# 5. 操作系统安全日志（auth.log/secure）

# 关键检测规则：
# - 多次认证失败后成功（凭证填充攻击）
# - 异常时间的访问（非工作时间大量API调用）
# - 单IP短时间内大量请求（扫描/DDoS）
# - SQL注入/XSS攻击特征匹配
# - 敏感文件访问（.env/.git/config等）
# - 特权用户操作（sudo/su/root登录）
# - 异常的数据导出行为（大批量查询/下载）
```

**检查清单**:
- [ ] 安全日志已集成到SIEM平台
- [ ] 已配置常见攻击检测规则
- [ ] 已配置异常行为基线检测
- [ ] 已建立安全事件分级标准
- [ ] 已配置自动化的初始响应（如自动封禁IP）

---

## 10. 部署自动化安全

### 10.1 CI/CD流水线安全检查

```yaml
# GitLab CI/CD Pipeline 示例 (.gitlab-ci.yml)
stages:
  - security-scan
  - build
  - test
  - image-scan
  - deploy

# 1. SAST静态应用安全测试
security-sast:
  stage: security-scan
  image: sonarsource/sonar-scanner-cli
  script:
    - sonar-scanner \
      -Dsonar.projectKey=food-trace \
      -Dsonar.sources=. \
      -Dsonar.host.url=${SONARQUBE_URL} \
      -Dsonar.login=${SONARQUBE_TOKEN}
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"

# 2. 依赖漏洞扫描
dependency-check:
  stage: security-scan
  image: owasp/dependency-check:latest
  script:
    - /usr/share/dependency-check/bin/dependencyCheck.sh \
      --project "Food Traceability" \
      --scan "." \
      --format JSON \
      --out dependency-check-report.json
  artifacts:
    paths: [dependency-check-report.json]
    expire_in: 1 week

# 3. Docker镜像扫描
image-security-scan:
  stage: image-scan
  image: aquasec/trivy:latest
  script:
    - trivy image --severity CRITICAL,HIGH \
      --exit-code 1 \
      --format table \
      registry.example.com/food-trace:${CI_COMMIT_SHA}
  allow_failure: false

# 4. 容器镜像签名验证（可选）
image-signing:
  stage: build
  image: cosign/cosign:v2.0.0
  script:
    - cosign sign --key cosign.key registry.example.com/food-trace:${CI_COMMIT_SHA}
  only:
    - main
```

**检查清单**:
- [ ] CI流水线包含SAST静态分析（SonarQube）
- [ ] CI流水线包含依赖漏洞扫描（OWASP Dependency-Check/Dependabot）
- [ ] CD流水线包含容器镜像扫描（Trivy/Clair）
- [ ] 已配置镜像签名和验证（Cosign/Notary）
- [ ] 安全扫描阻断部署（CRITICAL/HIGH漏洞）
- [ ] 扫描报告已归档并可追溯

---

### 10.2 密钥管理（Vault/KMS）

```bash
# HashiCorp Vault配置示例

# 1. 启动Vault服务器（生产环境使用HA模式 + Auto-Unseal）
vault server -config=/etc/vault/config.hcl

# 2. 配置KV Secret Engine（存储应用密钥）
vault secrets enable -path=secret kv-v2

# 3. 存储敏感配置
vault kv put secret/food-trace/db \
  username=food_trace_app \
  password=<strong_password>

vault kv put secret/food-trace/redis \
  password=<strong_redis_password>

vault kv put secret/food-trace/jwt \
  secret=<64_byte_random_string>

vault kv put secret/food-trace/rabbitmq \
  username=food_trace_app \
  password=<strong_rabbitmq_password>

# 4. 创建访问策略（最小权限）
vault policy write food-trace-app - <<EOF
path "secret/data/food-trace/*" {
  capabilities = ["read"]
}
EOF

# 5. 创建应用角色并获取Token
vault write auth/approle/role/food-trace-role \
  token_policies="food-trace-app" \
  token_ttl=1h \
  token_max_ttl=4h

# 6. 读取角色ID和Secret ID
vault read auth/approle/role/food-trace-role/role-id
vault write -f auth/approle/role/food-trace-role/secret-id

# 7. Spring Boot集成Vault（使用spring-cloud-starter-vault-config）
# application-prod.yml添加:
# spring.cloud.vault.uri=https://vault.food-trace.com:8200
# spring.cloud.vault.authentication=APPROLE
# spring.cloud.vault.app-role.role-id=${VAULT_ROLE_ID}
# spring.cloud.vault.app-role.secret-id=${VAULT_SECRET_ID}
# spring.cloud.vault.kv.application-name=food-trace
```

**检查清单**:
- [ ] 已部署HashiCorp Vault或AWS Secrets Manager/KMS
- [ ] 敏感信息已从配置文件迁移到Vault
- [ ] 已配置KV Secret Engine并存储所有密钥
- [ ] 已创建细粒度的访问策略（最小权限）
- [ ] 已使用AppRole认证（而非Token）
- [ ] 已配置Token TTL（短期有效）
- [ ] Spring Boot已集成Vault动态获取密钥
- [ ] Vault已启用审计日志
- [ ] 已配置Auto-Unseal（避免手动Unseal）

---

## 11. 应急响应预案

### 11.1 安全事件分级

| 等级 | 名称 | 描述 | 响应时间 | 通知范围 |
|------|------|------|----------|----------|
| P0 | 严重 | 数据泄露、勒索病毒、Rootkit、完全入侵 | < 15分钟 | 全员+管理层+法务 |
| P1 | 高危 | 成功的SQL注入/XSS、凭证窃取、未授权访问 | < 1小时 | 安全团队+开发负责人+运维 |
| P2 | 中危 | 扫描探测、暴力破解、DDoS攻击、异常行为 | < 4小时 | 安全团队+运维 |
| P3 | 低危 | 单次失败登录、误报、轻微配置偏差 | < 24小时 | 运维团队 |

---

### 11.2 应急响应流程

```
检测(1min) → 报警(5min) → 抑制(15min) → 根除(1h) → 恢复(2h) → 复盘(24h)
```

**各阶段具体行动**:

**阶段1: 检测与报警**
```bash
# 检查异常迹象
# 1. 查看异常登录
last -n 20
lastb -n 20  # 失败登录

# 2. 检查可疑进程
ps auxf | grep -E "(ncat|nc|bash -i|python.*socket|/dev/tcp)"

# 3. 检查网络连接
ss -tulnp | grep -E "(ESTABLISHED|LISTEN)"
netstat -tulnp | grep -v "127.0.0.1"

# 4. 检查异常定时任务
crontab -l
ls -la /etc/cron.* /var/spool/cron/

# 5. 检查最近修改的系统文件
find /etc /usr/bin /usr/sbin -mtime -1 -type f 2>/dev/null

# 6. 检查应用日志中的异常
grep -i "error\|exception\|unauthorized\|forbidden" /opt/food-traceability/logs/*.log | tail -100
```

**阶段2: 抑制（遏制损害扩大）**
```bash
# 1. 隔离受影响服务器（网络层面）
# 从负载均衡器移除该节点
# 或使用防火墙阻断所有入站连接（除SSH管理端口）
sudo iptables -P INPUT DROP
sudo iptables -A INPUT -p tcp -s 管理站IP --dport 2222 -j ACCEPT
sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# 2. 如确认被入侵，立即停机（如果无法在线遏制）
sudo systemctl stop food-trace

# 3. 旋转所有凭证（密码/API Key/JWT密钥）
# - 数据库密码
# - Redis密码
# - RabbitMQ密码
# - JWT签名密钥
# - API密钥（第三方服务）
# - SSH密钥

# 4. 通知相关方
# - 内部安全团队
# - 合规/法务部门（如涉及用户数据）
# - 受影响客户（如需披露）
```

**阶段3: 根除（清除威胁）**
```bash
# 1. 全面系统扫描
# 使用专业工具进行恶意软件扫描
clamscan -r / --bell -i --log=/tmp/clamav_scan.log
rkhunter --check --sk
chkrootkit

# 2. 分析入侵向量
# - Web服务器日志（Nginx access/error log）
# - 应用日志（Spring Boot log）
# - 数据库审计日志（pg_audit）
# - 系统审计日志（auditd）
# - 进程历史记录

# 3. 识别并修补漏洞
# - 更新受影响的组件
# - 修复配置缺陷
# - 打补丁
# - 代码审查和修复

# 4. 部署加固后的新实例
# 不要直接清理被入侵的机器！应该销毁并重建！
```

**阶段4: 恢复**
```bash
# 1. 从可信备份恢复数据
# 确保备份是在入侵前创建的（通过备份时间戳判断）
# 验证备份完整性（校验和/数字签名）

# 2. 部署加固后的应用
# 使用经过安全扫描的新镜像
# 注入新的凭证（从Vault获取）

# 3. 逐步恢复流量
# 先恢复1%流量观察30分钟
# 再恢复10%流量观察1小时
# 最后恢复全部流量

# 4. 加强监控
# 提升告警灵敏度
# 增加审计日志采集粒度
# 实施额外的访问控制
```

**阶段5: 复盘与改进**
```markdown
## 安全事件复盘报告模板

### 事件概述
- 发生时间:
- 发现时间:
- 事件等级:
- 影响范围:

### 时间线
| 时间 | 事件 | 行动人 |
|------|------|--------|
| T+0min | 攻击发生 | - |
| T+Xmin | 检测告警 | Prometheus AlertManager |
| T+Ymin | 开始响应 | 安全团队 |
| ... | ... | ... |

### 根因分析
- 直接原因:
- 根本原因:
- 入侵向量:

### 影响评估
- 数据泄露数量:
- 服务中断时长:
- 业务损失估算:
- 品牌影响:

### 经验教训
1. ...
2. ...

### 改进措施
- [ ] 技术改进项
- [ ] 流程改进项
- [ ] 人员培训需求
- [ ] 工具/平台升级计划

### 行动计划
| 改进项 | 负责人 | 截止日期 | 状态 |
|--------|--------|----------|------|
| ... | ... | ... | ... |
```

---

### 11.3 回滚步骤

```bash
#!/bin/bash
# 快速回滚脚本 (/opt/food-traceability/scripts/emergency-rollback.sh)
set -e

BACKUP_VERSION=$1
if [ -z "$BACKUP_VERSION" ]; then
    echo "用法: $0 <备份版本号或时间戳>"
    exit 1
fi

echo "=== 开始紧急回滚到版本: ${BACKUP_VERSION} ==="
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

# 1. 停止当前应用
sudo systemctl stop food-trace
echo "[${TIMESTAMP}] 应用已停止"

# 2. 备份数据库（回滚前的当前状态）
PGPASSWORD="${BACKUP_PASSWORD}" pg_dump -h 10.0.0.1 -U backup_user \
  -d food_traceability --format=custom \
  > /opt/food-traceability/backups/pre-rollback-${DATE}.dump
echo "[${TIMESTAMP}] 当前数据库已备份"

# 3. 回滚数据库到指定版本
PGPASSWORD="${BACKUP_PASSWORD}" pg_restore \
  -h 10.0.0.1 -U backup_user -d food_traceability \
  --clean --if-exists \
  /opt/food-traceability/backups/postgresql/daily/*${BACKUP_VERSION}*.dump.gpg | \
  gpg --batch --yes --passphrase-file /opt/food-traceability/config/backup-key.bin -d
echo "[${TIMESTAMP}] 数据库已回滚到 ${BACKUP_VERSION}"

# 4. 回滚应用版本
cd /opt/food-traceability/app
CURRENT_JAR=$(readlink -f food-traceability.jar)
cp ${CURRENT_JAR} /opt/food-traceability/backups/broken-version-$(date +%Y%m%d%H%M%S).jar
ln -sf food-traceability-${BACKUP_VERSION}.jar food-traceability.jar
echo "[${TIMESTAMP}] 应用已回滚到版本 ${BACKUP_VERSION}"

# 5. 验证配置文件
# 确保配置文件是正确的版本
# git checkout ${BACKUP_VERSION} -- config/

# 6. 启动应用
sudo systemctl start food-trace
sleep 10

# 7. 健康检查
for i in {1..30}; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8081/api/actuator/health)
    if [ "$HTTP_CODE" = "200" ]; then
        echo "[${TIMESTAMP}] 健康检查通过，回滚成功！"
        exit 0
    fi
    echo "[${TIMESTAMP}] 等待应用启动... (${i}/30)"
    sleep 2
done

echo "[错误] ${TIMESTAMP} 应用启动失败，请检查日志！"
sudo journalctl -u food-trace -n 100 --no-pager
exit 1
```

**检查清单**:
- [ ] 已制定安全事件分级标准（P0-P3）
- [ ] 已定义明确的应急响应流程和时间线
- [ ] 已编写检测阶段的检查命令脚本
- [ ] 已制定抑制措施（网络隔离/凭证轮换）
- [ ] 已准备根除方案（全面扫描/重建）
- [ ] 已制定恢复计划和灰度发布策略
- [ ] 已编写快速回滚脚本并测试
- [ ] 已建立复盘机制和改进跟踪
- [ ] 应急联系人名单已更新（安全/运维/开发/法务/PR）
- [ ] 已定期演练应急响应流程（每季度至少一次）

---

## 附录：总检查清单汇总

### 部署前必检项（Go/No-Go Checklist）

- [ ] **操作系统**: sysctl内核参数、文件描述符、专用用户、SSH加固、防火墙、SELinux/AppArmor、auditd、自动更新
- [ ] **JDK/JVM**: java.security弱协议禁用、JVM安全启动参数、systemd服务配置、RMI/JMX禁用
- [ ] **PostgreSQL**: pg_hba.conf认证、postgresql.conf安全参数、SSL/TLS、用户权限最小化、备份加密、pg_audit
- [ ] **Redis**: 绑定地址、protected-mode、强密码、命令禁用/重命名、TLS、内存限制、AOF持久化
- [ ] **RabbitMQ**: guest用户删除、专用用户/Vhost、最小权限、TLS证书、管理界面限制、HA策略
- [ ] **Spring Boot**: 环境变量注入、SSL连接、Druid禁用、SQL注入防护、JWT安全、Actuator限制、安全Headers、CORS限制
- [ ] **Nginx**: HTTPS强制跳转、现代密码套件、安全Header、速率限制、WAF规则、敏感路径屏蔽
- [ ] **网络**: VPC子网划分、安全组规则、DDoS防护、SSL终止、Bastion主机
- [ ] **日志监控**: ELK集成、敏感数据脱敏、Prometheus告警规则、SIEM集成
- [ ] **CI/CD**: SAST/DSCA/IAC扫描、镜像扫描、镜像签名、Vault密钥管理
- [ ] **应急响应**: 事件分级、响应流程、回滚脚本、演练计划

---

> **文档维护说明**
> - 版本: v1.0
> - 创建日期: 2026-04-02
> - 适用场景: 食品溯源系统生产环境首次部署及安全审计
> - 审核周期: 每季度审核一次，重大变更后立即更新
> - 联系人: DevOps安全团队
