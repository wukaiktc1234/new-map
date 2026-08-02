package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.EmployeeLaborContractCreateDTO;
import com.foodtraceability.dto.EmployeeLaborContractUpdateDTO;
import com.foodtraceability.entity.EmployeeLaborContract;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.mapper.EmployeeLaborContractMapper;
import com.foodtraceability.service.EmployeeLaborContractService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 员工劳动合同服务实现
 *
 * 【法律合规】劳动合同前置签署流程
 * 入职流程：审批通过 → 创建合同 → 员工签署 → 生成邀请码 → 注册入职
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Service
public class EmployeeLaborContractServiceImpl extends ServiceImpl<EmployeeLaborContractMapper, EmployeeLaborContract> implements EmployeeLaborContractService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeLaborContractServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeLaborContract createContractFromArchive(OnboardingArchive archive) {
        if (archive == null) {
            log.error("创建劳动合同失败，档案为空");
            return null;
        }
        EmployeeLaborContract existingContract = baseMapper.selectByArchiveId(archive.getId());
        if (existingContract != null) {
            log.info("档案已存在劳动合同，档案ID：{}，合同ID：{}", archive.getId(), existingContract.getId());
            return existingContract;
        }
        EmployeeLaborContract contract = new EmployeeLaborContract();
        contract.setEmployeeCode(archive.getEmployeeCode());
        contract.setEmployeeName(archive.getCandidateName());
        contract.setContractNo(generateContractNo());
        contract.setContractType(EmployeeLaborContract.TYPE_FIXED_TERM);
        LocalDate startDate = archive.getOnboardDate() != null ? archive.getOnboardDate() : LocalDate.now();
        contract.setStartDate(startDate);
        contract.setEndDate(startDate.plusYears(3));
        contract.setProbationMonths(3);
        contract.setProbationEndDate(startDate.plusMonths(3));
        contract.setSalary(archive.getExpectedSalary());
        contract.setPosition(archive.getPosition());
        contract.setStatus(EmployeeLaborContract.STATUS_PENDING);
        contract.setSignMethod(EmployeeLaborContract.SIGN_ELECTRONIC);
        contract.setArchiveId(archive.getId());
        this.save(contract);
        log.info("创建劳动合同成功，合同编号：{}，档案ID：{}", contract.getContractNo(), archive.getId());
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeLaborContract createContract(EmployeeLaborContractCreateDTO createDTO) {
        if (createDTO == null) {
            log.error("手动创建劳动合同失败，参数为空");
            return null;
        }
        EmployeeLaborContract contract = new EmployeeLaborContract();
        contract.setEmployeeCode(createDTO.getEmployeeCode());
        contract.setEmployeeName(createDTO.getEmployeeName());
        contract.setEmployeeId(createDTO.getEmployeeId());
        contract.setContractNo(generateContractNo());
        contract.setContractType(createDTO.getContractType());
        contract.setStartDate(createDTO.getStartDate());
        contract.setEndDate(createDTO.getEndDate());
        contract.setProbationMonths(createDTO.getProbationMonths());
        // 试用期结束日期：开始日期 + 试用期月数
        if (createDTO.getProbationMonths() != null && createDTO.getStartDate() != null) {
            contract.setProbationEndDate(createDTO.getStartDate().plusMonths(createDTO.getProbationMonths()));
        }
        contract.setSalary(createDTO.getSalary());
        contract.setWorkLocation(createDTO.getWorkLocation());
        contract.setPosition(createDTO.getPosition());
        contract.setArchiveId(createDTO.getArchiveId());
        contract.setTemplateId(createDTO.getTemplateId());
        contract.setRemark(createDTO.getRemark());
        // 默认状态为草稿，签署方式默认电子签
        contract.setStatus(EmployeeLaborContract.STATUS_DRAFT);
        contract.setSignMethod(createDTO.getSignMethod() != null
                ? createDTO.getSignMethod()
                : EmployeeLaborContract.SIGN_ELECTRONIC);
        this.save(contract);
        log.info("手动创建劳动合同成功，合同编号：{}，员工姓名：{}", contract.getContractNo(), contract.getEmployeeName());
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeLaborContract updateContract(Long contractId, EmployeeLaborContractUpdateDTO updateDTO) {
        if (updateDTO == null) {
            log.error("更新劳动合同失败，参数为空");
            return null;
        }
        EmployeeLaborContract contract = this.getById(contractId);
        if (contract == null) {
            log.error("更新劳动合同失败，合同不存在：{}", contractId);
            return null;
        }
        // 仅更新非空字段，避免覆盖未传入的字段
        if (updateDTO.getEmployeeCode() != null) {
            contract.setEmployeeCode(updateDTO.getEmployeeCode());
        }
        if (updateDTO.getEmployeeName() != null) {
            contract.setEmployeeName(updateDTO.getEmployeeName());
        }
        if (updateDTO.getEmployeeId() != null) {
            contract.setEmployeeId(updateDTO.getEmployeeId());
        }
        if (updateDTO.getContractType() != null) {
            contract.setContractType(updateDTO.getContractType());
        }
        if (updateDTO.getStartDate() != null) {
            contract.setStartDate(updateDTO.getStartDate());
            // 同步更新试用期结束日期
            if (contract.getProbationMonths() != null) {
                contract.setProbationEndDate(updateDTO.getStartDate().plusMonths(contract.getProbationMonths()));
            }
        }
        if (updateDTO.getEndDate() != null) {
            contract.setEndDate(updateDTO.getEndDate());
        }
        if (updateDTO.getProbationMonths() != null) {
            contract.setProbationMonths(updateDTO.getProbationMonths());
            if (contract.getStartDate() != null) {
                contract.setProbationEndDate(contract.getStartDate().plusMonths(updateDTO.getProbationMonths()));
            }
        }
        if (updateDTO.getSalary() != null) {
            contract.setSalary(updateDTO.getSalary());
        }
        if (updateDTO.getWorkLocation() != null) {
            contract.setWorkLocation(updateDTO.getWorkLocation());
        }
        if (updateDTO.getPosition() != null) {
            contract.setPosition(updateDTO.getPosition());
        }
        if (updateDTO.getSignMethod() != null) {
            contract.setSignMethod(updateDTO.getSignMethod());
        }
        if (updateDTO.getArchiveId() != null) {
            contract.setArchiveId(updateDTO.getArchiveId());
        }
        if (updateDTO.getTemplateId() != null) {
            contract.setTemplateId(updateDTO.getTemplateId());
        }
        if (updateDTO.getRemark() != null) {
            contract.setRemark(updateDTO.getRemark());
        }
        boolean result = this.updateById(contract);
        if (!result) {
            log.error("更新劳动合同失败，合同ID：{}", contractId);
            return null;
        }
        log.info("更新劳动合同成功，合同ID：{}", contractId);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteContract(Long contractId) {
        EmployeeLaborContract contract = this.getById(contractId);
        if (contract == null) {
            log.error("删除劳动合同失败，合同不存在：{}", contractId);
            return false;
        }
        // 逻辑删除（@TableLogic 注解会自动处理 deleted 字段）
        boolean result = this.removeById(contractId);
        if (result) {
            log.info("删除劳动合同成功，合同ID：{}", contractId);
        } else {
            log.error("删除劳动合同失败，合同ID：{}", contractId);
        }
        return result;
    }

    @Override
    public EmployeeLaborContract getByArchiveId(Long archiveId) {
        return baseMapper.selectByArchiveId(archiveId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean signContract(Long contractId, String signDate) {
        EmployeeLaborContract contract = this.getById(contractId);
        if (contract == null) {
            log.error("签署合同失败，合同不存在：{}", contractId);
            return false;
        }
        if (!EmployeeLaborContract.STATUS_PENDING.equals(contract.getStatus())) {
            log.error("签署合同失败，合同状态不正确：{}", contract.getStatus());
            return false;
        }
        LocalDate signDateParsed = signDate != null ? LocalDate.parse(signDate) : LocalDate.now();
        contract.setSignDate(signDateParsed);
        contract.setStatus(EmployeeLaborContract.STATUS_SIGNED);
        boolean result = this.updateById(contract);
        if (result) {
            log.info("合同签署成功，合同ID：{}，签署日期：{}", contractId, signDateParsed);
        }
        return result;
    }

    @Override
    public List<EmployeeLaborContract> getPendingContracts() {
        return baseMapper.selectPendingContracts();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean extendContract(Long contractId, int months) {
        EmployeeLaborContract contract = this.getById(contractId);
        if (contract == null) {
            return false;
        }
        if (contract.getEndDate() != null) {
            contract.setEndDate(contract.getEndDate().plusMonths(months));
        }
        return this.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean terminateContract(Long contractId, String reason) {
        EmployeeLaborContract contract = this.getById(contractId);
        if (contract == null) {
            return false;
        }
        contract.setStatus(EmployeeLaborContract.STATUS_TERMINATED);
        contract.setRemark(reason);
        return this.updateById(contract);
    }

    private String generateContractNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "LC" + dateStr + random;
    }
}
