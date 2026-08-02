package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.FinanceVoucherCreateDTO;
import com.foodtraceability.dto.finance.FinanceVoucherVO;
import com.foodtraceability.dto.finance.FundFlowCreateDTO;
import com.foodtraceability.dto.finance.FundFlowVO;
import com.foodtraceability.dto.finance.ReceiptCreateDTO;
import com.foodtraceability.dto.finance.ReceiptVO;
import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.Receivable;
import com.foodtraceability.entity.finance.Receipt;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.AccountingSubjectMapper;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.ReceivableMapper;
import com.foodtraceability.mapper.finance.ReceiptMapper;
import com.foodtraceability.service.finance.FundFlowService;
import com.foodtraceability.service.finance.ReceiptService;
import com.foodtraceability.service.finance.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 收款单Service实现
 * 核心能力：四账联动（应收账款+银行账户+资金流水+会计凭证）
 *
 * 收款流程（一个事务内）：
 * 1. 校验应收账款和银行账户
 * 2. 创建收款单记录
 * 3. 更新应收账款余额、状态和最后收款日期
 * 4. 生成收款会计凭证（借：银行存款 / 贷：应收账款）
 * 5. 创建资金流水（自动联动银行账户余额增加）
 * 6. 回写收款单的凭证ID和流水ID
 */
