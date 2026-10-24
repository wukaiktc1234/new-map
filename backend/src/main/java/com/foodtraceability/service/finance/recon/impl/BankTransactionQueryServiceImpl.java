package com.foodtraceability.service.finance.recon.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.finance.recon.BankTransactionQueryDTO;
import com.foodtraceability.dto.finance.recon.BankTransactionVO;
import com.foodtraceability.entity.finance.recon.BankTransaction;
import com.foodtraceability.mapper.finance.recon.BankTransactionMapper;
import com.foodtraceability.service.finance.recon.BankTransactionQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 外部流水只读查询实现（P0 无写操作）。 */
@Service
public class BankTransactionQueryServiceImpl implements BankTransactionQueryService {

    private final BankTransactionMapper txnMapper;

    public BankTransactionQueryServiceImpl(BankTransactionMapper txnMapper) {
        this.txnMapper = txnMapper;
    }

    @Override
    public IPage<BankTransactionVO> page(BankTransactionQueryDTO q) {
        int current = q.getCurrent() == null || q.getCurrent() < 1 ? 1 : q.getCurrent();
        int size = q.getSize() == null || q.getSize() < 1 ? 20 : Math.min(q.getSize(), 200);
        LambdaQueryWrapper<BankTransaction> w = new LambdaQueryWrapper<BankTransaction>()
                .eq(BankTransaction::getDeleted, 0)
                .eq(q.getAccountId() != null, BankTransaction::getAccountId, q.getAccountId())
                .eq(q.getSourceType() != null && !q.getSourceType().isBlank(), BankTransaction::getSourceType, q.getSourceType())
                .eq(q.getMatchStatus() != null && !q.getMatchStatus().isBlank(), BankTransaction::getMatchStatus, q.getMatchStatus())
                .eq(q.getNeedsReview() != null, BankTransaction::getNeedsReview, q.getNeedsReview())
                .eq(q.getBatchId() != null, BankTransaction::getBatchId, q.getBatchId())
                .ge(q.getFrom() != null, BankTransaction::getBusinessDate, q.getFrom())
                .le(q.getTo() != null, BankTransaction::getBusinessDate, q.getTo())
                .orderByDesc(BankTransaction::getTxnTime)
                .orderByDesc(BankTransaction::getTransactionId);
        Page<BankTransaction> page = txnMapper.selectPage(new Page<>(current, size), w);
        IPage<BankTransactionVO> out = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<BankTransactionVO> records = new ArrayList<>();
        for (BankTransaction t : page.getRecords()) {
            BankTransactionVO vo = new BankTransactionVO();
            vo.setTransactionId(t.getTransactionId());
            vo.setBatchId(t.getBatchId());
            vo.setAccountId(t.getAccountId());
            vo.setSourceType(t.getSourceType());
            vo.setExternalTxnId(t.getExternalTxnId());
            vo.setMerchantOrderNo(t.getMerchantOrderNo());
            vo.setDirection(t.getDirection());
            vo.setIsRefund(t.getIsRefund());
            vo.setAmount(t.getAmount());
            vo.setFee(t.getFee());
            vo.setTxnTime(t.getTxnTime());
            vo.setBusinessDate(t.getBusinessDate());
            vo.setCounterpartyName(t.getCounterpartyName());
            vo.setCounterpartyAccount(t.getCounterpartyAccount());
            vo.setSummary(t.getSummary());
            vo.setDedupeBasis(t.getDedupeBasis());
            vo.setMatchStatus(t.getMatchStatus());
            vo.setNeedsReview(t.getNeedsReview());
            vo.setCreateUserId(t.getCreateUserId());
            vo.setCreateTime(t.getCreateTime());
            records.add(vo);
        }
        out.setRecords(records);
        return out;
    }
}
