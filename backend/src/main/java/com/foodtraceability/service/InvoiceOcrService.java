package com.foodtraceability.service;

import com.foodtraceability.dto.InvoiceOcrResultDTO;

import java.awt.image.BufferedImage;

/**
 * 发票OCR识别服务接口
 * 
 * 功能：
 * 1. 从发票图片中识别文字
 * 2. 提取发票关键信息
 * 3. 支持多种OCR引擎
 */
public interface InvoiceOcrService {
    
    /**
     * 从PDF文件识别发票信息
     * @param pdfContent PDF文件字节数组
     * @return OCR识别结果
     */
    InvoiceOcrResultDTO recognizeFromPdf(byte[] pdfContent);
    
    /**
     * 从图片识别发票信息
     * @param image 图片
     * @return OCR识别结果
     */
    InvoiceOcrResultDTO recognizeFromImage(BufferedImage image);
    
    /**
     * 从图片字节数组识别发票信息
     * @param imageData 图片字节数组
     * @param format 图片格式 (png, jpg等)
     * @return OCR识别结果
     */
    InvoiceOcrResultDTO recognizeFromImageBytes(byte[] imageData, String format);
    
    /**
     * 检查OCR服务是否可用
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取OCR引擎名称
     * @return 引擎名称
     */
    String getEngineName();
}
