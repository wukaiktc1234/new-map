package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.FinanceInvoice;

/**
 * 发票管理Service接口
 *
 * <p>Sprint 3.1 P0 F-003/F-004：发票 CRUD + 状态流转管理。</p>
 *
 * <p>状态机（销项发票开具流程）：
 * 0=draft（草稿）→ 5=issued（已开具）→ 6=void（已作废） / 4=red-flushed（已红冲）。</p>
 *
 * <p>所有写操作由实现类加 {@code @Transactional(rollbackFor = Exception.class)}。</p>
 */
public interface InvoiceService extends IService<FinanceInvoice> {

    /**
     * 创建发票记录
     *
     * @param dto 创建DTO
     * @return 发票VO
     */
    FinanceInvoiceVO create(FinanceInvoiceCreateDTO dto);

    /**
     * 获取发票详情
     *
     * @param invoiceId 发票ID
     * @return 发票VO
     */
    FinanceInvoiceVO getDetail(Long invoiceId);

    /**
     * 分页查询发票列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<FinanceInvoiceVO> getPage(FinanceInvoiceQueryDTO query);

    /**
     * 更新发票（仅 draft 状态可改）
     *
     * @param dto 更新DTO
     * @return 更新后的发票VO
     */
    FinanceInvoiceVO update(FinanceInvoiceUpdateDTO dto);

    /**
     * 删除发票（逻辑删除）
     *
     * @param invoiceId 发票ID
     * @return 是否删除成功
     */
    boolean delete(Long invoiceId);

    /**
     * 开具发票（draft → issued）
     *
     * @param invoiceId 发票ID
     * @return 开具后的发票VO
     */
    FinanceInvoiceVO issue(Long invoiceId);

    /**
     * 作废发票（issued → void）
     *
     * @param invoiceId 发票ID
     * @return 作废后的发票VO
     */
    FinanceInvoiceVO voidInvoice(Long invoiceId);

    /**
     * 红冲发票（issued → red-flushed）
     *
     * @param invoiceId 发票ID
     * @return 红冲后的发票VO
     */
    FinanceInvoiceVO redFlush(Long invoiceId);
}
