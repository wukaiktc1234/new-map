package com.foodtraceability.service;

import com.foodtraceability.dto.SignatureVerifyResultDTO;

/**
 * 电子凭证工具包集成服务接口
 * 
 * 依据：
 * - 财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * - 基础工具包（推广应用版V1.0）
 * - 用友网络科技股份有限公司个性化工具包（推广应用版V1.0）
 * 
 * 集成的工具包：
 * 1. pdf-xml-extractor-1.0.jar - PDF转XML提取
 * 2. xbrl-json-1.0.jar - XBRL与JSON互转
 * 3. DJFileUtil-1.0.jar - 文件处理和验签
 */
public interface VoucherToolkitService {
    
    /**
     * 使用官方工具包从PDF提取XML
     * 
     * @param pdfContent PDF文件字节内容
     * @return 提取的XML内容
     */
    String extractXmlFromPdfByToolkit(byte[] pdfContent);
    
    /**
     * 使用官方工具包将XBRL转换为JSON
     * 
     * @param xbrlContent XBRL格式内容
     * @return JSON格式内容
     */
    String convertXbrlToJson(String xbrlContent);
    
    /**
     * 使用官方工具包将JSON转换为XBRL
     * 
     * @param jsonContent JSON格式内容
     * @return XBRL格式内容
     */
    String convertJsonToXbrl(String jsonContent);
    
    /**
     * 使用用友工具包验证电子签名
     * 
     * @param fileContent 文件字节内容
     * @param fileType 文件类型（pdf/ofd/xml）
     * @return 验签结果
     */
    SignatureVerifyResultDTO verifySignatureByToolkit(byte[] fileContent, String fileType);
    
    /**
     * 使用用友工具包验证OFD文件签名
     * 
     * @param ofdContent OFD文件字节内容
     * @return 验签结果
     */
    SignatureVerifyResultDTO verifyOfdSignatureByToolkit(byte[] ofdContent);
    
    /**
     * 检查工具包是否可用
     * 
     * @return 工具包可用状态
     */
    boolean isToolkitAvailable();
    
    /**
     * 获取工具包版本信息
     * 
     * @return 版本信息字符串
     */
    String getToolkitVersion();
}
