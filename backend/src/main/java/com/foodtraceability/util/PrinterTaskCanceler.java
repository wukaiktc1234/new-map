package com.foodtraceability.util;

import java.io.OutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;

/**
 * 打印机任务取消工具
 * 用于直接向打印机发送取消所有打印任务的命令
 */
public class PrinterTaskCanceler {
    
    public static void main(String[] args) {
        try {
            // 打印机配置信息 - 从日志中获取
            String printerIp = "192.168.0.5";
            int printerPort = 9100; // 默认Socket打印端口
            
            System.out.println("正在连接打印机...");
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(printerIp, printerPort), 5000);
            
            System.out.println("连接成功，发送取消打印任务命令...");
            OutputStream outputStream = socket.getOutputStream();
            
            // 发送ESC/POS命令取消所有打印任务
            // ESC @ 初始化打印机
            // GS @ 重置打印机
            // ESC c 5 取消所有打印任务
            byte[] cancelCommands = {
                0x1B, 0x40, // ESC @ 初始化
                0x1D, 0x40, // GS @ 重置
                0x1B, 0x63, 0x05 // ESC c 5 取消所有任务
            };
            
            outputStream.write(cancelCommands);
            outputStream.flush();
            
            System.out.println("取消打印任务命令已发送");
            
            outputStream.close();
            socket.close();
            
            System.out.println("操作完成，已关闭连接");
            
        } catch (Exception e) {
            System.err.println("取消打印任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}