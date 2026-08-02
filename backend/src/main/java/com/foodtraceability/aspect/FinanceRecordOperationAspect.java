package com.foodtraceability.aspect;

import com.foodtraceability.dto.finance.FinanceRecordCreateDTO;
import com.foodtraceability.dto.finance.FinanceRecordUpdateDTO;
import com.foodtraceability.dto.finance.FinanceRecordVO;
import com.foodtraceability.entity.finance.FinanceRecordOperationLog;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.FinanceRecordOperationLogMapper;
import com.foodtraceability.service.AuthService;
import com.foodtraceability.security.utils.JwtUtils;
import com.foodtraceability.security.model.SecurityUser;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 财务收支记录操作日志AOP切面
 * 用于记录财务收支记录的新增、编辑、删除、审批操作
 *
 * <p>已迁移到新版 {@link com.foodtraceability.service.finance.FinanceRecordService}，
 * 拦截 create/update/approve/remove 等方法，按 DTO 类型获取记录信息。</p>
 *
 * @author example
 * @since 2025-12-07
 */
@Aspect
@Component
public class FinanceRecordOperationAspect {


    public FinanceRecordOperationAspect(FinanceRecordOperationLogMapper operationLogMapper, JwtUtils jwtUtils, AuthService authService) {
        this.operationLogMapper = operationLogMapper;
        this.jwtUtils = jwtUtils;
        this.authService = authService;
    }

    private final FinanceRecordOperationLogMapper operationLogMapper;

    private final JwtUtils jwtUtils;

    private final AuthService authService;

    /**
     * 定义切点：拦截新版 FinanceRecordService 的所有方法
     */
    @Pointcut("execution(* com.foodtraceability.service.finance.FinanceRecordService.*(..))")
    public void financeRecordOperationPointcut() {
        // 切点定义
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around("financeRecordOperationPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        HttpServletRequest request = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            request = attributes.getRequest();
        }

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        // 构建操作日志
        FinanceRecordOperationLog operationLog = new FinanceRecordOperationLog();
        operationLog.setOperationTime(LocalDateTime.now());
        if (request != null) {
            operationLog.setIpAddress(request.getRemoteAddr());
            operationLog.setBrowserInfo(request.getHeader("User-Agent"));
        } else {
            operationLog.setIpAddress("0.0.0.0");
            operationLog.setBrowserInfo("System");
        }

        // 根据方法名判断操作类型（适配新版 Service 方法签名）
        String operationType = "";
        String operationContent = "";
        Long recordId = null;

        if ("create".equals(methodName)) {
            // 新增：create(FinanceRecordCreateDTO)
            operationType = "ADD";
            operationContent = "新增财务收支记录";
            if (args.length > 0 && args[0] instanceof FinanceRecordCreateDTO dto) {
                operationContent += ": " + dto.getRemark();
            }
        } else if ("update".equals(methodName)) {
            // 编辑：update(FinanceRecordUpdateDTO)
            operationType = "EDIT";
            operationContent = "编辑财务收支记录";
            if (args.length > 0 && args[0] instanceof FinanceRecordUpdateDTO dto) {
                recordId = dto.getRecordId();
                operationContent += ": ID=" + recordId + ", " + dto.getRemark();
            }
        } else if ("approve".equals(methodName)) {
            // 审批：approve(Long recordId, boolean approved)
            operationType = "APPROVE";
            operationContent = "审批财务收支记录";
            if (args.length > 0 && args[0] instanceof Long) {
                recordId = (Long) args[0];
                operationContent += ": ID=" + recordId;
            }
        } else if (methodName.startsWith("remove") || methodName.startsWith("delete")) {
            // 删除：removeById(Long) / removeByIds(Collection) 等
            operationType = "DELETE";
            operationContent = "删除财务收支记录";
            if (args.length > 0 && args[0] instanceof Long) {
                recordId = (Long) args[0];
                operationContent += ": ID=" + recordId;
            }
        }

        operationLog.setOperationType(operationType);
        operationLog.setOperationContent(operationContent);
        operationLog.setRecordId(recordId);

        // 从请求头中获取JWT令牌，解析出操作人信息
        String token = null;
        if (request != null) {
            token = request.getHeader("Authorization");
        }
        Long operatorId = 1L;
        String operatorName = "系统管理员";

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                // 从JWT令牌中获取用户信息
                Optional<SecurityUser> userOptional = jwtUtils.getUserFromToken(token);
                if (userOptional.isPresent()) {
                    SecurityUser securityUser = userOptional.get();
                    // 根据用户名查询用户信息
                    User user = authService.getUserByUsername(securityUser.getUsername());
                    if (user != null) {
                        operatorId = Long.parseLong(securityUser.getUserId());
                        operatorName = user.getFullName();
                    }
                }
            } catch (Exception e) {
                // 解析失败时使用默认值
                e.printStackTrace();
            }
        }

        // 设置操作人信息
        operationLog.setOperatorId(operatorId);
        operationLog.setOperatorName(operatorName);

        // 执行目标方法
        Object result = joinPoint.proceed();

        // 如果是新增操作，从返回值 VO 获取记录ID
        if ("ADD".equals(operationType) && result instanceof FinanceRecordVO vo) {
            operationLog.setRecordId(vo.getRecordId());
        }

        // 保存操作日志（仅对识别出的操作类型记录）
        if (!operationType.isEmpty()) {
            operationLogMapper.insert(operationLog);
        }

        return result;
    }
}
