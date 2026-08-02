package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.AccountingEvent;
import com.foodtraceability.entity.AutoVoucherLog;
import com.foodtraceability.service.voucher.AutoVoucherResult;
import java.util.List;

/**
 * 自动凭证服务接口
 * 提供自动记账引擎的核心功能，包括单条/批量生成凭证、日志查询、重复检查等
 * 实现基于规则引擎的自动化会计凭证生成
 * @author example
 * @since 2026-04-04
 */
public interface AutoVoucherService {

    /**
     * 根据会计事件自动生成凭证
     * 执行完整的9步流水线处理：幂等检查、事件标准化、规则匹配、模板加载、变量填充、
     * 借贷平衡校验、质量评分、凭证组装、持久化保存
     * @param event 会计事件（如POS销售事件、采购入库事件）
     * @return 自动凭证生成结果，包含凭证数据和处理日志
     */
    AutoVoucherResult generateVoucher(AccountingEvent event);

    /**
     * 批量自动生成凭证
     * 对多个会计事件依次执行自动凭证生成流程
     * 每个事件独立处理，互不影响，即使某个失败也不影响其他事件的生成
     * @param events 会计事件列表
     * @return 自动凭证生成结果列表
     */
    List<AutoVoucherResult> generateVouchersBatch(List<AccountingEvent> events);

    /**
     * 根据日志ID查询凭证处理日志
     * 用于查看某次自动记账的详细处理过程和结果
     * @param logId 日志ID
     * @return 凭证处理日志详情
     */
    AutoVoucherLog getVoucherLogById(Long logId);

    /**
     * 分页查询凭证处理日志
     * 支持按事件类型、状态、时间范围等条件筛选
     * @param query 查询条件对象
     * @return 分页的日志记录
     */
    IPage<AutoVoucherLog> queryVoucherLogs(AutoVoucherLogQuery query);

    /**
     * 检查是否为重复事件
     * 通过查询幂等控制表判断指定业务键是否已经生成过凭证
     * @param eventType 事件类型
     * @param businessId 业务单据ID
     * @return true表示已存在（重复），false表示不存在（可以生成）
     */
    boolean checkDuplicate(String eventType, String businessId);
}
