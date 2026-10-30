package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodtraceability.entity.PosShift;
import com.foodtraceability.entity.User;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.PosShiftMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.utils.JwtUtils;
import com.foodtraceability.security.utils.RoleCodeNormalizer;
import com.foodtraceability.service.PosAuthService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * POS认证服务实现类
 * 实现收银端的登录认证和班次管理功能
 */
@Service
public class PosAuthServiceImpl implements PosAuthService {

    private static final Logger logger = LoggerFactory.getLogger(PosAuthServiceImpl.class);


    public PosAuthServiceImpl(PosShiftMapper posShiftMapper, UserMapper userMapper,
                              PasswordEncoder passwordEncoder, JwtUtils jwtUtils,
                              ObjectMapper objectMapper,
                              com.foodtraceability.mapper.OrderNewMapper orderNewMapper,
                                 com.foodtraceability.security.cache.UserPermissionCache userPermissionCache,
                                 com.foodtraceability.mapper.ExceptionOpsMapper exceptionOpsMapper) {
        this.posShiftMapper = posShiftMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
        this.orderNewMapper = orderNewMapper;
        this.userPermissionCache = userPermissionCache;
        this.exceptionOpsMapper = exceptionOpsMapper;
    }

    private final JwtUtils jwtUtils;

    private final ObjectMapper objectMapper;

    /** Token过期时间（小时） */
    private static final long TOKEN_EXPIRE_HOURS = 8;

    /** Token存储最大容量 */
    private static final int TOKEN_MAX_CAPACITY = 1000;

    /** Token存储（线程安全，生产环境应使用Redis） */
    private static final Map<String, Map<String, Object>> tokenStore = new ConcurrentHashMap<>();

    private final PosShiftMapper posShiftMapper;

    private final UserMapper userMapper;

    /** ② 门店归属推导用（pos_shifts 无 store_id，只能经订单窗取门店） */
    private final com.foodtraceability.mapper.OrderNewMapper orderNewMapper;

    private final PasswordEncoder passwordEncoder;

    /** /v1/pos/auth/me 的权限来源：与登录同源（UserPermissionCache），保证撤权即时生效。 */
    private final com.foodtraceability.security.cache.UserPermissionCache userPermissionCache;

    /** 交班闸：待处理异常单计数（Lead 2026-10-29） */
    private final com.foodtraceability.mapper.ExceptionOpsMapper exceptionOpsMapper;

