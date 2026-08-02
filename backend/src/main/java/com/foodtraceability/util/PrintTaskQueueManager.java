package com.foodtraceability.util;

import com.foodtraceability.entity.PrintTask;
import com.foodtraceability.service.DevicePrintService;
import com.foodtraceability.service.HardwareDeviceService;
import com.foodtraceability.service.HardwareConfigService;
import com.foodtraceability.service.DeviceConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

/**
 * 打印任务队列管理器
 * 用于管理打印任务队列，支持并发打印请求，确保打印任务有序执行
 */
@Component
public class PrintTaskQueueManager {

    private static final Logger log = LoggerFactory.getLogger(PrintTaskQueueManager.class);

    // 打印任务队列，按设备类型分组
    private final ConcurrentHashMap<String, BlockingQueue<PrintTask>> taskQueues = new ConcurrentHashMap<>();

    // 正在处理的任务数，按设备类型分组
    private final ConcurrentHashMap<String, AtomicInteger> processingTasks = new ConcurrentHashMap<>();

    // 最大并发数，可配置
    private final int maxConcurrentTasksPerDevice = 2;

    // 任务执行线程池
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread thread = new Thread(r, "print-task-executor-");
                thread.setDaemon(true);
                return thread;
            }
    );

    // 自动注入的服务
    private final DevicePrintService devicePrintService;

    private final HardwareDeviceService hardwareDeviceService;

    private final HardwareConfigService hardwareConfigService;

    private final com.foodtraceability.service.DeviceConnectionService deviceConnectionService;

    private final com.foodtraceability.controller.websocket.DeviceWebSocketController deviceWebSocketController;
    
    // 所有打印任务存储，按任务ID索引
    private final ConcurrentHashMap<String, PrintTask> allTasks = new ConcurrentHashMap<>();
    
    // 打印任务历史记录，用于查询过去的打印记录
    private final List<PrintTask> taskHistory = Collections.synchronizedList(new ArrayList<>());

    public PrintTaskQueueManager(@Lazy DevicePrintService devicePrintService, HardwareDeviceService hardwareDeviceService, HardwareConfigService hardwareConfigService, com.foodtraceability.service.DeviceConnectionService deviceConnectionService, com.foodtraceability.controller.websocket.DeviceWebSocketController deviceWebSocketController) {
        this.devicePrintService = devicePrintService;
        this.hardwareDeviceService = hardwareDeviceService;
        this.hardwareConfigService = hardwareConfigService;
        this.deviceConnectionService = deviceConnectionService;
        this.deviceWebSocketController = deviceWebSocketController;
        log.info("打印任务队列管理器初始化完成");
    }

    /**
     * 提交打印任务
     * @param task 打印任务
     * @return 提交结果，包含任务ID
     */
    public PrintTask submitTask(PrintTask task) {
        if (task == null) {
            throw new IllegalArgumentException("打印任务不能为空");
        }

        // 生成任务ID
        if (task.getTaskId() == null) {
            task.setTaskId(System.currentTimeMillis());
        }

        // 设置默认值
        task.setPrintStatus(0);
        task.setCreateTime(LocalDateTime.now());

        // 获取或创建设备类型对应的队列
        String deviceType = String.valueOf(task.getDeviceType());
        BlockingQueue<PrintTask> queue = taskQueues.computeIfAbsent(deviceType, k -> new LinkedBlockingQueue<>());

        // 将任务添加到队列
        queue.offer(task);
        
        // 存储任务到全局任务存储
        allTasks.put(String.valueOf(task.getTaskId()), task);
        
        log.info("提交打印任务: 任务ID={}, 设备类型={}, 打印类型={}", task.getTaskId(), deviceType, task.getPrintType());
        
        // 推送任务状态变化（适配新PrintTask实体：printStatus为Integer类型）
        if (deviceWebSocketController != null) {
            deviceWebSocketController.pushPrintTaskStatusChange(
                String.valueOf(task.getTaskId()), 
                String.valueOf(task.getPrintStatus())
            );
        }

        // 尝试执行任务
        executeTask(deviceType);

        return task;
    }

    /**
     * 执行打印任务
     * @param deviceType 设备类型
     */
    private void executeTask(String deviceType) {
        BlockingQueue<PrintTask> queue = taskQueues.get(deviceType);
        if (queue == null) {
            return;
        }

        // 获取或创建设备类型对应的处理任务计数器
        AtomicInteger count = processingTasks.computeIfAbsent(deviceType, k -> new AtomicInteger(0));

        // 如果当前处理的任务数未达到最大并发数，继续执行任务
        while (count.get() < maxConcurrentTasksPerDevice) {
            PrintTask task = queue.poll();
            if (task == null) {
                break; // 队列空，退出循环
            }

            // 增加处理任务计数
            count.incrementAndGet();
            log.info("开始处理打印任务: 任务ID={}, 设备类型={}", task.getTaskId(), deviceType);

            // 提交任务到线程池执行
            taskExecutor.submit(() -> {
                try {
                    // 更新任务状态为处理中（新实体：printStatus=1表示打印中）
                    task.setPrintStatus(1);
                    task.setPrintTime(LocalDateTime.now());
                    
                    // 更新全局存储中的任务状态
                    allTasks.put(String.valueOf(task.getTaskId()), task);

                    // 推送任务状态变化
                    if (deviceWebSocketController != null) {
                        deviceWebSocketController.pushPrintTaskStatusChange(
                            String.valueOf(task.getTaskId()),
                            String.valueOf(task.getPrintStatus())
                        );
                    }

                    // 执行打印任务
                    boolean success = executePrintTask(task);

                    // 更新任务状态（新实体：2=已完成, 3=失败）
                    if (success) {
                        task.setPrintStatus(2);
                        log.info("打印任务成功: 任务ID={}", task.getTaskId());
                    } else {
                        task.setPrintStatus(3);
                        log.warn("打印任务失败: 任务ID={}", task.getTaskId());
                    }

                    // 更新全局存储中的任务状态
                    allTasks.put(String.valueOf(task.getTaskId()), task);

                    // 推送任务状态变化
                    if (deviceWebSocketController != null) {
                        deviceWebSocketController.pushPrintTaskStatusChange(
                            String.valueOf(task.getTaskId()),
                            String.valueOf(task.getPrintStatus())
                        );
                    }
                } catch (Exception e) {
                    task.setPrintStatus(3);  // 3=失败
                    task.setErrorMsg(e.getMessage());
                    log.error("打印任务异常: 任务ID={}, 错误信息={}", task.getTaskId(), e.getMessage(), e);
                    
                    // 更新全局存储中的任务状态
                    allTasks.put(String.valueOf(task.getTaskId()), task);

                    // 推送任务状态变化
                    if (deviceWebSocketController != null) {
                        deviceWebSocketController.pushPrintTaskStatusChange(
                            String.valueOf(task.getTaskId()),
                            String.valueOf(task.getPrintStatus())
                        );
                    }
                } finally {
                    // 更新任务完成时间（新实体使用completeTime字段）
                    task.setCompleteTime(LocalDateTime.now());
                    
                    // 更新全局存储中的任务状态
                    allTasks.put(String.valueOf(task.getTaskId()), task);

                    // 添加到历史记录
                    taskHistory.add(task);
                    
                    // 限制历史记录数量，只保留最近30天的记录
                    cleanHistoryTasks();

                    // 减少处理任务计数
                    count.decrementAndGet();

                    // 尝试执行下一个任务
                    executeTask(deviceType);
                }
            });
        }
    }

    /**
     * 执行具体的打印任务
     * @param task 打印任务
     * @return 执行结果
     */
    private boolean executePrintTask(PrintTask task) {
        boolean result = false;

        try {
            // 适配新PrintTask实体字段名
            String printType = String.valueOf(task.getTaskType());  // Integer→String
            String deviceType = String.valueOf(task.getDeviceId());   // Long→String

            log.info("执行打印任务: 任务ID={}, 打印类型={}, 设备类型={}", task.getTaskId(), printType, deviceType);

            // 获取设备配置
            com.foodtraceability.entity.HardwareConfig printerConfig = hardwareConfigService.getConfig(null, deviceType);
            if (printerConfig == null) {
                throw new IllegalStateException("打印机配置不存在: " + deviceType);
            }

            // 根据打印类型执行不同的打印操作
            switch (printType) {
                case "THERMAL_PAPER":
                    // 打印热敏纸（新实体使用contentJson字段）
                    String content = task.getContentJson();
                    if (content != null) {
                        // 生成热敏纸打印数据，使用ESC/POS指令
                        StringBuilder thermalSb = new StringBuilder();
                        thermalSb.append((char) 27).append((char) 64); // 初始化打印机
                        thermalSb.append((char) 27).append((char) 61).append((char) 0); // 选择标准模式
                        thermalSb.append(content); // 打印内容
                        thermalSb.append((char) 10); // 换行
                        thermalSb.append((char) 27).append((char) 100).append((char) 5); // 走纸5行
                        thermalSb.append((char) 29).append((char) 86).append((char) 65).append((char) 0); // 切纸
                        
                        result = sendPrintCommand(printerConfig, thermalSb.toString());
                    }
                    break;
                    
                case "FILE":
                    // 打印文件
                    if (task.getFilePath() != null) {
                        java.io.File file = new java.io.File(task.getFilePath());
                        if (!file.exists() || !file.isFile()) {
                            throw new IllegalArgumentException("文件不存在或不是文件: " + task.getFilePath());
                        }
                        
                        // 读取文件内容
                        StringBuilder fileSb = new StringBuilder();
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                fileSb.append(line).append("\n");
                            }
                        }
                        
                        // 生成打印数据
                        StringBuilder filePrintSb = new StringBuilder();
                        filePrintSb.append((char) 27).append((char) 64); // 初始化打印机
                        filePrintSb.append((char) 27).append((char) 61).append((char) 0); // 选择标准模式
                        filePrintSb.append(fileSb.toString()); // 打印文件内容
                        filePrintSb.append((char) 27).append((char) 100).append((char) 5); // 走纸5行
                        filePrintSb.append((char) 29).append((char) 86).append((char) 65).append((char) 0); // 切纸
                        
                        result = sendPrintCommand(printerConfig, filePrintSb.toString());
                    }
                    break;
                    
                case "INVOICE":
                    // 打印发票（新实体：发票数据存储在contentJson JSON字段中）
                    String invoiceJson = task.getContentJson();
                    if (invoiceJson != null && !invoiceJson.isEmpty()) {
                        log.info("打印发票任务: 任务ID={}, 内容长度={}", task.getTaskId(), invoiceJson.length());
                        // TODO: 解析contentJson中的发票数据并格式化打印
                        // 当前简化处理：直接将JSON内容发送到打印机
                        StringBuilder invoiceSb = new StringBuilder();
                        invoiceSb.append((char) 27).append((char) 64); // 初始化打印机
                        invoiceSb.append(invoiceJson); // 打印内容
                        invoiceSb.append((char) 10); // 换行
                        invoiceSb.append((char) 27).append((char) 100).append((char) 5); // 走纸5行
                        invoiceSb.append((char) 29).append((char) 86).append((char) 65).append((char) 0); // 切纸

                        result = sendPrintCommand(printerConfig, invoiceSb.toString());
                    }
                    break;
                    
                case "TRACEABILITY_LABEL":
                    // 打印追溯码标签
                    if (task.getTraceabilityCode() != null && task.getProductName() != null) {
                        // 生成打印数据
                        String printData = devicePrintService.generatePrintData(
                                task.getTraceabilityCode(), task.getProductName(), printerConfig);
                        
                        // 发送打印命令
                        result = sendPrintCommand(printerConfig, printData);
                    }
                    break;
                    
                case "CUSTOM_TEMPLATE":
                    // 打印自定义模板
                    if (task.getContent() != null) {
                        // 使用 EscPosUtil 生成指令
                        byte[] escPosData = com.foodtraceability.util.EscPosUtil.generateEscPos(task.getContent());
                        
                        // 发送原始打印命令
                        result = sendRawPrintCommand(printerConfig, escPosData);
                    }
                    break;
                    
                default:
                    throw new IllegalArgumentException("不支持的打印类型: " + printType);
            }

            return result;
        } catch (Exception e) {
            log.error("执行打印任务异常: 任务ID={}, 错误信息={}", task.getTaskId(), e.getMessage(), e);
            task.setErrorMessage(e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送打印命令
     * @param config 设备配置
     * @param printData 打印数据
     * @return 发送结果
     */
    private boolean sendPrintCommand(com.foodtraceability.entity.HardwareConfig config, String printData) {
        try {
            String ip = config.getIpAddress();
            String port = config.getPort();
            
            log.info("正在向EPSON打印机发送打印数据: {}:{} 型号: {}", ip, port, config.getDeviceModel());
            
            // 直接使用已经生成好的EPSON ESC/POS格式数据
            Boolean result;
            if (port != null && port.contains("WSD")) {
                // WSD协议使用HTTP POST请求
                result = deviceConnectionService.sendWsdPrintCommand(ip, printData);
            } else {
                // 传统端口使用Socket连接
                int numericPort = port != null ? Integer.parseInt(port) : 9100;
                result = deviceConnectionService.sendSocketPrintCommand(ip, numericPort, printData);
            }
            
            return result;
        } catch (Exception e) {
            log.error("EPSON打印机打印失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送原始打印命令（字节数组）
     * @param config 设备配置
     * @param printData 打印数据（字节数组）
     * @return 发送结果
     */
    private boolean sendRawPrintCommand(com.foodtraceability.entity.HardwareConfig config, byte[] printData) {
        try {
            String ip = config.getIpAddress();
            String port = config.getPort();
            
            log.info("正在向EPSON打印机发送原始打印数据: {}:{} 型号: {}", ip, port, config.getDeviceModel());
            
            if (port != null && port.contains("WSD")) {
                // WSD协议使用HTTP POST请求，需要将byte[]发送
                // 由于deviceConnectionService不支持byte[]，这里简单实现一个
                try {
                    String url = String.format("http://%s:80/WsdPrintService", ip);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/octet-stream");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setDoOutput(true);
                    
                    try (java.io.OutputStream os = connection.getOutputStream()) {
                        os.write(printData);
                        os.flush();
                    }
                    
                    int responseCode = connection.getResponseCode();
                    connection.disconnect();
                    return responseCode >= 200 && responseCode < 300;
                } catch (Exception e) {
                    log.error("WSD打印失败: {}", e.getMessage(), e);
                    return false;
                }
            } else {
                // 传统端口使用Socket连接
                int numericPort = port != null ? Integer.parseInt(port) : 9100;
                try (java.net.Socket socket = new java.net.Socket()) {
                    socket.connect(new java.net.InetSocketAddress(ip, numericPort), 5000);
                    try (java.io.OutputStream outputStream = socket.getOutputStream()) {
                        outputStream.write(printData);
                        outputStream.flush();
                    }
                    return true;
                } catch (Exception e) {
                    log.error("Socket打印失败: {}", e.getMessage(), e);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("发送原始打印数据异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取设备类型对应的任务队列大小
     * @param deviceType 设备类型
     * @return 队列大小
     */
    public int getQueueSize(String deviceType) {
        BlockingQueue<PrintTask> queue = taskQueues.get(deviceType);
        return queue != null ? queue.size() : 0;
    }

    /**
     * 获取设备类型对应的正在处理的任务数
     * @param deviceType 设备类型
     * @return 正在处理的任务数
     */
    public int getProcessingCount(String deviceType) {
        AtomicInteger count = processingTasks.get(deviceType);
        return count != null ? count.get() : 0;
    }

    /**
     * 清理历史任务，只保留最近30天的记录
     */
    private void cleanHistoryTasks() {
        // 计算30天前的时间
        long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
        
        // 遍历历史记录，移除超过30天的任务
        Iterator<PrintTask> iterator = taskHistory.iterator();
        while (iterator.hasNext()) {
            PrintTask task = iterator.next();
            if (task.getCreateTime() != null &&
                java.sql.Timestamp.valueOf(task.getCreateTime()).getTime() < thirtyDaysAgo) {
                iterator.remove();
            } else {
                // 因为历史记录是按时间顺序添加的，所以一旦遇到不满足条件的任务，后面的任务也不会满足条件
                break;
            }
        }
    }
    
    /**
     * 根据任务ID获取打印任务
     * @param taskId 任务ID
     * @return 打印任务，如果不存在则返回null
     */
    public PrintTask getTaskById(String taskId) {
        return allTasks.get(taskId);
    }
    
    /**
     * 获取指定设备类型的所有任务
     * @param deviceType 设备类型，null表示获取所有设备类型的任务
     * @return 打印任务列表
     */
    public List<PrintTask> getTasksByDeviceType(String deviceType) {
        List<PrintTask> result = new ArrayList<>();
        for (PrintTask task : allTasks.values()) {
            if (deviceType == null || deviceType.equals(task.getDeviceType())) {
                result.add(task);
            }
        }
        return result;
    }
    
    /**
     * 获取打印任务历史记录
     * @param deviceType 设备类型，null表示获取所有设备类型的任务
     * @param limit 限制数量，0表示获取所有记录
     * @return 打印任务历史记录列表
     */
    public List<PrintTask> getTaskHistory(String deviceType, int limit) {
        List<PrintTask> result = new ArrayList<>();
        
        // 倒序遍历，获取最新的记录
        for (int i = taskHistory.size() - 1; i >= 0 && (limit == 0 || result.size() < limit); i--) {
            PrintTask task = taskHistory.get(i);
            if (deviceType == null || deviceType.equals(task.getDeviceType())) {
                result.add(task);
            }
        }
        
        return result;
    }
    
    /**
     * 关闭打印任务队列管理器
     */
    public void shutdown() {
        log.info("关闭打印任务队列管理器");
        taskExecutor.shutdown();
        try {
            if (!taskExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                taskExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            taskExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
