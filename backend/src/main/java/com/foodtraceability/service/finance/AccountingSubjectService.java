package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.AccountingSubject;
import java.util.List;

/**
 * 会计科目Service接口
 */
public interface AccountingSubjectService extends IService<AccountingSubject> {

    /**
     * 创建会计科目
     * @param dto 创建DTO
     * @return 创建的科目
     */
    AccountingSubject create(AccountingSubjectCreateDTO dto);

    /**
     * 更新会计科目
     * @param dto 更新DTO
     * @return 是否成功
     */
    boolean update(AccountingSubjectUpdateDTO dto);

    /**
     * 获取科目树形结构（全部）
     * @return 科目树列表
     */
    List<AccountingSubjectVO> getSubjectTree();

    /**
     * 根据ID获取科目详情
     * @param subjectId 科目ID
     * @return 科目VO
     */
    AccountingSubjectVO getDetail(Long subjectId);

    /**
     * 分页查询科目列表
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<AccountingSubjectVO> getPage(AccountingSubjectQueryDTO query);

    /**
     * 切换科目启用/停用状态
     * @param subjectId 科目ID
     * @return 是否成功
     */
    boolean toggleStatus(Long subjectId);

    /**
     * 根据科目编码查询
     * @param code 科目编码
     * @return 科目实体
     */
    AccountingSubject getByCode(String code);

    /**
     * 获取所有叶子节点科目（用于凭证录入）
     * @return 叶子科目列表
     */
    List<AccountingSubject> getLeafSubjects();
}
