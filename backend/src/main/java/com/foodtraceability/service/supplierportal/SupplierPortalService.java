package com.foodtraceability.service.supplierportal;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.entity.supplierportal.SupplierSignLink;

import java.util.Map;

/**
 * 供应商签署门户服务接口
 * 提供签署链接的管理端 CRUD 与供应商端 H5 签署流程能力
 *
 * <p>接口分组：
 * <ul>
 *   <li>管理端：getList / getById / createLink / sendLink / revokeLink</li>
 *   <li>供应商端 H5：getContractByToken / viewContract / verify / signByToken / rejectByToken</li>
 * </ul>
 */
public interface SupplierPortalService {

    /**
     * 查询签署链接列表（分页+筛选）
     * @param page 当前页码（从1开始）
     * @param size 每页条数
     * @param status 状态筛选（可空）
     * @param keyword 关键词（合同名称/供应商名称/合同编号，可空）
     * @return 分页结果
     */
    PageResult<SupplierSignLink> getList(Integer page, Integer size, String status, String keyword);

    /**
     * 根据链接ID获取签署链接详情
     * @param linkId 链接ID
     * @return 签署链接详情，不存在返回 null
     */
    SupplierSignLink getById(String linkId);

    /**
     * 生成签署链接
     * 业务逻辑：根据电子合同ID查询合同信息，生成唯一 token 与过期时间，初始状态为 pending
     * @param eContractId 电子合同ID
     * @param contactPhone 供应商联系人手机号
     * @param contactEmail 供应商联系人邮箱（可空）
     * @param expireDays 过期天数
     * @param createBy 创建人
     * @return 生成的签署链接
     */
    SupplierSignLink createLink(String eContractId, String contactPhone,
                                 String contactEmail, Integer expireDays, String createBy);

    /**
     * 发送签署链接（短信/邮件）
     * 业务规则：状态由 pending → sent，记录发送时间
     * @param linkId 链接ID
     * @param channel 发送渠道（sms/email/both）
     * @return 更新后的签署链接
     */
    SupplierSignLink sendLink(String linkId, String channel);

    /**
     * 作废签署链接
     * 业务规则：状态置为 expired，已签署的链接不允许作废
     * @param linkId 链接ID
     */
    void revokeLink(String linkId);

    /**
     * 供应商通过 token 获取合同信息
     * 业务规则：token 无效或链接已过期返回 null
     * @param token H5访问token
     * @return 签署链接信息，无效返回 null
     */
    SupplierSignLink getContractByToken(String token);

    /**
     * 供应商查看合同（更新状态为 viewed）
     * 业务规则：仅 pending/sent 状态可流转为 viewed，记录首次查看时间
     * @param token H5访问token
     * @return 更新后的签署链接
     */
    SupplierSignLink viewContract(String token);

    /**
     * 发送实名认证验证码
     * 业务规则：生成6位数字验证码，存入 Redis（10分钟过期），通过日志输出（暂未对接真实短信网关）
     * @param token H5访问token，用于校验链接有效性后再发送验证码
     * @param phone 接收验证码的手机号
     * @return 发送结果，包含 expireSeconds 等元信息
     */
    Map<String, Object> sendVerifyCode(String token, String phone);

    /**
     * 供应商实名认证
     * 业务规则：校验验证码（与 Redis 中存储的验证码匹配，校验后删除以保证一次性使用），生成脱敏后的认证信息
     * @param token H5访问token
     * @param realName 真实姓名
     * @param idCard 身份证号
     * @param phone 手机号
     * @param code 验证码
     * @return 实名认证信息（含脱敏字段）
     */
    Map<String, Object> verify(String token, String realName, String idCard,
                                String phone, String code);

    /**
     * 供应商确认签署
     * 业务规则：链接必须有效且未过期、未签署，签署后状态置为 signed 并记录签署时间
     * @param token H5访问token
     * @param verification 实名认证信息
     * @return 更新后的签署链接
     */
    SupplierSignLink signByToken(String token, Map<String, Object> verification);

    /**
     * 供应商拒绝签署
     * 业务规则：链接必须有效且未过期、未签署，拒绝后状态置为 rejected 并记录拒绝时间与原因
     * @param token H5访问token
     * @param reason 拒绝原因
     * @return 更新后的签署链接
     */
    SupplierSignLink rejectByToken(String token, String reason);
}
