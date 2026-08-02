package com.foodtraceability;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class TestQRCode {
    public static void main(String[] args) throws Exception {
        String pdfPath = "P:\\my-new-project\\032002000411_09135145_苏宁易购官方旗舰店.pdf";
        System.out.println("=== 测试PDF二维码提取 ===");
        System.out.println("文件: " + pdfPath);
        
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            System.out.println("页数: " + doc.getNumberOfPages());
            
            PDFRenderer renderer = new PDFRenderer(doc);
            
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                System.out.println("\n--- 第 " + (i+1) + " 页 ---");
                
                BufferedImage img = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
                System.out.println("图像尺寸: " + img.getWidth() + "x" + img.getHeight());
                
                LuminanceSource source = new BufferedImageLuminanceSource(img);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                
                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
                
                try {
                    GenericMultipleBarcodeReader multiReader = 
                            new GenericMultipleBarcodeReader(new MultiFormatReader());
                    Result[] results = multiReader.decodeMultiple(bitmap, hints);
                    
                    System.out.println("找到 " + results.length + " 个二维码:");
                    for (int j = 0; j < results.length; j++) {
                        System.out.println("  二维码 " + (j+1) + ": " + results[j].getText());
                    }
                } catch (NotFoundException e) {
                    System.out.println("未找到二维码，尝试分区域检测...");
                    
                    int minSize = 100;
                    int cols = (int) Math.ceil((double) img.getWidth() / minSize);
                    int rows = (int) Math.ceil((double) img.getHeight() / minSize);
                    System.out.println("分区域检测: " + cols + "x" + rows + " 区域");
                    
                    boolean found = false;
                    for (int r = 0; r < rows && !found; r++) {
                        for (int c = 0; c < cols && !found; c++) {
                            int x = c * minSize;
                            int y = r * minSize;
                            int w = Math.min(minSize, img.getWidth() - x);
                            int h = Math.min(minSize, img.getHeight() - y);
                            
                            BufferedImage subImg = img.getSubimage(x, y, w, h);
                            LuminanceSource subSource = new BufferedImageLuminanceSource(subImg);
                            BinaryBitmap subBitmap = new BinaryBitmap(new HybridBinarizer(subSource));
                            
                            try {
                                Result result = new MultiFormatReader().decode(subBitmap, hints);
                                System.out.println("  在区域(" + r + "," + c + ")找到二维码: " + result.getText());
                                found = true;
                            } catch (NotFoundException ignored) {
                            }
                        }
                    }
                    
                    if (!found) {
                        System.out.println("分区域检测也未找到二维码");
                    }
                }
            }
        }
    }
}
