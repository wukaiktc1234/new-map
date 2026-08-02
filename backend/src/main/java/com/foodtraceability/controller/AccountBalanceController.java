package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 账户余额控制器
 */
@RestController
@RequestMapping("/v1/finance/account-balance")
@Tag(name = "账户余额管理")
public class AccountBalanceController {

    @GetMapping
    @Operation(summary = "获取账户余额列表")
    @PreAuthorize("hasAuthority('finance:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getAccountBalanceList(
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        List<Map<String, Object>> records = new ArrayList<>();
        
        Map<String, Object> r1 = new HashMap<>();
        r1.put("id", "1");
        r1.put("subjectCode", "1001");
        r1.put("subjectName", "库存现金");
        r1.put("category", "ASSET");
        r1.put("openingDebit", new BigDecimal("5000.00"));
        r1.put("openingCredit", BigDecimal.ZERO);
        r1.put("currentDebit", new BigDecimal("50000.00"));
        r1.put("currentCredit", new BigDecimal("48000.00"));
        r1.put("endingDebit", new BigDecimal("7000.00"));
        r1.put("endingCredit", BigDecimal.ZERO);
        records.add(r1);
        
        Map<String, Object> r2 = new HashMap<>();
        r2.put("id", "2");
        r2.put("subjectCode", "1002");
        r2.put("subjectName", "银行存款");
        r2.put("category", "ASSET");
        r2.put("openingDebit", new BigDecimal("200000.00"));
        r2.put("openingCredit", BigDecimal.ZERO);
        r2.put("currentDebit", new BigDecimal("350000.00"));
        r2.put("currentCredit", new BigDecimal("280000.00"));
        r2.put("endingDebit", new BigDecimal("270000.00"));
        r2.put("endingCredit", BigDecimal.ZERO);
        records.add(r2);
        
        Map<String, Object> r3 = new HashMap<>();
        r3.put("id", "3");
        r3.put("subjectCode", "2202");
        r3.put("subjectName", "应付账款");
        r3.put("category", "LIABILITY");
        r3.put("openingDebit", BigDecimal.ZERO);
        r3.put("openingCredit", new BigDecimal("80000.00"));
        r3.put("currentDebit", new BigDecimal("60000.00"));
        r3.put("currentCredit", new BigDecimal("90000.00"));
        r3.put("endingDebit", BigDecimal.ZERO);
        r3.put("endingCredit", new BigDecimal("110000.00"));
        records.add(r3);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        result.put("current", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    @GetMapping("/{subjectCode}/detail")
    @Operation(summary = "获取账户余额明细")
    public Result<Map<String, Object>> getBalanceDetail(
            @PathVariable String subjectCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        List<Map<String, Object>> records = new ArrayList<>();
        
        Map<String, Object> d1 = new HashMap<>();
        d1.put("voucherDate", "2026-03-01");
        d1.put("voucherNo", "记-2026-0001");
        d1.put("summary", "收到销售货款");
        d1.put("debit", new BigDecimal("50000.00"));
        d1.put("credit", BigDecimal.ZERO);
        d1.put("balance", new BigDecimal("250000.00"));
        records.add(d1);
        
        Map<String, Object> d2 = new HashMap<>();
        d2.put("voucherDate", "2026-03-02");
        d2.put("voucherNo", "记-2026-0002");
        d2.put("summary", "支付采购货款");
        d2.put("debit", BigDecimal.ZERO);
        d2.put("credit", new BigDecimal("30000.00"));
        d2.put("balance", new BigDecimal("220000.00"));
        records.add(d2);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        result.put("current", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新账户余额")
    public Result<Void> refreshAccountBalance() {
        return Result.success(null);
    }

    @GetMapping("/export")
    @Operation(summary = "导出账户余额")
    public void exportAccountBalance(
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) String category) {
        // 导出逻辑
    }
}
