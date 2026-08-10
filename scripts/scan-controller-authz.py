#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P0-SEC-001 鉴权覆盖率扫描脚本（CI 可执行）
===========================================
职责：扫描 backend/src/main/java/com/foodtraceability/controller/** 下所有
Controller，统计 handler 方法（带有 @RequestMapping/@GetMapping/@PostMapping/
@PutMapping/@DeleteMapping/@PatchMapping 注解的 public/protected 方法）中
带方法级或类级 @PreAuthorize 的比例。覆盖率 != 100% 时以退出码 1 失败（CI 门禁）。

用法：
    python scripts/scan-controller-authz.py [--base ...] [--strict]

判定规则：
- handler 方法 = 带有路由映射注解的 public/protected 方法（注解可跨行，方法声明
  前允许存在其他注解，如 @Transactional、@ResponseBody）。
- 已覆盖 = 方法自身有 @PreAuthorize，或所属类有类级 @PreAuthorize。
- 豁免（显式清单）：AuthController / PosAuthController（认证入口，无业务权限码，
  由 SecurityConfig permitAll + 登录链路保证，不属业务越权面）。
- 输出 CSV：scan-controller-authz-report.csv（工作目录）。

退出码：0 = 通过；1 = --strict 下覆盖率不足 100%；2 = 参数/目录错误。
"""
import argparse
import csv
import os
import re
import sys

ROUTE_ANNOTATIONS = {
    "RequestMapping", "GetMapping", "PostMapping", "PutMapping",
    "DeleteMapping", "PatchMapping",
}
EXEMPT_CLASSES = ("AuthController", "PosAuthController")

# 设计公开端点豁免清单（KL-043：SecurityConfig permitAll 34 端点，方法级）
# 与 SecurityConfig.java L84-164 permitAll 设计一一对应；--strict 时豁免端点
# 不计入未覆盖（无 @PreAuthorize 亦不构成裸奔，由 SecurityConfig URL 白名单控制）。
# 格式：(Controller类名, 方法名) -> SecurityConfig permitAll 依据
PERMIT_ALL_METHODS = {
    # /v1/device-registrations/status|activate（L161-162，APK 设备激活流程，激活码即凭证）
    ("AppDeviceRegistrationController", "getDeviceStatus"): "/v1/device-registrations/status",
    ("AppDeviceRegistrationController", "activate"): "/v1/device-registrations/activate",
    # /v1/receipt-confirmations/verify/**（L164，防伪码查验，RateLimit 防遍历）
    ("ReceiptConfirmationController", "verifyByCode"): "/v1/receipt-confirmations/verify/**",
    # /v1/users/check-username|email|phone（L89-91，注册表单校验）
    ("UserController", "checkUsername"): "/v1/users/check-username",
    ("UserController", "checkEmail"): "/v1/users/check-email",
    ("UserController", "checkPhone"): "/v1/users/check-phone",
    # /v1/tray/** GET（L108，托盘查询公开，写操作需认证）
    ("TrayController", "getAllTrays"): "GET /v1/tray/**",
    ("TrayController", "getIdleTrays"): "GET /v1/tray/**",
    ("TrayController", "getStats"): "GET /v1/tray/**",
    ("TrayController", "getByTrayCode"): "GET /v1/tray/**",
    # /v1/kitchen/** GET（L98，厨房查询公开，写操作需认证）
    ("KitchenOrderController", "list"): "GET /v1/kitchen/**",
    ("KitchenOrderController", "getById"): "GET /v1/kitchen/**",
    ("KitchenOrderController", "getFullById"): "GET /v1/kitchen/**",
    ("KitchenOrderController", "getActiveOrdersFull"): "GET /v1/kitchen/**",
    ("KitchenOrderController", "getRecentOrdersFull"): "GET /v1/kitchen/**",
    ("KitchenOrderController", "getByOrderId"): "GET /v1/kitchen/**",
    ("KitchenOrderController", "getStoreActiveOrders"): "GET /v1/kitchen/**",
    ("KitchenOrderController", "getChefActiveOrders"): "GET /v1/kitchen/**",
    ("KitchenOrderController", "statistics"): "GET /v1/kitchen/**",
    ("KitchenScanController", "getPendingOrders"): "GET /v1/kitchen/**",
    ("KitchenScanController", "getMakingOrders"): "GET /v1/kitchen/**",
    ("KitchenScanController", "getOrderRequirements"): "GET /v1/kitchen/**",
    ("KitchenScanController", "getPendingRequirementsByMaterial"): "GET /v1/kitchen/**",
    ("KitchenScanController", "getTraceCodeInfo"): "GET /v1/kitchen/**",
    ("KitchenScanController", "getUsageRecords"): "GET /v1/kitchen/**",
    ("KitchenScanController", "getKitchenStats"): "GET /v1/kitchen/**",
    ("KitchenScanController", "getOverdueOrders"): "GET /v1/kitchen/**",
    ("PreMakeController", "getReadyFoods"): "GET /v1/kitchen/**",
    # /v1/wecom/**（L87，企业微信集成：OAuth 免登、JSAPI 鉴权）
    ("WeComController", "detect"): "/v1/wecom/**",
    ("WeComController", "getJsApiConfig"): "/v1/wecom/**",
    ("WeComController", "oauthLogin"): "/v1/wecom/**",
    ("WeComController", "sendTextMessage"): "/v1/wecom/**",
    ("WeComController", "sendTemplateCard"): "/v1/wecom/**",
    ("WeComController", "getAuthUrl"): "/v1/wecom/**",
    # /v1/mp/**（L146，小程序 getOpenId：code 换 openId，无用户会话免登流）
    ("MiniProgramController", "getOpenId"): "/v1/mp/**",
}

ANNOT_LINE_RE = re.compile(r"^\s*@(\w+)")
ROUTE_ANNOT_RE = re.compile(
    r"^\s*@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)")
METHOD_DECL_RE = re.compile(
    r"^\s*(?:public|protected)\s+(?:(?!class\b)[\w<>,.?\[\]\s])+?(\w+)\s*\(")


CLASS_DECL_RE = re.compile(r"\bclass\s+\w+")


def parse_class_level(lines):
    """返回 (is_controller, class_has_preauth, class_decl_line)

    类级 @PreAuthorize：位于类声明行（含）之前的所有 @PreAuthorize 属类级。
    class_decl_line 同时作为第一个 handler 方法注解区间的下界。
    """
    is_controller = any("@RestController" in ln or "@Controller" in ln for ln in lines)
    cls_has_preauth = False
    cls_decl_line = -1
    for idx, ln in enumerate(lines):
        if CLASS_DECL_RE.search(ln):
            cls_decl_line = idx
            break
        if re.match(r"^\s*@PreAuthorize", ln):
            cls_has_preauth = True
    if cls_decl_line == -1:
        # 兜底：找不到类声明行（非常规 Controller），注解区间从 0 开始
        cls_decl_line = 0
    return is_controller, cls_has_preauth, cls_decl_line


def scan(base):
    rows = []
    total_methods = 0
    covered = 0
    controllers = 0
    for root, _dirs, files in os.walk(base):
        for name in sorted(files):
            if not name.endswith(".java"):
                continue
            path = os.path.join(root, name)
            with open(path, "r", encoding="utf-8", errors="replace") as f:
                text = f.read()
            lines = text.splitlines()
            is_controller, cls_has_preauth, cls_decl_line = parse_class_level(lines)
            if not is_controller:
                continue
            controllers += 1
            cls_name = name[:-5]
            exempt = cls_name in EXEMPT_CLASSES

            i = 0
            n = len(lines)
            # 归属边界：@PreAuthorize 必须位于「上一方法声明行之后、本方法声明行之前」区间。
            # 初始下界 = 类声明行（类声明行之前的 @PreAuthorize 属类级，已单独统计）。
            prev_decl_line = cls_decl_line
            while i < n:
                ln = lines[i]
                m = ROUTE_ANNOT_RE.match(ln)
                if not m:
                    i += 1
                    continue
                # 消费注解块：@Xxx(...) 可能跨行
                j = i
                open_paren = ln.count("(")
                close_paren = ln.count(")")
                while open_paren > close_paren and j + 1 < n:
                    j += 1
                    open_paren += lines[j].count("(")
                    close_paren += lines[j].count(")")
                # 从注解块结束向后找方法声明（允许中间有其他注解行/空白）
                k = j + 1
                method_name = None
                method_line = None
                while k < n and k <= j + 20:
                    lk = lines[k]
                    if METHOD_DECL_RE.search(lk):
                        method_name = METHOD_DECL_RE.search(lk).group(1)
                        method_line = k
                        break
                    if not lk.strip() or ANNOT_LINE_RE.match(lk) or lk.strip().startswith(("*/", "/*", "*")):
                        k += 1
                        continue
                    break  # 非注解行，不是方法声明 → 不匹配（如接口默认方法等）
                if method_name:
                    total_methods += 1
                    # 方法自身 @PreAuthorize：仅限「上一方法声明行 + 1 … 本方法声明行」区间，
                    # 严禁跨方法借用上一方法的注解（O-A-1：8 处假覆盖根因）。
                    window = "\n".join(lines[prev_decl_line + 1:method_line + 1])
                    method_has_preauth = "@PreAuthorize" in window
                    # 设计公开端点豁免（KL-043：SecurityConfig permitAll 34 端点，方法级）
                    permitall_exempt = (cls_name, method_name) in PERMIT_ALL_METHODS
                    ok = method_has_preauth or cls_has_preauth or exempt or permitall_exempt
                    if ok:
                        covered += 1
                    rows.append({
                        "controller": name,
                        "method": method_name,
                        "line": method_line + 1,
                        "class_preauth": cls_has_preauth,
                        "method_preauth": method_has_preauth,
                        "covered": ok,
                        "exempt": exempt,
                        "permitall": permitall_exempt,
                    })
                    prev_decl_line = method_line
                i = j + 1
    return controllers, total_methods, covered, rows


def main():
    parser = argparse.ArgumentParser(description="Controller 鉴权覆盖率扫描")
    parser.add_argument(
        "--base",
        default=os.path.join("backend", "src", "main", "java",
                             "com", "foodtraceability", "controller"),
    )
    parser.add_argument("--strict", action="store_true",
                        help="覆盖率 != 100%% 时退出码 1（CI 门禁模式）")
    parser.add_argument("--csv", default="scan-controller-authz-report.csv")
    args = parser.parse_args()

    if not os.path.isdir(args.base):
        print(f"ERROR: 目录不存在 {args.base}", file=sys.stderr)
        return 2

    controllers, total, covered, rows = scan(args.base)
    pct = (100.0 * covered / total) if total else 0.0
    print(f"Controllers          : {controllers}")
    print(f"Handler methods      : {total}")
    print(f"Covered              : {covered}")
    print(f"Coverage             : {pct:.1f}%")

    with open(args.csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)

    # 未覆盖 = 无注解且非设计豁免（permitAll 端点不计入未覆盖，见 KL-043）
    uncovered = [r for r in rows if not r["covered"] and not r["permitall"]]
    exempted = [r for r in rows if r["permitall"]]
    if exempted:
        print(f"\n设计豁免端点（SecurityConfig permitAll，KL-043）: {len(exempted)}")
        for r in exempted:
            print(f"  {r['controller']}:{r['line']} {r['method']}")
    if uncovered:
        print(f"\n未覆盖方法 ({len(uncovered)}):")
        for r in uncovered[:60]:
            print(f"  {r['controller']}:{r['line']} {r['method']}")
        if len(uncovered) > 60:
            print(f"  ... 共 {len(uncovered)} 个，完整清单见 {args.csv}")

    if args.strict and pct < 100.0:
        print("\n[CI] FAIL: 鉴权覆盖率未达 100%")
        return 1
    print("\n[CI] PASS" if args.strict else "")
    return 0


if __name__ == "__main__":
    sys.exit(main())
