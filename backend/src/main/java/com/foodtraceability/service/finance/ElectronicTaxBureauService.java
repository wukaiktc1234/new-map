package com.foodtraceability.service.finance;

import com.foodtraceability.common.Result;

import java.util.Map;

/**
 * 电子税务局对接服务接口（预留）
 * <p>
 * 本接口定义与电子税务局对接的标准方法，目前所有方法为占位实现。
 * 真正对接电子税务局时，需根据各地税务局API规范实现具体逻辑。
 * </p>
 *
 * <p>
 * 待实现事项：
 * <ul>
 *   <li>对接国家税务总局电子税务局API</li>
 *   <li>支持纳税申报表提交</li>
 *   <li>支持申报状态实时查询</li>
 *   <li>支持缴款信息查询</li>
 *   <li>纳税人信息验证</li>
 * </ul>
 * </p>
 */
public interface ElectronicTaxBureauService {

    /**
     * 提交纳税申报表到电子税务局
     * <p>
     * 将系统生成的纳税申报表数据提交至电子税务局。
     * 当前为占位实现，返回待对接提示。
     * </p>
     *
     * @param taxReturn 纳税申报表数据
     * @return 提交结果，包含受理回执信息
     */
    Result<Map<String, Object>> submitTaxReturn(Map<String, Object> taxReturn);

    /**
     * 查询申报提交状态
     * <p>
     * 根据提交单号查询电子税务局的申报处理状态。
     * 当前为占位实现，返回待对接提示。
     * </p>
     *
     * @param submissionId 提交单号
     * @return 申报状态信息
     */
    Result<Map<String, Object>> querySubmissionStatus(String submissionId);

    /**
     * 查询缴款信息
     * <p>
     * 根据纳税期间查询电子税务局的缴款记录。
     * 当前为占位实现，返回待对接提示。
     * </p>
     *
     * @param taxPeriod 纳税期间，格式：YYYYMM
     * @return 缴款信息
     */
    Result<Map<String, Object>> queryPaymentInfo(String taxPeriod);

    /**
     * 验证纳税人信息
     * <p>
     * 向电子税务局验证纳税人信息的真实性。
     * 当前为占位实现，返回待对接提示。
     * </p>
     *
     * @param taxpayerId 纳税人识别号
     * @return 验证结果
     */
    Result<Boolean> verifyTaxpayerInfo(String taxpayerId);
}
