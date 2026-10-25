package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * POS 支付方式 → 资金账户解析器（**一处实现，SALE 与 REFUND 两处复用**，P4-5 抽取）。
 *
 * <p>语义与抽取前完全一致（原 {@code PosSaleFinanceRecorder#resolveAccount}，行为未改）：</p>
 * <ol>
 *   <li>候选集：只取 {@code status=1 且 store_id = 订单门店} 的账户（**门店隔离，不用 NULL 全局账户、不跨店借用**）；</li>
 *   <li>关键字命中：现金 → 名称/银行名/备注含「现金」；微信 → 「微信」；支付宝 → 「支付宝」；</li>
 *   <li>类型命中：现金(1/5) → account_type=2；微信/支付宝/银行卡 → account_type=1；</li>
 *   <li>都没有 → 返回 null（上层跳过入账并记 ERROR，由人工补配账户）。</li>
 * </ol>
 */
@Component
public class PosCashAccountResolver {

    private static final Logger log = LoggerFactory.getLogger(PosCashAccountResolver.class);

    private final BankAccountMapper bankAccountMapper;

    public PosCashAccountResolver(BankAccountMapper bankAccountMapper) {
        this.bankAccountMapper = bankAccountMapper;
    }

    /** 解析结果（accountId 必非空；reason 供 metric 日志） */
    public record Resolved(Long accountId, String accountName, boolean matchedByKeyword, String reason) {
    }

    /**
     * 解析账户。
     *
     * @param storeId       订单门店（null → 直接返回 null 并记 ERROR）
     * @param paymentMethod POS 支付方式（1 现金 / 2 微信 / 3 支付宝 / 5 余额…）
     * @param sceneRef      日志用引用（orderId 或 orderNumber）
     * @param scene         场景标签（SALE / REFUND），仅用于日志区分
     */
    public Resolved resolve(Long storeId, Integer paymentMethod, String sceneRef, String scene) {
        int method = paymentMethod != null ? paymentMethod : 1;

        if (storeId == null) {
            log.error("[METRIC] pos_fund_flow_recorded result=no_account reason=order_store_missing "
                            + "scene={} ref={} paymentMethod={} storeId=null candidates=[]",
                    scene, sceneRef, method);
            return null;
        }

        List<BankAccount> candidates = bankAccountMapper.selectList(
                new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getStatus, 1)
                        .eq(BankAccount::getStoreId, storeId)
                        .orderByAsc(BankAccount::getAccountId));
        if (candidates == null || candidates.isEmpty()) {
            log.error("[METRIC] pos_fund_flow_recorded result=no_account reason=no_store_account "
                            + "scene={} ref={} storeId={} paymentMethod={} candidates=[] "
                            + "hint=执行 scripts/db-fix/2026-10-23-seed-store-cash-accounts.sql 补建门店现金账户",
                    scene, sceneRef, storeId, method);
            return null;
        }

        String keyword = switch (method) {
            case 2 -> "微信";
            case 3 -> "支付宝";
            case 1, 5 -> "现金";
            default -> null;
        };

        if (keyword != null) {
            for (BankAccount account : candidates) {
                if (containsKeyword(account, keyword)) {
                    return new Resolved(account.getAccountId(), account.getAccountName(), true, "keyword:" + keyword);
                }
            }
        }

        int preferType = (method == 1 || method == 5) ? 2 : 1;
        for (BankAccount account : candidates) {
            if (account.getAccountType() != null && account.getAccountType() == preferType) {
                return new Resolved(account.getAccountId(), account.getAccountName(), false, "accountType:" + preferType);
            }
        }

        log.error("[METRIC] pos_fund_flow_recorded result=no_account reason=no_matching_account_in_store "
                        + "scene={} ref={} storeId={} paymentMethod={} candidates={}",
                scene, sceneRef, storeId, method, describeCandidates(candidates));
        return null;
    }

    private String describeCandidates(List<BankAccount> candidates) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < candidates.size(); i++) {
            BankAccount a = candidates.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a.getAccountId()).append(':').append(a.getAccountName())
                    .append(":type=").append(a.getAccountType());
        }
        return sb.append(']').toString();
    }

    private boolean containsKeyword(BankAccount account, String keyword) {
        return contains(account.getAccountName(), keyword)
                || contains(account.getBankName(), keyword)
                || contains(account.getRemark(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }
}
