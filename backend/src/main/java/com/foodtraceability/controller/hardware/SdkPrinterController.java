package com.foodtraceability.controller.hardware;

import com.foodtraceability.printer.XprinterSdkService;
import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * SDK 打印机管理控制器
 *
 * <p>通过 XprinterSdkService 调用打印机 SDK 实现标签打印和打印机列表查询。
 * 包含真实的 SDK 调用逻辑。</p>
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 {@link com.foodtraceability.driver.TsplLabelPrinterDriver}、
 *             {@link com.foodtraceability.driver.PrinterDriver} 等驱动实现类，
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理。
 *             旧控制器的 SDK 调用逻辑应下沉为 DeviceDriver 实现类，被新系统调用。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/sdk-printer")
@Tag(name = "SDK打印机管理", description = "通过SDK调用打印机的标签打印和打印机列表查询接口")
public class SdkPrinterController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SdkPrinterController.class);
    private final XprinterSdkService sdkService;

    @PostMapping("/print")
    @Operation(summary = "打印标签")
    @PreAuthorize("hasAuthority('device:sdk-printer:manage') or hasAuthority('*')")
    public Result<Boolean> printLabel(@RequestParam String printerName, @RequestParam int labelWidthMm, @RequestParam int labelHeightMm, @RequestBody List<Map<String, Object>> elements, @RequestBody Map<String, Object> data) {
        log.info("SDK打印请求: printer={}, size={}x{}mm", printerName, labelWidthMm, labelHeightMm);
        boolean success = sdkService.printLabel(printerName, labelWidthMm, labelHeightMm, elements, data);
        if (success) {
            return Result.success(true, "打印成功");
        } else {
            return Result.error("打印失败");
        }
    }

    @GetMapping("/printers")
    @Operation(summary = "查询打印机列表")
    @PreAuthorize("hasAuthority('device:sdk-printer:view') or hasAuthority('*')")
    public Result<List<String>> listPrinters() {
        List<String> printers = sdkService.listPrinters();
        return Result.success(printers);
    }

    public SdkPrinterController(final XprinterSdkService sdkService) {
        this.sdkService = sdkService;
    }
}
