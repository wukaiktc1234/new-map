package com.foodtraceability.exception;

/**
 * 业务异常类（**保留兼容层**）。
 *
 * <p><b>①（Lead 2026-10-30 批准，选项 A）</b>：本类改为**继承**
 * {@code com.foodtraceability.common.exception.BusinessException}。
 * 理由：仓库里曾有两个互不相关的同名类 ——
 * <ul>
 *   <li>{@code common.exception.BusinessException}：新代码用（81 处 import），带 Integer code / ErrorCode / httpStatus</li>
 *   <li>{@code exception.BusinessException}：旧代码用（30 处 import，219 个 (String) 构造 + 21 个 (int,String) 构造）</li>
 * </ul>
 * 二者无继承关系 → 凡 catch / 白名单只认新类的地方（如 PosShiftController 的透传、
 * 宽 catch 白名单），**旧类抛出的异常照样被吞成 500**（实测：DELETE /v1/roles/999999 → 500「角色不存在」）。</p>
 *
 * <p>改为继承后，30 处旧 import 一行不用改，所有按新类捕获/透传的代码自动覆盖旧类。</p>
 *
 * <p><b>兼容性说明</b>：父类 {@code getCode()} 返回 {@code Integer}，本类不再覆写（原返回 int 与之冲突）；
 * 原 {@code setCode(int)} 一并移除。若调用方把结果当 int 用，由自动拆箱兼容；空值时需调用方自行判空。</p>
 */
public class BusinessException extends com.foodtraceability.common.exception.BusinessException {
    private static final long serialVersionUID = 1L;

    public BusinessException(int code, String message) {
        super(code, message);
    }

    /** 无 code 的旧用法：默认 500（与原实现一致） */
    public BusinessException(String message) {
        super(500, message);
    }

    public BusinessException(String message, Throwable cause) {
        super(500, message);
        if (cause != null) {
            initCause(cause);
        }
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(code, message);
        if (cause != null) {
            initCause(cause);
        }
    }
}
