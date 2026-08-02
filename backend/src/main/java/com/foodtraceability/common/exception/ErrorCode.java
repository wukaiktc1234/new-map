package com.foodtraceability.common.exception;

/**
 * 错误码枚举类
 * 定义系统中的错误码和错误信息
 */
public enum ErrorCode {

    SUCCESS(200, "成功"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    
    PRODUCT_NOT_FOUND(1001, "产品不存在"),
    PRODUCT_CREATE_FAILED(1002, "产品创建失败"),
    PRODUCT_UPDATE_FAILED(1003, "产品更新失败"),
    PRODUCT_DELETE_FAILED(1004, "产品删除失败"),
    PRODUCT_STATUS_UPDATE_FAILED(1005, "产品状态更新失败"),
    PRODUCT_BATCH_IMPORT_FAILED(1006, "产品批量导入失败"),

    CATEGORY_NOT_FOUND(2001, "分类不存在"),
    CATEGORY_CREATE_FAILED(2002, "分类创建失败"),
    CATEGORY_UPDATE_FAILED(2003, "分类更新失败"),
    CATEGORY_DELETE_FAILED(2004, "分类删除失败"),

    TRACE_NOT_FOUND(3001, "溯源记录不存在"),
    TRACE_CREATE_FAILED(3002, "溯源记录创建失败"),
    TRACE_UPDATE_FAILED(3003, "溯源记录更新失败"),
    TRACE_DELETE_FAILED(3004, "溯源记录删除失败"),
    TRACE_STATUS_INVALID(3005, "追溯码状态非法"),
    TRACE_BATCH_NOT_FOUND(3006, "批次号不存在"),
    TRACE_SUPPLIER_NOT_FOUND(3007, "供应商不存在"),
    INSPECTION_NOT_FOUND(3008, "检验记录不存在"),
    RECALL_NOT_FOUND(3009, "召回记录不存在"),
    LABEL_TEMPLATE_NOT_FOUND(3010, "标签模板不存在"),
    QUALITY_STANDARD_NOT_FOUND(3011, "质量标准不存在"),
    TRACE_CHAIN_NODE_NOT_FOUND(3012, "追溯链节点不存在"),
    TRACE_DUPLICATE_STOCKIN(3013, "重复扫码入库"),
    TRACE_EXPIRED(3014, "追溯码已过期"),
    RECALL_CONFIRMATION_REQUIRED(3015, "召回操作需二次确认"),
    SCALE_LEVEL_NOT_SUPPORTED(3016, "当前规模级别不支持此功能"),

    /** 招聘名额相关 (3017-3099) */
    QUOTA_NOT_FOUND(3017, "招聘名额不存在"),
    QUOTA_STATUS_TRANSITION_INVALID(3018, "名额状态非法流转"),
    QUOTA_DUPLICATE_ISSUE(3019, "名额重复下发"),
    QUOTA_ADJUSTMENT_COUNT_INVALID(3020, "名额追加数量非法"),
    QUOTA_EXHAUSTED(3021, "名额已用完"),
    QUOTA_STATUS_UNAVAILABLE(3022, "名额状态不可用"),

    /** 招聘反馈相关 (3101-3199) */
    REQUIREMENT_STATUS_NOT_FEEDBACKABLE(3101, "当前状态不可反馈"),
    FEEDBACK_TYPE_INVALID(3102, "反馈类型非法"),
    REQUIREMENT_NOT_FOUND(3103, "招聘需求不存在"),
    STORE_EVALUATION_SCORE_OUT_OF_RANGE(3111, "面评评分超出范围"),
    INTERVIEW_NOT_FOUND(3112, "面试记录不存在"),
    REQUIREMENT_STATUS_NOT_RESUBMITTABLE(3121, "当前状态不可重新提交"),

    /** 录用 Offer 相关 (3201-3299) */
    OFFER_STATUS_NOT_SENDABLE(3201, "当前状态不可发送"),
    OFFER_STATUS_NOT_ACCEPTABLE(3202, "当前状态不可接受"),
    OFFER_STATUS_NOT_REJECTABLE(3203, "当前状态不可拒绝"),
    OFFER_STATUS_NOT_WITHDRAWABLE(3204, "当前状态不可撤回"),
    OFFER_STATUS_NOT_ONBOARDABLE(3205, "当前状态不可转入职"),

    /** 通用状态机校验失败 */
    INVALID_STATUS_TRANSITION(3999, "状态机校验失败"),

    AUTH_CAPTCHA_ERROR(4001, "验证码错误"),
    AUTH_LOGIN_FAILED(4002, "用户名或密码错误"),
    AUTH_USER_DISABLED(4003, "用户账号已禁用"),
    AUTH_PASSWORD_INCONSISTENT(4004, "两次输入的密码不一致"),
    AUTH_USERNAME_EXISTS(4005, "用户名已存在"),
    AUTH_EMAIL_EXISTS(4006, "邮箱已存在"),
    AUTH_TOKEN_EXPIRED(4007, "令牌已过期"),
    AUTH_TOKEN_INVALID(4008, "令牌无效"),
    AUTH_REGISTRATION_CODE_REQUIRED(4009, "注册码不能为空"),
    AUTH_REGISTRATION_CODE_INVALID(4010, "注册码无效或已过期"),
    AUTH_PHONE_CODE_REQUIRED(4011, "手机验证码不能为空"),
    AUTH_PHONE_CODE_INVALID(4012, "手机验证码无效或已过期"),
    AUTH_REGISTRATION_CODE_EXPIRED(4013, "注册码已过期"),
    
    USER_NOT_FOUND(5001, "用户不存在"),
    USER_OLD_PASSWORD_ERROR(5002, "旧密码错误"),
    
    DEVICE_NOT_FOUND(6001, "设备不存在"),
    DEVICE_CONFIG_ERROR(6002, "设备配置错误"),
    DEVICE_CONNECTION_FAILED(6003, "设备连接失败"),
    DEVICE_OPERATION_FAILED(6004, "设备操作失败"),
    DEVICE_STATUS_CHECK_FAILED(6005, "设备状态检测失败"),
    DEVICE_VERSION_INCOMPATIBLE(6006, "设备版本不兼容"),
    DEVICE_PERMISSION_DENIED(6007, "设备操作权限不足"),
    DEVICE_BATCH_OPERATION_FAILED(6008, "设备批量操作失败"),
    
    OPERATION_NOT_ALLOWED(7001, "操作不允许"),
    
    PURCHASE_ORDER_NOT_FOUND(8001, "采购订单不存在"),
    PURCHASE_ORDER_ITEMS_EMPTY(8002, "商品明细不能为空"),
    PURCHASE_ORDER_STATUS_ERROR(8003, "订单状态错误"),
    PURCHASE_ORDER_ALREADY_EXISTS(8004, "采购订单已存在"),
    
    PURCHASE_REQUEST_NOT_FOUND(8101, "采购申请不存在"),
    PURCHASE_REQUEST_ITEMS_EMPTY(8102, "申请明细不能为空"),
    PURCHASE_REQUEST_STATUS_ERROR(8103, "申请状态错误"),
    PURCHASE_REQUEST_CANNOT_MODIFY(8104, "只有草稿或已拒绝状态的申请才能修改"),
    PURCHASE_REQUEST_CANNOT_DELETE(8105, "只有草稿状态的申请才能删除"),
    PURCHASE_REQUEST_CANNOT_SUBMIT(8106, "只有草稿或已拒绝状态的申请才能提交"),
    PURCHASE_REQUEST_CANNOT_APPROVE(8107, "只有待审批状态的申请才能审批"),
    PURCHASE_REQUEST_CANNOT_GENERATE_ORDER(8108, "只有已审批的采购申请才能生成订单"),

    /** 采购入库相关 (8200-8299) */
    PURCHASE_STOCKIN_NOT_FOUND(8201, "入库单不存在"),
    PURCHASE_STOCKIN_ITEMS_EMPTY(8202, "入库明细不能为空"),
    PURCHASE_STOCKIN_STATUS_ERROR(8203, "入库单状态错误，当前操作不允许"),
    PURCHASE_STOCKIN_ORDER_NOT_APPROVED(8204, "关联的采购订单未审核通过，无法创建入库单"),
    PURCHASE_STOCKIN_QC_REQUIRED(8205, "请先完成质检操作后再确认入库"),
    PURCHASE_STOCKIN_QC_FAILED(8206, "质检不合格的入库单不允许入库"),

    /** 供应商评估相关 (8300-8399) */
    SUPPLIER_EVALUATION_DUPLICATE(8301, "该供应商在指定周期已有评估记录"),
    SUPPLIER_EVALUATION_NO_DATA(8302, "该供应商在指定周期内无订单数据，无法自动计算"),

    /** 物资需求提报相关 (8400-8499) */
    MATERIAL_REQUEST_NOT_FOUND(8401, "物资需求提报不存在"),
    MATERIAL_REQUEST_ITEMS_EMPTY(8402, "需求明细不能为空"),
    MATERIAL_REQUEST_STATUS_ERROR(8403, "提报状态错误，当前操作不允许"),
    MATERIAL_REQUEST_CANNOT_MODIFY(8404, "只有草稿状态的提报才能修改"),
    MATERIAL_REQUEST_CANNOT_DELETE(8405, "只有草稿状态的提报才能删除"),
    MATERIAL_REQUEST_CANNOT_SUBMIT(8406, "只有草稿状态的提报才能提交审核"),
    MATERIAL_REQUEST_CANNOT_APPROVE(8407, "只有待审核状态的提报才能审批"),
    MATERIAL_REQUEST_CANNOT_CONVERT(8408, "只有已审核状态的提报才能转为采购申请"),

    /** 库存相关 (8800-8899) */
    /** 财务凭证相关 (8500-8799) — 会计法合规专用错误码 */
    VOUCHER_HEADER_NOT_FOUND(8501, "财务凭证不存在"),
    VOUCHER_HEADER_ALREADY_APPROVED(8502, "凭证已审核，不可修改或删除，请使用红字凭证冲销"),
    VOUCHER_HEADER_CANNOT_DELETE(8503, "财务凭证禁止删除（违反会计法），请使用红字凭证冲销"),
    VOUCHER_HEADER_SOD_VIOLATION(8504, "制单人不能审核自己制作的凭证（违反职责分离原则）"),
    VOUCHER_HEADER_BALANCE_ERROR(8505, "凭证借贷不平，借方合计与贷方合计不一致"),
    VOUCHER_HEADER_NOT_APPROVED(8506, "凭证未审核，当前操作不允许"),
    VOUCHER_HEADER_ALREADY_REVERSED(8507, "凭证已被红冲，不允许重复冲销"),
    VOUCHER_HEADER_REASON_REQUIRED(8508, "红冲原因不能为空"),
    VOUCHER_HEADER_NUMBER_DUPLICATE(8509, "凭证号已存在，请重新生成"),

    INVENTORY_NOT_FOUND(8801, "库存记录不存在"),
    INVENTORY_INSUFFICIENT(8802, "库存不足"),
    INVENTORY_CONFLICT(8803, "库存操作并发冲突，请重试"),
    INVENTORY_LOCK_INSUFFICIENT(8804, "可用库存不足，无法锁定"),
    INVENTORY_LOCK_CONFLICT(8805, "库存锁定操作并发冲突，请重试"),

    VOUCHER_NOT_FOUND(9001, "电子凭证不存在"),
    VOUCHER_FILE_INVALID(9002, "电子凭证文件无效"),
    VOUCHER_PARSE_FAILED(9003, "电子凭证解析失败"),
    VOUCHER_VERIFY_FAILED(9004, "电子凭证验签失败"),
    VOUCHER_DUPLICATE(9005, "电子凭证重复"),
    
    INVOICE_VERIFY_LIMIT_EXCEEDED(9101, "发票验真次数超限"),
    INVOICE_VERIFY_SERVICE_UNAVAILABLE(9102, "发票验真服务不可用"),
    INVOICE_INFO_MISMATCH(9103, "发票信息不匹配"),
    INVOICE_ALREADY_VOIDED(9104, "发票已作废"),
    INVOICE_NOT_FOUND(9105, "发票信息不存在"),
    
    INTERNAL_ERROR(9999, "系统内部错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
