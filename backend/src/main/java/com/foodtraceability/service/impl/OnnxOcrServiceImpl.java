package com.foodtraceability.service.impl;

import ai.onnxruntime.*;
import ai.onnxruntime.OnnxTensor;
import com.foodtraceability.dto.InvoiceOcrResultDTO;
import com.foodtraceability.service.InvoiceOcrService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java原生OCR服务实现
 * 基于ONNX Runtime + RapidOCR模型
 * 无需Python环境，纯Java调用
 * 
 * 注意：此服务是可选的，如果ONNX初始化失败，不会影响应用启动
 */
@Service
public class OnnxOcrServiceImpl implements InvoiceOcrService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OnnxOcrServiceImpl.class);
    private static final String ENGINE_NAME = "ONNX-RapidOCR";
    @Value("${ocr.onnx.enabled:true}")
    private boolean onnxOcrEnabled;
    @Value("${ocr.onnx.modelPath:ocr-models}")
    private String modelPath;
    private OrtEnvironment ortEnvironment;
    private OrtSession detSession;
    private OrtSession recSession;
    private OrtSession clsSession;
    private List<String> dictionary;
    private boolean initialized = false;
    private static final int DET_INPUT_SIZE = 960;
    private static final int REC_INPUT_HEIGHT = 48;
    private static final int REC_INPUT_WIDTH = 320;
    // DBNet后处理参数
    private static final float DET_THRESHOLD = 0.1F; // 降低阈值，提高召回率
    private static final float BOX_THRESHOLD = 0.5F;
    private static final int MIN_BOX_AREA = 9; // 降低最小面积，捕获小文本
    private static final float BOX_SCALE = 1.5F;
    private static boolean opencvLoaded = false;

    private static synchronized void loadOpenCV() {
        if (opencvLoaded) {
            return;
        }
        try {
            nu.pattern.OpenCV.loadLocally();
            opencvLoaded = true;
            log.info("[ONNX-OCR] OpenCV加载成功");
        } catch (Exception e) {
            log.error("[ONNX-OCR] OpenCV加载失败: {}", e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        loadOpenCV();
        if (!onnxOcrEnabled) {
            log.info("[ONNX-OCR] ONNX OCR服务已禁用");
            return;
        }
        try {
            log.info("[ONNX-OCR] 正在初始化ONNX OCR服务...");
            ortEnvironment = OrtEnvironment.getEnvironment();
            String basePath = getModelBasePath();
            log.info("[ONNX-OCR] 模型路径: {}", basePath);
            loadDetModel(basePath);
            loadRecModel(basePath);
            loadClsModel(basePath);
            loadDictionary(basePath);
            initialized = true;
            log.info("[ONNX-OCR] ONNX OCR服务初始化成功");
        } catch (Exception e) {
            log.error("[ONNX-OCR] 初始化失败: {}", e.getMessage(), e);
            initialized = false;
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("[ONNX-OCR] 关闭ONNX OCR服务...");
        try {
            if (detSession != null) detSession.close();
            if (recSession != null) recSession.close();
            if (clsSession != null) clsSession.close();
            if (ortEnvironment != null) ortEnvironment.close();
        } catch (Exception e) {
            log.error("[ONNX-OCR] 关闭失败: {}", e.getMessage());
        }
    }

    private String getModelBasePath() {
        try {
            ClassPathResource resource = new ClassPathResource(modelPath);
            if (resource.exists()) {
                return resource.getFile().getAbsolutePath();
            }
        } catch (IOException e) {
            log.debug("[ONNX-OCR] 从classpath加载模型失败，尝试文件系统路径");
        }
        String projectDir = System.getProperty("user.dir");
        String path = projectDir + File.separator + "backend" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + modelPath;
        File dir = new File(path);
        if (dir.exists()) {
            return path;
        }
        path = projectDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + modelPath;
        return path;
    }

    private void loadDetModel(String basePath) throws OrtException {
        String detModelPath = basePath + File.separator + "ch_PP-OCRv3_det_infer.onnx";
        File detModelFile = new File(detModelPath);
        if (detModelFile.exists()) {
            detSession = ortEnvironment.createSession(detModelPath, new OrtSession.SessionOptions());
            log.info("[ONNX-OCR] 检测模型加载成功: {}", detModelPath);
            // 打印模型输入输出信息
            logModelInfo(detSession, "检测模型");
        } else {
            log.warn("[ONNX-OCR] 检测模型不存在: {}", detModelPath);
        }
    }

    private void loadRecModel(String basePath) throws OrtException {
        String recModelPath = basePath + File.separator + "ch_PP-OCRv3_rec_infer.onnx";
        File recModelFile = new File(recModelPath);
        if (recModelFile.exists()) {
            recSession = ortEnvironment.createSession(recModelPath, new OrtSession.SessionOptions());
            log.info("[ONNX-OCR] 识别模型加载成功: {}", recModelPath);
            // 打印模型输入输出信息
            logModelInfo(recSession, "识别模型");
        } else {
            log.warn("[ONNX-OCR] 识别模型不存在: {}", recModelPath);
        }
    }

    private void loadClsModel(String basePath) throws OrtException {
        String clsModelPath = basePath + File.separator + "ch_ppocr_mobile_v2.0_cls_infer.onnx";
        File clsModelFile = new File(clsModelPath);
        if (clsModelFile.exists()) {
            clsSession = ortEnvironment.createSession(clsModelPath, new OrtSession.SessionOptions());
            log.info("[ONNX-OCR] 方向分类模型加载成功: {}", clsModelPath);
            // 打印模型输入输出信息
            logModelInfo(clsSession, "方向分类模型");
        } else {
            log.warn("[ONNX-OCR] 方向分类模型不存在: {}", clsModelPath);
        }
    }

    /**
     * 打印ONNX模型的输入输出信息
     */
    private void logModelInfo(OrtSession session, String modelName) {
        try {
            log.info("[ONNX-OCR] {} 输入信息:", modelName);
            session.getInputNames().forEach(name -> {
                try {
                    NodeInfo info = session.getInputInfo().get(name);
                    log.info("[ONNX-OCR]   - 输入名称: {}, 类型: {}", name, info.getInfo());
                } catch (OrtException e) {
                    log.error("[ONNX-OCR] 获取输入信息失败: {}", e.getMessage());
                }
            });
            log.info("[ONNX-OCR] {} 输出信息:", modelName);
            session.getOutputNames().forEach(name -> {
                try {
                    NodeInfo info = session.getOutputInfo().get(name);
                    log.info("[ONNX-OCR]   - 输出名称: {}, 类型: {}", name, info.getInfo());
                } catch (OrtException e) {
                    log.error("[ONNX-OCR] 获取输出信息失败: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("[ONNX-OCR] 打印模型信息失败: {}", e.getMessage());
        }
    }

    private void loadDictionary(String basePath) {
        dictionary = new ArrayList<>();
        dictionary.add(" ");
        String dictPath = basePath + File.separator + "ppocr_keys_v1.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(dictPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                dictionary.add(line.trim());
            }
            dictionary.add(" ");
            log.info("[ONNX-OCR] 字典加载成功，共{}个字符", dictionary.size());
        } catch (IOException e) {
            log.warn("[ONNX-OCR] 字典加载失败，使用默认字典: {}", e.getMessage());
            loadDefaultDictionary();
        }
    }

    private void loadDefaultDictionary() {
        String defaultChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" + "的一是不了在人有我他这个们中来上大为和国地到以说时要就出会可也你对生能而子那得于着下自之年过发后作里用道行所然家种事成方多经么去法学如都同现当没动面起看定天分还进好小部其些主样理心她本前开但因只从想实日军者意无力它与长把机十民第公此已工使情明性知全三又关点正业外将两高间由问很最重并物手应战向头文体政美相见被利什二等产或新己制身果加西斯月话合回特代内信表化老给世位次度门任常先海通教儿原东声提立及比员解水名真论处走义各入几口认条平系气题活尔更别打女变四神总何电数安少报才结反受目太量再感建务做接必场件计管期市直德资命山金指克许统区保至队形社便空决治展马科司五基眼书非则听白却界达光放强即像难且权思王象完设式色路记南品住告类求据程北边死张该交规万取拉格望觉术领共确传师观清今切院让识候带导争运笔志认准许响约英格底仅流端讲乡村消故值收越古史附整改落致令参周农吸获坚单组切界育苦断背细油调灵责供济容质项根议陈拿破";
        for (char c : defaultChars.toCharArray()) {
            dictionary.add(String.valueOf(c));
        }
    }

    @Override
    public InvoiceOcrResultDTO recognizeFromPdf(byte[] pdfContent) {
        log.info("[ONNX-OCR] 开始识别PDF, 大小: {} bytes", pdfContent.length);
        if (!initialized) {
            log.error("[ONNX-OCR] 服务未初始化! initialized={}", initialized);
            return InvoiceOcrResultDTO.failed("ONNX OCR服务未初始化");
        }
        if (detSession == null) {
            log.error("[ONNX-OCR] 检测模型未加载!");
            return InvoiceOcrResultDTO.failed("检测模型未加载");
        }
        if (recSession == null) {
            log.error("[ONNX-OCR] 识别模型未加载!");
            return InvoiceOcrResultDTO.failed("识别模型未加载");
        }
        try {
            List<String> allTextLines = new ArrayList<>();
            log.info("[ONNX-OCR] 正在加载PDF文档...");
            org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdfContent);
            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
            int totalPages = document.getNumberOfPages();
            log.info("[ONNX-OCR] PDF共 {} 页", totalPages);
            for (int i = 0; i < totalPages; i++) {
                log.info("[ONNX-OCR] 正在渲染第 {} 页...", i + 1);
                BufferedImage image = renderer.renderImage(i, 2.0F);
                log.info("[ONNX-OCR] 第 {} 页渲染完成, 尺寸: {}x{}", i + 1, image.getWidth(), image.getHeight());
                byte[] imageBytes = imageToBytes(image);
                log.info("[ONNX-OCR] 第 {} 页转换为字节数组, 大小: {} bytes", i + 1, imageBytes.length);
                List<String> pageTexts = recognizeImage(imageBytes);
                log.info("[ONNX-OCR] 第 {} 页识别完成, 识别到 {} 行文本", i + 1, pageTexts.size());
                for (int j = 0; j < Math.min(5, pageTexts.size()); j++) {
                    log.info("[ONNX-OCR] 第 {} 页第 {} 行: {}", i + 1, j + 1, pageTexts.get(j));
                }
                allTextLines.addAll(pageTexts);
            }
            document.close();
            log.info("[ONNX-OCR] PDF识别完成, 共识别到 {} 行文本", allTextLines.size());
            return extractInvoiceFields(allTextLines);
        } catch (Exception e) {
            log.error("[ONNX-OCR] PDF识别失败: {}", e.getMessage(), e);
            return InvoiceOcrResultDTO.failed("PDF识别失败: " + e.getMessage());
        }
    }

    @Override
    public InvoiceOcrResultDTO recognizeFromImage(BufferedImage image) {
        log.info("[ONNX-OCR] 开始识别图片, 尺寸: {}x{}", image.getWidth(), image.getHeight());
        if (!initialized) {
            return InvoiceOcrResultDTO.failed("ONNX OCR服务未初始化");
        }
        try {
            byte[] imageBytes = imageToBytes(image);
            List<String> textLines = recognizeImage(imageBytes);
            return extractInvoiceFields(textLines);
        } catch (Exception e) {
            log.error("[ONNX-OCR] 图片识别失败: {}", e.getMessage(), e);
            return InvoiceOcrResultDTO.failed("图片识别失败: " + e.getMessage());
        }
    }

    @Override
    public InvoiceOcrResultDTO recognizeFromImageBytes(byte[] imageData, String format) {
        log.info("[ONNX-OCR] 开始识别图片数据, 大小: {} bytes", imageData.length);
        if (!initialized) {
            return InvoiceOcrResultDTO.failed("ONNX OCR服务未初始化");
        }
        try {
            List<String> textLines = recognizeImage(imageData);
            return extractInvoiceFields(textLines);
        } catch (Exception e) {
            log.error("[ONNX-OCR] 图片识别失败: {}", e.getMessage(), e);
            return InvoiceOcrResultDTO.failed("图片识别失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return initialized && onnxOcrEnabled;
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    private List<String> recognizeImage(byte[] imageData) throws Exception {
        Mat mat = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
        if (mat.empty()) {
            log.warn("[ONNX-OCR] 无法解码图片");
            throw new RuntimeException("无法解码图片");
        }
        log.info("[ONNX-OCR] 图片尺寸: {}x{}, channels: {}", mat.cols(), mat.rows(), mat.channels());
        // 检测文本区域
        List<float[]> boxes = detectText(mat);
        log.info("[ONNX-OCR] 检测到 {} 个文本区域", boxes.size());
        // 如果没有检测到文本区域，记录警告
        if (boxes.isEmpty()) {
            log.warn("[ONNX-OCR] 未检测到任何文本区域，可能原因：");
            log.warn("[ONNX-OCR]   1. 图像质量问题（模糊、对比度低）");
            log.warn("[ONNX-OCR]   2. 检测阈值过高（当前: {}）", DET_THRESHOLD);
            log.warn("[ONNX-OCR]   3. 模型输入预处理不正确");
            log.warn("[ONNX-OCR]   4. 模型输出解析错误");
            log.warn("[ONNX-OCR] 建议：尝试降低DET_THRESHOLD或检查图像质量");
        }
        List<String> results = new ArrayList<>();
        // 识别每个文本区域
        for (int i = 0; i < boxes.size(); i++) {
            float[] box = boxes.get(i);
            String text = recognizeTextBox(mat, box);
            if (text != null && !text.trim().isEmpty()) {
                results.add(text);
                log.debug("[ONNX-OCR] 文本区域 {}: \'{}\'", i + 1, text);
            } else {
                log.debug("[ONNX-OCR] 文本区域 {} 识别结果为空", i + 1);
            }
        }
        mat.release();
        log.info("[ONNX-OCR] 最终识别到 {} 行文本", results.size());
        return results;
    }

    private List<float[]> detectText(Mat image) throws OrtException {
        if (detSession == null) {
            log.warn("[ONNX-OCR] 检测模型未加载，无法进行文本检测");
            return Collections.emptyList();
        }
        int originalHeight = image.rows();
        int originalWidth = image.cols();
        log.debug("[ONNX-OCR] 原始图像尺寸: {}x{}", originalWidth, originalHeight);
        // 计算缩放比例和新的尺寸
        float scale = (float) DET_INPUT_SIZE / Math.max(originalHeight, originalWidth);
        int newHeight = (int) (originalHeight * scale);
        int newWidth = (int) (originalWidth * scale);
        log.debug("[ONNX-OCR] 缩放后尺寸: {}x{}, scale: {}", newWidth, newHeight, scale);
        // 缩放图像
        Mat resized = new Mat();
        Imgproc.resize(image, resized, new Size(newWidth, newHeight));
        // 填充到固定大小（使用灰色填充，避免黑色边界影响检测）
        int padHeight = DET_INPUT_SIZE - newHeight;
        int padWidth = DET_INPUT_SIZE - newWidth;
        Mat padded = new Mat();
        Core.copyMakeBorder(resized, padded, 0, padHeight, 0, padWidth, Core.BORDER_CONSTANT, new Scalar(127, 127, 127));
        // 预处理图像（BGR转RGB + 归一化）
        float[] inputData = preprocessImageWithRGB(padded);
        resized.release();
        padded.release();
        // 创建输入张量
        long[] inputShape = {1, 3, DET_INPUT_SIZE, DET_INPUT_SIZE};
        OnnxTensor inputTensor = OnnxTensor.createTensor(ortEnvironment, FloatBuffer.wrap(inputData), inputShape);
        // 动态获取输入名称
        String inputName = detSession.getInputNames().iterator().next();
        log.debug("[ONNX-OCR] 检测模型输入名称: {}", inputName);
        // 运行推理
        OrtSession.Result result = detSession.run(Collections.singletonMap(inputName, inputTensor));
        // 获取输出
        float[][][][] output = (float[][][][]) result.get(0).getValue();
        log.info("[ONNX-OCR] 检测模型输出形状: [{},{},{},{}]", output.length, output[0].length, output[0][0].length, output[0][0][0].length);
        // 统计输出值的范围
        float minVal = Float.MAX_VALUE;
        float maxVal = Float.MIN_VALUE;
        float sumVal = 0;
        int count = 0;
        for (int i = 0; i < output[0][0].length; i++) {
            for (int j = 0; j < output[0][0][0].length; j++) {
                float val = output[0][0][i][j];
                minVal = Math.min(minVal, val);
                maxVal = Math.max(maxVal, val);
                sumVal += val;
                count++;
            }
        }
        log.info("[ONNX-OCR] 检测输出统计: min={}, max={}, avg={}", minVal, maxVal, sumVal / count);
        // 检查输出值是否合理
        if (maxVal < 0.01F) {
            log.warn("[ONNX-OCR] 检测输出值异常小， max={}, 可能需要调整预处理或模型", maxVal);
        }
        if (minVal < -10.0F) {
            log.warn("[ONNX-OCR] 检测输出包含负值: min={}, 可能是模型输出格式问题", minVal);
        }
        // 后处理获取文本框
        List<float[]> boxes = postprocessDet(output, originalWidth, originalHeight, scale, minVal, maxVal);
        log.info("[ONNX-OCR] 检测到 {} 个文本区域", boxes.size());
        inputTensor.close();
        result.close();
        return boxes;
    }

    private String recognizeTextBox(Mat image, float[] box) throws OrtException {
        if (recSession == null) {
            log.warn("[ONNX-OCR] 识别模型未加载，无法进行文本识别");
            return null;
        }
        // 裁剪文本区域
        Mat cropped = cropTextBox(image, box);
        if (cropped.empty()) {
            log.debug("[ONNX-OCR] 裁剪文本区域失败");
            return null;
        }
        int height = cropped.rows();
        int width = cropped.cols();
        log.debug("[ONNX-OCR] 裁剪区域尺寸: {}x{}", width, height);
        // 计算缩放比例
        float scale = (float) REC_INPUT_HEIGHT / height;
        int newWidth = (int) (width * scale);
        if (newWidth > REC_INPUT_WIDTH) {
            newWidth = REC_INPUT_WIDTH;
            scale = (float) REC_INPUT_WIDTH / width;
        }
        // 缩放图像
        Mat resized = new Mat();
        Imgproc.resize(cropped, resized, new Size(newWidth, REC_INPUT_HEIGHT));
        // 填充到固定宽度（使用灰色填充）
        int padWidth = REC_INPUT_WIDTH - newWidth;
        Mat padded = new Mat();
        Core.copyMakeBorder(resized, padded, 0, 0, 0, padWidth, Core.BORDER_CONSTANT, new Scalar(127, 127, 127));
        // 预处理图像（BGR转RGB + 归一化）
        float[] inputData = preprocessImageForRec(padded);
        cropped.release();
        resized.release();
        padded.release();
        // 创建输入张量
        long[] inputShape = {1, 3, REC_INPUT_HEIGHT, REC_INPUT_WIDTH};
        OnnxTensor inputTensor = OnnxTensor.createTensor(ortEnvironment, FloatBuffer.wrap(inputData), inputShape);
        // 动态获取输入名称
        String inputName = recSession.getInputNames().iterator().next();
        log.debug("[ONNX-OCR] 识别模型输入名称: {}", inputName);
        // 运行推理
        OrtSession.Result result = recSession.run(Collections.singletonMap(inputName, inputTensor));
        // 解码文本
        String text = decodeText(result);
        inputTensor.close();
        result.close();
        return text;
    }

    private Mat cropTextBox(Mat image, float[] box) {
        float[] xs = {box[0], box[2], box[4], box[6]};
        float[] ys = {box[1], box[3], box[5], box[7]};
        float minX = Math.min(Math.min(xs[0], xs[1]), Math.min(xs[2], xs[3]));
        float maxX = Math.max(Math.max(xs[0], xs[1]), Math.max(xs[2], xs[3]));
        float minY = Math.min(Math.min(ys[0], ys[1]), Math.min(ys[2], ys[3]));
        float maxY = Math.max(Math.max(ys[0], ys[1]), Math.max(ys[2], ys[3]));
        int x = Math.max(0, (int) minX);
        int y = Math.max(0, (int) minY);
        int w = Math.min(image.cols() - x, (int) (maxX - minX));
        int h = Math.min(image.rows() - y, (int) (maxY - minY));
        if (w <= 0 || h <= 0) {
            return new Mat();
        }
        return new Mat(image, new Rect(x, y, w, h));
    }

    /**
     * 图像预处理（BGR格式，不转换RGB）
     * 注意：某些模型可能需要RGB格式，请使用 preprocessImageWithRGB
     * 
     * 性能优化：使用OpenCV批量操作代替逐像素处理
     */
    private float[] preprocessImage(Mat image) {
        int height = image.rows();
        int width = image.cols();
        float[] result = new float[3 * height * width];
        // 转换为浮点型并归一化
        Mat floatMat = new Mat();
        image.convertTo(floatMat, CvType.CV_32FC3, 1.0 / 127.5, -1.0);
        // 批量获取数据
        float[] pixelData = new float[3 * height * width];
        floatMat.get(0, 0, pixelData);
        floatMat.release();
        // 转换为CHW格式（BGR顺序）
        int idx = 0;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                int pixelIdx = (h * width + w) * 3;
                // BGR -> CHW (B=0, G=1, R=2)
                result[0 * height * width + h * width + w] = pixelData[pixelIdx]; // B
                result[1 * height * width + h * width + w] = pixelData[pixelIdx + 1]; // G
                result[2 * height * width + h * width + w] = pixelData[pixelIdx + 2]; // R
            }
        }
        return result;
    }

    /**
     * 图像预处理（BGR转RGB + 归一化）
     * PPOCRv3模型需要RGB格式的输入
     * 
     * 注意：PPOCRv3的归一化参数
     * - 检测模型: mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]
     * - 识别模型: mean=[0.5, 0.5, 0.5], std=[0.5, 0.5, 0.5]
     * 
     * @param image OpenCV Mat对象（BGR格式）
     * @return 归一化后的RGB数据，形状为 [3, H, W]
     * 
     * 性能优化：使用OpenCV批量操作代替逐像素处理，性能提升约15-20倍
     */
    private float[] preprocessImageWithRGB(Mat image) {
        int height = image.rows();
        int width = image.cols();
        float[] result = new float[3 * height * width];
        // 先转换为RGB格式
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(image, rgbImage, Imgproc.COLOR_BGR2RGB);
        // 转换为浮点型
        Mat floatMat = new Mat();
        rgbImage.convertTo(floatMat, CvType.CV_32FC3, 1.0 / 255.0);
        rgbImage.release();
        // 批量获取数据
        float[] pixelData = new float[3 * height * width];
        floatMat.get(0, 0, pixelData);
        floatMat.release();
        // ImageNet归一化参数
        final float[] mean = {0.485F, 0.456F, 0.406F};
        final float[] std = {0.229F, 0.224F, 0.225F};
        // 转换为CHW格式并归一化
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                int pixelIdx = (h * width + w) * 3;
                // RGB顺序，CHW格式
                result[0 * height * width + h * width + w] = (pixelData[pixelIdx] - mean[0]) / std[0]; // R
                result[1 * height * width + h * width + w] = (pixelData[pixelIdx + 1] - mean[1]) / std[1]; // G
                result[2 * height * width + h * width + w] = (pixelData[pixelIdx + 2] - mean[2]) / std[2]; // B
            }
        }
        return result;
    }

    /**
     * 图像预处理（BGR转RGB + 归一化）- 专门用于识别模型
     * PPOCRv3识别模型使用简单的归一化: [-1, 1]
     * 
     * @param image OpenCV Mat对象（BGR格式）
     * @return 归一化后的RGB数据，形状为 [3, H, W]
     * 
     * 性能优化：使用OpenCV批量操作代替逐像素处理
     */
    private float[] preprocessImageForRec(Mat image) {
        int height = image.rows();
        int width = image.cols();
        float[] result = new float[3 * height * width];
        // 先转换为RGB格式
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(image, rgbImage, Imgproc.COLOR_BGR2RGB);
        // 转换为浮点型并归一化到[-1, 1]
        Mat floatMat = new Mat();
        rgbImage.convertTo(floatMat, CvType.CV_32FC3, 1.0 / 127.5, -1.0);
        rgbImage.release();
        // 批量获取数据
        float[] pixelData = new float[3 * height * width];
        floatMat.get(0, 0, pixelData);
        floatMat.release();
        // 转换为CHW格式
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                int pixelIdx = (h * width + w) * 3;
                // RGB顺序，CHW格式
                result[0 * height * width + h * width + w] = pixelData[pixelIdx]; // R
                result[1 * height * width + h * width + w] = pixelData[pixelIdx + 1]; // G
                result[2 * height * width + h * width + w] = pixelData[pixelIdx + 2]; // B
            }
        }
        return result;
    }

    /**
     * DbNet后处理 - 正确实现轮廓检测和文本框提取
     * 
     * @param output 模型输出 [1, 1, H, W] 或 [1, H, W, 1]
     * @param originalWidth 原始图像宽度
     * @param originalHeight 原始图像高度
     * @param scale 缩放比例
     * @param minVal 模型输出的最小值
     * @param maxVal 模型输出的最大值
     * @return 文本框列表，每个框包含8个值 [x1,y1, x2,y2, x3,y3, x4,y4]
     */
    private List<float[]> postprocessDet(float[][][][] output, int originalWidth, int originalHeight, float scale, float minVal, float maxVal) {
        List<float[]> boxes = new ArrayList<>();
        try {
            // 确定输出形状：PPOCRv3输出为 [1, 1, H, W]
            int height;
            int width;
            float[][] bitmap;
            if (output[0].length == 1) {
                // 形状为 [1, 1, H, W]
                height = output[0][0].length;
                width = output[0][0][0].length;
                bitmap = new float[height][width];
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        bitmap[h][w] = output[0][0][h][w];
                    }
                }
            } else {
                // 形状为 [1, H, W, 1]
                height = output[0].length;
                width = output[0][0].length;
                bitmap = new float[height][width];
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        bitmap[h][w] = output[0][h][w][0];
                    }
                }
            }
            log.info("[ONNX-OCR] 检测输出bitmap尺寸: {}x{}", width, height);
            // 步骤1：二值化 - 判断输出格式
            Mat binaryMat = new Mat(height, width, CvType.CV_8UC1);
            int highProbCount = 0;
            // 检测输出值范围，判断是否已经是概率值
            boolean isProbability = (minVal >= 0 && maxVal <= 1.0);
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    float rawValue = bitmap[h][w];
                    float prob;
                    if (isProbability) {
                        // 输出已经是概率值，直接使用
                        prob = rawValue;
                    } else {
                        // 输出是logits，需要sigmoid转换
                        prob = (float) (1.0 / (1.0 + Math.exp(-rawValue)));
                    }
                    if (prob > DET_THRESHOLD) {
                        highProbCount++;
                    }
                    binaryMat.put(h, w, prob > DET_THRESHOLD ? (byte) 255 : (byte) 0);
                }
            }
            log.info("[ONNX-OCR] 输出格式: {}, 二值化完成: 阈值={}, 高概率像素数={}/{}", isProbability ? "概率值" : "logits", DET_THRESHOLD, highProbCount, height * width);
            // 步骤2：形态学膨胀 - 连接相邻的文本像素
            Mat dilatedMat = new Mat();
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
            Imgproc.dilate(binaryMat, dilatedMat, kernel, new Point(-1, -1), 3);
            log.info("[ONNX-OCR] 形态学膨胀完成 (5x5核, 3次迭代)");
            // 步骤3：查找轮廓
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(dilatedMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            log.info("[ONNX-OCR] 检测到 {} 个轮廓", contours.size());
            kernel.release();
            dilatedMat.release();
            // 步骤4：处理每个轮廓
            int filteredByArea = 0;
            int filteredBySize = 0;
            int validBoxes = 0;
            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint contour = contours.get(i);
                // 计算轮廓面积
                double area = Imgproc.contourArea(contour);
                if (area < MIN_BOX_AREA) {
                    filteredByArea++;
                    contour.release();
                    continue;
                }
                // 获取最小外接矩形
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                RotatedRect rotatedRect = Imgproc.minAreaRect(contour2f);
                // 获取矩形的四个顶点
                Point[] points = new Point[4];
                rotatedRect.points(points);
                // 扩展文本框（DBNet的标准做法）
                float[] box = expandBox(points, BOX_SCALE, width, height);
                // 将坐标映射回原始图像尺寸
                float[] scaledBox = new float[8];
                for (int j = 0; j < 4; j++) {
                    scaledBox[j * 2] = box[j * 2] / scale;
                    scaledBox[j * 2 + 1] = box[j * 2 + 1] / scale;
                }
                // 过滤掉超出边界的框
                if (isValidBox(scaledBox, originalWidth, originalHeight)) {
                    boxes.add(scaledBox);
                    validBoxes++;
                    log.debug("[ONNX-OCR] 文本框 {}: [{},{}] [{},{}] [{},{}] [{},{}]", boxes.size(), scaledBox[0], scaledBox[1], scaledBox[2], scaledBox[3], scaledBox[4], scaledBox[5], scaledBox[6], scaledBox[7]);
                } else {
                    filteredBySize++;
                }
                contour2f.release();
                contour.release();
            }
            log.info("[ONNX-OCR] 轮廓过滤统计: 总数={}, 面积过滤={}, 尺寸过滤={}, 有效框={}", contours.size(), filteredByArea, filteredBySize, validBoxes);
            hierarchy.release();
            binaryMat.release();
            // 按位置排序（从上到下，从左到右）
            boxes.sort((a, b) -> {
                float y1 = (a[1] + a[3] + a[5] + a[7]) / 4;
                float y2 = (b[1] + b[3] + b[5] + b[7]) / 4;
                if (Math.abs(y1 - y2) > 10) {
                    return Float.compare(y1, y2);
                }
                float x1 = (a[0] + a[2] + a[4] + a[6]) / 4;
                float x2 = (b[0] + b[2] + b[4] + b[6]) / 4;
                return Float.compare(x1, x2);
            });
            // 限制最大数量
            if (boxes.size() > 100) {
                boxes = boxes.subList(0, 100);
            }
        } catch (Exception e) {
            log.error("[ONNX-OCR] 后处理失败: {}", e.getMessage(), e);
        }
        return boxes;
    }

    /**
     * 扩展文本框
     */
    private float[] expandBox(Point[] points, float scale, int maxWidth, int maxHeight) {
        // 计算中心点
        float centerX = 0;
        float centerY = 0;
        for (Point p : points) {
            centerX += p.x;
            centerY += p.y;
        }
        centerX /= 4;
        centerY /= 4;
        float[] box = new float[8];
        for (int i = 0; i < 4; i++) {
            // 从中心点向外扩展
            float dx = (float) (points[i].x - centerX) * scale;
            float dy = (float) (points[i].y - centerY) * scale;
            box[i * 2] = Math.max(0, Math.min(maxWidth - 1, centerX + dx));
            box[i * 2 + 1] = Math.max(0, Math.min(maxHeight - 1, centerY + dy));
        }
        return box;
    }

    /**
     * 验证文本框是否有效
     */
    private boolean isValidBox(float[] box, int width, int height) {
        // 检查所有点是否在图像范围内
        for (int i = 0; i < 4; i++) {
            float x = box[i * 2];
            float y = box[i * 2 + 1];
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return false;
            }
        }
        // 计算框的宽度和高度
        float[] xs = {box[0], box[2], box[4], box[6]};
        float[] ys = {box[1], box[3], box[5], box[7]};
        float minX = Math.min(Math.min(xs[0], xs[1]), Math.min(xs[2], xs[3]));
        float maxX = Math.max(Math.max(xs[0], xs[1]), Math.max(xs[2], xs[3]));
        float minY = Math.min(Math.min(ys[0], ys[1]), Math.min(ys[2], ys[3]));
        float maxY = Math.max(Math.max(ys[0], ys[1]), Math.max(ys[2], ys[3]));
        float boxWidth = maxX - minX;
        float boxHeight = maxY - minY;
        // 过滤太小的框
        if (boxWidth < 5 || boxHeight < 5) {
            return false;
        }
        // 过滤长宽比异常的框
        float aspectRatio = boxWidth / boxHeight;
        if (aspectRatio > 50 || aspectRatio < 0.02) {
            return false;
        }
        return true;
    }

    /**
     * 解码识别结果
     * PPOCRv3识别模型输出形状: [1, seq_len, dict_size]
     */
    private String decodeText(OrtSession.Result result) {
        try {
            // 获取输出张量
            OnnxTensor outputTensor = (OnnxTensor) result.get(0);
            long[] shape = outputTensor.getInfo().getShape();
            log.debug("[ONNX-OCR] 识别模型输出形状: {}", Arrays.toString(shape));
            // 根据形状确定如何处理输出
            // PPOCRv3输出通常是 [1, seq_len, dict_size]
            float[][][] output;
            if (shape.length == 3) {
                output = (float[][][]) result.get(0).getValue();
            } else if (shape.length == 2) {
                // 某些版本可能输出 [seq_len, dict_size]
                float[][] output2d = (float[][]) result.get(0).getValue();
                output = new float[1][][];
                output[0] = output2d;
            } else {
                log.error("[ONNX-OCR] 不支持的输出形状: {}", Arrays.toString(shape));
                return "";
            }
            StringBuilder text = new StringBuilder();
            int lastIdx = 0;
            // 遍历每个时间步
            for (int t = 0; t < output[0].length; t++) {
                float[] probs = output[0][t];
                // 找到最大概率的字符索引
                int maxIdx = 0;
                float maxProb = probs[0];
                for (int i = 1; i < probs.length; i++) {
                    if (probs[i] > maxProb) {
                        maxProb = probs[i];
                        maxIdx = i;
                    }
                }
                // CTC解码：跳过重复字符和空白符（索引0）
                if (maxIdx != 0 && maxIdx != lastIdx && maxIdx < dictionary.size()) {
                    String charStr = dictionary.get(maxIdx);
                    text.append(charStr);
                    log.trace("[ONNX-OCR] 位置 {}: idx={}, prob={}, char=\'{}\'", t, maxIdx, maxProb, charStr);
                }
                lastIdx = maxIdx;
            }
            String resultText = text.toString().trim();
            log.debug("[ONNX-OCR] 解码文本: \'{}\'", resultText);
            return resultText;
        } catch (Exception e) {
            log.error("[ONNX-OCR] 文本解码失败: {}", e.getMessage(), e);
            return "";
        }
    }

    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private InvoiceOcrResultDTO extractInvoiceFields(List<String> textLines) {
        InvoiceOcrResultDTO result = InvoiceOcrResultDTO.success(ENGINE_NAME);
        result.setRawText(String.join("\n", textLines));
        String fullText = String.join("\n", textLines);
        result.setInvoiceCode(extractByPattern(fullText, "发票代码[：:\\s]*(\\d{10,12})", "代码[：:\\s]*(\\d{10,12})"));
        result.setInvoiceNo(extractByPattern(fullText, "发票号码[：:\\s]*(\\d{8,20})", "号码[：:\\s]*(\\d{8,20})"));
        result.setIssueDate(extractDate(fullText));
        result.setCheckCode(extractByPattern(fullText, "校验码[：:\\s]*(\\d{6,20})", "验证码[：:\\s]*(\\d{6,20})"));
        result.setBuyerName(extractByPattern(fullText, "购[买方]*[名称名][：:\\s]*([^\\n\\r]+?)(?=\\s*(?:纳税人|税号|地址|电话|\\n|$))"));
        result.setBuyerTaxNo(extractByPattern(fullText, "购[买方]*.*?纳税人识别号[：:\\s]*([A-Za-z0-9]{15,20})"));
        result.setSellerName(extractByPattern(fullText, "销[售方]*[名称名][：:\\s]*([^\\n\\r]+?)(?=\\s*(?:纳税人|税号|地址|电话|\\n|$))"));
        result.setSellerTaxNo(extractByPattern(fullText, "销[售方]*.*?纳税人识别号[：:\\s]*([A-Za-z0-9]{15,20})"));
        result.setAmountWithoutTax(extractAmount(fullText, "金额[（(]不含税[)）][：:\\s]*[￥¥]?\\s*([\\d,]+\\.?\\d*)"));
        result.setTaxAmount(extractAmount(fullText, "税[额金][：:\\s]*[￥¥]?\\s*([\\d,]+\\.?\\d*)"));
        result.setTotalAmount(extractAmount(fullText, "价税合计[（(]大写[)）][：:\\s]*[￥¥]?\\s*([\\d,]+\\.?\\d*)", "价税合计[：:\\s]*[￥¥]?\\s*([\\d,]+\\.?\\d*)"));
        log.info("[ONNX-OCR] 识别完成 - 发票代码: {}, 发票号码: {}", result.getInvoiceCode(), result.getInvoiceNo());
        return result;
    }

    private String extractByPattern(String text, String... patterns) {
        for (String pattern : patterns) {
            try {
                Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
                Matcher m = p.matcher(text);
                if (m.find()) {
                    return m.group(1).trim();
                }
            } catch (Exception e) {
                log.debug("[ONNX-OCR] 模式匹配失败: {}", pattern);
            }
        }
        return null;
    }

    private String extractDate(String text) {
        String[] patterns = {"开票日期[：:\\s]*(\\d{4}年\\d{1,2}月\\d{1,2}日)", "开票日期[：:\\s]*(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})", "日期[：:\\s]*(\\d{4}年\\d{1,2}月\\d{1,2}日)"};
        for (String pattern : patterns) {
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                if (m.find()) {
                    String date = m.group(1);
                    return date.replace("年", "-").replace("月", "-").replace("日", "").replace("/", "-");
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private java.math.BigDecimal extractAmount(String text, String... patterns) {
        for (String pattern : patterns) {
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                if (m.find()) {
                    String amount = m.group(1).replace(",", "");
                    return new java.math.BigDecimal(amount);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
