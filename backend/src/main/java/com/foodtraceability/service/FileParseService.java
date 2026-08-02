package com.foodtraceability.service;

import com.foodtraceability.dto.FileParseResultDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 电子凭证文件解析服务接口
 * 
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * 
 * 支持解析格式：
 * 1. XML/XBRL - 财政部标准电子凭证格式
 * 2. PDF - 含内嵌XML的电子发票PDF
 * 3. OFD - 电子文件格式（版式文档）
 */
public interface FileParseService {
    
    /**
     * 解析电子凭证文件
     * 
     * @param file 上传的文件
     * @return 解析结果，包含凭证类型、XML内容、JSON数据等
     */
    FileParseResultDTO parseFile(MultipartFile file);
    
    /**
     * 解析PDF格式电子发票
     * 
     * PDF电子发票通常包含：
     * 1. 可视化发票版面
     * 2. 内嵌的XML元数据（符合XBRL标准）
     * 3. 数字签名（XML-DSig格式）
     * 
     * @param fileContent PDF文件内容
     * @return 解析结果
     */
    FileParseResultDTO parsePdf(byte[] fileContent);
    
    /**
     * 解析OFD格式电子凭证
     * 
     * OFD（Open Fixed-layout Document）是中国版式文档国家标准
     * OFD电子发票包含：
     * 1. 版式文档内容
     * 2. 内嵌的XML元数据
     * 3. 电子签名/签章
     * 
     * @param fileContent OFD文件内容
     * @return 解析结果
     */
    FileParseResultDTO parseOfd(byte[] fileContent);
    
    /**
     * 解析XML/XBRL格式电子凭证
     * 
     * XBRL是财政部电子凭证标准格式
     * 
     * @param fileContent XML文件内容
     * @return 解析结果
     */
    FileParseResultDTO parseXml(byte[] fileContent);
    
    /**
     * 从PDF中提取内嵌XML
     * 
     * 电子发票PDF通常在特定位置嵌入XML数据
     * 
     * @param pdfContent PDF文件内容
     * @return 提取的XML内容，如果没有则返回null
     */
    String extractXmlFromPdf(byte[] pdfContent);
    
    /**
     * 从OFD中提取内嵌XML
     * 
     * OFD文件本质是ZIP压缩包，包含多个文件
     * XML元数据通常位于特定路径
     * 
     * @param ofdContent OFD文件内容
     * @return 提取的XML内容，如果没有则返回null
     */
    String extractXmlFromOfd(byte[] ofdContent);
    
    /**
     * 检测文件类型
     * 
     * @param fileContent 文件内容
     * @param fileName 文件名
     * @return 文件类型：pdf, ofd, xml, unknown
     */
    String detectFileType(byte[] fileContent, String fileName);
}
