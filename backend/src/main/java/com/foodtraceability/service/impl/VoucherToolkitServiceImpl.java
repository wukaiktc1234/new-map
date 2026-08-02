package com.foodtraceability.service.impl;

import com.foodtraceability.dto.SignatureVerifyResultDTO;
import com.foodtraceability.service.VoucherToolkitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 电子凭证工具包服务实现
 *
 * <p>工具包集成状态由配置项 {@code voucher.toolkit.enabled} 控制：
 * <ul>
 *   <li>{@code false}（默认）：工具包未集成，所有方法走降级路径，调用方应通过
 *       {@link #isToolkitAvailable()} 判断后再调用工具包方法</li>
 *   <li>{@code true}：工具包已集成，方法将调用真实的工具包实现</li>
 * </ul>
 *
 * <p>当前状态：用友 DJFileUtil、pdf-xml-extractor、xbrl-json 等工具包尚未集成，
 * 调用方（FileParseServiceImpl、SignatureVerifyServiceImpl 等）均有内置替代实现
 * （PDFBox 解析、OCR 识别等），工具包不可用时不影响核心功能。</p>
 */
@Service
public class VoucherToolkitServiceImpl implements VoucherToolkitService {

    /**
     * 工具包启用开关
     * 配置项：voucher.toolkit.enabled（默认 false）
     * 未来集成用友工具包后，设置为 true 即可启用，无需修改代码
     */
    @Value("${voucher.toolkit.enabled:false}")
    private boolean toolkitEnabled;

    @Override
    public String extractXmlFromPdfByToolkit(byte[] pdfContent) {
        // TODO: 集成 pdf-xml-extractor 工具包（toolkitEnabled=true 时启用）
        throw new UnsupportedOperationException("工具包未集成");
    }

    @Override
    public String convertXbrlToJson(String xbrlContent) {
        // TODO: 集成 xbrl-json 工具包（toolkitEnabled=true 时启用）
        throw new UnsupportedOperationException("工具包未集成");
    }

    @Override
    public String convertJsonToXbrl(String jsonContent) {
        // TODO: 集成 xbrl-json 工具包（toolkitEnabled=true 时启用）
        throw new UnsupportedOperationException("工具包未集成");
    }

    @Override
    public SignatureVerifyResultDTO verifySignatureByToolkit(byte[] fileContent, String fileType) {
        // TODO: 集成 DJFileUtil 工具包（toolkitEnabled=true 时启用）
        return new SignatureVerifyResultDTO();
    }

    @Override
    public SignatureVerifyResultDTO verifyOfdSignatureByToolkit(byte[] ofdContent) {
        // TODO: 集成 OFD 签名验证（toolkitEnabled=true 时启用）
        return new SignatureVerifyResultDTO();
    }

    @Override
    public boolean isToolkitAvailable() {
        return toolkitEnabled;
    }

    @Override
    public String getToolkitVersion() {
        if (!toolkitEnabled) {
            return "not-integrated";
        }
        // TODO: 工具包集成后返回真实版本号
        return "stub-1.0.0";
    }
}
