package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 调节表视图（S3 响应 / S4 列表 / S5 详情共用；详情多带 items） */
public class ReconciliationStatementVO {
    public Long statementId;
    public String statementNo;
    public Long accountId;
    public Long storeId;
    public LocalDate periodStart;
    public LocalDate periodEnd;
    /** 企业账面余额（分） */
    public Long bookBalance;
    /** 对账单余额（分） */
    public Long bankBalance;
    /** A 企业已收银行未收（分） */
    public Long unclearedInCredit;
    /** B 企业已付银行未付（分） */
    public Long unclearedInDebit;
    /** C 银行已收企业未收（分） */
    public Long unclearedOutCredit;
    /** D 银行已付企业未付（分） */
    public Long unclearedOutDebit;
    /** 企业调节后余额 = book + C − D（分） */
    public Long adjustedBookBalance;
    /** 银行调节后余额 = bank + A − B（分） */
    public Long adjustedBankBalance;
    /** 两侧相等（V5 门禁用） */
    public Boolean isBalanced;
    public String status;
    public Long submittedBy;
    public LocalDateTime submittedAt;
    public Long approvedBy;
    public LocalDateTime approvedAt;
    public String remark;
    public Long createUserId;
    public LocalDateTime createTime;
    /** BL-6：未解决的同额歧义条数（>0 时 isBalanced 恒为 false，禁止 submit/approve） */
    public Integer ambiguousItemCount;
    public List<ReconciliationItemVO> items;
}
