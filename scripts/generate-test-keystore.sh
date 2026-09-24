#!/usr/bin/env bash
# 生成自签测试 keystore（PKCS12，CN=localhost）
# 用途：仅限本地/测试环境 SSL 联调；生产不适用（生产须使用正式证书 + 外部挂载，
#       路径经 SSL_KEYSTORE_PATH / SSL_KEYSTORE_PASSWORD 环境变量注入，勿入库）。
# 产物默认输出到 backend/keystore.p12（.gitignore 已排除 *.p12/*.jks，不会误入库）。
#
# 用法：bash scripts/generate-test-keystore.sh [输出路径] [别名]
# 依赖：JDK keytool

set -euo pipefail

OUT="${1:-backend/keystore.p12}"
ALIAS="${2:-food-traceability}"
PASS="${SSL_KEYSTORE_PASSWORD:-password}"

keytool -genkeypair \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365 \
  -dname "CN=localhost, OU=Dev, O=FoodTraceability, L=Local, ST=Local, C=CN" \
  -ext "SAN=dns:localhost,ip:127.0.0.1" \
  -keystore "$OUT" \
  -storetype PKCS12 \
  -storepass "$PASS"

echo "OK: $OUT (alias=$ALIAS, storetype=PKCS12)"
echo "启用本地 SSL 示例："
echo "  SSL_ENABLED=true SSL_KEYSTORE_PATH=$PWD/$OUT SSL_KEYSTORE_PASSWORD=\$SSL_KEYSTORE_PASSWORD ./mvnw spring-boot:run"
