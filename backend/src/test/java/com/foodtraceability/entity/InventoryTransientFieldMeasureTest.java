package com.foodtraceability.entity;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * C7 前置实测（design D4「阶段 3 前置核实项」）——**档案性证据，非单元测试**（Owner 2026-10-09：保留，随 C7 提交）。
 * MyBatis-Plus 3.5.5 对 {@link Inventory} transient 字段（storeId/warehouseId，:122-128）
 * 的实际处理：映射清单跳过 transient 字段 + LambdaQueryWrapper 解析即抛
 * {@code MybatisPlusException: can not find lambda cache}（实测结论，见 {@code c7pre-mvn-measure.log:52-54}）。
 *
 * <p>本测试只打印证据，不断言。已 {@link Disabled}：MP 行为已由 C7 修法（warehouse→location 解析 +
 * location_id 查询/写入）消化，保留本类作为行为档案；如需重跑取证，移除 @Disabled 后
 * {@code mvn test -Dtest=InventoryTransientFieldMeasureTest}。
 */
@Disabled("C7 档案性证据（2026-10-09 实测 landed，c7pre-mvn-measure.log）；非单元测试，重跑需先移除本注解")
class InventoryTransientFieldMeasureTest {

    @Test
    @DisplayName("MP transient 实测：TableInfo 列映射 + LambdaQueryWrapper 生成的 SQL 片段")
    void measureWarehouseIdTransient() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("inventory");
        TableInfo ti = TableInfoHelper.initTableInfo(assistant, Inventory.class);
        String mapped = ti.getFieldList().stream()
                .map(f -> f.getProperty() + "->" + f.getColumn())
                .reduce((a, b) -> a + "; " + b)
                .orElse("<empty>");
        System.out.println("MEASURE_MAPPED_FIELDS=" + mapped);

        try {
            LambdaQueryWrapper<Inventory> lw = new LambdaQueryWrapper<>();
            lw.eq(Inventory::getWarehouseId, 1L);
            System.out.println("MEASURE_LAMBDA_SQL=" + lw.getSqlSegment());
        } catch (Throwable t) {
            System.out.println("MEASURE_LAMBDA_EXCEPTION=" + t.getClass().getName() + ": " + t.getMessage());
        }

        try {
            LambdaQueryWrapper<Inventory> lw = new LambdaQueryWrapper<>();
            lw.eq(Inventory::getWarehouseId, 1L);
            System.out.println("MEASURE_LAMBDA_TARGET_SQL=" + lw.getTargetSql());
        } catch (Throwable t) {
            System.out.println("MEASURE_LAMBDA_TARGET_SQL_EXCEPTION=" + t.getClass().getName() + ": " + t.getMessage());
        }
    }
}
