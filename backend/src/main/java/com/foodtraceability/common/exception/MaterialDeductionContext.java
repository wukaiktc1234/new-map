package com.foodtraceability.common.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * KDS 出餐扣料上下文（P0：扣料失败审计载体）。
 * <p>
 * 贯穿 deductMaterialsForServe 全程，收集失败审计行所需的全部事实：
 * 订单/后厨单标识、托盘、BOM 展开结果、已尝试扣减清单、最终失败原料。
 * <p>
 * 规则：BOM 未成功展开前不得填充 bomMaterials/attemptedMaterials（禁止编造）；
 * 零数量（requiredQty==0 等）跳过项记入 notes/zeroQtyMaterials 供报告与日志使用。
 */
public class MaterialDeductionContext {

    /** 后厨单ID（kitchen_order.kitchen_order_id） */
    private String kitchenOrderId;
    /** 订单ID（orders.order_id） */
    private String orderId;
    /** 订单编号（orders.order_number） */
    private String orderNumber;
    /** 门店ID */
    private Long storeId;
    /** 门店名称 */
    private String storeName;
    /** 触发服务时的托盘码（可空） */
    private String trayCode;
    /** 调用来源引用 */
    private String sourceRef;

    /** 已展开 BOM 的原料清单（"materialId:qty"，按展开顺序；仅 BOM 展开成功后填充） */
    private final List<String> bomMaterials = new ArrayList<>();
    /** 已尝试扣减的原料清单（"materialId:qty"，含最终失败原料） */
    private final List<String> attemptedMaterials = new ArrayList<>();
    /** 零数量（PENDING_BUSINESS_DECISION）跳过的原料说明 */
    private final List<String> zeroQtyNotes = new ArrayList<>();
    /** 其他说明（已退款行、qty<=0 跳过等） */
    private final List<String> notes = new ArrayList<>();

    /** 最终导致失败的原料ID（未定位到具体原料时为 null） */
    private Long failedMaterialId;
    /** 最终失败原料名称或定位描述 */
    private String failedMaterialName;

    public void setKitchenOrderId(String kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void setTrayCode(String trayCode) {
        this.trayCode = trayCode;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public void setFailedMaterial(Long materialId, String materialName) {
        this.failedMaterialId = materialId;
        this.failedMaterialName = materialName;
    }

    public void addBomMaterial(Long materialId, java.math.BigDecimal qty, String materialName) {
        bomMaterials.add(materialId + ":" + qty.toPlainString() + (materialName != null ? "(" + materialName + ")" : ""));
    }

    public void addAttempted(Long materialId, java.math.BigDecimal qty, String materialName) {
        attemptedMaterials.add(materialId + ":" + qty.toPlainString() + (materialName != null ? "(" + materialName + ")" : ""));
    }

    public void addZeroQtyNote(String note) {
        zeroQtyNotes.add(note);
    }

    public void addNote(String note) {
        notes.add(note);
    }

    public String getKitchenOrderId() {
        return kitchenOrderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getTrayCode() {
        return trayCode;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public List<String> getBomMaterials() {
        return bomMaterials;
    }

    public List<String> getAttemptedMaterials() {
        return attemptedMaterials;
    }

    public List<String> getZeroQtyNotes() {
        return zeroQtyNotes;
    }

    public List<String> getNotes() {
        return notes;
    }

    public Long getFailedMaterialId() {
        return failedMaterialId;
    }

    public String getFailedMaterialName() {
        return failedMaterialName;
    }
}
