package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.AccountingSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 会计科目Mapper接口
 */
@Mapper
public interface AccountingSubjectMapper extends BaseMapper<AccountingSubject> {

    /**
     * 根据父科目ID查询子科目列表
     * @param parentId 父科目ID
     * @return 子科目列表
     */
    List<AccountingSubject> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据科目类型查询科目列表
     * @param subjectType 科目类型
     * @return 科目列表
     */
    List<AccountingSubject> selectBySubjectType(@Param("subjectType") Integer subjectType);

    /**
     * 模糊搜索科目（根据编码或名称）
     * @param keyword 搜索关键词
     * @return 科目列表
     */
    List<AccountingSubject> selectByKeyword(@Param("keyword") String keyword);

    /**
     * 查询一级科目（父ID为空）
     * @return 一级科目列表
     */
    List<AccountingSubject> selectFirstLevelSubjects();

    /**
     * 根据科目编码查询
     * @param subjectCode 科目编码
     * @return 科目信息
     */
    AccountingSubject selectByCode(@Param("subjectCode") String subjectCode);

    /**
     * 查询所有叶子科目（没有子科目的科目）
     * @return 叶子科目列表
     */
    List<AccountingSubject> selectLeafSubjects();

    /**
     * 乐观锁更新科目余额（Sprint 3.1 P0 T-021，ADR-005）
     *
     * <p>SQL: UPDATE accounting_subjects SET balance = balance + #{delta},
     * version = version + 1, update_time = CURRENT_TIMESTAMP
     * WHERE subject_id = #{subjectId} AND version = #{version} AND deleted = 0</p>
     *
     * <p>返回受影响行数：1=成功，0=乐观锁冲突（版本号不匹配）。</p>
     *
     * @param subjectId 科目ID
     * @param delta 余额变化量（正负皆可，单位：分）
     * @param version 当前版本号（乐观锁）
     * @return 受影响行数（1=成功，0=冲突）
     */
    @Update("UPDATE accounting_subjects SET balance = balance + #{delta}, " +
            "version = version + 1, update_time = CURRENT_TIMESTAMP " +
            "WHERE subject_id = #{subjectId} AND version = #{version} AND deleted = 0")
    int updateBalanceWithOptimisticLock(@Param("subjectId") Long subjectId,
                                        @Param("delta") Long delta,
                                        @Param("version") Integer version);
}
