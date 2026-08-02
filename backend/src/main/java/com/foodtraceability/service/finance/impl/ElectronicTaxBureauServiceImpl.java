package com.foodtraceability.service.finance.impl;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.finance.ElectronicTaxBureauService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 电子税务局对接服务实现（占位）
 * <p>
 * 当前为占位实现，所有方法返回"待对接"提示信息。
 * 待电子税务局对接时，需根据各地税务局API规范实现具体逻辑。
 * </p>
 */
@Service
public class ElectronicTaxBureauServiceImpl implements ElectronicTaxBureauService {

    private static final Logger log = LoggerFactory.getLogger(ElectronicTaxBureauServiceImpl.class);

    /** 待对接提示消息 */
    private static final String NOT_IMPLEMENTED_MSG = "电子税务局对接功能尚未开通，敬请期待后续版本";

    @Override
    public Result<Map<String, Object>> submitTaxReturn(Map<String, Object> taxReturn) {
        log.warn("电子税务局提交申报表 - 功能尚未实现，收到的数据: {}", taxReturn);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submissionId", "PENDING-" + System.currentTimeMillis());
        result.put("status", "PENDING");
        result.put("message", NOT_IMPLEMENTED_MSG);
        result.put("timestamp", System.currentTimeMillis());

        return Result.success(result, NOT_IMPLEMENTED_MSG);
    }

    @Override
    public Result<Map<String, Object>> querySubmissionStatus(String submissionId) {
        log.warn("电子税务局查询申报状态 - 功能尚未实现，submissionId: {}", submissionId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submissionId", submissionId);
        result.put("status", "UNKNOWN");
        result.put("message", NOT_IMPLEMENTED_MSG);
        result.put("timestamp", System.currentTimeMillis());

        return Result.success(result, NOT_IMPLEMENTED_MSG);
    }

    @Override
    public Result<Map<String, Object>> queryPaymentInfo(String taxPeriod) {
        log.warn("电子税务局查询缴款信息 - 功能尚未实现，taxPeriod: {}", taxPeriod);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taxPeriod", taxPeriod);
        result.put("status", "UNKNOWN");
        result.put("message", NOT_IMPLEMENTED_MSG);
        result.put("timestamp", System.currentTimeMillis());

        return Result.success(result, NOT_IMPLEMENTED_MSG);
    }

    @Override
    public Result<Boolean> verifyTaxpayerInfo(String taxpayerId) {
        log.warn("电子税务局验证纳税人信息 - 功能尚未实现，taxpayerId: {}", taxpayerId);

        return Result.success(false, NOT_IMPLEMENTED_MSG);
    }
}
