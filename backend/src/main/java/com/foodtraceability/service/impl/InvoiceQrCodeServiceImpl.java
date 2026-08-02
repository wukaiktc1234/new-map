package com.foodtraceability.service.impl;

import com.foodtraceability.dto.InvoiceQrInfoDTO;
import com.foodtraceability.service.InvoiceQrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 发票二维码解析服务实现
 * <p>
 * 基于 ZXing 和 PDFBox 实现发票二维码的提取与解析。
 * 支持从 PDF 发票和图片中提取二维码，并按国家税务总局标准格式解析。
 * </p>
 * <p>
 * 二维码内容格式（国家税务总局标准）：
 * 01,发票代码,发票号码,金额,开票日期,校验码后6位
 * 例如：01,032002000411,09135145,84.80,20160420,123456
 * </p>
 */
@Service
public class InvoiceQrCodeServiceImpl implements InvoiceQrCodeService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceQrCodeServiceImpl.class);

    /**
     * 发票二维码内容正则校验：01,发票代码,发票号码,金额,开票日期,校验码后6位
     * 其中金额为整数或小数，开票日期为8位数字（YYYYMMDD），校验码后6位为6位数字
     */
    private static final Pattern INVOICE_QR_PATTERN =
            Pattern.compile("^01,\\d+,\\d+,\\d+(\\.\\d+)?,\\d{8},\\d{6}$");

    /** PDF 渲染 DPI，影响二维码识别精度 */
    private static final float PDF_RENDER_DPI = 300F;

    /**
     * 从PDF文件中提取二维码并解析发票信息
     * <p>
     * 将PDF每页渲染为图片，再用ZXing解码二维码。能同时处理嵌入图片
     * 和矢量绘制的二维码。
     * </p>
     * @param pdfContent PDF文件字节内容
     * @return 发票二维码信息，如果提取失败返回失败结果对象
     */
    @Override
    public InvoiceQrInfoDTO extractQrCodeFromPdf(byte[] pdfContent) {
        if (pdfContent == null || pdfContent.length == 0) {
            return InvoiceQrInfoDTO.failed("PDF内容为空");
        }
        try (PDDocument document = PDDocument.load(pdfContent)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(i, PDF_RENDER_DPI, ImageType.RGB);
                InvoiceQrInfoDTO dto = extractQrCodeFromImage(pageImage);
                if (dto != null && dto.isSuccess()) {
                    log.info("从PDF第{}页成功提取发票二维码", i + 1);
                    return dto;
                }
            }
            log.warn("PDF中未识别到发票二维码，页数: {}", pageCount);
            return InvoiceQrInfoDTO.failed("PDF中未识别到发票二维码");
        } catch (Exception e) {
            log.error("从PDF提取发票二维码失败: {}", e.getMessage(), e);
            return InvoiceQrInfoDTO.failed("从PDF提取发票二维码失败: " + e.getMessage());
        }
    }

    /**
     * 从图片中提取二维码并解析发票信息
     * <p>
     * 使用ZXing的MultiFormatReader解码图片中的二维码，
     * 解码成功后调用 parseInvoiceQrContent 解析为发票信息。
     * </p>
     * @param image 图片
     * @return 发票二维码信息，识别失败返回失败结果对象
     */
    @Override
    public InvoiceQrInfoDTO extractQrCodeFromImage(BufferedImage image) {
        if (image == null) {
            return InvoiceQrInfoDTO.failed("图片为空");
        }
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            Result result = new MultiFormatReader().decode(bitmap, hints);
            String qrContent = result.getText();
            log.info("成功从图片识别二维码内容: {}", qrContent);
            return parseInvoiceQrContent(qrContent);
        } catch (Exception e) {
            log.warn("从图片识别发票二维码失败: {}", e.getMessage());
            return InvoiceQrInfoDTO.failed("从图片识别发票二维码失败: " + e.getMessage());
        }
    }

    /**
     * 解析二维码内容
     * <p>
     * 按国标格式解析：01,发票代码,发票号码,金额,开票日期,校验码后6位。
     * 二维码中的金额字段对应DTO的价税合计（totalAmount）。
     * </p>
     * @param qrCodeContent 二维码内容字符串
     * @return 发票二维码信息，解析失败返回失败结果对象
     */
    @Override
    public InvoiceQrInfoDTO parseInvoiceQrContent(String qrCodeContent) {
        if (qrCodeContent == null || qrCodeContent.isEmpty()) {
            return InvoiceQrInfoDTO.failed("二维码内容为空");
        }
        String trimmed = qrCodeContent.trim();
        if (!isValidInvoiceQrCode(trimmed)) {
            log.warn("二维码内容格式不符合发票标准: {}", trimmed);
            return InvoiceQrInfoDTO.failed("二维码内容格式不符合发票标准: " + trimmed);
        }
        // 格式：01,发票代码,发票号码,金额,开票日期,校验码后6位
        String[] parts = trimmed.split(",");
        if (parts.length < 6) {
            return InvoiceQrInfoDTO.failed("二维码内容字段数不足");
        }
        String invoiceCode = parts[1];
        String invoiceNo = parts[2];
        BigDecimal amount = new BigDecimal(parts[3]);
        String issueDate = parts[4];
        String checkCode = parts[5];
        return InvoiceQrInfoDTO.builder()
                .success(true)
                .rawContent(trimmed)
                .invoiceCode(invoiceCode)
                .invoiceNo(invoiceNo)
                .totalAmount(amount)
                .issueDate(issueDate)
                .checkCode(checkCode)
                .build();
    }

    /**
     * 验证二维码内容格式是否正确
     * <p>
     * 校验规则：以01开头，6个字段以逗号分隔，格式为
     * 01,数字,数字,数字(可选小数),8位日期,6位校验码
     * </p>
     * @param qrCodeContent 二维码内容
     * @return 是否为有效的发票二维码
     */
    @Override
    public boolean isValidInvoiceQrCode(String qrCodeContent) {
        if (qrCodeContent == null || qrCodeContent.isEmpty()) {
            return false;
        }
        return INVOICE_QR_PATTERN.matcher(qrCodeContent.trim()).matches();
    }
}
