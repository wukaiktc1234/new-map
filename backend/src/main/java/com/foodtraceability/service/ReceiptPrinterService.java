package com.foodtraceability.service;

import com.foodtraceability.dto.OrderResultDTO;
import com.foodtraceability.hardware.BinaryDriver;
import com.foodtraceability.hardware.DriverRegistry;
import com.foodtraceability.hardware.HardwareDriver;
import com.foodtraceability.hardware.TextBasedDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 热敏打印机小票打印服务
 * <p>
 * 基于 ESC/POS 协议实现热敏打印机自动打印功能。
 * 支付成功后自动调用此服务打印消费小票。
 * </p>
 *
 * <p><strong>核心特性：</strong></p>
 * <ul>
 *   <li>标准 ESC/POS 指令集支持</li>
 *   <li>GBK/GB2312 中文编码</li>
 *   <li>58mm/80mm 纸宽自适应</li>
 *   <li>异常隔离：打印失败不影响主流程</li>
 * </ul>
 *
 * @see DriverRegistry 驱动注册中心
 * @see BinaryDriver 二进制数据接口
 */
@Service
public class ReceiptPrinterService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptPrinterService.class);


    public ReceiptPrinterService(DriverRegistry driverRegistry) {
        this.driverRegistry = driverRegistry;
    }

    /** ESC/POS 指令常量 */
    private static final byte ESC = 0x1B;  // ESC字符
    private static final byte GS = 0x1D;   // GS字符
    private static final byte LF = 0x0A;   // 换行

    /** ESC/POS 命令 */
    private static final byte[] INIT_PRINTER = {ESC, 0x40};           // 初始化打印机 ESC @
    private static final byte[] BOLD_ON = {ESC, 0x21, 0x08};          // 加粗开启 ESC ! n
    private static final byte[] BOLD_OFF = {ESC, 0x21, 0x00};         // 加粗关闭
    private static final byte[] DOUBLE_HEIGHT_WIDTH = {ESC, 0x21, 0x38}; // 双倍高宽 ESC ! n(0x38)
    private static final byte[] DOUBLE_HEIGHT = {ESC, 0x21, 0x10};     // 双倍高 ESC ! n(0x10)
    private static final byte[] NORMAL_FONT = {ESC, 0x21, 0x00};       // 正常字体
    private static final byte[] ALIGN_CENTER = {ESC, 0x61, 0x01};      // 居中对齐 ESC a n
    private static final byte[] ALIGN_LEFT = {ESC, 0x61, 0x00};        // 左对齐
    private static final byte[] ALIGN_RIGHT = {ESC, 0x61, 0x02};       // 右对齐
    private static final byte[] CUT_PAPER = {GS, 0x56, 0x00};          // 切纸 GS V m(全切)
    private static final byte[] FEED_LINES = {ESC, 0x64, 0x05};        // 走纸5行 ESC d n

    private final DriverRegistry driverRegistry;

    @Value("${receipt.printer.enabled:true}")
    private boolean printerEnabled;

    @Value("${receipt.printer.paper-width:32}")
    private int paperWidthChars;

    @Value("${receipt.shop.name:美味餐厅}")
    private String shopName;

    @Value("${receipt.shop.address:}")
    private String shopAddress;

    @Value("${receipt.shop.phone:}")
    private String shopPhone;

    /**
     * 打印消费小票
     * <p>
     * 根据订单结果构建 ESC/POS 格式的小票数据并发送到热敏打印机。
     * 此方法内部捕获所有异常，确保打印失败不会影响支付主流程。
     * </p>
     *
     * @param orderResult 订单支付结果，包含订单号、金额、商品明细等信息
     */
    public void printReceipt(OrderResultDTO orderResult) {
        if (!printerEnabled) {
            log.info("打印机功能已禁用，跳过打印");
            return;
        }

        if (orderResult == null) {
            log.warn("订单结果为空，无法打印小票");
            return;
        }

        log.info("开始打印小票: orderNumber={}, totalAmount={}",
            orderResult.getOrderNumber(), orderResult.getTotalAmount());

        try {
            // 1. 构建ESC/POS格式的小票数据
            byte[] receiptData = buildReceiptData(orderResult);

            // 2. 通过驱动注册中心获取串口驱动
            BinaryDriver printerDriver = getPrinterDriver();
            if (printerDriver == null) {
                log.error("未找到可用的打印机驱动，请检查硬件配置");
                return;
            }

            // 3. 检查连接状态
            if (!printerDriver.isConnected()) {
                log.warn("打印机未连接，尝试重新连接...");
                boolean connected = reconnectPrinter(printerDriver);
                if (!connected) {
                    log.error("打印机重连失败，无法打印小票");
                    return;
                }
            }

            // 4. 发送打印数据
            printerDriver.sendRawData(receiptData);
            log.info("小票打印成功: orderNumber={}", orderResult.getOrderNumber());

        } catch (Exception e) {
            // 关键：打印异常必须隔离，不能影响支付流程
            log.error("小票打印异常: orderNumber={}, error={}",
                orderResult.getOrderNumber(), e.getMessage(), e);
        }
    }

    /**
     * 根据订单ID重新打印小票
     * <p>
     * 用于手动触发重打功能，如打印机故障恢复后补打。
     * </p>
     *
     * @param orderResult 订单信息
     * @return true 打印成功；false 打印失败
     */
    public boolean reprintReceipt(OrderResultDTO orderResult) {
        if (orderResult == null) {
            log.warn("重打小票：订单结果为空");
            return false;
        }

        log.info("手动重打小票: orderNumber={}", orderResult.getOrderNumber());

        try {
            byte[] receiptData = buildReceiptData(orderResult);
            BinaryDriver printerDriver = getPrinterDriver();

            if (printerDriver == null || !printerDriver.isConnected()) {
                log.error("重打小票失败：打印机不可用");
                return false;
            }

            printerDriver.sendRawData(receiptData);
            log.info("小票重打成功: orderNumber={}", orderResult.getOrderNumber());
            return true;

        } catch (Exception e) {
            log.error("重打小票异常: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建完整的ESC/POS小票数据
     * <p>
     * 小票内容结构：
     * <ol>
     *   <li>店铺名称（居中、双倍大字）</li>
     *   <li>店铺地址和电话</li>
     *   <li>分隔线</li>
     *   <li>小票编号和日期时间</li>
     *   <li>分隔线</li>
     *   <li>消费明细（菜品名 × 数量 = 单价 → 小计）</li>
     *   <li>分隔线</li>
     *   <li>合计金额（加粗）</li>
     *   <li>支付方式</li>
     *   <li>分隔线</li>
     *   <li>"谢谢光临"</li>
     *   <li>切纸命令</li>
     * </ol>
     * </p>
     *
     * @param orderResult 订单结果
     * @return 编码后的字节数组（GBK编码）
     */
    private byte[] buildReceiptData(OrderResultDTO orderResult) {
        StringBuilder sb = new StringBuilder(1024);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // ====== 1. 店铺头部信息 ======
        sb.append(bytesToString(INIT_PRINTER));         // 初始化打印机
        sb.append(bytesToString(ALIGN_CENTER));          // 居中对齐
        sb.append(bytesToString(DOUBLE_HEIGHT_WIDTH));   // 双倍高宽字体

        // 店铺名称（最多显示16个中文字符）
        String displayName = truncateChinese(shopName, 16);
        sb.append(displayName).append("\n");

        sb.append(bytesToString(NORMAL_FONT));           //恢复正常字体
        sb.append(bytesToString(BOLD_OFF));

        // 地址和电话
        if (shopAddress != null && !shopAddress.isEmpty()) {
            sb.append(truncateChinese(shopAddress, 24)).append("\n");
        }
        if (shopPhone != null && !shopPhone.isEmpty()) {
            sb.append("电话: ").append(shopPhone).append("\n");
        }
        sb.append("\n");

        // ====== 2. 分隔线 ======
        sb.append(getSeparatorLine()).append("\n\n");

        // ====== 3. 小票编号和时间 ======
        sb.append(bytesToString(ALIGN_LEFT));            // 左对齐
        sb.append("小票编号: ").append(orderResult.getOrderNumber()).append("\n");
        sb.append("打印时间: ").append(LocalDateTime.now().format(dateFormatter)).append("\n");

        // 桌号信息
        if (orderResult.getTableNumber() != null && !orderResult.getTableNumber().isEmpty()) {
            sb.append("桌    号: ").append(orderResult.getTableNumber()).append("号桌\n");
        }

        // 取餐号
        if (orderResult.getPickupNumber() != null && !orderResult.getPickupNumber().isEmpty()) {
            sb.append("取 餐 号: ").append(orderResult.getPickupNumber()).append("\n");
        }
        sb.append("\n");

        // ====== 4. 分隔线 ======
        sb.append(getSeparatorLine()).append("\n");

        // ====== 5. 消费明细 ======
        sb.append(bytesToString(BOLD_ON));               // 加粗
        sb.append("消 费 明 细\n");
        sb.append(bytesToString(BOLD_OFF));

        List<OrderResultDTO.OrderItemInfo> items = orderResult.getOrderItems();
        if (items != null && !items.isEmpty()) {
            for (OrderResultDTO.OrderItemInfo item : items) {
                String itemLine = formatItemLine(
                    item.getName(),
                    item.getQuantity(),
                    item.getPrice()
                );
                sb.append(itemLine);
            }
        } else {
            sb.append("(无商品明细)\n");
        }
        sb.append("\n");

        // ====== 6. 分隔线 ======
        sb.append(getSeparatorLine()).append("\n");

        // ====== 7. 合计金额 ======
        BigDecimal totalAmount = orderResult.getTotalAmount();
        if (totalAmount != null) {
            sb.append(bytesToString(ALIGN_RIGHT));           // 右对齐
            sb.append(bytesToString(DOUBLE_HEIGHT));          // 双倍高度
            sb.append(String.format("合计: ¥%.2f", totalAmount.setScale(2, RoundingMode.HALF_UP)));
            sb.append("\n");
            sb.append(bytesToString(NORMAL_FONT));
            sb.append(bytesToString(ALIGN_LEFT));
        }

        // ====== 8. 支付方式（从状态推断）=====
        sb.append("\n");
        sb.append("支付方式: ");
        String paymentMethod = inferPaymentMethod(orderResult.getStatus());
        sb.append(paymentMethod).append("\n");

        // ====== 9. 结束语 ======
        sb.append("\n");
        sb.append(bytesToString(ALIGN_CENTER));
        sb.append("------------------------\n");
        sb.append("  谢谢光临  欢迎再来  \n");
        sb.append("  Thank You           \n");
        sb.append("------------------------\n");
        sb.append("\n\n\n");  // 多走几行纸

        // ====== 10. 切纸命令 ======
        sb.append(bytesToString(CUT_PAPER));
        sb.append(bytesToString(FEED_LINES));

        // 转换为字节数组（使用GBK编码支持中文）
        return sb.toString().getBytes(Charset.forName("GBK"));
    }

    /**
     * 格式化单个商品行为固定宽度字符串
     * <p>
     * 格式：菜品名（左对齐） + 数量 + "x" + 单价 → 小计（右对齐）
     * 示例："红烧牛肉面      x1  ¥28.00  ¥28.00"
     * </p>
     *
     * @param name 商品名称
     * @param quantity 数量
     * @param price 单价
     * @return 格式化后的字符串
     */
    private String formatItemLine(String name, Integer quantity, BigDecimal price) {
        if (name == null) name = "(未知商品)";
        if (quantity == null) quantity = 1;
        if (price == null) price = BigDecimal.ZERO;

        // 商品名截断到14个中文字符（约28字节）
        String truncatedName = truncateChinese(name, 14);

        // 计算小计
        BigDecimal subtotal = price.multiply(new BigDecimal(quantity));
        String quantityStr = String.valueOf(quantity);
        String priceStr = String.format("¥%.2f", price.setScale(2, RoundingMode.HALF_UP));
        String subtotalStr = String.format("¥%.2f", subtotal.setScale(2, RoundingMode.HALF_UP));

        // 构建格式化的行
        StringBuilder line = new StringBuilder(paperWidthChars + 10);
        line.append(truncatedName);

        // 填充空格使单价和小计右对齐
        int currentLength = getDisplayWidth(truncatedName);
        int targetLength = paperWidthChars - (quantityStr.length() + priceStr.length() + subtotalStr.length() + 6);

        if (targetLength > currentLength) {
            line.append(repeatChar(' ', targetLength - currentLength));
        }

        line.append(" x").append(quantityStr).append(" ");
        line.append(priceStr).append("  ");
        line.append(subtotalStr).append("\n");

        return line.toString();
    }

    /**
     * 生成分隔线
     * <p>根据纸宽生成对应长度的"-"分隔线</p>
     *
     * @return 分隔线字符串
     */
    private String getSeparatorLine() {
        return repeatChar('-', paperWidthChars);
    }

    /**
     * 截断中文字符串到指定长度
     * <p>
     * 正确处理中文字符（每个中文占2个显示宽度），
     * 确保不会在中文中间截断导致乱码。
     * </p>
     *
     * @param str 原始字符串
     * @param maxChars 最大字符数（中文算1个字符）
     * @return 截断后的字符串
     */
    private String truncateChinese(String str, int maxChars) {
        if (str == null) return "";

        int displayWidth = 0;
        int charIndex = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // 中文字符占2个显示宽度
            int charWidth = (c >= '\u4e00' && c <= '\u9fff') ? 2 : 1;

            if (displayWidth + charWidth > maxChars * 2) {
                break;
            }

            displayWidth += charWidth;
            charIndex++;
        }

        return str.substring(0, charIndex);
    }

    /**
     * 计算字符串的显示宽度
     * <p>中文算2个宽度，英文/数字算1个宽度</p>
     *
     * @param str 输入字符串
     * @return 显示宽度
     */
    private int getDisplayWidth(String str) {
        if (str == null) return 0;

        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            width += (c >= '\u4e00' && c <= '\u9fff') ? 2 : 1;
        }
        return width;
    }

    /**
     * 重复字符指定次数
     *
     * @param c 要重复的字符
     * @param count 重复次数
     * @return 重复后的字符串
     */
    private String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 将字节数组转换为可追加到StringBuilder的字符串表示
     * <p>用于将ESC/POS控制码插入到文本流中</p>
     *
     * @param bytes 字节数组
     * @return 对应的新字符串对象
     */
    private String bytesToString(byte[] bytes) {
        return new String(bytes, Charset.forName("ISO-8859-1"));
    }

    /**
     * 从订单状态推断支付方式
     * <p>由于 OrderResultDTO 不直接包含支付方式字段，
     * 此处根据业务逻辑进行推断。实际项目中应扩展DTO。</p>
     *
     * @param status 订单状态
     * @return 支付方式描述
     */
    private String inferPaymentMethod(String status) {
        // 默认返回现金，实际应从订单实体获取准确值
        return "现金";
    }

    /**
     * 获取打印机驱动实例
     * <p>
     * 优先从 DriverRegistry 获取已注册的串口驱动（SERIAL类型），
     * 如果没有则尝试其他支持的连接类型。
     * </p>
     *
     * @return BinaryDriver 接口的驱动实例；无可用驱动时返回null
     */
    private BinaryDriver getPrinterDriver() {
        try {
            // 优先查找串口驱动的二进制通信能力
            HardwareDriver driver = driverRegistry.getDriver("SERIAL", "SERIAL");
            if (driver instanceof BinaryDriver binaryDriver) {
                log.debug("获取到串口打印机驱动: {}", driver.getClass().getSimpleName());
                return binaryDriver;
            }

            // 尝试USB驱动
            driver = driverRegistry.getDriver("SERIAL", "USB");
            if (driver instanceof BinaryDriver binaryDriver) {
                log.debug("获取到USB打印机驱动: {}", driver.getClass().getSimpleName());
                return binaryDriver;
            }

            // 尝试网络驱动
            driver = driverRegistry.getDriver("NETWORK", "NETWORK");
            if (driver instanceof BinaryDriver binaryDriver) {
                log.debug("获取到网络打印机驱动: {}", driver.getClass().getSimpleName());
                return binaryDriver;
            }

            log.warn("未找到任何可用的打印机驱动");
            return null;

        } catch (Exception e) {
            log.error("获取打印机驱动异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 尝试重新连接打印机
     * <p>当检测到打印机离线时，尝试使用默认配置重新建立连接</p>
     *
     * @param driver 打印机驱动实例
     * @return true 重连成功；false 重连失败
     */
    private boolean reconnectPrinter(BinaryDriver driver) {
        try {
            log.info("尝试重新连接打印机...");
            // 这里可以添加自动重连逻辑
            // 当前版本仅记录日志，实际项目可根据配置文件中的端口参数进行重连
            return driver.isConnected() || driver.testConnection();
        } catch (Exception e) {
            log.error("打印机重连失败: {}", e.getMessage());
            return false;
        }
    }
}
