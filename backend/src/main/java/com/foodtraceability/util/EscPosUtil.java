package com.foodtraceability.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ESC/POS 指令生成工具类
 * 用于将 JSON 格式的打印模板转换为 ESC/POS 指令
 */
public class EscPosUtil {

    private static final Logger log = LoggerFactory.getLogger(EscPosUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CHARSET = "GBK";

    // ESC/POS 指令常量
    private static final byte[] INIT = {0x1B, 0x40}; // 初始化
    private static final byte[] LF = {0x0A}; // 换行
    private static final byte[] CUT_FULL = {0x1D, 0x56, 0x00}; // 全切
    private static final byte[] CUT_PARTIAL = {0x1D, 0x56, 0x01}; // 半切

    /**
     * 将模板 JSON 转换为 ESC/POS 指令
     *
     * @param templateJson 模板 JSON 字符串
     * @return ESC/POS 指令字节数组
     */
    public static byte[] generateEscPos(String templateJson) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 1. 初始化
            baos.write(INIT);

            // 2. 解析 JSON
            JsonNode root = objectMapper.readTree(templateJson);
            if (!root.has("elements") || !root.get("elements").isArray()) {
                log.warn("Invalid template JSON: missing elements array");
                return baos.toByteArray();
            }

            // 3. 将元素转换为列表并按 Y 坐标排序（模拟流式打印）
            // 注意：ESC/POS 是流式的，如果前端是绝对定位，我们需要尽量还原顺序
            // 对于复杂的绝对定位（如左右并排），ESC/POS 需要使用绝对位置指令 ESC $
            List<JsonNode> elements = new ArrayList<>();
            root.get("elements").forEach(elements::add);
            
            // 按 Y 坐标排序，如果 Y 相同则按 X 排序
            Collections.sort(elements, new Comparator<JsonNode>() {
                @Override
                public int compare(JsonNode o1, JsonNode o2) {
                    double y1 = o1.has("y") ? o1.get("y").asDouble() : 0;
                    double y2 = o2.has("y") ? o2.get("y").asDouble() : 0;
                    int yCompare = Double.compare(y1, y2);
                    if (yCompare != 0) return yCompare;
                    
                    double x1 = o1.has("x") ? o1.get("x").asDouble() : 0;
                    double x2 = o2.has("x") ? o2.get("x").asDouble() : 0;
                    return Double.compare(x1, x2);
                }
            });

            // 4. 遍历元素生成指令
            double lastY = 0;
            for (JsonNode element : elements) {
                String type = element.has("type") ? element.get("type").asText() : "text";
                double currentY = element.has("y") ? element.get("y").asDouble() : 0;
                
                // 简单的流式模拟：如果 Y 坐标增加了，插入换行
                // 注意：这里只是非常粗略的模拟，精确的绝对定位需要计算行高和偏移
                if (currentY > lastY + 20) { // 假设行高约 20px
                    baos.write(LF);
                }
                lastY = currentY;

                switch (type) {
                    case "text":
                        writeText(baos, element);
                        break;
                    case "line":
                        writeLine(baos, element);
                        break;
                    case "barcode":
                        writeBarcode(baos, element);
                        break;
                    case "qrcode":
                        writeQrCode(baos, element);
                        break;
                    // 图片和表格暂不支持，留空
                    default:
                        break;
                }
                
                // 每个元素后默认不换行，除非是文本块且 explicitly 需要
                // 但为了防止重叠，我们可以在文本后加一个换行，或者依赖上面的 Y 坐标判断
                baos.write(LF); 
            }

            // 5. 走纸和切纸
            baos.write(new byte[]{0x1B, 0x64, 0x05}); // 走纸 5 行
            baos.write(CUT_PARTIAL);

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating ESC/POS commands", e);
            return new byte[0];
        }
    }

    private static void writeText(ByteArrayOutputStream baos, JsonNode element) throws IOException {
        // 设置对齐
        String align = element.has("textAlign") ? element.get("textAlign").asText() : "left";
        int alignVal = 0;
        if ("center".equalsIgnoreCase(align)) alignVal = 1;
        else if ("right".equalsIgnoreCase(align)) alignVal = 2;
        baos.write(new byte[]{0x1B, 0x61, (byte) alignVal});

        // 设置字体大小 (GS ! n)
        // 假设 fontSize 12 是正常，24 是两倍
        int fontSize = element.has("fontSize") ? element.get("fontSize").asInt() : 12;
        int sizeByte = 0;
        if (fontSize >= 24) sizeByte = 0x11; // 宽高双倍
        else if (fontSize >= 18) sizeByte = 0x01; // 仅高双倍（或者根据需求调整）
        baos.write(new byte[]{0x1D, 0x21, (byte) sizeByte});

        // 设置加粗 (ESC E n)
        boolean bold = element.has("fontStyle") && element.get("fontStyle").has("bold") && element.get("fontStyle").get("bold").asBoolean();
        baos.write(new byte[]{0x1B, 0x45, (byte) (bold ? 1 : 0)});

        // 写入内容
        String content = element.has("content") ? element.get("content").asText() : "";
        baos.write(content.getBytes(CHARSET));

        // 重置样式
        baos.write(new byte[]{0x1B, 0x45, 0}); // 取消加粗
        baos.write(new byte[]{0x1D, 0x21, 0}); // 取消放大
        baos.write(new byte[]{0x1B, 0x61, 0}); // 取消对齐
    }

    private static void writeLine(ByteArrayOutputStream baos, JsonNode element) throws IOException {
        // 打印分隔线
        baos.write("--------------------------------".getBytes(CHARSET));
    }

    private static void writeBarcode(ByteArrayOutputStream baos, JsonNode element) throws IOException {
        String content = element.has("content") ? element.get("content").asText() : "";
        if (content.isEmpty()) return;

        // CODE128
        baos.write(new byte[]{0x1D, 0x6B, 73, (byte) content.length()});
        baos.write(content.getBytes(CHARSET));
    }

    private static void writeQrCode(ByteArrayOutputStream baos, JsonNode element) throws IOException {
        String content = element.has("content") ? element.get("content").asText() : "";
        if (content.isEmpty()) return;

        // QR Code 指令比较复杂，这里简化处理，假设打印机支持 GS ( k 系列指令
        // 1. Model
        baos.write(new byte[]{0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00});
        // 2. Module size
        baos.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06});
        // 3. Error correction level
        baos.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31});
        // 4. Store data
        int len = content.length() + 3;
        baos.write(new byte[]{0x1D, 0x28, 0x6B, (byte) (len % 256), (byte) (len / 256), 0x31, 0x50, 0x30});
        baos.write(content.getBytes(CHARSET));
        // 5. Print
        baos.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30});
    }
}
