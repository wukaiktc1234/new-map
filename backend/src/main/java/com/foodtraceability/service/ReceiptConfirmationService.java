package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.ReceiptConfirmationCreateDTO;
import com.foodtraceability.dto.ReceiptConfirmationQueryDTO;
import com.foodtraceability.dto.ReceiptSignatureDTO;
import com.foodtraceability.dto.ReceiptVerifyVO;
import com.foodtraceability.entity.ReceiptConfirmation;
import com.foodtraceability.entity.ReceiptSignature;

import java.util.List;

/**
 * 收货确认单服务接口
 * 门店/仓库共用，负责实物确认、库存增加、应付生成
 */
public interface ReceiptConfirmationService extends IService<ReceiptConfirmation> {

    /**
     * 创建收货确认单
     * @param createDTO 创建参数
     * @return 创建后的收货确认单详情
     */
    ReceiptConfirmation createConfirmation(ReceiptConfirmationCreateDTO createDTO);

    /**
     * 分页查询收货确认单
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<ReceiptConfirmation> getConfirmationPage(ReceiptConfirmationQueryDTO queryDTO);

    /**
     * 根据ID获取收货确认单详情（含明细）
     * @param confirmationId 确认单ID
     * @return 确认单详情
     */
    ReceiptConfirmation getConfirmationDetail(Long confirmationId);

    /**
     * 新增收货确认签名（收货方/供应商双签）
     * @param confirmationId 确认单ID
     * @param signatureDTO 签名参数
     * @return 签名记录
     */
    ReceiptSignature addSignature(Long confirmationId, ReceiptSignatureDTO signatureDTO);

    /**
     * 查询收货确认单的签名存证列表
     * @param confirmationId 确认单ID
     * @return 签名记录列表
     */
    List<ReceiptSignature> listSignatures(Long confirmationId);

    /**
     * 登记一次打印（每次打印 A4 前调用）。
     * 服务端校验打印次数上限并自增 print_count、刷新 last_print_time，返回最新打印信息。
     * @param confirmationId 确认单ID
     * @return 更新后的确认单（含 printCount / lastPrintTime）
     */
    ReceiptConfirmation recordPrint(Long confirmationId);

    /**
     * 防伪码查验：输入 16 位防伪码查询单据基本信息与完整打印历史
     * @param verifyCode 防伪码
     * @return 查验结果（valid=false 表示码无效）
     */
    ReceiptVerifyVO verifyByCode(String verifyCode);
}
