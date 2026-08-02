package com.foodtraceability.service.impl;

import com.foodtraceability.dto.OCRResultDTO;
import com.foodtraceability.entity.MaterialTemplate;
import com.foodtraceability.mapper.MaterialTemplateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OCR服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class OCRServiceImplTest {
    
    @Mock
    private MaterialTemplateMapper materialTemplateMapper;
    
    @Mock
    private RestTemplate restTemplate;
    
    @InjectMocks
    private OCRServiceImpl ocrService;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() {
        // 设置配置参数
        ReflectionTestUtils.setField(ocrService, "ocrServiceUrl", "http://localhost:9000/ocr/predict");
        ReflectionTestUtils.setField(ocrService, "ocrEnabled", true);
        ReflectionTestUtils.setField(ocrService, "zhipuApiKey", "");
        ReflectionTestUtils.setField(ocrService, "zhipuModel", "glm-4v-flash");
    }
    
    @Test
    @DisplayName("测试空图片数据")
    void testRecognizeImage_EmptyImage() {
        // Given: 空图片数据
        String imageBase64 = "";
        
        // When: 执行识别
        OCRResultDTO result = ocrService.recognizeImage(imageBase64);
        
        // Then: 验证错误处理
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMessage());
        assertEquals("图片数据不能为空", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("测试null图片数据")
    void testRecognizeImage_NullImage() {
        // Given: null图片数据
        String imageBase64 = null;
        
        // When: 执行识别
        OCRResultDTO result = ocrService.recognizeImage(imageBase64);
        
        // Then: 验证错误处理
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMessage());
        assertEquals("图片数据不能为空", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("测试空白图片数据")
    void testRecognizeImage_BlankImage() {
        // Given: 空白图片数据
        String imageBase64 = "   ";
        
        // When: 执行识别
        OCRResultDTO result = ocrService.recognizeImage(imageBase64);
        
        // Then: 验证错误处理
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMessage());
        assertEquals("图片数据不能为空", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("测试无可用OCR服务")
    void testRecognizeImage_NoAvailableService() {
        // Given: RestTemplate为null，无智谱AI密钥
        ReflectionTestUtils.setField(ocrService, "restTemplate", null);
        ReflectionTestUtils.setField(ocrService, "zhipuApiKey", "");
        
        String imageBase64 = Base64.getEncoder().encodeToString("test_image_data".getBytes());
        
        // When: 执行识别
        OCRResultDTO result = ocrService.recognizeImage(imageBase64);
        
        // Then: 验证错误处理
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("OCR识别失败"));
    }
    
    @Test
    @DisplayName("测试匹配已有商品档案 - 条码匹配")
    void testMatchExistingTemplate_ByBarcode() {
        // Given: 准备测试数据
        String productName = "测试商品";
        String barcode = "6901234567890";
        
        MaterialTemplate template = new MaterialTemplate();
        template.setId(1L);
        template.setMaterialName("测试商品");
        template.setBarcode(barcode);
        template.setStatus("active");
        
        when(materialTemplateMapper.selectOne(any())).thenReturn(template);
        
        // When: 执行匹配
        Long matchedId = ocrService.matchExistingTemplate(productName, barcode);
        
        // Then: 验证结果
        assertNotNull(matchedId);
        assertEquals(1L, matchedId);
        
        // 验证调用了条码查询
        verify(materialTemplateMapper, times(1)).selectOne(any());
    }
    
    @Test
    @DisplayName("测试匹配已有商品档案 - 名称匹配")
    void testMatchExistingTemplate_ByName() {
        // Given: 准备测试数据
        String productName = "测试商品";
        String barcode = null;
        
        MaterialTemplate template = new MaterialTemplate();
        template.setId(1L);
        template.setMaterialName("测试商品");
        template.setStatus("active");
        
        when(materialTemplateMapper.selectList(any())).thenReturn(Collections.singletonList(template));
        
        // When: 执行匹配
        Long matchedId = ocrService.matchExistingTemplate(productName, barcode);
        
        // Then: 验证结果
        assertNotNull(matchedId);
        assertEquals(1L, matchedId);
        
        // 验证调用了名称查询
        verify(materialTemplateMapper, times(1)).selectList(any());
    }
    
    @Test
    @DisplayName("测试匹配已有商品档案 - 未匹配")
    void testMatchExistingTemplate_NotFound() {
        // Given: 准备测试数据
        String productName = "不存在的商品";
        String barcode = "9999999999999";
        
        when(materialTemplateMapper.selectOne(any())).thenReturn(null);
        when(materialTemplateMapper.selectList(any())).thenReturn(Collections.emptyList());
        
        // When: 执行匹配
        Long matchedId = ocrService.matchExistingTemplate(productName, barcode);
        
        // Then: 验证结果
        assertNull(matchedId);
    }
    
    @Test
    @DisplayName("测试对比OCR结果与档案差异")
    void testCompareWithTemplate() {
        // Given: 准备测试数据
        Long templateId = 1L;
        
        MaterialTemplate template = new MaterialTemplate();
        template.setId(templateId);
        template.setMaterialName("测试商品");
        template.setDefaultShelfLife(365);
        template.setStorageCondition("常温");
        
        OCRResultDTO ocrResult = new OCRResultDTO();
        ocrResult.setShelfLifeDays(180);  // 不同的保质期
        ocrResult.setStorageCondition("冷藏");  // 不同的存储条件
        
        when(materialTemplateMapper.selectById(templateId)).thenReturn(template);
        
        // When: 执行对比
        var differences = ocrService.compareWithTemplate(templateId, ocrResult);
        
        // Then: 验证结果
        assertNotNull(differences);
        assertEquals(2, differences.size());
        assertTrue(differences.get(0).contains("保质期"));
        assertTrue(differences.get(1).contains("存储条件"));
    }
    
    @Test
    @DisplayName("测试对比OCR结果与档案 - 无差异")
    void testCompareWithTemplate_NoDifference() {
        // Given: 准备测试数据
        Long templateId = 1L;
        
        MaterialTemplate template = new MaterialTemplate();
        template.setId(templateId);
        template.setMaterialName("测试商品");
        template.setDefaultShelfLife(365);
        template.setStorageCondition("常温");
        
        OCRResultDTO ocrResult = new OCRResultDTO();
        ocrResult.setShelfLifeDays(365);  // 相同的保质期
        ocrResult.setStorageCondition("常温");  // 相同的存储条件
        
        when(materialTemplateMapper.selectById(templateId)).thenReturn(template);
        
        // When: 执行对比
        var differences = ocrService.compareWithTemplate(templateId, ocrResult);
        
        // Then: 验证结果
        assertNotNull(differences);
        assertTrue(differences.isEmpty());
    }
}
