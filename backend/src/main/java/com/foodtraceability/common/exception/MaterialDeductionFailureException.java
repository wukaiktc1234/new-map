package com.foodtraceability.common.exception;

/**
 * KDS 出餐扣料失败异常（P0）。
 * <p>
 * 语义：本次扣料整体失败——主事务必须回滚，material_consumed 回到 0，
 * 且由独立 REQUIRES_NEW 事务写入 material_consumption FAILED 审计行。
 * <p>
 * 与既有显式失败（如 decreaseStock 的库存不足 BusinessException）的区别：
 * 本异常额外携带结构化上下文（订单/后厨单/托盘/BOM/已尝试/最终失败原料），
 * 用于失败审计行落库；原 decreaseStock 的 BusinessException 在本异常内以 cause 保留。
 */
public class MaterialDeductionFailureException extends RuntimeException {

    // ==================== 失败类型常量（写入 material_consumption.failure_type） ====================

    /** 后厨单不存在 */
    public static final String KITCHEN_ORDER_NOT_FOUND = "KITCHEN_ORDER_NOT_FOUND";
    /** 订单不存在 */
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    /** 订单无任何明细 */
    public static final String ORDER_ITEMS_EMPTY = "ORDER_ITEMS_EMPTY";
    /** 订单明细 productType 为空 */
    public static final String PRODUCT_TYPE_NULL = "PRODUCT_TYPE_NULL";
    /** 订单明细 productType 非法（非 1/2） */
    public static final String PRODUCT_TYPE_INVALID = "PRODUCT_TYPE_INVALID";
    /** 订单明细 foodId 为空（单品无菜品ID / 套餐无套餐ID） */
    public static final String ITEM_FOOD_ID_NULL = "ITEM_FOOD_ID_NULL";
    /** 菜品不存在 */
    public static final String FOOD_NOT_FOUND = "FOOD_NOT_FOUND";
    /** 套餐不存在 */
    public static final String COMBO_NOT_FOUND = "COMBO_NOT_FOUND";
    /** 套餐配料为空（BOM 未展开） */
    public static final String COMBO_INGREDIENTS_EMPTY = "COMBO_INGREDIENTS_EMPTY";
    /** 套餐配料 foodId 为空 */
    public static final String INGREDIENT_FOOD_ID_NULL = "INGREDIENT_FOOD_ID_NULL";
    /** 套餐配料 foodId 无法解析为 Long */
    public static final String INGREDIENT_FOOD_ID_PARSE_ERROR = "INGREDIENT_FOOD_ID_PARSE_ERROR";
    /** 套餐配料正数量经 int 换算为 0（数据/配置异常） */
    public static final String COMBO_INGREDIENT_QTY_TRUNCATED = "COMBO_INGREDIENT_QTY_TRUNCATED";
    /** 菜品无任何 BOM 配方 */
    public static final String FOOD_NO_RECIPE = "FOOD_NO_RECIPE";
    /** 配方 materialId 为空 */
    public static final String RECIPE_MATERIAL_ID_NULL = "RECIPE_MATERIAL_ID_NULL";
    /** 订单门店为空，无法定位库存 */
    public static final String ORDER_STORE_ID_NULL = "ORDER_STORE_ID_NULL";
    /** 门店库存记录不存在（decreaseStock NOT_FOUND 映射） */
    public static final String STORE_INVENTORY_NOT_FOUND = "STORE_INVENTORY_NOT_FOUND";
    /** 库存不足（decreaseStock INVENTORY_INSUFFICIENT 映射） */
    public static final String STOCK_INSUFFICIENT = "STOCK_INSUFFICIENT";
    /** 库存乐观锁冲突（decreaseStock INVENTORY_CONFLICT 映射） */
    public static final String STOCK_DEDUCTION_CONFLICT = "STOCK_DEDUCTION_CONFLICT";
    /** 扣减数量非法（decreaseStock PARAM_ERROR 映射） */
    public static final String DECREASE_STOCK_QUANTITY_INVALID = "DECREASE_STOCK_QUANTITY_INVALID";
    /** 扣减过程其他异常 */
    public static final String DECREASE_STOCK_ERROR = "DECREASE_STOCK_ERROR";

    private static final int CODE_NOT_FOUND = 404;
    private static final int CODE_INVENTORY_INSUFFICIENT = 8802;
    private static final int CODE_INVENTORY_CONFLICT = 8803;
    private static final int CODE_PARAM_ERROR = 400;

    /** 失败类型 */
    private final String failureType;
    /** 失败上下文 */
    private final MaterialDeductionContext context;

    public MaterialDeductionFailureException(String failureType,
                                             String message,
                                             MaterialDeductionContext context,
                                             Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
        this.context = context;
    }

    /**
     * 便捷构造（无 cause）。
     */
    public static MaterialDeductionFailureException of(MaterialDeductionContext ctx,
                                                       String failureType,
                                                       String message) {
        return new MaterialDeductionFailureException(failureType, message, ctx, null);
    }

    /**
     * 便捷构造（携带 cause）。
     */
    public static MaterialDeductionFailureException of(MaterialDeductionContext ctx,
                                                       String failureType,
                                                       String message,
                                                       Throwable cause) {
        return new MaterialDeductionFailureException(failureType, message, ctx, cause);
    }

    /**
     * 将 decreaseStock 抛出的 BusinessException 映射为扣料失败类型。
     * <p>decreaseStock 本身是"显式失败且当前事务回滚正确"，此处只做类型归类，不改变其语义。
     */
    public static String mapBusinessExceptionType(BusinessException e) {
        Integer code = e.getCode();
        if (code == null) {
            return DECREASE_STOCK_ERROR;
        }
        if (code == CODE_NOT_FOUND) {
            return STORE_INVENTORY_NOT_FOUND;
        }
        if (code == CODE_INVENTORY_INSUFFICIENT) {
            return STOCK_INSUFFICIENT;
        }
        if (code == CODE_INVENTORY_CONFLICT) {
            return STOCK_DEDUCTION_CONFLICT;
        }
        if (code == CODE_PARAM_ERROR) {
            return DECREASE_STOCK_QUANTITY_INVALID;
        }
        return DECREASE_STOCK_ERROR;
    }

    public String getFailureType() {
        return failureType;
    }

    public MaterialDeductionContext getContext() {
        return context;
    }
}