    /**
     * POS 端允许登录的角色码（归一化后的小写码）。
     * 与 POS 控制器白名单对齐：ROLE_ADMIN / ROLE_STORE_MANAGER / ROLE_CASHIER / ROLE_POS_OPERATOR。
     * owner 由 admin 同级授权一并放行（超级管理员）。
     */
    /**
     * ② POS「上一班备用金」默认值：当前登录人所属门店的**最近一条已结束班次**。
     *
     * <p>门店归属推导（pos_shifts 无 store_id 列）：
     * 先用「当前登录人自己最近一条已结束班次」的时间窗内订单所属门店作为其门店；
     * 再在同门店中取 end_time 最大的一条已结束班次。</p>
     *
     * <p>无历史 / 找不到门店 → 返回 null（控制器回 code=0 data=null，不 404），并打 WARN。</p>
     */
    @Override
    public Map<String, Object> lastEndedShiftOfMyStore() {
        Long userId = com.foodtraceability.utils.SecurityUtils.getCurrentUserId();
        if (userId == null) {
            logger.warn("[METRIC] last_shift_no_current_user");
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.warn("[METRIC] last_shift_user_not_found userId={}", userId);
            return null;
        }
        String employeeId = user.getEmployeeCode() != null && !user.getEmployeeCode().isBlank()
                ? user.getEmployeeCode() : user.getUsername();
        // 1) 本人最近一条已结束班次
        List<PosShift> mine = posShiftMapper.selectList(new LambdaQueryWrapper<PosShift>()
                .eq(PosShift::getDeleted, 0)
                .eq(PosShift::getEmployeeId, employeeId)
                .in(PosShift::getStatus, Arrays.asList("CLOSED", "completed"))
                .isNotNull(PosShift::getEndTime)
                .orderByDesc(PosShift::getEndTime)
                .last("LIMIT 1"));
        if (mine == null || mine.isEmpty()) {
            logger.warn("[METRIC] last_shift_none employeeId={}", employeeId);
            return null;
        }
        Long myStore = storeOfShift(mine.get(0));
        if (myStore == null) {
            logger.warn("[METRIC] last_shift_store_unresolved employeeId={} shiftId={}",
                    employeeId, mine.get(0).getShiftId());
            return null;
        }
        // 2) 同门店最近一条已结束班次（扫最近 60 条，逐条按订单窗推导门店）
        List<PosShift> recent = posShiftMapper.selectList(new LambdaQueryWrapper<PosShift>()
                .eq(PosShift::getDeleted, 0)
                .in(PosShift::getStatus, Arrays.asList("CLOSED", "completed"))
                .isNotNull(PosShift::getEndTime)
                .orderByDesc(PosShift::getEndTime)
                .last("LIMIT 60"));
        PosShift picked = null;
        if (recent != null) {
            for (PosShift ps : recent) {
                Long store = storeOfShift(ps);
                if (store != null && store.equals(myStore)) {
                    picked = ps;
                    break;
                }
            }
        }
        if (picked == null) {
            logger.warn("[METRIC] last_shift_none_for_store storeId={}", myStore);
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("shiftId", picked.getShiftId());
        out.put("terminalId", picked.getTerminalId());
        out.put("openingCash", picked.getOpeningCash());
        out.put("startTime", picked.getStartTime());
        out.put("endTime", picked.getEndTime());
        logger.info("[METRIC] last_shift_ok storeId={} shiftId={} openingCash={}",
                myStore, picked.getShiftId(), picked.getOpeningCash());
        return out;
    }

    /** 班次所属门店：取其时间窗内订单的 store_id（取出现次数最多的一个）；无订单 → null */
    private Long storeOfShift(PosShift shift) {
        if (shift == null || shift.getStartTime() == null || shift.getEndTime() == null) {
            return null;
        }
        List<com.foodtraceability.entity.OrderNew> orders = orderNewMapper.selectList(
                new LambdaQueryWrapper<com.foodtraceability.entity.OrderNew>()
                        .eq(com.foodtraceability.entity.OrderNew::getDeleted, 0)
                        .ge(com.foodtraceability.entity.OrderNew::getCreateTime, shift.getStartTime())
                        .le(com.foodtraceability.entity.OrderNew::getCreateTime, shift.getEndTime())
                        .isNotNull(com.foodtraceability.entity.OrderNew::getStoreId)
                        .select(com.foodtraceability.entity.OrderNew::getStoreId));
        if (orders == null || orders.isEmpty()) {
            return null;
        }
        Map<Long, Integer> counts = new HashMap<>();
        for (com.foodtraceability.entity.OrderNew o : orders) {
            if (o.getStoreId() != null) {
                counts.merge(o.getStoreId(), 1, Integer::sum);
            }
        }
        return counts.isEmpty() ? null
                : counts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }


    /**
 * POS 终端基础权限集（**不可被 DB 撤销**，要收回必须改代码）。
 *
 * <p>每一条都写明「为什么必须无条件给」，**改之前先读理由**（Lead 2026-10-29 要求）。</p>
 *
 * <p><b>已迁出（改按角色授予，见迁移）</b>：
 * member:manage（V20261029_003）、pos:member:recharge / pos:member:consume / pos:shift:manage（V20261029_004）
 * —— 它们都是**写权限或涉资金/钱箱**，硬编码等于「任何能登 POS 的人都有，且后台撤不掉」。</p>
 */
private static final List<String> POS_BASE_PERMISSIONS = Collections.unmodifiableList(Arrays.asList(
        // 收银台存在的前提：没有这三码，POS 无法开单收款查单（撤了等于废掉收银台）
        "pos:order:create", "pos:order:pay", "pos:order:query",
        // 只读：查会员余额（不扣钱），充值前必须能查
        "pos:member:query",
        // 只读：读菜单/菜品列表，收银台首页渲染必需
        "pos:menu:query",
        // 只读：菜品图像识别（YOLO），不写任何数据
        "pos:yolo:recognize",
        // 只读三码：菜品/分类/套餐展示。若改为 DB 授，新店初始化漏配就会「菜品看不到」，
        //   而这是高频故障（历史上已发生过一次：POS 首页菜品 403）
        "product:food:view",
        "product:category:view",
        "product:combo:view"
));

    private static final Set<String> POS_ALLOWED_ROLE_CODES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "admin", "owner", "store_manager", "cashier", "pos_operator")));

    @Override
    public Map<String, Object> login(String employeeId, String password, String terminalId) {
        logger.info("POS收银端登录: employeeId={}, terminalId={}", employeeId, terminalId);

        // 1. 查询用户信息（通过employee_code或username查询）
        User user = findUserByEmployeeId(employeeId);
        if (user == null) {
            logger.warn("登录失败: 用户不存在 - {}", employeeId);
            throw new BusinessException(400, "员工号或密码错误");
        }

        // 2. 验证密码（支持BCrypt哈希和明文自动升级）
        boolean passwordMatched = false;
        String storedPassword = user.getPassword();
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            // BCrypt哈希密码，直接验证
            passwordMatched = passwordEncoder.matches(password, storedPassword);
        } else {
            // 明文密码兼容：验证成功后自动升级为BCrypt哈希
            passwordMatched = password.equals(storedPassword);
            if (passwordMatched) {
                String encodedPassword = passwordEncoder.encode(password);
                user.setPassword(encodedPassword);
                userMapper.updateById(user);
                logger.info("密码已自动升级为BCrypt哈希: employeeId={}", employeeId);
            }
        }
        if (!passwordMatched) {
            logger.warn("登录失败: 密码错误 - {}", employeeId);
            throw new BusinessException(400, "员工号或密码错误");
        }

        // 3. 检查用户状态
        // 修复：User.status 是 Integer 类型，不能用 String "0"/"inactive" 比较
        // （String.equals(非String) 永远返回 false，导致 POS 端永远判定为启用）
        // 统一采用 status != 1 表示禁用，与管理端保持一致
        if (user.getStatus() == null || user.getStatus() != 1) {
            logger.warn("登录失败: 用户已禁用 - {}, status={}", employeeId, user.getStatus());
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 4. 检查用户角色是否包含收银相关角色
        if (!hasPosRole(user)) {
            logger.warn("登录失败: 无收银权限 - {}", employeeId);
            throw new BusinessException(403, "无收银操作权限");
        }

        // 5. 生成JWT Token（与管理系统一致，确保通过 Spring Security 校验）
        SecurityUser securityUser = buildSecurityUser(user);
        String token = jwtUtils.generateToken(securityUser);

        // 6. 构建用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("employeeId", user.getEmployeeCode() != null ? user.getEmployeeCode() : user.getUsername());
        userInfo.put("employeeName", user.getFullName());
        userInfo.put("terminalId", terminalId != null ? terminalId : "");
        // 前端按钮级权限（退款等敏感操作）需要知道当前账号有哪些角色/权限码。
        // 这里只做"回显"：直接复用上面 buildSecurityUser 已经算好的结果（DB → roles / permissions），
        // 不新增任何授权判断，也不放宽任何限制。
        userInfo.put("roles", securityUser.getRoles() != null ? securityUser.getRoles() : new ArrayList<>());
        userInfo.put("permissions", securityUser.getPermissions() != null ? securityUser.getPermissions() : new ArrayList<>());
        // ★ ①（Lead 2026-10-29）：permissionSource 明确来源，避免「静默空权限」最难查。
        //   DB / CACHE / WILDCARD_ADMIN / FALLBACK_EMPTY
        java.util.List<String> permsForSource = securityUser.getPermissions() != null
                ? securityUser.getPermissions() : new ArrayList<>();
        java.util.List<String> rolesForSource = securityUser.getRoles() != null
                ? securityUser.getRoles() : new ArrayList<>();
        String permissionSource;
        if (permsForSource.contains("*")) {
            permissionSource = "WILDCARD_ADMIN";
        } else if (permsForSource.isEmpty()) {
            // ⚠ 对 POS **不可达**：buildSecurityUser 无条件并入 POS_BASE_PERMISSIONS（8 个 pos:* 码），
            //   非 admin 分支权限集永远非空。保留该分支是为「万一基础集被移除」时仍有显式标记 + metric，
            //   真正的 FALLBACK_EMPTY 风险在**管理端 JWT 回源路径**（没有基础集兜底），见 AuthServiceImpl/JwtUtils。
            permissionSource = "FALLBACK_EMPTY";
            // 静默给空权限 = 所有人 403，是最难查的一类；必须同时打 metric + WARN
            logger.warn("[METRIC] permission_assembly_failed userId={} username={} roles={} reason=FALLBACK_EMPTY（权限装配结果为空，该用户所有接口将 403）",
                    user.getId(), user.getUsername(), rolesForSource);
        } else {
            permissionSource = "DB";
        }
        userInfo.put("permissionSource", permissionSource);
        userInfo.put("cacheMode", userPermissionCache.cacheMode());

        // 7. 清理过期Token并检查容量（保留内存存储用于登出等场景）
        cleanExpiredTokens();
        if (tokenStore.size() >= TOKEN_MAX_CAPACITY) {
            evictOldestTokens();
        }

        // 8. 存储Token到内存（生产环境应使用Redis）
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", token);
        tokenData.put("userInfo", userInfo);
        tokenData.put("loginTime", LocalDateTime.now());
        tokenData.put("expireTime", LocalDateTime.now().plusHours(TOKEN_EXPIRE_HOURS));
        tokenStore.put(token, tokenData);

        // 9. 获取当前活跃班次
        PosShift currentShift = null;
        if (terminalId != null && !terminalId.isEmpty()) {
            currentShift = getCurrentShift(terminalId);
        }

        // 10. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        result.put("shiftInfo", currentShift != null ? buildShiftInfo(currentShift) : null);

        logger.info("POS登录成功: employeeId={}, token={}", employeeId, token.substring(0, 8) + "...");
        return result;
    }

    @Override
    public PosShift getCurrentShift(String terminalId) {
        if (terminalId == null || terminalId.isEmpty()) {
            return null;
        }
        return posShiftMapper.findActiveShiftByTerminalId(terminalId);
    }

    @Override
    public PosShift startShift(String terminalId, String employeeId, String employeeName,
                               String shiftType, BigDecimal openingCash) {
        logger.info("开始新班次: terminalId={}, employeeId={}, shiftType={}", terminalId, employeeId, shiftType);

        // 1. 参数校验
        if (terminalId == null || terminalId.isEmpty()) {
            throw new BusinessException(400, "终端ID不能为空");
        }
        if (employeeId == null || employeeId.isEmpty()) {
            throw new BusinessException(400, "员工ID不能为空");
        }

        // 2. 检查该终端是否已有活跃班次
        PosShift activeShift = posShiftMapper.findActiveShiftByTerminalId(terminalId);
        if (activeShift != null) {
            logger.warn("开始班次失败: 终端{}已有活跃班次{}", terminalId, activeShift.getShiftId());
            throw new BusinessException(409, "该终端已有进行中的班次，请先结束当前班次");
        }

        // 3. 创建新班次记录
        PosShift shift = new PosShift();
        shift.setTerminalId(terminalId);
        // ★★ 一.1 身份伪造修复（Lead 2026-10-29）：
        //    修前实测：vcashier01 带 admin 的 employeeId 开班 → code=0，pos_shifts 65→66，
        //    employee_id 落成 admin 的 2110437068670001200（可冒他人开班交班）。
        //    规则：**以 JWT 里的 userId 为准**；body.employeeId 只用于校验，不一致直接 403。
        Long jwtUserId = SecurityUtils.getCurrentUserId();
        if (jwtUserId == null) {
            throw new BusinessException(401, "登录状态无效，无法开班");
        }
        String selfKey = String.valueOf(jwtUserId);
        try {
            User self = userMapper.selectById(jwtUserId);
            if (self != null && self.getEmployeeCode() != null && !self.getEmployeeCode().isBlank()) {
                selfKey = self.getEmployeeCode();
            }
        } catch (Exception e) {
            logger.warn("开班：解析当前登录人标识失败，使用 userId={}", jwtUserId);
        }
        boolean samePerson = employeeId.equals(selfKey)
                || employeeId.equals(String.valueOf(jwtUserId))
                || employeeId.equals(SecurityUtils.getCurrentUsername());
        if (!samePerson) {
            logger.warn("开班被拒（身份伪造）：登录人={}(id={}) 试图以 employeeId={} 开班",
                    SecurityUtils.getCurrentUsername(), jwtUserId, employeeId);
            throw new BusinessException(403, "不得为他人开班");
        }
        // 写入时**以 JWT 解析出的标识为准**，不用 body 值
        employeeId = selfKey;
        shift.setEmployeeId(employeeId);
        shift.setEmployeeName(employeeName != null ? employeeName : "");
        shift.setShiftType(shiftType != null && !shiftType.isEmpty() ? shiftType : "day");
        shift.setStartTime(LocalDateTime.now());
        shift.setOpeningCash(openingCash != null ? openingCash : BigDecimal.ZERO);
        shift.setTotalOrders(0);
        shift.setTotalAmount(BigDecimal.ZERO);
        shift.setStatus("active");

        // 4. 保存到数据库
        posShiftMapper.insert(shift);

        logger.info("班次创建成功: shiftId={}, terminalId={}", shift.getShiftId(), terminalId);
        return shift;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> endShift(String terminalId, String remark) {
        logger.info("结束班次: terminalId={}", terminalId);

        // 1. 参数校验
        if (terminalId == null || terminalId.isEmpty()) {
            throw new BusinessException(400, "终端ID不能为空");
        }

        // 2. 查找当前活跃班次
        // ★ ③ 交班重写（Lead 2026-10-28）：WHERE employee_id = 当前登录人 AND terminal_id = 请求终端 AND status = 'active'
        //   SQL 原文（PosShiftMapper.findActiveShiftByEmployeeAndTerminal，全部 #{} 参数化、无字符串拼接）：
        //   SELECT * FROM pos_shifts WHERE employee_id = #{employeeId} AND terminal_id = #{terminalId}
        //     AND status = 'active' ORDER BY start_time DESC LIMIT 1
        //   修前只按 terminal 取「最近一个 active」→ 同 terminal 残留两个 active 时会把**别人**的班次关掉。
        // 当前登录人标识：POS 班次的 employee_id 存的是员工号；拿不到员工号时退回用户名（两者都是唯一标识）。
        // ★ 修正（2026-10-28）：pos_shifts.employee_id 存的是**员工号**（如 2110437068670001210），
        //   不是登录用户名；用用户名查 → 恒不命中 → 交班被误拒（实测 code=500 文案却是拒绝语）。
        //   这里按当前登录用户名回查 users.employee_code，取不到再退回用户名。
        // ★ 员工标识解析（Lead 2026-10-28）：pos_shifts.employee_id 在不同入口写入的**不是同一个值** ——
        //   POS 开班传的是前端给的 employeeId（可能是工号/用户ID字符串），有的班次写用户名。
        //   因此按**候选集**依次尝试：employee_code → username → user_id 字符串，命中即用；全不命中才拒绝。
        String username = SecurityUtils.getCurrentUsername();
        java.util.List<String> employeeKeys = new java.util.ArrayList<>();
        try {
            User me = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username).last("LIMIT 1"));
            if (me != null) {
                if (me.getEmployeeCode() != null && !me.getEmployeeCode().isBlank()) employeeKeys.add(me.getEmployeeCode());
                if (me.getId() != null) employeeKeys.add(String.valueOf(me.getId()));
            }
        } catch (Exception e) {
            logger.warn("交班：解析员工标识失败，仅用用户名 username={}, err={}", username, e.getMessage());
        }
        if (username != null && !username.isBlank()) employeeKeys.add(username);

        PosShift shift = null;
        String employeeKey = null;
        for (String key : employeeKeys) {
            shift = posShiftMapper.findActiveShiftByEmployeeAndTerminal(key, terminalId);
            if (shift != null) { employeeKey = key; break; }
        }
        if (shift == null) {
            // ★ 拒绝而不是退化（Lead 2026-10-28 ①）：**「查不到就按终端找最近 active」正是被修掉的漏洞本身** ——
            //   它会让「任何人关掉别人残留的 active 班次」。这里改为明确 400，并打印该终端所有 active 班次的归属，便于排查。
            String actives = String.valueOf(posShiftMapper.selectList(
                    new LambdaQueryWrapper<PosShift>()
                            .eq(PosShift::getTerminalId, terminalId)
                            .eq(PosShift::getStatus, "active"))
                    .stream().map(s2 -> s2.getEmployeeId() + "#" + s2.getShiftId()).toList());
            logger.warn("交班被拒：当前登录人 employee_id={} 在 terminal={} 无 active 班次；该终端 active 班次归属={}",
                    employeeKey, terminalId, actives);
            throw new BusinessException(400, "当前终端没有你的在班班次，无法交班");
        }

        // ★ ⑤ 交班闸（Lead 2026-10-29）：任何写操作前先查本班次待处理异常单
        long pendingExceptions = exceptionOpsMapper.countPendingExceptionOrders(shift.getShiftId());
        if (pendingExceptions > 0) {
            logger.warn("[METRIC] shift_end_blocked_pending_exceptions shiftId={} pendingExceptionCount={}",
                    shift.getShiftId(), pendingExceptions);
            throw new BusinessException(40902,
                    "本班次还有 " + pendingExceptions + " 笔待处理异常单，请先处理后再交班 [PENDING_EXCEPTIONS pendingExceptionCount="
                            + pendingExceptions + "]");
        }

        // 3. 统计该班次的订单数据
        Map<String, Object> orderStats = countShiftOrders(terminalId, shift.getStartTime(), LocalDateTime.now());

        // 4. 更新班次状态
        shift.setEndTime(LocalDateTime.now());
        shift.setStatus("completed");
        shift.setHandoverRemark(remark);
        
        // 更新订单统计（如果有数据的话）
        if (orderStats != null) {
            Integer totalOrders = orderStats.get("total_orders") != null ?
                    ((Number) orderStats.get("total_orders")).intValue() : 0;
            BigDecimal totalAmount = orderStats.get("total_amount") != null ?
                    new BigDecimal(orderStats.get("total_amount").toString()) : BigDecimal.ZERO;
            
            shift.setTotalOrders(totalOrders);
            shift.setTotalAmount(totalAmount);
        }

        // 5. 写 closing_cash（Lead 2026-10-28 ③ 第 2 条）：交班必须落「关班现金」，并带出「营收−退款=净额」三元组
        BigDecimal revenueYuan = shift.getTotalAmount() != null ? shift.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal refundYuan = orderStats != null && orderStats.get("refund_amount") != null
                ? new BigDecimal(orderStats.get("refund_amount").toString()) : BigDecimal.ZERO;
        BigDecimal netYuan = orderStats != null && orderStats.get("net_amount") != null
                ? new BigDecimal(orderStats.get("net_amount").toString())
                : revenueYuan.subtract(refundYuan);
        BigDecimal closingBefore = shift.getClosingCash();
        // ★★ 一.2 钱箱公式修复（Lead 2026-10-29）：
        //    修前：closing_cash = 净营收（含微信/支付宝/银行卡）→ 抽屉现金对不上。
        //    正确：closing_cash = opening_cash + **现金**收入 − **现金**退款（payment_method=1），
        //    非现金（微信 2 / 支付宝 3 / 银行卡 4 / 余额 5）**不进钱箱**。
        BigDecimal openingCash = shift.getOpeningCash() != null ? shift.getOpeningCash() : BigDecimal.ZERO;
        BigDecimal cashInYuan = BigDecimal.ZERO;
        BigDecimal cashRefundYuan = BigDecimal.ZERO;
        if (orderStats != null && orderStats.get("payment_breakdown") instanceof java.util.Map<?, ?> pb) {
            Object cashPart = pb.get("cash");
            if (cashPart != null) cashInYuan = new BigDecimal(cashPart.toString());
        }
        // 现金退款：按退款记录的原支付方式归集（cash 部分）
        if (orderStats != null && orderStats.get("cash_refund_amount") != null) {
            cashRefundYuan = new BigDecimal(orderStats.get("cash_refund_amount").toString());
        }
        BigDecimal closingCash = openingCash.add(cashInYuan).subtract(cashRefundYuan)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        shift.setClosingCash(closingCash);
        shift.setHandoverRemark(((remark == null ? "" : remark)
                + " [钱箱口径] 期初=" + openingCash + " + 现金收入=" + cashInYuan
                + " − 现金退款=" + cashRefundYuan + " = 钱箱=" + closingCash
                + " ｜ [营收口径] 营收=" + revenueYuan + " − 退款=" + refundYuan + " = 净额=" + netYuan).trim());
        logger.info("交班钱箱: shiftId={} 期初={} 现金收入={} 现金退款={} 钱箱={} ｜ 营收={} 退款={} 净额={}",
                shift.getShiftId(), openingCash, cashInYuan, cashRefundYuan, closingCash,
                revenueYuan, refundYuan, netYuan);

        // 6. 更新数据库
        posShiftMapper.updateById(shift);

        // 6. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("shiftInfo", buildShiftInfo(shift));
        result.put("orderStats", orderStats);

        logger.info("班次结束成功: shiftId={}, terminalId={}, totalOrders={}, totalAmount={}",
                shift.getShiftId(), terminalId, shift.getTotalOrders(), shift.getTotalAmount());

        return result;
    }

    @Override
    public Map<String, Object> getShiftSummary(String terminalId) {
        logger.debug("获取班次摘要: terminalId={}", terminalId);

        // 1. 获取当前活跃班次
        PosShift shift = getCurrentShift(terminalId);

        // 无活跃班次时不再返回空对象（前端会显示全 0，像"假数据"）：退化为「今日汇总」
        LocalDateTime start = shift != null ? shift.getStartTime() : LocalDate.now().atStartOfDay();
        Map<String, Object> orderStats = countShiftOrders(terminalId, start, LocalDateTime.now());

        Map<String, Object> summary = new HashMap<>();
        summary.put("shiftInfo", shift != null ? buildShiftInfo(shift) : null);
        summary.put("shiftActive", shift != null);
        summary.put("summaryStartTime", start);
        summary.put("orderStats", orderStats);
        summary.put("currentTime", LocalDateTime.now());
        return summary;
    }

    @Override
    public List<PosShift> getTodayShifts(String terminalId) {
        logger.debug("获取今日班次列表: terminalId={}", terminalId);
        if (terminalId == null || terminalId.isEmpty()) {
            return Collections.emptyList();
        }
        return posShiftMapper.findTodayShiftsByTerminalId(terminalId);
    }

    /**
     * 构建 SecurityUser（用于生成 JWT）
     */
    private SecurityUser buildSecurityUser(User user) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setUserId(String.valueOf(user.getId()));
        securityUser.setUsername(user.getUsername());
        securityUser.setPassword(user.getPassword());
        securityUser.setName(user.getFullName());
        securityUser.setEmail(user.getEmail());
        securityUser.setPhone(user.getPhone());
        securityUser.setStatus(user.getStatus());
        // P1-USER-LOCATION-001：POS 班次 token 同样携带归属位置（POS 无 refresh，到期重登）
        securityUser.setLocationId(user.getLocationId());
        securityUser.setMfaEnabled(false);
        securityUser.setMfaVerified(true);

        // 2026-10-15 修复：角色改用授权权威源 user_roles → roles.role_id_str（如 ROLE_STORE_MANAGER /
        // ROLE_CASHIER）。原实现读 users.roles（存的是数字角色 ID，如 ["9"] / ["4216"]），
        // 导致签发的 token 里角色变成 ROLE_9 / ROLE_4216，POS 控制器类级
        // hasAnyAuthority('ROLE_ADMIN','ROLE_STORE_MANAGER','ROLE_CASHIER','ROLE_POS_OPERATOR','*') 全部落空。
        List<String> roles = new ArrayList<>();
        try {
            List<String> dbRoles = userMapper.getUserRoles(user.getId());
            if (dbRoles != null) {
                for (String role : dbRoles) {
                    if (role != null && !role.isBlank()) {
                        roles.add(role.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("读取 user_roles 角色失败，回退 users.roles JSON: userId={}, error={}", user.getId(), e.getMessage());
        }
        // P1-AUTH-CHAIN-001：删除「user_roles 为空时回退 users.roles」的分支。
        // users.roles 是历史展示字段（数字角色ID），既不能转成 authority，也会成为提权后门；
        // user_roles 没授角色就是没角色（fail-closed）。
        //
        // 【全系统唯一的用户名特判】内置超级管理员账号 username=admin 保留硬编码 admin 角色：
        // 它必须能在 user_roles 尚未初始化（如初始化脚本失败）时登录后台救场。
        // 除本处外，任何地方都不得再按 username 授予角色。
        if ((roles.isEmpty() || roles.stream().noneMatch(r -> "admin".equalsIgnoreCase(r)))
                && "admin".equalsIgnoreCase(user.getUsername())) {
            roles = new ArrayList<>(Collections.singletonList("admin"));
        }
        securityUser.setRoles(roles);

        // VULN-02 修复：不再给所有 POS 用户通配符权限 "*"，遵循最小权限原则。
        // admin 用户保留 "*" 以兼容管理端 hasAuthority('*') 检查；
        // 非 admin 用户 = DB 真实权限（user_permissions + role_permissions） ∪ POS 终端基础集。
        //
        // 2026-10-15 修复：原实现给的是**硬编码清单**，完全不读 DB 授权，导致
        //   ① 菜品/分类/套餐读取所需的 product:food:view / product:category:view / product:combo:view
        //      一律缺失 → POS 首页「菜品无法查看」并返回
        //      GlobalExceptionHandler 的 403「权限不足，无法访问该资源」；
        //   ② 角色被当成权限塞进 authorities（"ROLE_CASHIER"/"ROLE_POS_OPERATOR" 字面量），
        //      而真实角色反而缺失。
        // 现有 POS 基础集只保留终端运行必需、且可能不在 DB 目录里的 pos:* 业务码 + 菜品只读三码。
        boolean isAdmin = roles.stream().anyMatch("admin"::equalsIgnoreCase)
                || (user.getUsername() != null && user.getUsername().equalsIgnoreCase("admin"));
        if (isAdmin) {
            securityUser.setPermissions(Collections.singletonList("*"));
        } else {
            LinkedHashSet<String> permissions = new LinkedHashSet<>();
            try {
                List<String> direct = userMapper.getUserPermissions(user.getId());
                if (direct != null) {
                    permissions.addAll(direct);
                }
                List<String> fromRoles = userMapper.getUserRolePermissions(user.getId());
                if (fromRoles != null) {
                    permissions.addAll(fromRoles);
                }
            } catch (Exception e) {
                logger.warn("[METRIC] permission_assembly_failed userId={} username={} reason=DB_ERROR error={}（已降级为 POS 基础集）",
                    user.getId(), user.getUsername(), e.getMessage());
            }
            // P0 fix (Lead 2026-10-30): 原先无条件并入 POS_BASE_PERMISSIONS = 代码层提权
            // 现改为仅当 DB 授权为空时兜底（避免「新店漏配导致收银台全瘫」的历史故障），
            // 正常情况一切权限来自 role_permissions / user_permissions。
            if (permissions.isEmpty()) {
                logger.warn("[SECURITY] permission_empty_fallback userId={} -> using POS_BASE_PERMISSIONS（请在 Admin 补齐角色授权）", user.getId());
                permissions.addAll(POS_BASE_PERMISSIONS);
            }
            securityUser.setPermissions(new ArrayList<>(permissions));
        }

        return securityUser;
    }

    /**
     * 根据员工ID查找用户
     * 支持通过employee_code或username查询
     */
    private User findUserByEmployeeId(String employeeId) {
        // 先尝试通过employee_code查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmployeeCode, employeeId)
               .eq(User::getDeleted, 0)
               .last("LIMIT 1");
        User user = userMapper.selectOne(wrapper);

        // 如果没找到，再通过username查询
        if (user == null) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, employeeId)
                   .eq(User::getDeleted, 0)
                   .last("LIMIT 1");
            user = userMapper.selectOne(wrapper);
        }

        return user;
    }

    /**
     * 检查用户是否有收银权限
     * <p>
     * 安全策略（VULN-03 修复）：默认拒绝（fail-closed），仅当用户角色明确包含
     * admin/cashier/pos/operator 等关键词时才放行。防止非授权用户登录 POS 终端。
     * </p>
     */
    private boolean hasPosRole(User user) {
        // 管理员拥有所有权限
        if (user.getUsername() != null && user.getUsername().equalsIgnoreCase("admin")) {
            return true;
        }

        // 2026-10-15 修复：原实现读 user.getRoles()（users.roles 列，存的是【数字角色 ID】，
        // 如 vtest01=["9"]、vcashier01=["4216"]），再对该字符串做 contains("cashier") 之类的匹配；
        // 数字 ID 里永远不含这些字母 → 除 admin 外所有账号（含真正的收银员）都会被拒并抛
        // 「无收银操作权限」。现改为读授权权威源：user_roles → roles.role_id_str（如 ROLE_CASHIER）。
        //
        // 放行集合与 POS 控制器既有白名单严格对齐：
        //   PosApiController / PosOrderController / PosCouponController / YoloProductRecognitionController
        //   = hasAnyAuthority('ROLE_ADMIN','ROLE_STORE_MANAGER','ROLE_CASHIER','ROLE_POS_OPERATOR','*')
        try {
            List<String> roleCodes = userMapper.getUserRoles(user.getId());
            if (roleCodes != null) {
                for (String roleCode : roleCodes) {
                    String normalized = RoleCodeNormalizer.normalize(roleCode);
                    if (normalized != null && POS_ALLOWED_ROLE_CODES.contains(normalized)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 查询异常按 fail-closed 处理，不放行
            logger.warn("POS 角色判定异常，按 fail-closed 拒绝: userId={}, error={}", user.getId(), e.getMessage());
        }

        // 安全默认：拒绝（fail-closed）。非收银角色用户禁止登录POS终端。
        // 如需开放更多角色，须同时改本集合与控制器白名单，避免两处口径不一致。
        return false;
    }

    /**
     * 统计指定时间范围内的订单数据
     */
    private Map<String, Object> countShiftOrders(String terminalId, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();
        try {
            Map<String, Object> raw = posShiftMapper.countOrdersByTimeRange(terminalId, startTime, endTime);
            long orders = raw != null && raw.get("total_orders") != null
                    ? ((Number) raw.get("total_orders")).longValue() : 0L;
            long amountCents = raw != null && raw.get("total_amount_cents") != null
                    ? ((Number) raw.get("total_amount_cents")).longValue() : 0L;

            stats.put("total_orders", orders);
            // 新表金额是「分」，对外统一「元」
            BigDecimal totalYuan = BigDecimal.valueOf(amountCents).movePointLeft(2);
            stats.put("total_amount", totalYuan);
        // ★ ③ 交班口径（Lead 2026-10-28）：**必须扣本班次已退款**，否则把退掉的钱算成营收。
        //   实测（修前）：订单A 36 元 + 订单B 18 元（B 已全额退款）→ 交班报 total_amount=54、closing_cash=54，
        //   而净额应为 54 − 18 = 36。raw 里已带 refund_cents / net_amount_cents（见 PosShiftMapper 统计 SQL）。
        long refundCents = raw != null && raw.get("refund_cents") != null
                ? ((Number) raw.get("refund_cents")).longValue() : 0L;
        long netCents = raw != null && raw.get("net_amount_cents") != null
                ? ((Number) raw.get("net_amount_cents")).longValue() : amountCents - refundCents;
        BigDecimal refundYuanCalc = BigDecimal.valueOf(refundCents).movePointLeft(2);
        BigDecimal netYuanCalc = BigDecimal.valueOf(netCents).movePointLeft(2);
        stats.put("refund_amount", refundYuanCalc);
        stats.put("net_amount", netYuanCalc);
        // 一.2：现金退款单列（钱箱只认现金口径；退款按 orders.payment_method=1 归集）
        long cashRefundCents = 0L;
        try {
            Map<String, Object> cr = posShiftMapper.sumCashRefund(startTime, endTime);
            if (cr != null && cr.get("cash_refund_cents") != null) {
                cashRefundCents = ((Number) cr.get("cash_refund_cents")).longValue();
            }
        } catch (Exception e) {
            logger.warn("交班：现金退款归集失败（按 0 处理）: {}", e.getMessage());
        }
        stats.put("cash_refund_amount", BigDecimal.valueOf(cashRefundCents).movePointLeft(2));
            stats.put("avg_amount", orders > 0
                    ? totalYuan.divide(BigDecimal.valueOf(orders), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            // 支付方式拆解：1现金 2微信 3支付宝 4银行卡 5余额
            Map<String, BigDecimal> breakdown = new java.util.LinkedHashMap<>();
            breakdown.put("cash", BigDecimal.ZERO);
            breakdown.put("wechat", BigDecimal.ZERO);
            breakdown.put("alipay", BigDecimal.ZERO);
            breakdown.put("card", BigDecimal.ZERO);
            breakdown.put("balance", BigDecimal.ZERO);
            List<Map<String, Object>> rows = posShiftMapper.sumPaymentBreakdown(startTime, endTime);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    int method = row.get("method") != null ? ((Number) row.get("method")).intValue() : 0;
                    long cents = row.get("amount_cents") != null ? ((Number) row.get("amount_cents")).longValue() : 0L;
                    String key = switch (method) {
                        case 1 -> "cash";
                        case 2 -> "wechat";
                        case 3 -> "alipay";
                        case 4 -> "card";
                        case 5 -> "balance";
                        default -> "other";
                    };
                    breakdown.merge(key, BigDecimal.valueOf(cents).movePointLeft(2), BigDecimal::add);
                }
            }
            // 兜底对账：历史/导入单可能 payment_status=2 却缺 order_payment_records
            // （活库实测 4 笔：2026-08-08 / 2026-09-09 测试夹具，共 3330 元）。
            // 若拆解合计小于总额，把差额落到 unrecorded，保证「各方式合计 == total_amount」恒成立，
            // 而不是让收银员看到一个对不上的交班表（2026-10-23 复核补）。
            BigDecimal breakdownSum = breakdown.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gap = totalYuan.subtract(breakdownSum);
            if (gap.compareTo(new BigDecimal("0.005")) > 0) {
                breakdown.put("unrecorded", gap.setScale(2, java.math.RoundingMode.HALF_UP));
            }
            stats.put("payment_breakdown", breakdown);
            return stats;
        } catch (Exception e) {
            logger.warn("统计订单数据失败: terminalId={}, error={}", terminalId, e.getMessage());
            stats.put("total_orders", 0);
            stats.put("total_amount", BigDecimal.ZERO);
            stats.put("avg_amount", BigDecimal.ZERO);
            stats.put("payment_breakdown", Map.of());
            return stats;
        }
    }

    /**
     * 构建班次信息Map（用于返回给前端）
     */
    private Map<String, Object> buildShiftInfo(PosShift shift) {
        Map<String, Object> info = new HashMap<>();
        info.put("shiftId", shift.getShiftId());
        info.put("terminalId", shift.getTerminalId());
        info.put("employeeId", shift.getEmployeeId());
        info.put("employeeName", shift.getEmployeeName());
        info.put("shiftType", shift.getShiftType());
        info.put("startTime", shift.getStartTime());
        info.put("endTime", shift.getEndTime());
        info.put("openingCash", shift.getOpeningCash());
        info.put("closingCash", shift.getClosingCash());
        info.put("totalOrders", shift.getTotalOrders());
        info.put("totalAmount", shift.getTotalAmount());
        info.put("status", shift.getStatus());
        info.put("handoverRemark", shift.getHandoverRemark());
        return info;
    }

    /**
     * 清理已过期的Token
     */
    private void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenStore.entrySet().removeIf(entry -> {
            LocalDateTime expireTime = (LocalDateTime) entry.getValue().get("expireTime");
            return expireTime != null && expireTime.isBefore(now);
        });
    }

    /**
     * 淘汰最旧的Token（容量不足时）
     */
    private void evictOldestTokens() {
        // 按登录时间排序，移除最旧的20%Token
        int removeCount = Math.max(1, TOKEN_MAX_CAPACITY / 5);
        tokenStore.entrySet().stream()
                .sorted(Comparator.comparing(e -> (LocalDateTime) e.getValue().get("loginTime")))
                .limit(removeCount)
                .map(Map.Entry::getKey)
                .forEach(tokenStore::remove);
        logger.info("Token存储容量已满，已清理{}个最旧Token", removeCount);
    }

    /**
     * GET /v1/pos/auth/me（Lead 2026-10-29，联调期解前端阻塞）。
     *
     * <p>前端已按要求拔除 localStorage 权限，刷新/新标签页会丢内存快照 → 只能重新登录。
     * 本接口让前端「刷新时调 /me 恢复能力快照」，**不必把权限写回本地存储**（安全与体验兼得）。</p>
     *
     * <p><b>权限来源必须与登录同源</b>：登录走 {@link #buildSecurityUser}（DB），
     * 这里走 {@code UserPermissionCache}（同样回源 DB，TTL 45s + 版本轮询），
     * 并**并入与登录相同的 POS 基础集**，保证同一时刻两边的 permissions 集合一致。</p>
     */
    @Override
    public Map<String, Object> getCurrentUserInfo(String terminalId) {
        Long userId = com.foodtraceability.utils.SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
        // ★（Lead 2026-10-30）：这里**不得**用「账号是否存在」类文案——那是账号枚举泄露（message 直接给答案）。
        //   这里没有密码校验（用户已持有效 token），所以用**中性文案**而不是「用户名或密码错误」：
        //   会话确实无效 → 仍是 401（真会话失效，与「业务 401 押后映射」不冲突）。
        logger.warn("[METRIC] pos_me_user_missing userId={}", userId);
        throw new BusinessException(401, "登录状态无效，请重新登录");
        }

        // 角色（与登录同源：user_roles → roles.role_id_str）
        List<String> roles = new ArrayList<>();
        try {
            List<String> dbRoles = userMapper.getUserRoles(user.getId());
            if (dbRoles != null) {
                for (String r : dbRoles) {
                    if (r != null && !r.isBlank()) {
                        roles.add(r.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[METRIC] permission_assembly_failed userId={} reason=ROLE_QUERY_FAILED error={}",
                    userId, e.getMessage());
        }
        if ((roles.isEmpty() || roles.stream().noneMatch(r -> "admin".equalsIgnoreCase(r)))
                && "admin".equalsIgnoreCase(user.getUsername())) {
            roles = new ArrayList<>(Collections.singletonList("admin"));
        }

        boolean isAdmin = roles.stream().anyMatch("admin"::equalsIgnoreCase)
                || (user.getUsername() != null && user.getUsername().equalsIgnoreCase("admin"));

        List<String> permissions;
        String permissionSource;
        if (isAdmin) {
            permissions = new ArrayList<>(Collections.singletonList("*"));
            permissionSource = "WILDCARD_ADMIN";
        } else {
            LinkedHashSet<String> set = new LinkedHashSet<>();
            try {
                // ★ 与登录的差异点：这里走**缓存**（撤权即时生效），登录走 DB 直查
                List<String> fromRoles = userPermissionCache.get(String.valueOf(user.getId()), uid -> {
                    try {
                        return userMapper.getUserRolePermissions(Long.valueOf(uid));
                    } catch (Exception e) {
                        // ★ ③（Lead 2026-10-29）：**内层 catch 必须自己打 metric** ——
                        //   外层 catch 永远收不到（异常已被这里吞掉），靠外层 = metric 永不触发。
                        logger.warn("[METRIC] permission_reload_failed userId={} exception={} error={}（loader 回源失败，本次按空权限处理）",
                                uid, e.getClass().getSimpleName(), e.getMessage());
                        logger.warn("[METRIC] permission_assembly_failed userId={} reason=CACHE_LOADER_FAILED error={}",
                                uid, e.getMessage());
                        return new ArrayList<>();
                    }
                });
                if (fromRoles != null) {
                    set.addAll(fromRoles);
                }
                List<String> direct = userMapper.getUserPermissions(user.getId());
                if (direct != null) {
                    set.addAll(direct);
                }
                permissionSource = "CACHE";
            } catch (Exception e) {
                logger.warn("[METRIC] permission_assembly_failed userId={} roles={} reason=CACHE_RELOAD_FAILED error={}",
                        userId, roles, e.getMessage());
                permissionSource = "FALLBACK_EMPTY";
            }
            // P0 fix (Lead 2026-10-30): 同上，仅在 DB 授权为空时兜底，不再无条件并入
            if (set.isEmpty()) {
                logger.warn("[SECURITY] permission_empty_fallback userId={} -> using POS_BASE_PERMISSIONS（请在 Admin 补齐角色授权）", userId);
                set.addAll(POS_BASE_PERMISSIONS);
            }
            permissions = new ArrayList<>(set);
            if (permissions.isEmpty()) {
                permissionSource = "FALLBACK_EMPTY";
                logger.warn("[METRIC] permission_assembly_failed userId={} roles={} reason=FALLBACK_EMPTY（/me 权限装配为空）",
                        userId, roles);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("employeeId", user.getEmployeeCode() != null && !user.getEmployeeCode().isBlank()
                ? user.getEmployeeCode() : user.getUsername());
        out.put("employeeName", user.getFullName());
        out.put("userId", String.valueOf(user.getId()));
        out.put("terminalId", terminalId != null ? terminalId : "");
        out.put("roles", roles);
        out.put("permissions", permissions);
        out.put("permissionSource", permissionSource);
        // ② 缓存模式：VERSION_POLLING（直接改库 ~1s 生效）/ TTL_ONLY（只能等 ≤45s TTL）
        out.put("cacheMode", userPermissionCache.cacheMode());
        return out;
    }

    /**
     * 交班面板统计：**与 endShift 同源**（同一个 countShiftOrders + 同一套钱箱公式）。
     * 字段严格对齐前端 ShiftStats（frontend-pos/src/api/shiftExceptions.ts:21-40）。
     */
    @Override
    public java.util.Map<String, Object> getShiftStatsForPanel(String terminalId) {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        PosShift shift = posShiftMapper.findActiveShiftByTerminalId(terminalId);
        if (shift == null) {
            return null;   // 前端约定：null = 无进行中班次（不 404）
        }
        java.time.LocalDateTime start = shift.getStartTime();
        java.util.Map<String, Object> stats = countShiftOrders(terminalId, start, java.time.LocalDateTime.now());

        java.math.BigDecimal cashIn = java.math.BigDecimal.ZERO;
        if (stats.get("payment_breakdown") instanceof java.util.Map<?, ?> pb && pb.get("cash") != null) {
            cashIn = new java.math.BigDecimal(pb.get("cash").toString());
        }
        java.math.BigDecimal cashRefund = stats.get("cash_refund_amount") != null
                ? new java.math.BigDecimal(stats.get("cash_refund_amount").toString()) : java.math.BigDecimal.ZERO;
        java.math.BigDecimal openingCash = shift.getOpeningCash() != null ? shift.getOpeningCash() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal netRevenue = stats.get("net_amount") != null
                ? new java.math.BigDecimal(stats.get("net_amount").toString()) : java.math.BigDecimal.ZERO;
        java.math.BigDecimal handoverCash = cashIn.subtract(cashRefund).setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal drawerExpected = openingCash.add(handoverCash).setScale(2, java.math.RoundingMode.HALF_UP);
        long pendingCount = exceptionOpsMapper.countPendingExceptionOrders(shift.getShiftId());

        out.put("cashIn", cashIn);
        out.put("cashRefund", cashRefund);
        out.put("handoverCash", handoverCash);
        out.put("drawerExpected", drawerExpected);
        out.put("openingCash", openingCash);
        out.put("pendingExceptionCount", pendingCount);
        // ②（Lead 2026-10-29）：**历史全部异常单数（含已处理与已过期）**，不代表待处理。
        //   与 pendingExceptionCount 并列返回，避免前端拿「订单明细里的异常单数」冒充待处理数
        //   （实测前端曾显示 38 笔 vs pendingExceptionCount=0，是两套口径）。
        //   前端徽标**只认 pendingExceptionCount**。
        out.put("exceptionOrderTotal", exceptionOpsMapper.countExceptionOrderTotal(shift.getShiftId()));
        out.put("netRevenue", netRevenue);
        out.put("matched", stats.get("total_orders"));
        out.put("refunded", stats.get("refund_amount"));
        out.put("generatedAt", java.time.LocalDateTime.now().toString());
        return out;
    }

    /**
     * 交班面板异常单列表。口径（Lead 2026-10-29 裁定②，写死）：
     * pending   = 本班次 && order_status=0 && payment_status&lt;&gt;2
     *             && cancel_reason&lt;&gt;'EXPIRED_AUTO' && NOT EXISTS(exception_ops)
     * processed = 本班次 && EXISTS(exception_ops)
     * **processed 必须按 exception_ops 判定** —— 不能用 order_status 推断（TTL 关单也会改成 3）。
     */
    @Override
    public java.util.List<java.util.Map<String, Object>> listShiftExceptions(String terminalId, String tab) {
        PosShift shift = posShiftMapper.findActiveShiftByTerminalId(terminalId);
        if (shift == null) {
            return java.util.Collections.emptyList();
        }
        boolean processed = "processed".equalsIgnoreCase(tab);
        String sql = processed
                ? "SELECT o.order_id, o.order_number, o.create_time, COALESCE(o.final_amount,o.total_amount,0) AS amount, "
                  + "o.payment_method, e.op_type, e.create_time AS handled_at, e.operator_id "
                  + "FROM orders o JOIN exception_ops e ON e.order_id=o.order_id AND e.deleted=0 "
                  + "WHERE o.shift_id=#{shiftId} AND o.deleted=0 ORDER BY e.create_time DESC LIMIT 200"
                : "SELECT o.order_id, o.order_number, o.create_time, COALESCE(o.final_amount,o.total_amount,0) AS amount, "
                  + "o.payment_method, NULL AS op_type, NULL AS handled_at, NULL AS operator_id "
                  + "FROM orders o WHERE o.shift_id=#{shiftId} AND o.deleted=0 "
                  + "  AND o.order_status=0 AND COALESCE(o.payment_status,0)<>2 "
                  + "  AND COALESCE(o.cancel_reason,'')<>'EXPIRED_AUTO' "
                  + "  AND NOT EXISTS (SELECT 1 FROM exception_ops e WHERE e.order_id=o.order_id AND e.deleted=0) "
                  + "ORDER BY o.create_time DESC LIMIT 200";
        java.util.List<java.util.Map<String, Object>> rows = posShiftMapper.selectExceptionRows(sql, shift.getShiftId());
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> r : rows) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("orderId", r.get("order_id"));
            m.put("orderNumber", r.get("order_number"));
            m.put("createTime", String.valueOf(r.get("create_time")));
            m.put("amount", r.get("amount"));
            m.put("paymentMethod", r.get("payment_method"));
            String opType = r.get("op_type") == null ? null : String.valueOf(r.get("op_type"));
            m.put("type", processed ? "other" : "unpaid");
            m.put("typeLabel", processed ? ("已处理:" + opType) : "未支付");
            if (processed && opType != null) {
                m.put("handledAs", "VOID".equalsIgnoreCase(opType) ? "voided" : "resolved");
                m.put("handledAt", String.valueOf(r.get("handled_at")));
                m.put("handledBy", r.get("operator_id"));
            }
            out.add(m);
        }
        return out;
    }
}

