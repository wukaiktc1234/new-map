package com.foodtraceability.service.impl.supplierportal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.entity.ElectronicContract;
import com.foodtraceability.entity.supplierportal.SupplierSignLink;
import com.foodtraceability.mapper.ElectronicContractMapper;
import com.foodtraceability.mapper.supplierportal.SupplierSignLinkMapper;
import com.foodtraceability.service.supplierportal.SupplierPortalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 供应商签署门户服务实现类
 * 实现签署链接的管理端 CRUD 与供应商端 H5 签署流程
 *
 * <p>核心业务规则：
 * <ul>
 *   <li>创建链接时根据电子合同ID查询合同信息，生成唯一 token，初始状态 pending</li>
 *   <li>发送链接：仅 pending 状态可发送，发送后状态变为 sent</li>
 *   <li>作废链接：已签署的链接不允许作废</li>
 *   <li>查看合同：仅 pending/sent 状态可流转为 viewed，记录首次查看时间</li>
 *   <li>签署/拒绝：链接必须有效且未过期、未签署</li>
 * </ul>
 *
 * <p>重构说明：
 * <ul>
 *   <li>已移除 Redis 依赖（RedisTemplate）</li>
 *   <li>验证码改为内存 ConcurrentHashMap 存储，带 TTL 机制</li>
 *   <li>保留所有业务逻辑和数据库操作</li>
 * </ul>
 */
@Service
public class SupplierPortalServiceImpl implements SupplierPortalService {

    private static final Logger log = LoggerFactory.getLogger(SupplierPortalServiceImpl.class);

    /** JSON 序列化器 */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Token 前缀 */
    private static final String TOKEN_PREFIX = "tk_";

    /** 验证码过期时间（分钟），10 分钟 */
    private static final long VERIFY_CODE_EXPIRE_MINUTES = 10L;

    /** 验证码长度 */
    private static final int VERIFY_CODE_LENGTH = 6;

    /** 用于生成验证码的安全随机数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 验证码缓存条目，存储验证码和过期时间
     */
    private static final class VerifyCodeEntry {
        final String code;
        final long expireAt;

        VerifyCodeEntry(String code) {
            this.code = code;
            this.expireAt = System.currentTimeMillis() + VERIFY_CODE_EXPIRE_MINUTES * 60 * 1000L;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    /** 验证码缓存：phone -> 验证码条目 */
    private final ConcurrentHashMap<String, VerifyCodeEntry> verifyCodeStore = new ConcurrentHashMap<>();

    private final SupplierSignLinkMapper supplierSignLinkMapper;
    private final ElectronicContractMapper electronicContractMapper;

    public SupplierPortalServiceImpl(SupplierSignLinkMapper supplierSignLinkMapper,
                                      ElectronicContractMapper electronicContractMapper) {
        this.supplierSignLinkMapper = supplierSignLinkMapper;
        this.electronicContractMapper = electronicContractMapper;
    }

    // ==================== 管理端 API ====================

    @Override
    public PageResult<SupplierSignLink> getList(Integer page, Integer size, String status, String keyword) {
        int current = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        LambdaQueryWrapper<SupplierSignLink> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(SupplierSignLink::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SupplierSignLink::getContractName, kw)
                    .or().like(SupplierSignLink::getSupplierName, kw)
                    .or().like(SupplierSignLink::getEContractNo, kw));
        }
        wrapper.orderByDesc(SupplierSignLink::getCreateTime);

        Page<SupplierSignLink> pageParam = new Page<>(current, pageSize);
        IPage<SupplierSignLink> result = supplierSignLinkMapper.selectPage(pageParam, wrapper);

