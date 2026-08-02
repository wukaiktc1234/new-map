package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.EmployeeLaborContractCreateDTO;
import com.foodtraceability.dto.EmployeeLaborContractUpdateDTO;
import com.foodtraceability.entity.EmployeeLaborContract;
import com.foodtraceability.entity.OnboardingArchive;

import java.util.List;

/**
 * 员工劳动合同服务接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
public interface EmployeeLaborContractService extends IService<EmployeeLaborContract> {

    /**
     * 根据档案创建劳动合同
     *
     * @param archive 入职档案
     * @return 创建的合同
     */
    EmployeeLaborContract createContractFromArchive(OnboardingArchive archive);

    /**
     * 手动创建劳动合同
     *
     * @param createDTO 合同创建信息
     * @return 创建的合同
     */
    EmployeeLaborContract createContract(EmployeeLaborContractCreateDTO createDTO);

    /**
     * 更新劳动合同信息
     *
     * @param contractId 合同ID
     * @param updateDTO 更新信息
     * @return 更新后的合同，不存在返回null
     */
    EmployeeLaborContract updateContract(Long contractId, EmployeeLaborContractUpdateDTO updateDTO);

    /**
     * 删除劳动合同（逻辑删除）
     *
     * @param contractId 合同ID
     * @return 是否成功
     */
    boolean deleteContract(Long contractId);

    /**
     * 根据档案ID获取合同
     *
     * @param archiveId 档案ID
     * @return 合同信息
     */
    EmployeeLaborContract getByArchiveId(Long archiveId);

    /**
     * 签署合同
     *
     * @param contractId 合同ID
     * @param signDate 签署日期
     * @return 是否成功
     */
    boolean signContract(Long contractId, String signDate);

    /**
     * 获取待签署合同列表
     *
     * @return 待签署合同列表
     */
    List<EmployeeLaborContract> getPendingContracts();

    /**
     * 延长合同期限
     *
     * @param contractId 合同ID
     * @param months 延长月数
     * @return 是否成功
     */
    boolean extendContract(Long contractId, int months);

    /**
     * 终止合同
     *
     * @param contractId 合同ID
     * @param reason 终止原因
     * @return 是否成功
     */
    boolean terminateContract(Long contractId, String reason);
}
