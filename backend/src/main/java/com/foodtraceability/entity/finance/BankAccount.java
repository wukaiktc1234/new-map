package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.config.handler.LenientIntegerTypeHandler;
import com.baomidou.mybatisplus.annotation.Version;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 银行账户实体类
 * 用于管理餐饮企业的银行账户信息，包括基本户、一般户、备用金等，支持余额管理与脱敏展示
 */
@TableName(value = "bank_accounts", autoResultMap = true)
public class BankAccount extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账户ID */
    @TableId(type = IdType.AUTO)
    private Long accountId;

    /** 账户名称 */
    private String accountName;

    /** 银行名称 */
    private String bankName;

    /** 账号 */
    private String accountNumber;

    /** 脱敏账号 */
    private String accountNumberMasked;

    /**
     * 账户类型
     * 1-基本户 2-一般户 3-工资户 4-备用金 5-其他
     * <p>
     * 读取侧挂宽容处理器：历史上该列是 varchar（存在 'bank'/'cash' 等脏值），
     * 直接 getInt 会抛「不良的类型值 int : bank」并让整个列表接口 500。
     * 列已于 2026-10-23 归一为 integer，处理器仅作回滚/残余脏值的防御层。
     * </p>
     */
    @TableField(value = "account_type", typeHandler = LenientIntegerTypeHandler.class)
    private Integer accountType;

    /** 余额（分） */
    private Long balance;

    /** 币种 */
    private String currency;

    /**
     * 状态
     * 1-启用 0-停用
     */
    private Integer status;

    /** 开户日期 */
    private LocalDate openDate;

    /** 备注 */
    private String remark;

    /**
     * 归属门店ID（bank_accounts.store_id）
     * <p>NULL = 历史/全局账户（不参与门店级 POS 入账选账户）；
     * POS 支付入账必须按订单门店精确匹配，见 PosSaleFinanceRecorder。</p>
     */
    @TableField("store_id")
    private Long storeId;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountNumberMasked() {
        return accountNumberMasked;
    }

    public void setAccountNumberMasked(String accountNumberMasked) {
        this.accountNumberMasked = accountNumberMasked;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "accountId=" + accountId +
                ", accountName='" + accountName + '\'' +
                ", bankName='" + bankName + '\'' +
                ", accountNumberMasked='" + accountNumberMasked + '\'' +
                ", accountType=" + accountType +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                '}';
    }
}
