package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.InvoiceDTO;
import com.foodtraceability.entity.Invoice;

import java.util.List;

/**
 * 发票Service接口
 * 用于处理发票相关的业务逻辑
 */
public interface InvoiceService extends IService<Invoice> {
    /**
     * 分页查询发票
     * @param page 分页对象
     * @param invoiceDTO 发票DTO
     * @return 分页结果
     */
    IPage<Invoice> getInvoicePage(Page<Invoice> page, InvoiceDTO invoiceDTO);
    
    /**
     * 根据ID获取发票详情
     * @param id 发票ID
     * @return 发票详情
     */
    Invoice getInvoiceById(String id);
    
    /**
     * 创建发票
     * @param invoiceDTO 发票DTO
     * @return 是否创建成功
     */
    boolean createInvoice(InvoiceDTO invoiceDTO);
    
    /**
     * 更新发票
     * @param id 发票ID
     * @param invoiceDTO 发票DTO
     * @return 是否更新成功
     */
    boolean updateInvoice(String id, InvoiceDTO invoiceDTO);
    
    /**
     * 删除发票
     * @param id 发票ID
     * @return 是否删除成功
     */
    boolean deleteInvoice(String id);
    
    /**
     * 批量删除发票
     * @param ids 发票ID列表
     * @return 是否删除成功
     */
    boolean batchDeleteInvoice(String[] ids);
    
    /**
     * 开具发票
     * @param id 发票ID
     * @return 是否开具成功
     */
    boolean issueInvoice(String id);
    
    /**
     * 作废发票
     * @param id 发票ID
     * @return 是否作废成功
     */
    boolean voidInvoice(String id);
    
    /**
     * 红冲发票
     * @param id 发票ID
     * @return 是否红冲成功
     */
    boolean redIssueInvoice(String id);
    
    /**
     * 打印发票
     * @param id 发票ID
     * @return 是否打印成功
     */
    boolean printInvoice(String id);
    
    /**
     * 根据订单ID获取发票
     * @param orderId 订单ID
     * @return 发票
     */
    Invoice getInvoiceByOrderId(Long orderId);
    
    /**
     * 根据健康证ID获取发票列表
     * @param healthCertificateId 健康证ID
     * @return 发票列表
     */
    List<Invoice> getByHealthCertificateId(String healthCertificateId);
    
    /**
     * 审核发票
     * @param id 发票ID
     * @param approvalStatus 审核状态
     * @param approvedBy 审核人
     * @param rejectReason 拒绝原因
     * @return 是否审核成功
     */
    boolean approveInvoice(String id, String approvalStatus, String approvedBy, String rejectReason);
    
    /**
     * 上传发票图片
     * @param id 发票ID
     * @param imageUrl 图片URL
     * @return 是否上传成功
     */
    boolean uploadInvoiceImage(String id, String imageUrl);
}
