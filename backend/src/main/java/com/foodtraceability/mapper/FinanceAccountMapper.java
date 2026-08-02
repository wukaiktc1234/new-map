package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FinanceAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FinanceAccountMapper extends BaseMapper<FinanceAccount> {
    
    List<FinanceAccount> selectByParentId(@Param("parentId") Long parentId);
    
    List<FinanceAccount> selectByType(@Param("accountType") String accountType);
    
    List<FinanceAccount> selectAllActive();
}