@Service
public class ReceiptServiceImpl extends ServiceImpl<ReceiptMapper, Receipt>
        implements ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptServiceImpl.class);

    /** 应收账款科目编码 */
    private static final String SUBJECT_CODE_RECEIVABLE = "1122";
    /** 银行存款科目编码 */
    private static final String SUBJECT_CODE_BANK = "1002";

    private final ReceivableMapper receivableMapper;
    private final BankAccountMapper bankAccountMapper;
    private final AccountingSubjectMapper subjectMapper;
    private final FundFlowService fundFlowService;
    private final VoucherService voucherService;

    public ReceiptServiceImpl(ReceivableMapper receivableMapper,
                              BankAccountMapper bankAccountMapper,
                              AccountingSubjectMapper subjectMapper,
                              FundFlowService fundFlowService,
                              VoucherService voucherService) {
        this.receivableMapper = receivableMapper;
        this.bankAccountMapper = bankAccountMapper;
        this.subjectMapper = subjectMapper;
        this.fundFlowService = fundFlowService;
        this.voucherService = voucherService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceiptVO registerReceipt(ReceiptCreateDTO dto) {
        log.info("登记收款，应收ID：{}，金额：{}", dto.getReceivableId(), dto.getReceiptAmount());

        // ========== 1. 校验应收账款 ==========
        Receivable receivable = receivableMapper.selectById(dto.getReceivableId());
        if (receivable == null) {
            throw new BusinessException("应收账款不存在");
        }
        if (receivable.getStatus() != null && receivable.getStatus() == 4) {
            throw new BusinessException("该应收账款已核销，无需再次收款");
        }
        if (dto.getReceiptAmount() > receivable.getBalanceAmount()) {
            throw new BusinessException("收款金额（" + dto.getReceiptAmount() + "）不能大于未收余额（" + receivable.getBalanceAmount() + "）");
        }

        // ========== 2. 校验银行账户（收款不需要校验余额，钱是进来的） ==========
        BankAccount bankAccount = bankAccountMapper.selectById(dto.getBankAccountId());
        if (bankAccount == null) {
            throw new BusinessException("银行账户不存在");
        }
        if (bankAccount.getStatus() == null || bankAccount.getStatus() != 1) {
            throw new BusinessException("银行账户已停用");
        }

        // ========== 3. 创建收款单 ==========
        Receipt receipt = new Receipt();
        receipt.setReceiptNo(generateReceiptNo());
        receipt.setReceivableId(receivable.getReceivableId());
        receipt.setReceivableNo(receivable.getReceivableNo());
        receipt.setCustomerId(receivable.getCustomerId());
        receipt.setCustomerName(receivable.getCustomerName());
        receipt.setReceiptAmount(dto.getReceiptAmount());
        receipt.setReceiptMethod(dto.getReceiptMethod());
        receipt.setBankAccountId(bankAccount.getAccountId());
        receipt.setBankAccountName(bankAccount.getAccountName());
        receipt.setReceiptDate(dto.getReceiptDate());
        receipt.setStatus(1); // 已确认
        receipt.setRemark(dto.getRemark());
        baseMapper.insert(receipt);
        log.info("收款单已创建：{}", receipt.getReceiptNo());

        // ========== 4. 更新应收账款余额、状态和最后收款日期 ==========
        long newReceivedAmount = receivable.getReceivedAmount() + dto.getReceiptAmount();
        long newBalanceAmount = receivable.getBalanceAmount() - dto.getReceiptAmount();
        receivable.setReceivedAmount(newReceivedAmount);
        receivable.setBalanceAmount(newBalanceAmount);
        receivable.setLastPaymentDate(dto.getReceiptDate());
        if (newBalanceAmount <= 0) {
            receivable.setStatus(3); // 已核销（状态编码统一：1未收/2部分收/3已核销/4逾期）
        }
        // 否则保持当前状态（部分收款/逾期等由其他流程处理）
        receivableMapper.updateById(receivable);
        log.info("应收账款已更新：{}，已收：{}，余额：{}", receivable.getReceivableNo(), newReceivedAmount, newBalanceAmount);

        // ========== 5. 生成收款会计凭证 ==========
        FinanceVoucherCreateDTO voucherDTO = buildReceiptVoucherDTO(receipt, receivable, bankAccount);
        FinanceVoucherVO voucherVO = voucherService.create(voucherDTO);
        log.info("收款凭证已生成：{}", voucherVO.getVoucherNo());

        // ========== 6. 创建资金流水（自动联动银行账户余额增加） ==========
        FundFlowCreateDTO flowDTO = new FundFlowCreateDTO();
        flowDTO.setAccountId(bankAccount.getAccountId());
        flowDTO.setFlowDirection(1); // 收入
        flowDTO.setFlowCategory(1); // 销售收款
        flowDTO.setAmount(dto.getReceiptAmount());
        flowDTO.setCounterpartyName(receivable.getCustomerName());
        flowDTO.setBusinessDate(dto.getReceiptDate());
        flowDTO.setVoucherId(voucherVO.getVoucherId());
        flowDTO.setRemark("收款单：" + receipt.getReceiptNo() + "，应收：" + receivable.getReceivableNo());
        FundFlowVO flowVO = fundFlowService.create(flowDTO);
        log.info("资金流水已创建：{}", flowVO.getFlowNo());

        // ========== 7. 回写收款单的凭证ID和流水ID ==========
        receipt.setVoucherId(voucherVO.getVoucherId());
        receipt.setFundFlowId(flowVO.getFlowId());
        baseMapper.updateById(receipt);

        return convertToVO(receipt, voucherVO, flowVO);
    }

    @Override
    public List<ReceiptVO> getReceiptHistoryByReceivableId(Long receivableId) {
        LambdaQueryWrapper<Receipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Receipt::getReceivableId, receivableId)
               .orderByDesc(Receipt::getReceiptDate);
        List<Receipt> list = baseMapper.selectList(wrapper);
        List<ReceiptVO> result = new ArrayList<>();
        for (Receipt r : list) {
            result.add(convertToVO(r, null, null));
        }
        return result;
    }

    @Override
    public ReceiptVO getDetail(Long receiptId) {
        Receipt receipt = baseMapper.selectById(receiptId);
        if (receipt == null) {
            throw new BusinessException("收款单不存在");
        }
        return convertToVO(receipt, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceiptVO update(Long receiptId, ReceiptCreateDTO dto) {
        log.info("更新收款单，ID：{}", receiptId);
        Receipt receipt = baseMapper.selectById(receiptId);
        if (receipt == null) {
            throw new BusinessException("收款单不存在");
        }
        // 仅更新可编辑字段，不涉及四账联动重新生成
        receipt.setReceiptAmount(dto.getReceiptAmount());
        receipt.setReceiptMethod(dto.getReceiptMethod());
        receipt.setBankAccountId(dto.getBankAccountId());
        receipt.setReceiptDate(dto.getReceiptDate());
        receipt.setRemark(dto.getRemark());
        baseMapper.updateById(receipt);
        return getDetail(receiptId);
    }

    // ============================================================
    // 私有方法
    // ============================================================

    /**
     * 构建收款凭证DTO
     * 借：银行存款（1002）
     * 贷：应收账款（1122）
     */
    private FinanceVoucherCreateDTO buildReceiptVoucherDTO(Receipt receipt, Receivable receivable, BankAccount bankAccount) {
        // 查询科目ID
        AccountingSubject receivableSubject = getSubjectByCode(SUBJECT_CODE_RECEIVABLE);
        AccountingSubject bankSubject = getSubjectByCode(SUBJECT_CODE_BANK);

        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(receipt.getReceiptDate());
        dto.setVoucherType(6); // 收款
        dto.setSourceType(6); // 收款
        dto.setSourceId(receipt.getReceiptId());
        dto.setReferenceNo(receipt.getReceiptNo());
        dto.setAttachmentCount(1);
        dto.setRemark("收取客户：" + receivable.getCustomerName() + "，收款单：" + receipt.getReceiptNo());

        // 借方分录：银行存款
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        debitItem.setSubjectId(bankSubject.getSubjectId());
        debitItem.setSummary("收款账户：" + bankAccount.getAccountName());
        debitItem.setDebitAmount(receipt.getReceiptAmount());
        debitItem.setCreditAmount(0L);
        debitItem.setSortOrder(1);

        // 贷方分录：应收账款
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        creditItem.setSubjectId(receivableSubject.getSubjectId());
        creditItem.setSummary("收取" + receivable.getCustomerName() + "货款");
        creditItem.setDebitAmount(0L);
        creditItem.setCreditAmount(receipt.getReceiptAmount());
        creditItem.setSortOrder(2);

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();
        details.add(debitItem);
        details.add(creditItem);
        dto.setDetails(details);

        return dto;
    }

    /**
     * 根据科目编码查询科目
     */
    private AccountingSubject getSubjectByCode(String subjectCode) {
        LambdaQueryWrapper<AccountingSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountingSubject::getSubjectCode, subjectCode);
        AccountingSubject subject = subjectMapper.selectOne(wrapper);
        if (subject == null) {
            throw new BusinessException("会计科目不存在：" + subjectCode);
        }
        return subject;
    }

    /**
     * 生成收款单号：SK + yyyyMMdd + 4位序号
     */
    private String generateReceiptNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<Receipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Receipt::getReceiptNo, "SK" + datePart)
               .orderByDesc(Receipt::getReceiptId).last("LIMIT 1");
        Receipt last = this.getOne(wrapper, false);
        int seq = 1;
        if (last != null && last.getReceiptNo() != null) {
            String no = last.getReceiptNo();
            seq = Integer.parseInt(no.substring(no.length() - 4)) + 1;
        }
        return String.format("SK%s%04d", datePart, seq);
    }

    /**
     * 实体转VO
     */
    private ReceiptVO convertToVO(Receipt receipt, FinanceVoucherVO voucherVO, FundFlowVO flowVO) {
        ReceiptVO vo = new ReceiptVO();
        vo.setReceiptId(receipt.getReceiptId());
        vo.setReceiptNo(receipt.getReceiptNo());
        vo.setReceivableId(receipt.getReceivableId());
        vo.setReceivableNo(receipt.getReceivableNo());
        vo.setCustomerId(receipt.getCustomerId());
        vo.setCustomerName(receipt.getCustomerName());
        vo.setReceiptAmount(receipt.getReceiptAmount());
        vo.setReceiptAmountDisplay(formatFenToYuan(receipt.getReceiptAmount()));
        vo.setReceiptMethod(receipt.getReceiptMethod());
        vo.setReceiptMethodName(getReceiptMethodName(receipt.getReceiptMethod()));
        vo.setBankAccountId(receipt.getBankAccountId());
        vo.setBankAccountName(receipt.getBankAccountName());
        vo.setReceiptDate(receipt.getReceiptDate());
        vo.setFundFlowId(receipt.getFundFlowId());
        vo.setVoucherId(receipt.getVoucherId());
        vo.setStatus(receipt.getStatus());
        vo.setStatusName(receipt.getStatus() != null && receipt.getStatus() == 1 ? "已确认" : "已作废");
        vo.setRemark(receipt.getRemark());
        vo.setCreateUserId(receipt.getCreateUserId());
        vo.setCreateUserName(receipt.getCreateUserName());
        vo.setCreateTime(receipt.getCreateTime());

        if (voucherVO != null) {
            vo.setVoucherNo(voucherVO.getVoucherNo());
        }
        if (flowVO != null) {
            vo.setFundFlowNo(flowVO.getFlowNo());
        }
        return vo;
    }

    /**
     * 分转元（保留2位小数）
     */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }

    /**
     * 获取收款方式名称
     */
    private String getReceiptMethodName(String method) {
        if (method == null) return "未知";
        switch (method) {
            case "bank_transfer": return "银行转账";
            case "cash": return "现金";
            case "check": return "支票";
            default: return method;
        }
    }
}
