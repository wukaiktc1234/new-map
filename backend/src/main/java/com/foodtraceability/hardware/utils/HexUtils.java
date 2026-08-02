package com.foodtraceability.hardware.utils;

/**
 * 十六进制编码工具类
 * <p>
 * 提供字节数组与十六进制字符串之间的相互转换功能。
 * 用于硬件驱动中二进制原始数据的统一编码格式，确保不同驱动实现
 * （串口、网络、USB等）的 raw data 行为一致。
 * </p>
 *
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>硬件驱动的 {@code sendRawData()} / {@code receiveRawData()} 方法</li>
 *   <li>固件升级时的二进制数据传输</li>
 *   <li>图像/视频数据的编码和解码</li>
 * </ul>
 *
 * <p><strong>编码规范：</strong></p>
 * <ul>
 *   <li>输出格式：大写十六进制字符串（如 "0A1B2C"）</li>
 *   <li>每个字节转换为2个十六进制字符</li>
 *   <li>输入字符串长度必须为偶数（每2个字符对应1个字节）</li>
 * </ul>
 *
 * @see com.foodtraceability.hardware.BinaryDriver 二进制协议接口
 */
public final class HexUtils {

    /** 私有构造函数，防止实例化 */
    private HexUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 将字节数组转换为大写十六进制字符串
     * <p>将每个字节转换为其对应的2位十六进制表示，使用大写字母（A-F）。</p>
     *
     * <p><strong>示例：</strong></p>
     * <pre>{@code
     * byte[] bytes = {0x0A, 0x1B, (byte)0xFF};
     * String hex = HexUtils.bytesToHex(bytes); // 结果: "0A1BFF"
     * }</pre>
     *
     * @param bytes 要转换的字节数组，不能为null
     * @return 大写十六进制字符串；空数组返回空字符串
     * @throws NullPointerException 当 bytes 为 null 时抛出
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("字节数组不能为null");
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 将十六进制字符串转换为字节数组
     * <p>将大写或小写的十六进制字符串解析为对应的字节数组。
     * 输入字符串中的非十六进制字符将被忽略或导致异常。</p>
     *
     * <p><strong>示例：</strong></p>
     * <pre>{@code
     * String hex = "0a1bFF";
     * byte[] bytes = HexUtils.hexToBytes(hex); // 结果: {0x0A, 0x1B, (byte)0xFF}
     * }</pre>
     *
     * @param hex 十六进制字符串，不能为null且长度必须为偶数
     * @return 解析后的字节数组；空字符串返回空数组
     * @throws NullPointerException 当 hex 为 null 时抛出
     * @throws IllegalArgumentException 当 hex 长度为奇数时抛出
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null) {
            throw new NullPointerException("十六进制字符串不能为null");
        }
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数，当前长度: " + len);
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
