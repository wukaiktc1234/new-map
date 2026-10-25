package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.recon.StoreReconDailyVO;
import com.foodtraceability.service.finance.StoreReconService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 门店对账视图控制器（AC-49，只读）。
 *
 * <p><b>为什么单独一个 Controller 而不并入 ReconController</b>：ReconController 承载的是
 * 「银行流水 × 企业内部流水」的勾稽链（run/item/difference），鉴权码是 {@code finance:bank:*}；
 * 本接口是**缴款单（deposit_slips）视角**的门店日对账，鉴权码是 {@code finance:deposit-slip:view}，
 * 数据范围也走另一套 store 过滤。混在一个类里会让「按权限码划分的接口边界」变模糊，
 * 因此单开一个 Controller（两个类各自只挂一种能力域）。</p>
 */
@Tag(name = "财务-门店对账", description = "门店日对账（AC-49，只读；服务端强制门店范围过滤）")
@RestController
@RequestMapping("/v1/finance/store-recon")
public class StoreReconController {

    private final StoreReconService storeReconService;

    public StoreReconController(StoreReconService storeReconService) {
        this.storeReconService = storeReconService;
    }

    /**
     * 门店日对账。
     *
     * <p>storeId 可省略（店长取自己门店）；date 缺省=今天。越权一律 403。</p>
     */
    @Operation(summary = "门店日对账（storeId 可省略=自己门店；越权 403）")
    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('finance:deposit-slip:view')")
    public Result<StoreReconDailyVO> daily(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(storeReconService.daily(storeId, date));
    }
}
