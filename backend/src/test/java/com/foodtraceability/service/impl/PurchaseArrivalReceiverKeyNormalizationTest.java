package com.foodtraceability.service.impl;

import com.foodtraceability.entity.PurchaseOrderItem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P1-PROCUREMENT-BLOCKERS-001（A2 修复补测）：收货地点分组键 plannedReceiverType 归一化。
 *
 * <p>被测逻辑位于 {@link PurchaseArrivalServiceImpl} 私有静态内部类 {@code ReceiverKey}
 * 的构造器（归一为 trim + toUpperCase，空值兜底 "STORE"；DB CHECK 仅允许 STORE/WAREHOUSE，
 * 历史数据可能存小写）。</p>
 *
 * <p>本测试通过反射实例化私有内部类，不依赖 Spring 上下文；只补测，不改业务代码。</p>
 */
class PurchaseArrivalReceiverKeyNormalizationTest {

    /**
     * 反射构造 ReceiverKey 并取其 receiverType 字段——等价于
     * {@code new ReceiverKey(item).receiverType}。
     */
    private String normalize(PurchaseOrderItem item) throws Exception {
        Class<?> receiverKeyClass = Class.forName(
                PurchaseArrivalServiceImpl.class.getName() + "$ReceiverKey");
        Constructor<?> ctor = receiverKeyClass.getDeclaredConstructor(PurchaseOrderItem.class);
        ctor.setAccessible(true);
        Object key = ctor.newInstance(item);
        Field field = receiverKeyClass.getDeclaredField("receiverType");
        field.setAccessible(true);
        return (String) field.get(key);
    }

    private static PurchaseOrderItem itemWith(String plannedReceiverType) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPlannedReceiverType(plannedReceiverType);
        return item;
    }

    @Test
    void lowercaseIsUpperCased() throws Exception {
        assertEquals("STORE", normalize(itemWith("store")));
    }

    @Test
    void surroundingWhitespaceIsTrimmed() throws Exception {
        assertEquals("STORE", normalize(itemWith(" STORE ")));
    }

    @Test
    void mixedCaseIsUpperCased() throws Exception {
        assertEquals("STORE", normalize(itemWith("Store")));
    }

    @Test
    void warehouseValuesAreUpperCased() throws Exception {
        assertEquals("WAREHOUSE", normalize(itemWith("warehouse")));
        assertEquals("WAREHOUSE", normalize(itemWith(" Warehouse ")));
    }

    @Test
    void blankOrNullFallsBackToStoreDefault() throws Exception {
        assertEquals("STORE", normalize(itemWith(null)));
        assertEquals("STORE", normalize(itemWith("")));
        assertEquals("STORE", normalize(itemWith("   ")));
    }
}
