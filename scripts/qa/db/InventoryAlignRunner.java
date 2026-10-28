import com.foodtraceability.service.InventoryService;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;

/**
 * 幽灵库存对齐执行器（Owner 2026-10-28 拍板 B 改进版）。
 *
 * 为什么需要它：6 组「有库存但无流水 / 库存与流水末值不一致」的历史脏数据，
 * 必须**经 InventoryService** 写调整流水把账做平，**禁止手写 SQL INSERT 流水**。
 * 本项目 Service 不是 Controller 暴露的（无 adjust 端点），故用无 Web 的 Spring 上下文直接调 Service。
 *
 * 用法：java -cp <classpath>:<this> InventoryAlignRunner <locationId> <materialId> <changeQty> <mode> [<pairQty>]
 *
 * ⚠⚠ **成对调整必须同事务**（Lead 2026-10-28 指出的隐患）：
 *   产品路径下 increase/decrease 都是「流水 + 库存」同改，无法只补流水。
 *   若用「+qty 再 −qty」把净库存还原，**两次调用若各自 commit，中间态库存（如 200 / 40）会真实外泄**，
 *   并发的 POS 扣料 / 库存查询会读到错误可用量。
 *   因此本类用 TransactionTemplate 把成对调整包在**同一个事务**里，中间态不对外可见。
 *
 *   mode=IN   → increaseStockAtLocation(..., movementTypeOverride="ADJUST")
 *   mode=OUT  → decreaseStockAtLocation(..., "ADJUST")   （负数调整走这条，qty 传正数）
 *   pairQty    → 若传入，则在同一事务内先执行 args[2] 的调整，再执行相反方向的 pairQty 调整（净库存还原）
 */
public final class InventoryAlignRunner {

    public static void main(String[] args) {
        Long locationId = Long.valueOf(args[0]);
        Long materialId = Long.valueOf(args[1]);
        BigDecimal qty = new BigDecimal(args[2]).abs();
        String mode = args[3];
        BigDecimal signedQty = new BigDecimal(args[2]);

        long pairQty = args.length > 4 ? Long.parseLong(args[4]) : 0L;

        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(
                com.foodtraceability.FoodTraceabilityApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.main.banner-mode=off", "--logging.level.root=WARN")) {
            InventoryService svc = ctx.getBean(InventoryService.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            String ref = "[系统修复] 历史无流水库存对齐 location=" + locationId + " material=" + materialId
                    + (pairQty > 0 ? "（成对调整，同事务，中间态不对外）" : "");

            // ★ 关键：成对调整放进同一个事务，中间态不外泄
            tx.executeWithoutResult(status -> {
                apply(svc, locationId, materialId, signedQty, qty, ref, mode);
                if (pairQty > 0) {
                    BigDecimal back = BigDecimal.valueOf(pairQty);
                    String opposite = signedQty.compareTo(BigDecimal.ZERO) > 0 ? "OUT" : "IN";
                    apply(svc, locationId, materialId, back.negate(), back, ref, opposite);
                }
            });
            System.out.println("ALIGN_OK location=" + locationId + " material=" + materialId
                    + " change=" + signedQty + " mode=" + mode + " pairQty=" + pairQty + " tx=single");
        } catch (Exception e) {
            System.out.println("ALIGN_FAIL " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void apply(InventoryService svc, Long locationId, Long materialId,
                              BigDecimal signedQty, BigDecimal absQty, String ref, String mode) {
        if (signedQty.compareTo(BigDecimal.ZERO) > 0 || "IN".equals(mode)) {
            svc.increaseStockAtLocation(locationId, materialId, null, absQty, null, null,
                    null, "OTHER", ref, "ADJUST");
        } else if (signedQty.compareTo(BigDecimal.ZERO) < 0 || "OUT".equals(mode)) {
            svc.decreaseStockAtLocation(locationId, materialId, absQty, "OTHER", ref, "ADJUST");
        }
    }

    private InventoryAlignRunner() {
    }
}
