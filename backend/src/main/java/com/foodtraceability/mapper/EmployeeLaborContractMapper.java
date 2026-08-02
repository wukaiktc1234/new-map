package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.EmployeeLaborContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工劳动合同Mapper
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Mapper
public interface EmployeeLaborContractMapper extends BaseMapper<EmployeeLaborContract> {

    @Select("SELECT * FROM employee_labor_contract WHERE archive_id = #{archiveId} AND deleted = 0")
    EmployeeLaborContract selectByArchiveId(@Param("archiveId") Long archiveId);

    @Select("SELECT * FROM employee_labor_contract WHERE employee_code = #{employeeCode} AND deleted = 0 ORDER BY create_time DESC LIMIT 1")
    EmployeeLaborContract selectLatestByEmployeeCode(@Param("employeeCode") String employeeCode);

    @Select("SELECT * FROM employee_labor_contract WHERE status = #{status} AND deleted = 0 ORDER BY create_time DESC")
    List<EmployeeLaborContract> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM employee_labor_contract WHERE status = 'pending' AND deleted = 0")
    List<EmployeeLaborContract> selectPendingContracts();

    @Select("SELECT COUNT(*) FROM employee_labor_contract WHERE status = #{status} AND deleted = 0")
    int countByStatus(@Param("status") String status);
}
