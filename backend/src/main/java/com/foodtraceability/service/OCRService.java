package com.foodtraceability.service;

import com.foodtraceability.dto.OCRResultDTO;

import java.util.List;

public interface OCRService {
    
    /**
     * 识别图片中的文字
     * @param imageBase64 Base64编码的图片
     * @return 识别结果
     */
    OCRResultDTO recognizeImage(String imageBase64);
    
    /**
     * 从识别结果中提取商品信息
     * @param ocrText OCR识别的原始文本
     * @return 提取的商品信息
     */
    OCRResultDTO extractProductInfo(String ocrText);
    
    /**
     * 匹配已有商品档案
     * @param productName 商品名称
     * @param barcode 条码
     * @return 匹配的档案ID，未匹配返回null
     */
    Long matchExistingTemplate(String productName, String barcode);
    
    /**
     * 对比OCR结果与档案差异
     * @param templateId 档案ID
     * @param ocrResult OCR识别结果
     * @return 差异列表
     */
    List<String> compareWithTemplate(Long templateId, OCRResultDTO ocrResult);
}
