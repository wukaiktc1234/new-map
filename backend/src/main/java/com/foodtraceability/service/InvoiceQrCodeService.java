package com.foodtraceability.service;

import com.foodtraceability.dto.InvoiceQrInfoDTO;

import java.awt.image.BufferedImage;

/**
 * 发票二维码解析服务接口
 * 
 * 用于从PDF发票中提取二维码并解析发票信息
 * 
 * 二维码内容格式（国家税务总局标准）：
 * 01,发票代码,发票号码,金额,开票日期,校验码后6位
 * 例如：01,032002000411,09135145,84.80,20160420,123456
 */
public interface InvoiceQrCodeService {
    
    /**
     * 从PDF文件中提取二维码并解析发票信息
     * 
     * @param pdfContent PDF文件字节内容
     * @return 发票二维码信息，如果提取失败返回null
     */
    InvoiceQrInfoDTO extractQrCodeFromPdf(byte[] pdfContent);
    
    /**
     * 从图片中提取二维码并解析发票信息
     * 
     * @param image 图片
     * @return 发票二维码信息
     */
    InvoiceQrInfoDTO extractQrCodeFromImage(BufferedImage image);
    
    /**
     * 解析二维码内容
     * 
     * @param qrCodeContent 二维码内容字符串
     * @return 发票二维码信息
     */
    InvoiceQrInfoDTO parseInvoiceQrContent(String qrCodeContent);
    
    /**
     * 验证二维码内容格式是否正确
     * 
     * @param qrCodeContent 二维码内容
     * @return 是否为有效的发票二维码
     */
    boolean isValidInvoiceQrCode(String qrCodeContent);
}
