import com.foodtraceability.service.InventoryService;
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
 * 用法：java -cp <classpath>:<this> InventoryAlignRunner <locationId> <materialId> <changeQty> <mode>
 *   mode=IN   → increaseStockAtLocation(..., movementTypeOverride="ADJUST")
 *   mode=OUT  → decreaseStockAtLocation(..., "ADJUST")   （负数调整走这条，changeQty 传正数）
 */
public final class InventoryAlignRunner {

    public static void main(String[] args) {
        Long locationId = Long.valueOf(args[0]);
        Long materialId = Long.valueOf(args[1]);
        BigDecimal qty = new BigDecimal(args[2]).abs();
        String mode = args[3];
        BigDecimal signedQty = new BigDecimal(args[2]);

        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(
                com.foodtraceability.FoodTraceabilityApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.main.banner-mode=off", "--logging.level.root=WARN")) {
            InventoryService svc = ctx.getBean(InventoryService.class);
            String ref = "[系统修复] 历史无流水库存对齐 location=" + locationId + " material=" + materialId;
            if (signedQty.compareTo(BigDecimal.ZERO) > 0) {
                svc.increaseStockAtLocation(locationId, materialId, null, qty, null, null,
                        null, "OTHER", ref, "ADJUST");
            } else if (signedQty.compareTo(BigDecimal.ZERO) < 0) {
                svc.decreaseStockAtLocation(locationId, materialId, qty, "OTHER", ref, "ADJUST");
            } else {
                System.out.println("ALIGN_SKIP zero_change");
                return;
            }
            System.out.println("ALIGN_OK location=" + locationId + " material=" + materialId + " change=" + signedQty + " mode=" + mode);
        } catch (Exception e) {
            System.out.println("ALIGN_FAIL " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private InventoryAlignRunner() {
    }
}