        return new PageResult<>(result.getTotal(), result.getRecords(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public SupplierSignLink getById(String linkId) {
        if (linkId == null || linkId.trim().isEmpty()) {
            return null;
        }
        return supplierSignLinkMapper.selectById(linkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierSignLink createLink(String eContractId, String contactPhone,
                                        String contactEmail, Integer expireDays, String createBy) {
        if (eContractId == null || eContractId.trim().isEmpty()) {
            throw new RuntimeException("电子合同ID不能为空");
        }
        if (contactPhone == null || contactPhone.trim().isEmpty()) {
            throw new RuntimeException("联系人手机号不能为空");
        }
        int days = (expireDays == null || expireDays < 1) ? 7 : expireDays;

        SupplierSignLink link = new SupplierSignLink();
        link.setEContractId(eContractId);
        link.setContactPhone(contactPhone);
        link.setContactEmail(contactEmail);
        link.setStatus(SupplierSignLink.STATUS_PENDING);
        link.setToken(generateToken());
        link.setExpireTime(LocalDateTime.now().plusDays(days));
        link.setCreateBy(createBy);
        link.setCreateTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        link.setDeleted(0);

        // 尝试查询电子合同信息填充冗余字段
        fillContractInfo(link, eContractId);

        supplierSignLinkMapper.insert(link);
        log.info("创建签署链接: linkId={}, eContractId={}, token={}", link.getLinkId(), eContractId, link.getToken());
        return link;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierSignLink sendLink(String linkId, String channel) {
        SupplierSignLink link = requireLink(linkId);
        if (SupplierSignLink.STATUS_EXPIRED.equals(link.getStatus())) {
            throw new RuntimeException("链接已过期，无法发送");
        }
        if (SupplierSignLink.STATUS_SIGNED.equals(link.getStatus())) {
            throw new RuntimeException("合同已签署，无法重复发送");
        }
        // 仅 pending 状态可发送，已发送的允许重发（更新发送时间）
        link.setStatus(SupplierSignLink.STATUS_SENT);
        link.setSendTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());

        supplierSignLinkMapper.updateById(link);
        log.info("发送签署链接: linkId={}, channel={}", linkId, channel);
        return link;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeLink(String linkId) {
        SupplierSignLink link = requireLink(linkId);
        if (SupplierSignLink.STATUS_SIGNED.equals(link.getStatus())) {
            throw new RuntimeException("已签署的链接不允许作废");
        }
        link.setStatus(SupplierSignLink.STATUS_EXPIRED);
        link.setUpdateTime(LocalDateTime.now());

        supplierSignLinkMapper.updateById(link);
        log.info("作废签署链接: linkId={}", linkId);
    }

    // ==================== 供应商端 H5 API ====================

    @Override
    public SupplierSignLink getContractByToken(String token) {
        SupplierSignLink link = findByToken(token);
        if (link == null) {
            return null;
        }
        // 已过期的链接返回 null（不暴露给供应商端）
        if (SupplierSignLink.STATUS_EXPIRED.equals(link.getStatus())) {
            return null;
        }
        // 过期时间已到但状态未更新的，按过期处理
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())
                && !SupplierSignLink.STATUS_SIGNED.equals(link.getStatus())) {
            return null;
        }
        return link;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierSignLink viewContract(String token) {
        SupplierSignLink link = requireLinkByToken(token);
        ensureLinkEffective(link);

        // 仅 pending/sent 状态可流转为 viewed，记录首次查看时间
        if (SupplierSignLink.STATUS_PENDING.equals(link.getStatus())
                || SupplierSignLink.STATUS_SENT.equals(link.getStatus())) {
            link.setStatus(SupplierSignLink.STATUS_VIEWED);
            link.setViewTime(LocalDateTime.now());
            link.setUpdateTime(LocalDateTime.now());
            supplierSignLinkMapper.updateById(link);
            log.info("供应商查看合同: linkId={}", link.getLinkId());
        }
        return link;
    }

    @Override
    public Map<String, Object> sendVerifyCode(String token, String phone) {
        // 校验链接有效性后再发送验证码，避免对无效链接发送短信造成浪费
        SupplierSignLink link = requireLinkByToken(token);
        ensureLinkEffective(link);

        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("手机号不能为空");
        }

        String code = generateVerifyCode();
        verifyCodeStore.put(phone, new VerifyCodeEntry(code));

        // 暂未对接真实短信网关，通过日志输出验证码便于联调
        // TODO: 对接真实短信网关后替换为短信发送调用
        log.info("[供应商实名认证] 验证码已生成: phone={}, code={}", maskPhone(phone), code);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sent", true);
        result.put("phone", maskPhone(phone));
        result.put("expireSeconds", VERIFY_CODE_EXPIRE_MINUTES * 60);
        return result;
    }

    @Override
    public Map<String, Object> verify(String token, String realName, String idCard,
                                       String phone, String code) {
        SupplierSignLink link = requireLinkByToken(token);
        ensureLinkEffective(link);

        if (realName == null || realName.trim().isEmpty()) {
            throw new RuntimeException("真实姓名不能为空");
        }
        if (idCard == null || idCard.trim().isEmpty()) {
            throw new RuntimeException("身份证号不能为空");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("手机号不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new RuntimeException("验证码不能为空");
        }

        // 验证码校验：必须先调用 /h5/send-code 将验证码存入内存缓存
        VerifyCodeEntry entry = verifyCodeStore.get(phone);
        if (entry == null || entry.isExpired()) {
            // 清理过期验证码
            if (entry != null) {
                verifyCodeStore.remove(phone, entry);
            }
            throw new RuntimeException("验证码错误或已过期");
        }
        if (!code.trim().equals(entry.code)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        // 校验成功后立即删除验证码，保证一次性使用，防止验证码重放攻击
        verifyCodeStore.remove(phone, entry);

        // 构建脱敏后的认证信息
        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("verifyStatus", "verified");
        verification.put("realName", realName);
        verification.put("idCardMasked", maskIdCard(idCard));
        verification.put("phone", maskPhone(phone));
        verification.put("verifyTime", LocalDateTime.now().toString());
        verification.put("verifyMethod", "sms");
        return verification;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierSignLink signByToken(String token, Map<String, Object> verification) {
        SupplierSignLink link = requireLinkByToken(token);
        if (SupplierSignLink.STATUS_EXPIRED.equals(link.getStatus())) {
            throw new RuntimeException("链接已过期");
        }
        if (SupplierSignLink.STATUS_SIGNED.equals(link.getStatus())) {
            throw new RuntimeException("合同已签署");
        }
        if (SupplierSignLink.STATUS_REJECTED.equals(link.getStatus())) {
            throw new RuntimeException("合同已拒绝，无法签署");
        }
        // 过期时间校验
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("链接已过期");
        }

        link.setStatus(SupplierSignLink.STATUS_SIGNED);
        link.setSignTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        if (verification != null) {
            link.setVerification(toJson(verification));
        }

        supplierSignLinkMapper.updateById(link);
        log.info("供应商确认签署: linkId={}", link.getLinkId());
        return link;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierSignLink rejectByToken(String token, String reason) {
        SupplierSignLink link = requireLinkByToken(token);
        if (SupplierSignLink.STATUS_EXPIRED.equals(link.getStatus())) {
            throw new RuntimeException("链接已过期");
        }
        if (SupplierSignLink.STATUS_SIGNED.equals(link.getStatus())) {
            throw new RuntimeException("合同已签署，无法拒绝");
        }
        if (SupplierSignLink.STATUS_REJECTED.equals(link.getStatus())) {
            throw new RuntimeException("合同已拒绝");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("拒绝原因不能为空");
        }

        link.setStatus(SupplierSignLink.STATUS_REJECTED);
        link.setRejectTime(LocalDateTime.now());
        link.setRejectReason(reason);
        link.setUpdateTime(LocalDateTime.now());

        supplierSignLinkMapper.updateById(link);
        log.info("供应商拒绝签署: linkId={}", link.getLinkId());
        return link;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据 linkId 查询链接，不存在抛异常
     */
    private SupplierSignLink requireLink(String linkId) {
        if (linkId == null || linkId.trim().isEmpty()) {
            throw new RuntimeException("链接ID不能为空");
        }
        SupplierSignLink link = supplierSignLinkMapper.selectById(linkId);
        if (link == null) {
            throw new RuntimeException("链接不存在：" + linkId);
        }
        return link;
    }

    /**
     * 根据 token 查询链接，不存在抛异常
     */
    private SupplierSignLink requireLinkByToken(String token) {
        SupplierSignLink link = findByToken(token);
        if (link == null) {
            throw new RuntimeException("链接无效或已过期");
        }
        return link;
    }

    /**
     * 根据 token 查询链接（不抛异常）
     */
    private SupplierSignLink findByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<SupplierSignLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierSignLink::getToken, token);
        return supplierSignLinkMapper.selectOne(wrapper);
    }

    /**
     * 校验链接是否有效（未过期）
     */
    private void ensureLinkEffective(SupplierSignLink link) {
        if (SupplierSignLink.STATUS_EXPIRED.equals(link.getStatus())) {
            throw new RuntimeException("链接已过期");
        }
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())
                && !SupplierSignLink.STATUS_SIGNED.equals(link.getStatus())) {
            throw new RuntimeException("链接已过期");
        }
    }

    /**
     * 根据电子合同ID查询合同信息，填充链接的冗余字段
     * 查询失败时使用默认值，不影响链接创建
     */
    private void fillContractInfo(SupplierSignLink link, String eContractId) {
        try {
            Long contractId = Long.parseLong(eContractId);
            ElectronicContract contract = electronicContractMapper.selectById(contractId);
            if (contract != null) {
                link.setEContractNo(contract.getContractNo());
                link.setContractName(contract.getContractName());
                link.setSupplierId(contract.getSupplierId() == null ? null
                        : String.valueOf(contract.getSupplierId()));
                link.setSupplierName(contract.getSupplierName());
            } else {
                link.setEContractNo(eContractId);
                link.setContractName("电子合同");
            }
        } catch (NumberFormatException e) {
            // eContractId 非数字（如 mock 数据），使用默认值
            link.setEContractNo(eContractId);
            link.setContractName("电子合同");
        }
    }

    /**
     * 生成唯一的 H5 访问 token
     * 格式：tk_ + 32位UUID（去除横线）
     */
    private String generateToken() {
        return TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成6位数字验证码（使用 SecureRandom 避免可预测性）
     * 范围：100000 ~ 999999
     */
    private String generateVerifyCode() {
        int code = SECURE_RANDOM.nextInt((int) Math.pow(10, VERIFY_CODE_LENGTH));
        return String.format("%0" + VERIFY_CODE_LENGTH + "d", code);
    }

    /**
     * 身份证号脱敏：保留前4位和后4位
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 手机号脱敏：保留前3位和后4位
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 将 Map 序列化为 JSON 字符串
     */
    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("序列化 verification 失败: {}", e.getMessage());
            return null;
        }
    }
}
